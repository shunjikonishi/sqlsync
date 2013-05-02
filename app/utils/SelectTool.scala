package utils;

import java.sql.SQLException;
import play.api.db.DB;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.mvc.Action;
import play.api.mvc.Request;
import play.api.mvc.AnyContent;
import play.api.cache.Cache;

import jp.co.flect.javascript.jqgrid.ColModel;
import jp.co.flect.javascript.jqgrid.RdbColModelFactory;
import jp.co.flect.javascript.jqgrid.RdbQueryModel;

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
			
			DB.withConnection { con =>
				val queryModel = new RdbQueryModel(con, sql, model);
				queryModel.setUseOffset(true);
				if (gridParam.sortColumn.length > 0) {
					queryModel.setOrder(gridParam.sortColumn, gridParam.sortAsc);
				}
				val data = queryModel.getGridData(gridParam.page, gridParam.rows);
				Ok(data.toJson).as("application/json");
			}
		} catch {
			case e: SQLException => BadRequest(e.getMessage);
		}
	}
	
	private def getSQLandModel(implicit request: Request[AnyContent]) = {
		RequestUtils.getPostParam("sql") match {
			case Some(sql) =>
				val model = Cache.getOrElse[ColModel](sql) {
					DB.withConnection { con =>
						val factory = new RdbColModelFactory(con);
						factory.getQueryModel(sql);
					}
				}
				Cache.set(sql, model, CACHE_DURATION);
				(sql, model);
			case None => throw new SQLException("SQL not specified");
		}
	}
}

