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
import play.api.data.Forms.number;
import play.api.libs.json.JsArray;

import models.Schedule;
import models.SqlInfo;
import models.StorageManager;
import models.Salesforce;
import models.MongoStorageManager;
import utils.AccessControl;
import utils.RequestUtils;

object Application extends Controller with AccessControl {
	
	private val man: StorageManager = new MongoStorageManager();
	val scheduledTime = Schedule(man, man.getScheduledTime);
	println("onStart - scheduled=" + scheduledTime + ", lastExecuted" + man.getDate("lastExecuted"));
	
	private lazy val objectList = {
		Salesforce(man).listObjectNames;
	}
	
	def main = filterAction { implicit request =>
		val list = man.list;
		val oList = objectList
		val verifyPage = Salesforce(man).verifyPage;
		val dragged = request.flash.get("dragName").getOrElse("");
		Ok(views.html.main(list, oList, verifyPage, scheduledTime, dragged))
	}
	
	private val form = Form(mapping(
		"name" -> text,
		"desc" -> text,
		"sql" -> text,
		"objectName" -> text,
		"externalIdFieldName" -> text,
		"seqNo" -> number
	){ (name, desc, sql, objectName, externalIdFieldName, seqNo) => SqlInfo(name, desc, sql, objectName, externalIdFieldName, new Date(0), new Date(0), seqNo=seqNo)}
	 { info => Some(info.name, info.desc, info.sql, info.objectName, info.externalIdFieldName, info.seqNo)}
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
	
	def setScheduleTime = filterAction { implicit request => 
		val time = RequestUtils.getPostParam("scheduledTime").get
		man.setScheduledTime(time);
		scheduledTime.scheduledTime = time;
		println("Next schedule=" + scheduledTime.calcNextSchedule);
		Ok("OK");
	}
	
	def sort = filterAction { implicit request =>
		try {
			val dragName = RequestUtils.getPostParam("dragName").get
			val names = RequestUtils.getPostParams("sortNames");
			man.sort(names);
			Ok("OK").flashing("dragName" -> dragName);
		} catch {
			case e: Exception => Ok(e.toString);
		}
	}
	
	def exportJob = filterAction { implicit request =>
		val list = man.list.map(_.toJson);
		Ok(JsArray(list)).as("application/octet-stream");
	}
	
	def importJob = filterAction { implicit request =>
		Ok("OK");
	}
	
	
}
