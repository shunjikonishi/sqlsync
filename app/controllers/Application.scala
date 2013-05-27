package controllers

import java.util.Date;
import java.text.SimpleDateFormat;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.mvc.Action;
import play.api.db.DB;
import play.api.data.Form;
import play.api.data.Forms.mapping;
import play.api.data.Forms.text;

import models.Schedule;
import models.SqlInfo;
import models.StorageManager;
import models.Salesforce;
import models.MongoStorageManager;
import utils.AccessControl;
import utils.RequestUtils;

object Application extends Controller with AccessControl {
	
	private val man: StorageManager = new MongoStorageManager();
	val scheduledTime = Schedule(man, sys.env.get("SCHEDULE_TIME").getOrElse("01:00:00"));
	println("onStart - scheduled=" + scheduledTime + ", lastExecuted" + man.getDate("lastExecuted"));
	
	private lazy val objectList = {
		Salesforce(man).listObjectNames;
	}
	
	def main = filterAction { implicit request =>
		val list = man.list;
		val oList = objectList
		val verifyPage = Salesforce(man).verifyPage;
		Ok(views.html.main(list, oList, verifyPage, scheduledTime))
	}
	
	private val form = Form(mapping(
		"name" -> text,
		"desc" -> text,
		"sql" -> text,
		"objectName" -> text,
		"externalIdFieldName" -> text
	){ (name, desc, sql, objectName, externalIdFieldName) => SqlInfo(name, desc, sql, objectName, externalIdFieldName, new Date(0), new Date(0))}
	 { info => Some(info.name, info.desc, info.sql, info.objectName, info.externalIdFieldName)}
	);
	
	
	def add = filterAction { implicit request =>
		val data = form.bindFromRequest;
		if (data.hasErrors) {
			BadRequest;
		} else {
			val info = data.get;
			val validation = Salesforce(man).validate(info, false);
			if (validation.hasError) {
				Ok(validation.msg);
			} else {
				man.add(info);
				Ok("OK");
			}
		}
	}
	
	def update = filterAction { implicit request =>
		val data = form.bindFromRequest;
		if (data.hasErrors) {
			BadRequest;
		} else {
			val oldName = RequestUtils.getPostParam("oldName").get;
			val info = data.get;
			val validation = Salesforce(man).validate(info, true);
			if (validation.hasError) {
				Ok(validation.msg);
			} else {
				val oldInfo = man.get(oldName);
				if (oldInfo.isEmpty) {
					Ok("更新対象のオブジェクトが見つかりません: " + oldName);
				} else {
					val newInfo = info.merge(oldInfo.get);
					man.remove(oldName);
					man.add(newInfo);
					Ok("OK");
				}
			}
		}
	}
	
	def delete = filterAction { implicit request =>
		RequestUtils.getPostParam("name") match {
			case Some(name) =>
				man.remove(name);
				Ok("OK");
			case None => BadRequest;
		}
	}
	
	
	def execute = filterAction { implicit request =>
		val data = form.bindFromRequest;
		if (data.hasErrors) {
			BadRequest;
		} else {
			val info = data.get;
			try {
				val strDate = RequestUtils.getPostParam("sql-datetime").get;
				val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
				Salesforce(man).execute(date, info);
				Ok("OK");
			} catch {
				case e: Exception =>
					e.printStackTrace();
					Ok(Option(e.getMessage()).getOrElse(e.toString()));
			}
		}
	}
	
	def executeAll = {
		val date = new Date();
		val sm = Salesforce(man);
		val list = man.list;
		println("executeAll: date=" + date + ", count=" + list.size);
		
		list.foreach { info =>
			sm.execute(info.lastExecuted, info);
		}
	}
	
}
