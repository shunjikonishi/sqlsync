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

case class SqlInfo(name: String, sql: String, 
	objectName: String, externalIdFieldName: String, 
	prevExecuted: Date, lastExecuted: Date) {
	
	def update(d: Date) = new SqlInfo(name, sql, objectName, externalIdFieldName, lastExecuted, d);
}

trait StorageManager {
	
	
	def list: List[SqlInfo];
	def add(info: SqlInfo): Boolean;
	def remove(info: SqlInfo): Boolean;
	def update(info: SqlInfo): Boolean;
	
	def removeAll: Boolean;
	
	def get(name: String) = {
		list.filter(_.name == name).headOption;
	}
	
}

case class MongoSqlInfo(
	@Key("_id") id: ObjectId = new ObjectId,
	name: String,
	sql: String,
	objectName: String, 
	externalIdFieldName: String, 
	prevExecuted: Date,
	lastExecuted: Date
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
	
	override def list = dao.find(MongoDBObject.empty).map { x =>
		SqlInfo(x.name, x.sql, x.objectName, x.externalIdFieldName, x.prevExecuted, x.lastExecuted);
	}.toList;
	
	override def add(info: SqlInfo) = {
		val obj = new MongoSqlInfo(
			name = info.name, 
			sql = info.sql,
			objectName = info.objectName,
			externalIdFieldName = info.externalIdFieldName,
			prevExecuted = info.prevExecuted,
			lastExecuted = info.lastExecuted
		);
		dao.insert(obj);
		true;
	};
	override def remove(info: SqlInfo) = {
		dao.remove(MongoDBObject("name" -> info.name));
		true;
	};
	
	override def update(info: SqlInfo) = {
		dao.update(
			MongoDBObject("name" -> info.name),
			MongoDBObject(
				"prevExecuted" -> info.prevExecuted,
				"lastExecuted" -> info.lastExecuted
			)
		);
		true;
	}
	
	override def removeAll = {
		dao.remove(MongoDBObject.empty);
		true;
	}
}

