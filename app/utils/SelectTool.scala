package utils;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import play.api.db.DB;
import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.mvc.Action;

/**
 * Return results of select statement with jqGrid JSON format.
 */
object SelectTool extends Controller {
	
	def colModel = Action { implicit request =>
		val sql = "SELECT * FROM (" + 
			RequestUtils.getPostParam("sql").getOrElse("") + 
			") TEMP WHERE 1 = 2";
		DB.withConnection { con =>
			val stmt = con.prepareStatement(sql);
			try {
				val rs = stmt.executeQuery;
				try {
				} finally {
					rs.close;
				}
			} finally {
				stmt.close;
			}
		}
		Ok("test");
	}
	
	private def createColModel(meta: ResultSetMetaData) = {
		for (i <- 1 to meta.getColumnCount) yield {
			val name = meta.getColumnLabel(i);
			val col = new JqGrid.Column(name);
			col;
		}
	}
}

