package com.gilt.nozzle.core


import akka.actor._
import akka.util.Timeout
import akka.event.LoggingAdapter
import akka.io.IO
import spray.can.Http
import spray.http.{HttpResponsePart, HttpResponse, HttpRequest}
import spray.client.pipelining.sendReceive
import scala.concurrent.duration._
import scala.language.postfixOps
import com.gilt.nozzle.core.DevInfo.DevInfoExtractor
import com.gilt.nozzle.core.TargetInfo.TargetInfoExtractor
import scala.concurrent.ExecutionContext.Implicits.global
import PolicyValidator._

trait NozzleServer extends App {

  import defaults.config
  import DefaultHandlers._

  implicit def timeout: Timeout = 5.seconds

  implicit val system = ActorSystem()

  def extractDevInfo: DevInfoExtractor

  def extractTargetInfo: TargetInfoExtractor

  def policyValidator: ValidatePolicy

  def enrichRequest: RequestTransformer = noopRequestEnricher(system.log)

  def enrichResponse: ResponseTransformer = noopResponseEnricher(system.log)

  def errorHandler: ValidationFailureHandler = defaultErrorHandler

  def forwardRequest: ForwardRequest = sendReceive

  val props = Props(classOf[ConnectionHandler], extractDevInfo, extractTargetInfo, policyValidator, enrichRequest,
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
    val forwarded = request.copy(uri = targetInfo.uri.copy(
          query = request.uri.query, fragment = request.uri.fragment),
          headers = request.headers.filter(_.isNot("host"))
    )
    log.debug("Forwarding request {} to downstream server", forwarded.toString)
    forwarded
  }

  def noopResponseEnricher(log: LoggingAdapter)(request: HttpRequest, response: HttpResponsePart, devInfo: DevInfo, targetInfo: TargetInfo) = {
    log.debug("Response received from downstream server: {}", response)
    response
  }
}

class ConnectionHandler(
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
    case c: Http.Connected =>
      val ipAddress = c.remoteAddress.getAddress
      val requestHandler = context.actorOf(Props(classOf[RequestReceiver], devInfoExtractor,
        extractTargetInfo, validatePolicy, enrichRequest,
        enrichResponse, errorHandler, forwardRequest, ipAddress))
      sender ! Http.Register(requestHandler)
  }
}
