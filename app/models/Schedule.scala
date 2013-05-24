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
	
	def apply(str: String) = new Schedule(str);
}

class Schedule(scheduledTime: String) {
	
	import Schedule._;
	
	private var lastExecuted: Date = new Date(0);
	
	def isScheduledTime = {
		val now = Calendar.getInstance
		if (now.getTimeInMillis - lastExecuted.getTime < 24 * 60 * 60 * 1000) {
			false;
		} else {
			val time1 = strToTime(scheduledTime);
			val time2 = calendarToTime(now);
			if (time2 > time1) {
				lastExecuted = now.getTime;
				true;
			} else {
				false;
			}
		}
	}
	override def toString = scheduledTime;
}
