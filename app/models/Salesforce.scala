package models;

import java.io.File;
import java.util.Date;
import java.sql.Connection;
import java.sql.Timestamp;
import play.api.db.DB;
import play.api.Play.current;
import play.api.cache.Cache;
import play.api.i18n.Messages;
import play.api.i18n.Lang;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.bulk.SQLSyncRequest;
import jp.co.flect.salesforce.bulk.JobInfo;
import jp.co.flect.salesforce.event.SQLSynchronizerListener;
import jp.co.flect.salesforce.event.SQLSynchronizerEvent;

import jp.co.flect.javascript.jqgrid.ColModel;
import jp.co.flect.javascript.jqgrid.RdbColModelFactory;

import scala.collection.JavaConversions._;

object Salesforce {
	
	private val USERNAME = sys.env("SALESFORCE_USERNAME");
	private val PASSWORD = sys.env("SALESFORCE_PASSWORD");
	private val SECRET   = sys.env.get("SALESFORCE_SECRET").getOrElse("");
	private val WSDL     = sys.env.get("SALESFORCE_WSDL").getOrElse("conf/salesforce/partner.wsdl");
	
	private val PROXY_HOST = sys.env.get("PROXY_HOST");
	private val PROXY_PORT = sys.env.get("PROXY_PORT").map(_.toInt).getOrElse(80);
	private val PROXY_USERNAME = sys.env.get("PROXY_USERNAME").getOrElse(null);
	private val PROXY_PASSWORD = sys.env.get("PROXY_PASSWORD").getOrElse(null);
	
	def apply(storage: StorageManager)(implicit lang: Lang) = {
		val client = Cache.getOrElse[SalesforceClient]("salesforce.cacheKey", 10 * 60) {
			val client = new SalesforceClient(new File(WSDL));
			PROXY_HOST.foreach { s =>
				client.setProxyInfo(s, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD);
			};
			client.login(USERNAME, PASSWORD, SECRET);
			println("Create SalesforceClient: " + new Date());
			client;
		}
		new Salesforce(storage, client, lang);
	}
	
	case class ValidationResult(hasError: Boolean, msg: String);
}

class Salesforce(storage: StorageManager, client: SalesforceClient, implicit val lang: Lang) {
	
	import Salesforce._;
	
	private val bulkClient = client.createBulkClient;
	
	def listObjectNames = {
		val meta = client.describeGlobal;
		meta.getObjectDefList.map(_.getName).toList;
	}
	
	def verifyPage = {
		val endpoint = client.getEndpoint;
		if (endpoint.indexOf("-api") != -1) {
			endpoint.substring(0, endpoint.indexOf("-api")) + ".salesforce.com/750";
		} else {
			endpoint.substring(0, endpoint.indexOf("/", 9)) + "/750";
		}
	}
	
	def validate(info: SqlInfo, update: Boolean) = {
		def columnCheck(model: ColModel, obj: SObjectDef) = {
			val notFound = model.getList.filter{ c => 
				val names = c.getName.split("\\.");
				names.length match {
					case 1 => obj.getField(names(0)) == null;
					case 2 => obj.getSingleRelation(names(0)) == null;
					case _ => true;
				}
			}.map(_.getName);
			if (notFound.size == 0) {
				new ValidationResult(false, null);
			} else {
				new ValidationResult(true, Messages("invalidFieldName", notFound.mkString(",")));
			}
		}
		try {
			val model = Cache.getOrElse[ColModel](info.sql) {
				DB.withConnection { con =>
					val factory = new RdbColModelFactory(con);
					factory.getQueryModel(info.sql);
				}
			}
			val objectDef = Option(client.getMetadata.getObjectDef(info.objectName))
				.filter(_.isComplete)
				.getOrElse(client.describeSObject(info.objectName));
			if (info.name == "") {
				new ValidationResult(true, Messages("nameRequired"));
			} else if (!update && !storage.get(info.name).isEmpty) {
				new ValidationResult(true, Messages("duplicateName", info.name));
			} else if (objectDef == null) {
				new ValidationResult(true, Messages("objectNotFound", info.objectName));
			} else if (info.sql.filter(_ == '?').length != 1) {
				new ValidationResult(true, Messages("dateParameterRequired", info.sql));
			} else if (objectDef.getField(info.externalIdFieldName) == null || 
				!objectDef.getField(info.externalIdFieldName).isExternalId) 
			{
				new ValidationResult(true, Messages("invalidExternalIdField", info.externalIdFieldName));
			} else {
				columnCheck(model, objectDef);
			}
		} catch {
			case e: Exception => 
				e.printStackTrace();
				new ValidationResult(true, e.getMessage());
		}
	}
	
