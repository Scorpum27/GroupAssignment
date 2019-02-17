//package tcDietlikon;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Map.Entry;
//
//public class CourtSchedule {
//
//	Integer courtNr;
//	Map<Integer,Day> weekdays = new HashMap<Integer,Day>();
//	
//	public CourtSchedule(Integer courtNr) {
//		this.courtNr = courtNr;
//		for (int t=1; t<=5; t++) {
//			weekdays.put(t, new Day(t));
//		}
//	}
//	
//	public CourtSchedule clone() {
//		CourtSchedule copy = new CourtSchedule(this.courtNr);
//		for (Entry<Integer, Day> entry : this.weekdays.entrySet()) {
//			copy.weekdays.put(entry.getKey(), entry.getValue().clone());
//		}
//		return copy;
//	}
//	
//}
