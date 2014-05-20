package com.gilt.nozzle.core

import com.gilt.nozzle.core.DevInfo._
import com.gilt.nozzle.core.TargetInfo._
import com.gilt.nozzle.core.PolicyValidator._
import akka.actor._
import java.net.InetAddress
import spray.http._
import spray.can.Http.ConnectionClosed
import akka.event.Logging
import spray.http.HttpRequest
import spray.http.HttpHeaders.RawHeader
import scala.Some
import spray.can.Http
import akka.io.IO

class RequestReceiver(
                       devInfoExtractor: DevInfoExtractor,
                       extractTargetInfo: TargetInfoExtractor,
                       validatePolicy: ValidatePolicy,
                       enrichRequest: RequestTransformer,
                       enrichResponse: ResponseTransformer,
                       errorHandler: ValidationFailureHandler,
                       ipAddress: InetAddress
                       ) extends Actor with ActorLogging {

  implicit lazy val ec = context.dispatcher
  val accessLog = Logging(context.system,"AccessLog")
  val io = IO(Http)(context.system)

  def receive = {

    case request: HttpRequest =>
      val replyTo = sender

      // Process getting developer and target info from the request in parallel
      // and process them
      val futureInfoExtractor = devInfoExtractor(request)
      val futureTargetInfo = extractTargetInfo(request)

      val infos = for {
        di <- futureInfoExtractor
        ti <- futureTargetInfo
      } yield ((di, ti))

      infos  map { case (d,t) =>
        handleInfos(request, replyTo)((d,t))
      } recover {
        case t: Exception => replyTo ! errorHandler(t, request, None, None)
      }

    //      futureResponse recover {
//        case t: Exception => errorHandler(t, request, None, None)
//      } onSuccess {
//        case r: HttpResponse =>
//          accessLog.info("{} {} {} {}", new Date(), ipAddress.getHostAddress, s""""${request.method} ${request.uri.path}"""", r.status.intValue)
//          replyTo ! r
//        case r: HttpResponsePart =>
//          replyTo ! r
//      }

    case _: ConnectionClosed => self ! PoisonPill
  }

  private[this] def handleInfos(request: HttpRequest, replyTo: ActorRef): ((DevInfo, Option[TargetInfo])) => Unit = {

    case (devInfo, Some(targetInfo)) => handleForwardRequest(replyTo,request, devInfo, targetInfo)
    case (_, None) => throw new NotFoundException(s"Rule not found to handle request for: ${request.uri}")

  }

  private[this] def handleForwardRequest(replyTo: ActorRef, request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo) = {
    val enrichedRequest = enrichRequest(request, devInfo, targetInfo)
    val forwarder = context.actorOf(Props(classOf[RequestForwarder], replyTo, enrichedRequest, devInfo, targetInfo, enrichResponse))
    implicit val system = context.system
    io.tell(enrichedRequest,forwarder)
//    forwardRequest(addForwardedFromHeader(enrichRequest(request, devInfo, targetInfo))) map {
//      response => enrichResponse(request, response, devInfo, targetInfo)
//    } recover {
//      case e: Exception => errorHandler(e, request, Some(devInfo), Some(targetInfo))
//    }
  }

  protected[core] def addForwardedFromHeader(orig: HttpRequest) = {
    val ip = ipAddress.getHostAddress
    val xForwFor = "X-Forwarded-For"
    val xForwForLower = xForwFor.toLowerCase

    def xffFilter(h: HttpHeader) = h.lowercaseName == xForwForLower

    val restHeaders = orig.headers.filterNot(xffFilter)

    val xffHeaders = orig.headers.filter(xffFilter) match {
      case Nil => List(RawHeader(xForwFor, ip))
      case x :: Nil => List(RawHeader(x.name, s"${x.value}, $ip"))
      case xs => xs :+ RawHeader(xForwFor, ip)
    }
    orig.copy(headers = restHeaders ::: xffHeaders)
  }
}

class RequestForwarder(replyTo: ActorRef, request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo,
                       responseTransformer: ResponseTransformer) extends Actor with ActorLogging {
  def receive = {
    case part: HttpResponsePart =>
      replyTo ! responseTransformer(request, part, devInfo, targetInfo)
    case _ => log.warning(s"Unexpected ")
  }
}
