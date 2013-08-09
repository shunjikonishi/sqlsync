package controllers

import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.db.DB;

import utils.AccessControl;
import utils.SelectTool;
import models.Salesforce;

object MySelectTool extends SelectTool with AccessControl {
  
  override def colModel = filterAction { implicit request =>
    super.colModel(request);
  }
  
  override def data = filterAction { implicit request =>
    super.data(request);
  }
  
  override def getSQLandModel(sql: String) = {
    val sql2 = Salesforce.replaceVar(sql);
println("test1: " + sql);
println("test2: " + sql2);
    super.getSQLandModel(sql2);
  }
}
