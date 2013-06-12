import play.api.GlobalSettings;
import play.api.Application;
import java.util.Date;

object Global extends GlobalSettings {
	
	override def onStart(app: Application) {
		import play.api.libs.concurrent.Akka;
		import play.api.Play.current;
		import scala.concurrent.duration.DurationInt;
		import play.api.libs.concurrent.Execution.Implicits.defaultContext;
		import play.api.libs.ws.WS
		
		Akka.system.scheduler.schedule(0 seconds, 10 minutes) {
			WS.url("http://flect-sqlsync.herokuapp.com/assets/ping.txt").get()
		}
		controllers.Application.scheduledTime.nextScheduledTime = new Date(0);
		println("Next schedule=" + controllers.Application.scheduledTime.calcNextSchedule);
	}
	
}
