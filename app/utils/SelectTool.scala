package utils;

import java.sql.SQLException;
import play.api.db.DB;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.mvc.Action;
import play.api.mvc.Request;
import play.api.mvc.AnyContent;
import play.api.cache.Cache;

import play.api.libs.json.Json;
import play.api.libs.json.JsArray;

import jp.co.flect.javascript.jqgrid.ColModel;
import jp.co.flect.javascript.jqgrid.RdbColModelFactory;
import jp.co.flect.javascript.jqgrid.RdbQueryModel;

import java.text.SimpleDateFormat;
/**
 * Return results of select statement with jqGrid JSON format.
 */
trait SelectTool extends Controller {
  
  private val CACHE_DURATION = 60 * 60;
  
  def colModel = Action { implicit request =>
    try {
      val (sql, model) = getSQLandModel;
      Ok(model.toJson).as("application/json");
    } catch {
      case e: SQLException => BadRequest(e.getMessage);
    }
  }
  
  def data = Action { implicit request =>
    try {
      val (sql, model) = getSQLandModel;
      val gridParam = JqGrid.JqGridForm.bindFromRequest.get;
      val sqlParams = getSQLParams(request);
      
      DB.withConnection { con =>
        val queryModel = new RdbQueryModel(con, sql, model);
        queryModel.setUseOffset(true);
        if (gridParam.sortColumn.length > 0) {
          queryModel.setOrder(gridParam.sortColumn, gridParam.sortAsc);
        }
        val data = queryModel.getGridData(gridParam.page, gridParam.rows, 
          scala.collection.JavaConversions.seqAsJavaList(sqlParams));
        Ok(data.toJson).as("application/json");
      }
    } catch {
      case e: SQLException => BadRequest(e.getMessage);
    }
  }
  
  private def getSQLParams(implicit request: Request[AnyContent]) = {
    RequestUtils.getPostParam("sql-param") match {
      case Some(json) =>
        Json.parse(json) match {
          case arr: JsArray =>
            arr.value.map { v =>
              val datatype = (v \ "type").as[String];
              val value = (v \ "value").as[String];
              datatype match {
                case "int" => Integer.parseInt(value);
                case "date" => new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(value).getTime);
                case "datetime" => new java.sql.Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value).getTime);
                case "string" => value;
                case _ => throw new IllegalStateException(datatype + ", " + value);
              }
            }.toList;
          case _ => Nil;
        }
      case None => Nil;
    }
  }
  
  def getSQLandModel(implicit request: Request[AnyContent]): (String, ColModel) = {
    RequestUtils.getPostParam("sql") match {
      case Some(sql) =>
        getSQLandModel(sql);
      case None => throw new SQLException("SQL not specified");
    }
  }
  
  def getSQLandModel(sql: String): (String, ColModel) = {
    val model = Cache.getOrElse[ColModel](sql, CACHE_DURATION) {
      DB.withConnection { con =>
        val factory = new RdbColModelFactory(con);
        factory.getQueryModel(sql);
      }
    }
    (sql, model);
  }
}