	def executeAll(list: List[SqlInfo]): Unit = {
		if (list.size > 0) {
			val head = list.head;
			val tail = list.tail;
			execute(head.lastExecuted, head, tail);
		}
	}
	
	def execute(date: Date, info: SqlInfo, queue: List[SqlInfo] = Nil) = {
		println("Start: " + info.name + ", targetDate=" + date);
		
		val con = DB.getConnection();
		val now = new Date();
		val newInfo = info.update(date, now);
		
		val request = new SQLSyncRequest(con, info.sql, info.objectName);
		storage.update(newInfo);
		
		request.setExternalIdFieldName(info.externalIdFieldName);
		request.setParams(new Timestamp(date.getTime));
		request.addSQLSynchronizerListener(new MyListener(con, newInfo, queue));
		client.syncSQL(request);
		
		storage.setDate("lastExecuted", now)
		newInfo;
	}
	
	private class MyListener(con: Connection, private var info: SqlInfo, queue: List[SqlInfo]) extends SQLSynchronizerListener {
		
		import SQLSynchronizerEvent.EventType._;
		
		override def handleEvent(e: SQLSynchronizerEvent) {
			println("BulkStatus: " + info.name + ": " + e.getType);
			val msg = if (e.getType == ERROR) {
				println("SyncError: " + info.name + ", " + e.getException().toString);
				e.getException().printStackTrace;
				e.getException().toString;
			} else {
				"";
			}
			val newInfo = (if (e.getType == ERROR) {
					info.copy(lastExecuted=info.prevExecuted);
				} else if (e.getType == OPEN_JOB) {
					info.copy(jobId=Some(e.getJobInfo.getId));
				} else {
					info;
		 		}).copy(status=e.getType.toString, message=msg);
			storage.update(newInfo);
			if (e.getType == ERROR || e.getType == MAKE_CSV || e.getType == NOT_PROCESSED) {
				con.close;
			}
			if (e.getType == OPEN_JOB) {
				scheduleObserve(newInfo, queue);
			}
			if (e.getType == ERROR || e.getType == NOT_PROCESSED || e.getType == ABORT_JOB) {
				executeAll(queue);
			}
			info = newInfo;
		}
	}
	
	private def scheduleObserve(info: SqlInfo, queue: List[SqlInfo]): Unit = {
		import play.api.libs.concurrent.Akka;
		import play.api.Play.current;
		import scala.concurrent.duration.DurationInt;
		import play.api.libs.concurrent.Execution.Implicits.defaultContext;
		
		Akka.system.scheduler.scheduleOnce(5 seconds) {
			val job = bulkClient.getJobStatus(new JobInfo(info.jobId.get));
			println("ObserveJob: " + info.name + ": " + job.getState);
			if (job.getState == JobInfo.JobState.Closed) {
				val newInfo = storage.get(info.name).get.copy(
					jobId = None, 
					updateCount = job.getRecordsProcessed, 
					errorCount = job.getRecordsFailed
				);
				storage.update(newInfo);
				if (newInfo.errorCount > 0) {
					println("SyncError: " + newInfo.name + ", errorCount=" + newInfo.errorCount);
				}
			}
			executeAll(queue);
		}
	}
	
}