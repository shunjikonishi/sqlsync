package models;

import java.io.File;
import play.api.db.DB;
import play.api.Play.current;
import play.api.cache.Cache;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;

import jp.co.flect.javascript.jqgrid.ColModel;
import jp.co.flect.javascript.jqgrid.RdbColModelFactory;

import scala.collection.JavaConversions._;

object Salesforce {
	
	private val USERNAME = sys.env("SALESFORCE_USERNAME");
	private val PASSWORD = sys.env("SALESFORCE_PASSWORD");
	private val SECRET   = sys.env("SALESFORCE_SECRET");
	private val WSDL     = sys.env("SALESFORCE_WSDL");
	
	def apply(storage: StorageManager) = {
		val client = Cache.getOrElse[SalesforceClient]("salesforce.cacheKey") {
			val client = new SalesforceClient(new File(WSDL));
			client.login(USERNAME, PASSWORD, SECRET);
			Cache.set("salesforce.cacheKey", client, 60 * 60);
			client;
		}
		new Salesforce(storage, client);
	}
	
	case class ValidationResult(hasError: Boolean, msg: String);
}

class Salesforce(storage: StorageManager, client: SalesforceClient) {
	
	import Salesforce._;
	
	def listObjectNames = {
		val meta = client.describeGlobal;
		meta.getObjectDefList.map(_.getName).toList;
	}
	
	def validate(info: SqlInfo) = {
		def columnCheck(model: ColModel, obj: SObjectDef) = {
			val notFound = model.getList.filter(c => obj.getField(c.getName) == null)
				.map(_.getName);
			if (notFound.size == 0) {
				new ValidationResult(false, null);
			} else {
				new ValidationResult(true, "フィールド名が不正です。: " + notFound.mkString(","));
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
				new ValidationResult(true, "名前は必須です。");
			} else if (!storage.get(info.name).isEmpty) {
				new ValidationResult(true, "名前が重複しています。: " + info.name);
			} else if (objectDef == null) {
				new ValidationResult(true, "オブジェクトが見つかりません。: " + info.objectName);
			} else if (info.sql.filter(_ == '?').length != 1) {
				new ValidationResult(true, "日付型フィールドのパラメータが必要です。: " + info.sql);
			} else if (objectDef.getField(info.externalIdFieldName) == null || 
				!objectDef.getField(info.externalIdFieldName).isExternalId) 
			{
				new ValidationResult(true, "外部IDが不正です。: " + info.externalIdFieldName);
			} else {
				columnCheck(model, objectDef);
			}
		} catch {
			case e: Exception => new ValidationResult(true, e.getMessage());
		}
	}
}