package models;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

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
	
	def apply(storage: StorageManager) = new Schedule(storage);
}

class Schedule(storage: StorageManager) {
	
	import Schedule._;
	
	def scheduledTime = storage.getString("scheduledTime").getOrElse("01:00:00");
	def scheduledTime_=(s: String) = storage.setString("scheduledTime", s);
	
	def scheduledTimeList = scheduledTime.split(",").toList;
	
	private def nextScheduledTime = storage.getDate("nextScheduledTime").getOrElse(new Date());
	private def nextScheduledTime_=(d: Date) = storage.setDate("nextScheduledTime", d);
	
	private def nextSettingTime: String = {
		val nowTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
		val array = scheduledTime.split(",");
		val ret = array.find(_ >= nowTime).getOrElse(array.head);
		println("nextSettingTime: now=" + nowTime + ", ret=" + ret);
		Thread.dumpStack;
		ret;
	}
	
	private def isScheduledTime = {
		val next = nextScheduledTime;
		val now = new Date();
		println("isScheduledTime: now=" + now + ", next=" + next + ", abs=" + (Math.abs(next.getTime - now.getTime)));
		Math.abs(next.getTime - now.getTime) < 10000;
	}
	
	def calcNextSchedule: Date = {
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
		nextScheduledTime = cal.getTime;
println("test1: " + cal.getTime);
		
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
			calcNextSchedule;
		}
		cal.getTime;
	}
	override def toString = scheduledTime;
}
