package com.gilt.nozzle.core

import spray.http.HttpHeaders._
import spray.http.HttpMethods.GET
import org.scalatest.{Matchers, FlatSpec}
import spray.http._
import ContentTypes._
import DefaultHandlers._
import akka.event.NoLogging
import spray.http.HttpRequest
import spray.http.HttpHeaders.RawHeader

class NoopRequestEnricherTest extends FlatSpec with Matchers {
  val devInfo: DefaultDevInfo = DefaultDevInfo("dev", Seq(), None, None)
  val targetInfo: DefaultTargetInfo = DefaultTargetInfo(Uri("http://localhost"), Seq())
  val commonHeaders = List(ETag("a"), RawHeader("X-My-Header", "my-value"),`Content-Type`(`application/json`))

  "A noop Request Enricher " should "remove host header" in {
    val headers = List(Host("localhost")) ::: commonHeaders
    val request = HttpRequest(method = GET, headers = headers, uri = Uri("http://localhost/myres"))
    val request1 = noopRequestEnricher(NoLogging)(request, devInfo, targetInfo)

    request1.headers should be (List(ETag("a"), RawHeader("X-My-Header", "my-value"), `Content-Type`(`application/json`)))

  }

  "A noop Response Enricher" should "remove Date, content-length, server and transfer-encoding" in {
    val headers = List(Date(DateTime.now)) ::: commonHeaders ::: List(`Transfer-Encoding`("chunked"), Server("myServer"), `Content-Length`(88))
    val response = HttpResponse(headers = headers)
    val response1 = noopResponseEnricher(NoLogging)(HttpRequest(),response, devInfo, targetInfo).asInstanceOf[HttpResponse]

    response1.headers should be (commonHeaders)
  }
}