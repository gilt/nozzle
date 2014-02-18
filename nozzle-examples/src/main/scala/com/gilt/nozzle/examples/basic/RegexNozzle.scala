package com.gilt.nozzle.examples.basic

import com.gilt.nozzle.core.{DevInfo, TargetInfo, NozzleServer}
import com.gilt.nozzle.core.modules.RegexTargetInfo.regexExtractTargetInfo
import scala.concurrent._
import spray.http.HttpRequest
import scala.util.Success
import ExecutionContext.Implicits.global


object RegexNozzle extends NozzleServer {

  val info: DevInfo = DevInfo("me")

  override def extractDevInfo = (r: HttpRequest) => future { info }
  override def extractTargetInfo = regexExtractTargetInfo
  override def policyValidator = (r: HttpRequest, d: DevInfo, t: TargetInfo) => Success({})
}
