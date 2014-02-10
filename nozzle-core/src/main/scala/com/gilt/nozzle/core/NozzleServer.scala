package com.gilt.nozzle.core


import akka.actor._
import akka.util.Timeout
import akka.pattern.pipe
import scala.concurrent.duration._
import spray.can.Http
import akka.io.IO
import spray.http.{HttpResponse, HttpRequest}
import spray.client.pipelining.sendReceive
import com.gilt.nozzle.core.DevInfo.DevInfoExtractor
import com.gilt.nozzle.core.TargetInfo.TargetInfoExtractor
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import PolicyValidator._
import akka.event.LoggingAdapter
import spray.http.Uri.Path
import akka.event.Logging.LogLevel
import java.util.concurrent.TimeUnit

trait NozzleServer extends App {
  import defaults.config
  import DefaultHandlers._

  implicit val timeout: Timeout = 5 seconds

  implicit val system = ActorSystem()

  def extractDevInfo: DevInfoExtractor
  def extractTargetInfo: TargetInfoExtractor
  def policyValidator: ValidatePolicy
  def enrichRequest: RequestEnricher = noopRequestEnricher(system.log)
  def enrichResponse: ResponseEnricher = noopResponseEnricher(system.log)
  def errorHandler: ValidationFailureHandler = defaultErrorHandler
  def forwardRequest: ForwardRequest = sendReceive

  val props = Props(classOf[RequestReceiver], extractDevInfo, extractTargetInfo, policyValidator, enrichRequest,
                      enrichResponse, errorHandler, forwardRequest)

  val httpServer = system.actorOf(props, "nozzle-server")
  // create a new HttpServer using our handler and tell it where to bind to
  IO(Http) ! Http.Bind(
                httpServer,
                interface = config.getString("service.interface"),
                port = config.getInt("service.port")
             )
}

object DefaultHandlers {

  def noopRequestEnricher(log: LoggingAdapter)(request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo) = {
    //Get the Uri from targetInfo and remove the host header from the request
    val forwarded = request.copy(uri = targetInfo.uri, headers = request.headers.filter(_.isNot("host")))
    log.debug("Forwarding request {} to downstream server", forwarded.toString)
    forwarded
  }

  def noopResponseEnricher(log: LoggingAdapter)(request: HttpRequest, response: HttpResponse, devInfo: DevInfo, targetInfo: TargetInfo) = {
    log.debug("Response received from downstream server: {}", response)
    response
  }
}

class RequestReceiver(
             devInfoExtractor: DevInfoExtractor,
             extractTargetInfo: TargetInfoExtractor,
             policyValidator: ValidatePolicy,
             enrichRequest: RequestEnricher,
             enrichResponse: ResponseEnricher,
             errorHandler: ValidationFailureHandler,
             forwardRequest: ForwardRequest
  ) extends Actor with ActorLogging {

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case request: HttpRequest => {
      val validatorActor = context.actorOf(Props(
              classOf[PolicyValidatorActor], request, policyValidator, errorHandler, sender))

      // Process getting developer and target info from the request in parallel
      // and process them in the validatorActor
      devInfoExtractor(request) pipeTo validatorActor
      extractTargetInfo(request) pipeTo validatorActor
    }

    case ValidationMessage(request, devInfo, targetInfo, replyTo) => try {
      forwardRequest(enrichRequest(request, devInfo, targetInfo)).onComplete {
        case Success(response) =>
          replyTo ! enrichResponse(request, response, devInfo, targetInfo)
        case Failure(e) =>
          replyTo ! errorHandler(e, request, devInfo, targetInfo)
      }
    } catch {
      case e: Exception => log.warning(e.getLocalizedMessage)
      replyTo ! HttpResponse(500)
    }
    case a =>
      log.warning(a.toString)
  }
}

/**
 * Process asynchronous responses from the DevInfo and TargetInfo extractors, so that
 * when both have been received it will send back the validation message
 * @param request
 * @param validatePolicy
 * @param errorHandler
 * @param replyTo
 */
class PolicyValidatorActor(
                            request: HttpRequest,
                            validatePolicy: ValidatePolicy,
                            errorHandler: ValidationFailureHandler,
                            replyTo: ActorRef
                           ) extends Actor with ActorLogging {

  var devInfo: Option[DevInfo] = None
  var targetInfo: Option[TargetInfo] = None

  //Shutdown after 10 seconds
  context.system.scheduler.scheduleOnce(10 seconds) { self ! PoisonPill }

  def receive = {
    case d: DevInfo =>
      processMessageArrival(d)
    case t: TargetInfo =>
      processMessageArrival(t)
    case Some(a) =>
      processMessageArrival(a)
    case None =>
      replyTo ! HttpResponse(404)
    case any =>
      //This should never happen
      log.warning(s"Unknown message ${any}")
      replyTo ! HttpResponse(500)

  }

  private def processMessageArrival(a: Any) = {
    a match {
      case t: TargetInfo => targetInfo = Some(t)
      case d: DevInfo => devInfo = Some(d)
      case o =>
        log.warning("Unable to process message {}", o)
        replyTo ! HttpResponse(500)
    }
    if( targetInfo.isDefined && devInfo.isDefined) {
      validatePolicy(request, devInfo.get, targetInfo.get) match {
        case Success(_) =>
          sender ! ValidationMessage(request, devInfo.get, targetInfo.get, replyTo)
        case Failure(e) =>
          replyTo ! errorHandler(e, request, devInfo.get, targetInfo.get)
      }
    }
  }
}

