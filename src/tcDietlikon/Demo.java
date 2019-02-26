package tcDietlikon;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class Demo {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws EncryptedDocumentException, InvalidFormatException, IOException {

		Map<Integer,Player> players = new HashMap<Integer,Player>();
		players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers.xml"));	
		Schedule schedule = new Schedule();
		schedule.players.putAll(players);
		for (Player player : schedule.players.values()) {
			for (Slot slot : player.selectedSlots) {
				schedule.slots.get(schedule.dayTimeCourt2slotId(slot.weekdayNr,slot.time,slot.courtNr)).players.put(player.playerNr, player);
			}
		}
		schedule.write("scheduleTest.xlsx");
		
// XXX
//		Map<Integer,Player> players = new HashMap<Integer,Player>();
//		players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers_BENCHMARK.xml"));			
//		
//		Map<Integer,P2> p2 = new HashMap<Integer,P2>();
//		for (Player player : players.values()) {
//			p2.put(player.playerNr, new P2(player));
//		}
//		XMLOps.writeToFile(p2, "samplePlayers_P2.xml");

//		Map<Integer,P2> p2 = new HashMap<Integer,P2>();
//		p2.putAll(XMLOps.readFromFile(p2.getClass(), "samplePlayers_P2.xml"));			
//		
//		Map<Integer,Player> players = new HashMap<Integer,Player>();
//		for (P2 p : p2.values()) {
//			players.put(p.playerNr, new Player(p));
//		}
//		XMLOps.writeToFile(players, "samplePlayers_New.xml");
		
		
// XXX
//		PlayerUtils.loadPlayers("Template_Tennischule_Einteilung.xlsx");
//		Date date = new Date();
//		Calendar calendar = new GregorianCalendar();
//		calendar.setTime(date);
//		int year = calendar.get(Calendar.YEAR);
//		System.out.println(year);
	}

}
