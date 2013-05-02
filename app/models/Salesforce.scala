package models;

import java.io.File;
import play.api.Play.current;
import play.api.cache.Cache;
import jp.co.flect.salesforce.SalesforceClient;

import scala.collection.JavaConversions._;

object Salesforce {
	
	private val USERNAME = sys.env("SALESFORCE_USERNAME");
	private val PASSWORD = sys.env("SALESFORCE_PASSWORD");
	private val SECRET   = sys.env("SALESFORCE_SECRET");
	private val WSDL     = sys.env("SALESFORCE_WSDL");
	
	def getInstance = {
		val client = Cache.getOrElse[SalesforceClient]("salesforce.cacheKey") {
			val client = new SalesforceClient(new File(WSDL));
println(USERNAME + ", " + PASSWORD + ", " + SECRET);
			client.login(USERNAME, PASSWORD, SECRET);
			Cache.set("salesforce.cacheKey", client, 60 * 60);
			client;
		}
		new Salesforce(client);
	}
}

class Salesforce(client: SalesforceClient) {
	
	def listObjectNames = {
		val meta = client.describeGlobal;
		meta.getObjectDefList.map(_.getName).toList;
	}
}