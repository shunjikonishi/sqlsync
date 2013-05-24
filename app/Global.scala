import play.api.GlobalSettings;
import play.api.Application;

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
		
		Akka.system.scheduler.schedule(0 seconds, 10 seconds) {
			if (controllers.Application.scheduledTime.isScheduledTime) {
				controllers.Application.executeAll;
			}
		}
	}
	
}
