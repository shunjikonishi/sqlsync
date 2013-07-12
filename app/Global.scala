import play.api.GlobalSettings;
import play.api.Application;
import java.util.Date;
import java.io.File;
import jp.co.flect.util.ResourceGen;

object Global extends GlobalSettings {
	
	override def onStart(app: Application) {
		//Generate messages and messages.ja
		val defaults = new File("conf/messages");
		val origin = new File("conf/messages.origin");
		if (origin.lastModified > defaults.lastModified) {
			val gen = new ResourceGen(defaults.getParentFile(), "messages");
			gen.process(origin);
		}
		
		import play.api.libs.concurrent.Akka;
		import play.api.Play.current;
		import scala.concurrent.duration.DurationInt;
		import play.api.libs.concurrent.Execution.Implicits.defaultContext;
		import play.api.libs.ws.WS
		
		val appname = sys.env.get("HEROKU_APPLICATION_NAME");
		if (appname.nonEmpty) {
			Akka.system.scheduler.schedule(0 seconds, 10 minutes) {
				WS.url("http://" + appname.get + ".herokuapp.com/assets/ping.txt").get()
			}
		}
		controllers.Application.scheduledTime.calcNextSchedule;
	}
	
}
