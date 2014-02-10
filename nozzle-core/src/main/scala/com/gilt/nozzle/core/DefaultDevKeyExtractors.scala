package com.gilt.nozzle.core

object DefaultDevKeyExtractors {

  private lazy val headerName = "X-Api-Key"
  private lazy val paramName = "apikey"

  def headerDevKeyExtractor: DevKeyExtractor = { request =>
    request.headers.filter(_.name == headerName).map(_.value).headOption
  }

  def queryParamDevKeyExtractors: DevKeyExtractor = { request =>
    request.uri. query.get(paramName)
  }

}
