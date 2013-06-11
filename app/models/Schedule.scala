package models;

import java.util.Calendar;
import java.util.Date;

object Schedule {
	
	def strToTime(str: String) = {
		var array = str.split(":");
		array.length match {
			case 1 => Integer.parseInt(array(0)) * 60 * 60;
			case 2 => Integer.parseInt(array(0)) * 60 * 60 +
			          Integer.parseInt(array(1)) * 60;
			case 3 => Integer.parseInt(array(0)) * 60 * 60 +
			          Integer.parseInt(array(1)) * 60 + 
			          Integer.parseInt(array(2));
			case _ => throw new IllegalArgumentException(str);
		}
	}
	
	def calendarToTime(cal: Calendar) = {
		cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 +
		cal.get(Calendar.MINUTE) * 60 +
		cal.get(Calendar.SECOND);
	}
	
	def apply(storage: StorageManager, str: String) = new Schedule(storage, str);
}

class Schedule(storage: StorageManager, var scheduledTime: String) {
	
	import Schedule._;
	
	private def isScheduledTime = {
		val now = Calendar.getInstance
		val time1 = strToTime(scheduledTime);
		val time2 = calendarToTime(now);
		Math.abs(time2 - time1) < 10;
	}
	
	def calcNextSchedule = {
		val cal = Calendar.getInstance
		val time1 = strToTime(scheduledTime);
		val time2 = calendarToTime(cal);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.SECOND, time1);
		if (time1 < time2) {
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		import play.api.libs.concurrent.Akka;
		import play.api.Play.current;
		import scala.concurrent.duration.FiniteDuration;
		import java.util.concurrent.TimeUnit;
		import play.api.libs.concurrent.Execution.Implicits.defaultContext;
		
		val duration = new FiniteDuration(cal.getTimeInMillis - System.currentTimeMillis, TimeUnit.MILLISECONDS);
		Akka.system.scheduler.scheduleOnce(duration) {
			if (isScheduledTime) {
				val date = new Date();
				val salesforce = Salesforce(storage);
				val list = storage.list.filter(_.enabled);
				println("executeAll: date=" + date + ", count=" + list.size);
				list.foreach { info =>
					salesforce.execute(info.lastExecuted, info);
				}
			}
		}
		cal.getTime;
	}
	override def toString = scheduledTime;
}
