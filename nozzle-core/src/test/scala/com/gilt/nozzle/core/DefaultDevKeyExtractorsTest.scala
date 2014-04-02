package com.gilt.nozzle.core

import org.scalatest.{Matchers, FlatSpec}
import spray.http.{Uri, HttpRequest}
import spray.http.HttpHeaders.RawHeader
import DefaultDevKeyExtractors._
import spray.http.Uri.Query

class DefaultDevKeyExtractorsTest extends FlatSpec with Matchers {

  "A header extractor" should "extract the dev token from the header" in {
    val request = HttpRequest(headers = List(RawHeader("X-Api-Key","aa")))
    headerDevKeyExtractor(request) should be (Some("aa"))
    queryParamDevKeyExtractors(request) should be (None)
  }

  "A query parameter extractor" should "extract the dev token from the query" in {
    val request = HttpRequest(uri = Uri(query = Query("apikey=aa")))
    queryParamDevKeyExtractors(request) should be (Some("aa"))
    headerDevKeyExtractor(request) should be (None)
  }
}
