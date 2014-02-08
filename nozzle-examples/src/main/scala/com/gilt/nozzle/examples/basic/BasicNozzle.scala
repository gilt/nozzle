package com.gilt.nozzle.examples.basic

import com.gilt.nozzle.core.{DevInfo, TargetInfo, NozzleServer}
import scala.util.Success
import spray.http.HttpRequest
import scala.concurrent.{ExecutionContext, future}
import spray.http.Uri.{Authority, Host}


object BasicNozzle extends NozzleServer {
  import ExecutionContext.Implicits.global

  val forwardAuthority = Authority(host = Host("www.google.com"), port = 80)
  val info: DevInfo = DevInfo("me")

  override def extractDevInfo = (r: HttpRequest) => future { info }
  override def extractTargetInfo = (request: HttpRequest) => future {
    TargetInfo(request.uri.copy(authority = forwardAuthority), Seq.empty[String]) }
  override def policyValidator = (r: HttpRequest, d: DevInfo, t: TargetInfo) => Success({})
}
