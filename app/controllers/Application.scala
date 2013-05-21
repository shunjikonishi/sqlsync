package controllers

import java.util.Date;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.db.DB;
import play.api.data.Form;
import play.api.data.Forms.mapping;
import play.api.data.Forms.text;

import models.SqlInfo;
import models.StorageManager;
import models.Salesforce;
import models.MongoStorageManager;
import utils.AccessControl;

object Application extends Controller with AccessControl {
	
	private val man: StorageManager = new MongoStorageManager();
	
	private lazy val objectList = {
		Salesforce(man).listObjectNames;
	}
	
	def index = filterAction { implicit request =>
		val list = man.list;
		val oList = objectList
		Ok(views.html.index(list, oList))
	}
	
	private val form = Form(mapping(
		"name" -> text,
		"sql" -> text,
		"objectName" -> text,
		"externalIdFieldName" -> text
	){ (name, sql, objectName, externalIdFieldName) => SqlInfo(name, sql, objectName, externalIdFieldName, new Date(0), new Date(0))}
	 { info => Some(info.name, info.sql, info.objectName, info.externalIdFieldName)}
	);
	
	
	def add = filterAction { implicit request =>
		val data = form.bindFromRequest;
		if (data.hasErrors) {
			BadRequest;
		} else {
			val info = data.get;
			val validation = Salesforce(man).validate(info);
			if (validation.hasError) {
				Redirect("/").flashing(
					"error" -> validation.msg,
					"name" -> info.name,
					"sql" -> info.sql,
					"objectName" -> info.objectName,
					"externalIdFieldName" -> info.externalIdFieldName
				);
			} else {
				man.add(info);
				Redirect("/");
			}
		}
		
	}
	
	def test = filterAction { implicit request =>
		val name = DB.withTransaction { con =>
			val stmt = con.prepareStatement("SELECT COUNT(*) FROM CONTENTS");
			try {
				val rs = stmt.executeQuery();
				try {
					if (rs.next) rs.getInt(1) else -1;
				} finally {
					rs.close();
				}
			} finally {
				stmt.close();
			}
		}
		Ok(name.toString);
	}
  
	//Schedule Task
	import play.api.libs.concurrent.Akka;
	import play.api.Play.current;
	import scala.concurrent.duration.DurationInt;
	import play.api.libs.concurrent.Execution.Implicits.defaultContext;
	import play.api.libs.ws.WS
	
	Akka.system.scheduler.schedule(0 seconds, 10 seconds) {
		WS.url("http://flect-sqlsync.herokuapp.com/").get()
	}
}