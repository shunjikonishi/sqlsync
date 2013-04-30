package controllers

import play.api.Play.current;
import play.api.mvc.Controller;
import play.api.db.DB;

import utils.AccessControl;

object Application extends Controller with AccessControl {
	
	def index = filterAction { request =>
		Ok(views.html.index("Your new application is ready."))
	}
	
	def test = filterAction { request =>
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
  
}