package com.gilt.nozzle.core

import spray.http.{Uri, HttpRequest}
import scala.concurrent.Future


case class TargetInfo(uri: Uri, roles: Iterable[String])

object TargetInfo {
  type TargetInfoExtractor = (HttpRequest) => Future[TargetInfo]
}
