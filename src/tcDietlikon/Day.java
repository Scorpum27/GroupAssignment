//package tcDietlikon;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Map.Entry;
//
//public class Day {
//
//	Integer weekdayNr;
//	Map<Integer, Timeslot> dailySlots = new HashMap<Integer, Timeslot>();
//	
//	public Day() {
//	}
//	
//	public Day(Integer weekdayNr) {
//		this.weekdayNr = weekdayNr;
//		for (int t=8; t<=21; t++) {
//			this.dailySlots.put(t, new Timeslot(weekdayNr, t));
//		}
//	}
//	
//	public Day clone() {
//		Day copy = new Day(this.weekdayNr);
//		for (Entry<Integer, Timeslot> entry : this.dailySlots.entrySet()) {
//			copy.dailySlots.put(entry.getKey(), entry.getValue().clone());
//		}
//		return copy;
//	}
//	
//	public String dayNr2Name(int dayNr) {
//		if      (dayNr == 1) {
//			return "Mo";
//		}
//		else if (dayNr == 2) {
//			return "Di";
//		}
//		else if (dayNr == 3) {
//			return "Mi";
//		}
//		else if (dayNr == 4) {
//			return "Do";
//		}
//		else if (dayNr == 5) {
//			return "Fr";
//		}
//		else {
//			return "Unkwn";
//		}
//	}
//	
//}
