package models;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.net.InetAddress;

object Schedule {
	
	val SCHEDULE_ENABLED = sys.env.get("SCHEDULE_ENABLED").getOrElse("true").toBoolean;
	
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
	
	def apply(storage: StorageManager) = new Schedule(storage);
}

class Schedule(storage: StorageManager) {
	
	import Schedule._;
	import play.api.libs.concurrent.Akka;
	import play.api.Play.current;
	import scala.concurrent.duration.FiniteDuration;
	import java.util.concurrent.TimeUnit;
	import play.api.libs.concurrent.Execution.Implicits.defaultContext;
	import akka.actor.Cancellable;
	
	private var registeredSchedule: Option[Cancellable] = None;
	
	def scheduledTime = storage.getString("scheduledTime").getOrElse("01:00:00");
	def scheduledTime_=(s: String) = storage.setString("scheduledTime", s);
	
	def scheduledTimeList = scheduledTime.split(",").toList;
	
	private def nextSettingTime: String = {
		val nowTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
		val array = scheduledTime.split(",");
		val ret = array.find(_ > nowTime).getOrElse(array.head);
		ret;
	}
	
	def calcNextSchedule: Unit = {
		if (SCHEDULE_ENABLED) {
			val cal = Calendar.getInstance;
			val time1 = strToTime(nextSettingTime);
			val time2 = calendarToTime(cal);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.SECOND, time1);
			if (time1 < time2) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}
			val next = cal.getTime;
			registeredSchedule.foreach { c =>
				c.cancel;
				registeredSchedule = None;
			}
			val duration = new FiniteDuration(next.getTime - System.currentTimeMillis, TimeUnit.MILLISECONDS);
			registeredSchedule = Option(Akka.system.scheduler.scheduleOnce(duration) {
				if (Akka.system.isTerminated) {
					println("Terminate Akka. Skip schedule");
				} else {
					val date = new Date();
					val salesforce = Salesforce(storage);
					val list = storage.list.filter(_.enabled);
					println("executeAll: date=" + date + ", count=" + list.size);
					try {
						salesforce.executeAll(list, date);
					} catch {
						case e: Exception => 
							println("SyncError: " + e.toString);
							e.printStackTrace;
					}
					calcNextSchedule;
				}
			});
			println("Next schedule=" + next + ", host=" + InetAddress.getLocalHost().toString());
		}
	}
	override def toString = scheduledTime;
}
