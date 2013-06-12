package models;

import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import se.radley.plugin.salat.Binders.ObjectId;
import se.radley.plugin.salat.mongoCollection;
import com.novus.salat.annotations.Key;
import com.novus.salat.dao.SalatDAO;
import com.novus.salat.Context;
import com.novus.salat.StringTypeHintStrategy;
import com.novus.salat.TypeHintFrequency;
import com.mongodb.casbah.Imports.MongoDBObject;

import play.api.Play;
import play.api.Play.current;
import play.api.libs.json.Json;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsBoolean;
import play.api.libs.json.JsValue;
import play.api.libs.json.JsString;
import play.api.libs.json.JsNumber;

import jp.co.flect.io.FileUtils;

case class SqlInfo(
	name: String, desc: String, sql: String, 
	objectName: String, externalIdFieldName: String, 
	prevExecuted: Date, lastExecuted: Date, 
	status: String = "", message: String = "", 
	seqNo: Int, enabled: Boolean = true,
	jobId: Option[String] = None, updateCount: Int = 0, errorCount: Int = 0) {
	
	def update(d: Date, now: Date) = copy(
		prevExecuted = d, 
		lastExecuted = now, 
		status = "", 
		message = "",
		jobId = None,
		updateCount = 0,
		errorCount = 0
	);
	
	def merge(oldInfo: SqlInfo) = copy(
		prevExecuted = oldInfo.prevExecuted,
		lastExecuted = oldInfo.lastExecuted,
		status = oldInfo.status,
		message = oldInfo.message,
		seqNo = oldInfo.seqNo,
		enabled = oldInfo.enabled,
		jobId = oldInfo.jobId,
		updateCount = oldInfo.updateCount,
		errorCount = oldInfo.errorCount
	);
	
	def toJson: JsValue = {
		val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Json.toJson(
			Map(
				"name" -> JsString(name),
				"desc" -> JsString(desc),
				"sql" -> JsString(sql),
				"objectName" -> JsString(objectName),
				"externalIdFieldName" -> JsString(externalIdFieldName),
				"prevExecuted" -> JsString(sdf.format(prevExecuted)),
				"lastExecuted" -> JsString(sdf.format(lastExecuted)),
				"seqNo" -> JsNumber(seqNo),
				"enabled" -> JsBoolean(enabled)
			)
		);
	}
	
}

trait StorageManager {
	
	def list: List[SqlInfo];
	def add(info: SqlInfo): Boolean;
	def remove(name: String): Boolean;
	def update(info: SqlInfo): Boolean;
	
	def removeAll: Boolean;
	
	def get(name: String) = {
		list.filter(_.name == name).headOption;
	}
	
	def getDate(key: String): Option[Date];
	def setDate(key: String, date: Date): Unit;
	
	def getString(key: String): Option[String];
	def setString(key: String, value: String): Unit;
	
	def sort(names: List[String]) = {
		val orgList = list;
		removeAll;
		val newList = names.zipWithIndex.foreach { case (s, i) =>
			val newInfo = orgList.filter(_.name == s).head.copy(seqNo=i+1);
			add(newInfo);
		}
	}
	
	def fromFile(file: File): List[SqlInfo] = {
		Json.parse(FileUtils.readFile(file)) match {
			case arr: JsArray =>
				val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				arr.value.map { obj =>
					val name = (obj \ "name").as[String];
					val desc = (obj \ "desc").as[String];
					val sql = (obj \ "sql").as[String];
					val objectName = (obj \ "objectName").as[String];
					val externalIdFieldName = (obj \ "externalIdFieldName").as[String];
					val prevExecuted = sdf.parse((obj \ "prevExecuted").as[String]);
					val lastExecuted = sdf.parse((obj \ "lastExecuted").as[String]);
					val seqNo = (obj \ "seqNo").as[Int];
					val enabled = (obj \ "enabled").asOpt[Boolean].getOrElse(true);
					SqlInfo(
						name=name,
						desc=desc,
						sql=sql,
						objectName=objectName,
						externalIdFieldName=externalIdFieldName,
						prevExecuted=prevExecuted,
						lastExecuted=lastExecuted,
						seqNo=seqNo,
						enabled = enabled
					);
				}.toList;
			case _ =>
				throw new IllegalArgumentException("Invalid file format");
		}
	}
}

