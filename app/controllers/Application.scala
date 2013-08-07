package controllers

import java.util.Date;
import java.text.SimpleDateFormat;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.mvc.Action;
import play.api.db.DB;
import play.api.data.Form;
import play.api.data.Forms.mapping;
import play.api.data.Forms.tuple;
import play.api.data.Forms.text;
import play.api.data.Forms.optional;
import play.api.data.Forms.number;
import play.api.data.Forms.boolean;
import play.api.libs.json.JsArray;
import play.api.i18n.Messages;

import models.Schedule;
import models.SqlInfo;
import models.StorageManager;
import models.Salesforce;
import models.MongoStorageManager;
import utils.AccessControl;
import utils.RequestUtils;

object Application extends Controller with AccessControl {
	
	private val man: StorageManager = new MongoStorageManager();
	val scheduledTime = Schedule(man);
	println("onStart - scheduled=" + scheduledTime + ", lastExecuted=" + man.getDate("lastExecuted"));
	
	private lazy val objectList = {
		Salesforce(man).listObjectNames;
	}
	
	def index = filterAction { implicit request =>
		Ok(views.html.index());
	}
	
	def main = filterAction { implicit request =>
		val list = man.list;
		val oList = objectList
		val verifyPage = Salesforce(man).verifyPage;
		val dragged = request.flash.get("dragName").getOrElse("");
		Ok(views.html.main(list, oList, verifyPage, scheduledTime.scheduledTimeList, dragged))
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
					Ok(Messages("updateTargetNotFound", oldName));
				} else {
					val newInfo = info.merge(oldInfo.get);
					man.update(newInfo);
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
			BadRequest(data.errors.mkString("<br>"));
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
		scheduledTime.scheduledTime = time;
		scheduledTime.calcNextSchedule;
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
		request.body.asMultipartFormData match {
			case Some(mdf) =>
				mdf.file("importFile") match {
					case Some(file) => 
						try  {
							val list = man.fromFile(file.ref.file);
							val sf = Salesforce(man);
							val validateList = list.map(sf.validate(_, true));
							if (validateList.exists(_.hasError)) {
								throw new IllegalArgumentException(
									validateList.foldLeft("") { (ret, v) =>
										if (v.msg == null || v.msg == "") {
											ret;
										} else {
											ret + "\n" + v.msg;
										}
									}
								);
							}
							man.removeAll;
							list.foreach(man.add(_));
							Redirect("/");
						} catch {
							case e: Exception => 
								e.printStackTrace;
								Ok(e.toString);
						}
					case None => BadRequest;
				}
			case None => BadRequest;
		}
	}
	
	def setEnabled = filterAction { implicit request => 
		try {
			val name = RequestUtils.getPostParam("name").get
			val enabled = RequestUtils.getPostParam("enabled").get;
			val info = man.get(name);
			if (info.isEmpty) {
				Ok(Messages("updateTargetNotFound", name));
			} else {
				val newInfo = info.get.copy(enabled=enabled.toBoolean);
				man.update(newInfo);
				Ok("OK");
			}
		} catch {
			case e: Exception => Ok(e.toString);
		}
	}
	
	private val apiForm = Form(tuple(
		"date" -> text,
		"name" -> text,
		"msg" -> optional(text)
	));
	
	def api = filterAction { implicit request =>
		val data = apiForm.bindFromRequest;
		if (data.hasErrors) {
			BadRequest(data.errors.mkString("<br>"));
		} else {
			val (dateStr, name, msg) = data.get;
			try {
				val list = if (name == "*") {
					man.list.filter(_.enabled);
				} else {
					man.get(name) match {
						case Some(info) => List(info);
						case None => throw new Exception("Undefined name: " + name);
					}
				}
				val listWithDate = if (dateStr.forall(_ == '0')) {
					list;
				} else {
					val date = new SimpleDateFormat("yyyyMMddHHmmss").parse(dateStr);
					list.map(_.copy(lastExecuted = date));
				}
				println("API execute: (" + name + ", " + dateStr + "), msg=" + msg.getOrElse("") + ", count=" + list.size);
				Salesforce(man).executeAll(listWithDate);
				Ok("OK");
			} catch {
				case e: Exception =>
					e.printStackTrace;
					InternalServerError(e.toString);
			}
		}
	}
	
	def list = filterAction { implicit request =>
		val list = man.list.map(_.toJsonForApi);
		Ok(JsArray(list)).as("application/json; charset=utf-8");
	}
}
