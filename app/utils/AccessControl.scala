package utils;

import jp.co.flect.net.IPFilter;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.api.mvc.Request;
import play.api.mvc.Result;
import play.api.mvc.Controller;
import org.apache.commons.codec.binary.Base64;

trait AccessControl extends Controller {
	
	//IP restriction setting, if required
	private val IP_FILTER = sys.env.get("ALLOWED_IP")
		.map(IPFilter.getInstance(_));
	
	//Basic authentication setting, if required
	private val BASIC_AUTH = sys.env.get("BASIC_AUTHENTICATION")
		.filter(_.split(":").length == 2)
		.map { str =>
			val strs = str.split(":");
			(strs(0), strs(1));
		};
	
	//Apply IP restriction and Basic authentication
	//and Logging
	def filterAction(f: Request[AnyContent] => Result): Action[AnyContent] = Action { request =>
		def ipFilter = {
			IP_FILTER match {
				case Some(filter) =>
					val ip = request.headers.get("x-forwarded-for").getOrElse(request.remoteAddress);
					filter.allow(ip);
				case None =>
					true;
			}
		}
		def basicAuth = {
			BASIC_AUTH match {
				case Some((username, password)) =>
					request.headers.get("Authorization").map { auth =>
						auth.split(" ").drop(1).headOption.map { encoded =>
							new String(Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
								case u :: p :: Nil => u == username && password == p;
								case _ => false;
							}
						}.getOrElse(false);
					}.getOrElse {
						false;
					}
				case None =>
					true;
			}
		}
		if (!ipFilter) {
			Forbidden;
		} else if (!basicAuth) {
		    Unauthorized.withHeaders("WWW-Authenticate" -> "Basic realm=\"Secured\"");
		} else {
			f(request);
		}
	}
	
}
