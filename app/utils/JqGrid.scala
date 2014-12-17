package utils;

import play.api.data.Form;
import play.api.data.Forms.mapping;
import play.api.data.Forms.text;
import play.api.data.Forms.number;
import play.api.data.Forms.default;

import play.api.libs.json.Json.toJson;
import play.api.libs.json.JsValue;

object JqGrid {
	
	case class Parameter(page: Int, rows: Int, sidx: String, sord: String) {
		
		def sortColumn = sidx;
		def sortAsc = sord == "asc";
	}
	
	val JqGridForm = Form(mapping(
		"page" -> default(number, 1),
		"rows" -> default(number, 50),
		"sidx" -> default(text, ""),
		"sord" -> default(text, "asc")
	)(Parameter.apply)(Parameter.unapply));
	
}
