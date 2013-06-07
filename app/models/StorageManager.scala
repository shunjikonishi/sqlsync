package models;


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
import play.api.libs.json.JsValue;
import play.api.libs.json.JsString;
import play.api.libs.json.JsNumber;

case class SqlInfo(name: String, desc: String, sql: String, 
	objectName: String, externalIdFieldName: String, 
	prevExecuted: Date, lastExecuted: Date, status: String = "", message: String = "", seqNo: Int) {
	
	def update(d: Date, now: Date) = new SqlInfo(name, desc, sql, objectName, externalIdFieldName, d, now, seqNo=seqNo);
	def merge(oldInfo: SqlInfo) = new SqlInfo(name, desc, sql, objectName, externalIdFieldName, 
		oldInfo.prevExecuted, oldInfo.lastExecuted, oldInfo.status, oldInfo.message, oldInfo.seqNo);
	def updateStatus(newStatus: String, newMessage: String) = new SqlInfo(name, desc, sql, 
		objectName, externalIdFieldName, prevExecuted, lastExecuted, newStatus, newMessage, seqNo);
	
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
				"seqNo" -> JsNumber(seqNo)
			)
		);
	}
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
	
	def getScheduledTime = {
		val cal = Calendar.getInstance;
		cal.setTime(getDate("scheduledTime"));
		val h = cal.get(Calendar.HOUR_OF_DAY);
		val m = cal.get(Calendar.MINUTE);
		val s = cal.get(Calendar.SECOND);
		
		val fmt = new DecimalFormat("00");
		fmt.format(h) + ":" + fmt.format(m) + ":" + fmt.format(s);
	}
	
	def setScheduledTime(time: String) = {
		val cal = Calendar.getInstance;
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.SECOND, Schedule.strToTime(time));
		
		setDate("scheduledTime", cal.getTime);
	}
	
	def sort(names: List[String]) = {
		val orgList = list;
		removeAll;
		val newList = names.zipWithIndex.foreach { case (s, i) =>
			val newInfo = orgList.filter(_.name == s).head.copy(seqNo=i+1);
			add(newInfo);
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
	seqNo: Option[Int]
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
		SqlInfo(x.name, x.desc, x.sql, x.objectName, x.externalIdFieldName, x.prevExecuted, x.lastExecuted, x.status, x.message, x.seqNo.getOrElse(0));
	}.toList.sortBy(_.seqNo);
	
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
			message = info.message,
			seqNo = Some(info.seqNo)
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

