package utils;

import jp.co.flect.net.IPFilter;
import play.api.mvc._;
import play.api.mvc.Results._;
import org.apache.commons.codec.binary.Base64;
import scala.concurrent.Future

object AccessControl extends ActionBuilder[Request] {
	
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
	def invokeBlock[A](request: Request[A], f: Request[A] => Future[SimpleResult]) = {
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
			Future.successful(Forbidden);
		} else if (!basicAuth) {
			Future.successful(Unauthorized.withHeaders("WWW-Authenticate" -> "Basic realm=\"Secured\""));
		} else {
			f(request);
		}
	}
	
}
