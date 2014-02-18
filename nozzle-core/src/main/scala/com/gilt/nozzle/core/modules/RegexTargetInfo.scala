package com.gilt.nozzle.core.modules

import java.util.regex.{Matcher, Pattern}
import spray.http.{Uri, HttpRequest}
import com.gilt.nozzle.core.{DefaultTargetInfo, Role, TargetInfo}
import com.gilt.nozzle.core.defaults.config
import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.{LoggerFactory, Logger}
import com.gilt.nozzle.core.TargetInfo.TargetInfoExtractor

case class TargetInfoMatcher(pattern: Pattern, replaces:Iterable[String], roles: Iterable[Role]) {
  def getUri(m: Matcher, text: String) = {

    if(m.groupCount() != replaces.size)
      throw new IllegalArgumentException(s"Wrong configuration for pattern ${pattern.pattern()}")
    val sb = new StringBuffer()
    var (i,lastEnd) = (1,0)
    replaces.foreach { r =>
      sb.append(text.substring(lastEnd, m.start(i))).append(r)
      lastEnd = m.end(i)
      i = i + 1
    }
    sb.append(text.substring(lastEnd))

    Uri(sb.toString)
  }
}

object RegexTargetInfo {

  val log = LoggerFactory.getLogger(getClass)

  private lazy val conf: Iterable[TargetInfoMatcher] = {
    val targetInfoConfig = config.getConfig("modules.target-info.regex")
    targetInfoConfig.getConfigList("paths").map { c =>
      val ti = TargetInfoMatcher(
        Pattern.compile(c.getString("pattern")),
        c.getStringList("replaces"),
        c.getStringList("roles")
      )
      log.debug("Added new regex pattern: {}", ti.toString)
      ti
    }
  }

  def regexExtractTargetInfo: TargetInfoExtractor = { (request) =>
    @tailrec
    def recursiveExtractTargetInfo(request: HttpRequest, col: List[TargetInfoMatcher]): Option[TargetInfo] = col match {
      case Nil => None
      case x :: xs =>
        val text: String = request.uri.toString()
        val matcher = x.pattern.matcher(text)
        if( matcher.matches()) {
          val ti = Some(DefaultTargetInfo(x.getUri(matcher, text), x.roles))
          log.debug("Uri resulted in {}", ti.get)
          ti
        } else
          recursiveExtractTargetInfo(request, xs)
    }
    future { recursiveExtractTargetInfo(request, conf.toList) }
  }
}
