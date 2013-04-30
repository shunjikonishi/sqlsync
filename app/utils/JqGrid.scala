package utils;

import play.api.data.Form;
import play.api.data.Forms.mapping;
import play.api.data.Forms.text;
import play.api.data.Forms.number;

import play.api.libs.json.Json.toJson;
import play.api.libs.json.JsValue;

object JqGrid {
	
	case class Parameter(page: Int, rows: Int, sidx: String, sord: String) {
		
		def sortColumn = sidx;
		def sortAsc = sord == "asc";
	}
	
	val JqGridForm = Form(mapping(
		"page" -> number,
		"rows" -> number,
		"sidx" -> text,
		"sord" -> text
	)(Parameter.apply)(Parameter.unapply));
	
	case class Column(val name: String) {
		
		private var properties: Map[String, Any] = Map();
		
		def get(key: String) = properties(key);
		def set(key: String, value: Any) = properties += key -> value;
		
		def getAsString(key: String) = get(key) match {
			case Some(x: String) => Some(x);
			case _ => None;
		}
		
		def label: Option[String] = getAsString("label");
		
		def toJson: JsValue = {
			val map = properties + ("name" -> name);
			toJson(map);
		}
		
		private def toJson(map: Map[String, Any]): Map[String, JsValue] = map.mapValues { v =>
			toJson(v);
		}
		
		private def toJson(v: Any): JsValue = v match {
			case x: String => toJson(x);
			case x: Int => toJson(x);
			case x: Any => toJson(x.toString);
		}
	}
}

class JqGrid {
	
}