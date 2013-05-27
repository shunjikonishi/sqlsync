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

class Schedule(storage: StorageManager, scheduledTime: String) {
	
	import Schedule._;
	
	def isScheduledTime = {
		val lastExecuted = storage.getDate("lastExecuted");
		val now = Calendar.getInstance
		if (now.getTimeInMillis - lastExecuted.getTime < 1 * 60 * 60 * 1000) {
			false;
		} else {
			val time1 = strToTime(scheduledTime);
			val time2 = calendarToTime(now);
println("isScheduledTime: " + scheduledTime + ", " + now + ", " + time1 + ", " + time2 + ", " + (time2 > time1));
			if (time2 > time1) {
				storage.setDate("lastExecuted", now.getTime);
				true;
			} else {
				false;
			}
		}
	}
	override def toString = scheduledTime;
}
