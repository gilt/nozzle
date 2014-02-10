package com.gilt.nozzle.modules.devinfo.mongo

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocument}

import com.gilt.nozzle.core.{DevInfo, DevKeyExtractor}
import com.gilt.nozzle.core.DevInfo.DevInfoExtractor
import com.gilt.nozzle.core.defaults.config

class MongoDevInfo(keyExtractor: DevKeyExtractor) {

  private lazy val mongoConfigRoot = config.getConfig("modules.dev-info.mongo")
  private lazy val servers = mongoConfigRoot.getStringList("servers")
  private lazy val dbName = mongoConfigRoot.getString("db")
  private lazy val collectionName = mongoConfigRoot.getString("collection")

  private lazy val mongo = new MongoDriver
  private lazy val mongoConn = mongo.connection(nodes = servers)
  private lazy val mongoDb = mongoConn.db(name = dbName)
  private lazy val mongoCollection: BSONCollection = mongoDb.collection(collectionName)

  implicit val devInfoConverter = DevInfoBSONConverter

  def extractDevInfo: DevInfoExtractor = { request =>
    keyExtractor(request) match {
      case None => future { None }
      case Some(key) =>
        val query = BSONDocument("key" -> key)
        mongoCollection.find(query).cursor[DevInfo].collect[List](1).map(_.headOption)
    }
  }

}

object DevInfoBSONConverter extends BSONDocumentReader[DevInfo] {
  def read(bson: BSONDocument): DevInfo = new DevInfo(
    bson.getAs[String]("devId").getOrElse{ throw new IllegalArgumentException("No devId field found")},
    bson.getAs[List[String]]("roles").getOrElse(Seq.empty[String]),
    bson.getAs[String]("name"),
    bson.getAs[String]("f")
  )
}