case class MongoSqlInfo(
	@Key("_id") id: ObjectId = new ObjectId,
	name: String,
	desc: String,
	sql: String,
	objectName: String, 
	externalIdFieldName: String, 
	prevExecuted: Date,
	lastExecuted: Date,
	status: String,
	message: String,
	seqNo: Option[Int],
	enabled: Option[Boolean],
	jobId: Option[String],
	updateCount: Option[Int],
	errorCount: Option[Int]
);	

case class MongoDateInfo(
	@Key("_id") id : ObjectId = new ObjectId,
	key: String,
	date: Date
);

case class MongoStrInfo(
	@Key("_id") key: String,
	value: String
);

object MongoStorageManager {
	
	implicit val context = {
		val context = new Context {
			val name = "global";
			override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t");
		}
		context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id");
		context.registerClassLoader(Play.classloader);
		context;
	}
}
class MongoStorageManager extends StorageManager {
	
	import MongoStorageManager._;
	
	val dao = new SalatDAO[MongoSqlInfo, ObjectId](mongoCollection("sqlInfo")) {}
	val dao2 = new SalatDAO[MongoDateInfo, ObjectId](mongoCollection("dateInfo")) {}
	val dao3 = new SalatDAO[MongoStrInfo, String](mongoCollection("keyValueStore")) {}
	
	override def list = dao.find(MongoDBObject.empty).map { x =>
		SqlInfo(
			x.name, x.desc, x.sql, 
			x.objectName, x.externalIdFieldName, 
			x.prevExecuted, x.lastExecuted, 
			x.status, x.message, 
			x.seqNo.getOrElse(0), x.enabled.getOrElse(true),
			x.jobId, x.updateCount.getOrElse(0), x.errorCount.getOrElse(0)
		);
	}.toList.sortBy(_.seqNo);
	
	override def add(info: SqlInfo) = {
		println("add: " + info.name);
		val obj = new MongoSqlInfo(
			name = info.name, 
			desc = info.desc,
			sql = info.sql,
			objectName = info.objectName,
			externalIdFieldName = info.externalIdFieldName,
			prevExecuted = info.prevExecuted,
			lastExecuted = info.lastExecuted,
			status = info.status,
			message = info.message,
			seqNo = Some(info.seqNo),
			enabled = Some(info.enabled),
			jobId = info.jobId,
			updateCount = Some(info.updateCount),
			errorCount = Some(info.errorCount)
		);
		dao.insert(obj);
		true;
	};
	
	override def remove(name: String) = {
		println("remove: " + name);
		dao.remove(MongoDBObject("name" -> name));
		true;
	};
	
	override def update(info: SqlInfo) = {
		println("update: " + info.name);
		val oldInfo = dao.find(MongoDBObject.empty).filter(_.name == info.name).toList.headOption;
		oldInfo match {
			case Some(oldInfo) =>
				val newInfo = oldInfo.copy(
					name = info.name, 
					desc = info.desc,
					sql = info.sql,
					objectName = info.objectName,
					externalIdFieldName = info.externalIdFieldName,
					prevExecuted = info.prevExecuted,
					lastExecuted = info.lastExecuted,
					status = info.status,
					message = info.message,
					seqNo = Some(info.seqNo),
					enabled = Some(info.enabled),
					jobId = info.jobId,
					updateCount = Some(info.updateCount),
					errorCount = Some(info.errorCount)
				);
				dao.save(newInfo);
			case None =>
				add(info);
		}
		true;
	}
	
	override def removeAll = {
		dao.remove(MongoDBObject.empty);
		true;
	}
	
	override def getDate(key: String) = {
		dao2.find(MongoDBObject.empty).filter( _.key == key)
			.toList.map(_.date).headOption;
	}
	
	override def setDate(key: String, date: Date): Unit = {
		dao2.remove(MongoDBObject("key" -> key));
		dao2.insert(MongoDateInfo(key=key, date=date));
	}
	
	override def getString(key: String) = {
		dao3.findOneById(key).map(_.value);
	}
	
	override def setString(key: String, value: String) {
		dao3.save(MongoStrInfo(key, value));
	}
}

