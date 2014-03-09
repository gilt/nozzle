package com.gilt.nozzle.core


import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import spray.can.Http
import akka.io.IO
import spray.http.{HttpHeader, HttpResponse, HttpRequest}
import spray.client.pipelining.sendReceive
import com.gilt.nozzle.core.DevInfo.DevInfoExtractor
import com.gilt.nozzle.core.TargetInfo.TargetInfoExtractor
import scala.concurrent.ExecutionContext.Implicits.global
import PolicyValidator._
import akka.event.LoggingAdapter
import scala.concurrent.Future
import java.net.InetAddress
import spray.http.HttpHeaders.RawHeader

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

  var origIp: Option[InetAddress] = None

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case c: Http.Connected =>
      origIp = Some(c.remoteAddress.getAddress)
      sender ! Http.Register(self)

    case request: HttpRequest => {
      val replyTo = sender

      // Process getting developer and target info from the request in parallel
      // and process them
      val futureInfoExtractor = devInfoExtractor(request)
      val futureTargetInfo = extractTargetInfo(request)

      val futureResponse = for {
        di <- futureInfoExtractor
        ti <- futureTargetInfo
        response <- handleInfos(request)((di, ti))
      } yield response

      futureResponse recover {
        case t: Exception => errorHandler(t, request, None, None)
      } onSuccess { case r => replyTo ! r }

    }

    case a => log.warning(a.toString)
  }

  private[this] def handleInfos(request: HttpRequest): ((DevInfo, Option[TargetInfo])) => Future[HttpResponse] = {

    case (devInfo, Some(targetInfo)) => handleForwardRequest(request, devInfo, targetInfo)
    case (_, None) => throw new NotFoundException(s"Rule not found to handle request for: ${request.uri}")

  }

  private[this] def handleForwardRequest(request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo): Future[HttpResponse] = {
    forwardRequest(addForwardedFromHeader(enrichRequest(request, devInfo, targetInfo))) map {
      response => enrichResponse(request, response, devInfo, targetInfo)
    } recover {
      case e: Exception => errorHandler(e, request, Some(devInfo), Some(targetInfo))
    }
  }

  protected[core] def addForwardedFromHeader(orig: HttpRequest) = {
    origIp.map(_.getHostAddress).map{ ip =>
      val xForwFor = "X-Forwarded-For"
      val xForwForLower = xForwFor.toLowerCase

      def xffFilter(h: HttpHeader) = h.lowercaseName == xForwForLower

      val restHeaders = orig.headers.filterNot(xffFilter)

      val xffHeaders = orig.headers.filter(xffFilter) match {
          case Nil => List(RawHeader(xForwFor, ip))
          case x :: Nil => List(RawHeader(x.name, s"${x.value}, ${ip}"))
          case xs => xs :+ RawHeader(xForwFor, ip)
      }
      orig.copy(headers = restHeaders ::: xffHeaders)
    }.getOrElse(orig)
  }
}