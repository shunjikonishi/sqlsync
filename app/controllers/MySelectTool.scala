package controllers

import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.db.DB;

import utils.AccessControl;
import utils.SelectTool;

object MySelectTool extends SelectTool with AccessControl {
	
	override def colModel = filterAction { implicit request =>
		super.colModel(request);
	}
	
	override def data = filterAction { implicit request =>
		super.data(request);
	}
}
