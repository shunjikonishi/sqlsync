package controllers

import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.db.DB;

import utils.AccessControl;
import utils.SelectTool;
import models.Salesforce;

object MySelectTool extends SelectTool {
  
  override def colModel = AccessControl { implicit request =>
    super.doColModel(request);
  }
  
  override def data = AccessControl { implicit request =>
    super.doData(request);
  }
  
  override def getSQLandModel(sql: String) = {
    val sql2 = Salesforce.replaceVar(sql);
    super.getSQLandModel(sql2);
  }
}
