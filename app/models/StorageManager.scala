package models;


import java.util.Date;
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

case class SqlInfo(name: String, desc: String, sql: String, 
	objectName: String, externalIdFieldName: String, 
	prevExecuted: Date, lastExecuted: Date, status: String = "", message: String = "") {
	
	def update(d: Date, now: Date) = new SqlInfo(name, desc, sql, objectName, externalIdFieldName, d, now);
	def merge(oldInfo: SqlInfo) = new SqlInfo(name, desc, sql, objectName, externalIdFieldName, 
		oldInfo.prevExecuted, oldInfo.lastExecuted, oldInfo.status, oldInfo.message);
	def updateStatus(newStatus: String, newMessage: String) = new SqlInfo(name, desc, sql, 
		objectName, externalIdFieldName, prevExecuted, lastExecuted, newStatus, newMessage);
}

trait StorageManager {
	
	def list: List[SqlInfo];
	def add(info: SqlInfo): Boolean;
	def remove(name: String): Boolean;
	
	def removeAll: Boolean;
	
	def get(name: String) = {
		list.filter(_.name == name).headOption;
	}
	
	def getDate(key: String): Date;
	def setDate(key: String, date: Date): Unit;
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
	message: String
);	

case class MongoDateInfo(
	@Key("_id") id : ObjectId = new ObjectId,
	key: String,
	date: Date
);

object MongoStorageManager {
	
	implicit val context = {
		val context = new Context {
			val name = "global";
			override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t");
		}
		context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
		context.registerClassLoader(Play.classloader)
		context
	}
}
class MongoStorageManager extends StorageManager {
	
	import MongoStorageManager._;
	
	val dao = new SalatDAO[MongoSqlInfo, ObjectId](mongoCollection("sqlInfo")) {}
	val dao2 = new SalatDAO[MongoDateInfo, ObjectId](mongoCollection("dateInfo")) {}
	
	override def list = dao.find(MongoDBObject.empty).map { x =>
		SqlInfo(x.name, x.desc, x.sql, x.objectName, x.externalIdFieldName, x.prevExecuted, x.lastExecuted, x.status, x.message);
	}.toList;
	
	override def add(info: SqlInfo) = {
		val obj = new MongoSqlInfo(
			name = info.name, 
			desc = info.desc,
			sql = info.sql,
			objectName = info.objectName,
			externalIdFieldName = info.externalIdFieldName,
			prevExecuted = info.prevExecuted,
			lastExecuted = info.lastExecuted,
			status = info.status,
			message = info.message
		);
		dao.insert(obj);
		true;
	};
	override def remove(name: String) = {
		dao.remove(MongoDBObject("name" -> name));
		true;
	};
	
	override def removeAll = {
		dao.remove(MongoDBObject.empty);
		true;
	}
	
	override def getDate(key: String) = {
		dao2.find(MongoDBObject.empty).filter( _.key == key)
			.toList.map(_.date).headOption.getOrElse(new Date(0));
	}
	
	override def setDate(key: String, date: Date): Unit = {
		dao2.remove(MongoDBObject("key" -> key));
		dao2.insert(MongoDateInfo(key=key, date=date));
	}
}

