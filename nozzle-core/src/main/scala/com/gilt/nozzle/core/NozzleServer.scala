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

  def enrichRequest: RequestTransformer = noopRequestEnricher(system.log)

  def enrichResponse: ResponseTransformer = noopResponseEnricher(system.log)

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
                       validatePolicy: ValidatePolicy,
                       enrichRequest: RequestTransformer,
                       enrichResponse: ResponseTransformer,
                       errorHandler: ValidationFailureHandler,
                       forwardRequest: ForwardRequest
                       ) extends Actor with ActorLogging {

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case request: HttpRequest => {
      val replyTo = sender

      // Process getting developer and target info from the request in parallel
      // and process them
      val futureInfoExtractor = devInfoExtractor(request)
      val futureTargetInfo = extractTargetInfo(request)

      val f = for {
        di <- futureInfoExtractor
        ti <- futureTargetInfo
      } yield (di, ti)

      f map handleInfos(request, replyTo) recover {
        case t: Exception =>
          replyTo ! errorHandler(t, request, None, None)
      }
    }
    case a =>
      log.warning(a.toString)
  }

  private[this] def handleInfos(request: HttpRequest, replyTo: ActorRef): ((DevInfo, Option[TargetInfo])) => Unit = {

    case (devInfo, Some(targetInfo)) =>
      validatePolicy(request, devInfo, targetInfo) match {
        case Success(_) =>
          forwardRequest(enrichRequest(request, devInfo, targetInfo)).collect {
            case response => replyTo ! enrichResponse(request, response, devInfo, targetInfo)
          }.recover {
            case e: Exception => replyTo ! errorHandler(e, request, Some(devInfo), Some(targetInfo))
          }
        case Failure(e) =>
          replyTo ! errorHandler(e, request, Some(devInfo), Some(targetInfo))
      }
    case (_, None) => replyTo ! HttpResponse(404)

  }
}