package utils;

import play.api.mvc.Request;
import play.api.mvc.AnyContent;

object RequestUtils {
	def getPostParam(name: String)(implicit request: Request[AnyContent]) = {
		request.body.asFormUrlEncoded.flatMap {
			_.get(name).map(_.head)
		}
	}
}
