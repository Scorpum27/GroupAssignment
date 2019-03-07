package tcDietlikon;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class Demo {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws EncryptedDocumentException, InvalidFormatException, IOException {

		int i = 7;
		int j = 9;
		int diff = j-i;
		System.out.println((i+(new Random().nextInt(diff+1))));
		
// XXX
		
//		Map<Integer,Player> players = new HashMap<Integer,Player>();
//		String playerRegistrationFile = "PlayersTCDietlikonWinter2018.xlsx";
//		players = PlayerUtils.loadPlayers(playerRegistrationFile);
//		Schedule schedule = new Schedule(3, 6, 6, 21);
//		Map<Integer,Integer> slotFrequencies = new HashMap<Integer,Integer>();
//		for (Player player : players.values()) {
//			for (Slot slot : player.desiredSlots) {
//				int slotId = schedule.dayTimeCourt2slotId(slot.weekdayNr, slot.time, 1);
//				if (slotFrequencies.containsKey(slotId)) {
//					slotFrequencies.put(slotId, slotFrequencies.get(slotId)+1);
//				}
//				else {
//					slotFrequencies.put(slotId, 1);
//				}
//			}
//		}
//		
//		Map<String,Integer> slotFrequenciesX = new HashMap<String,Integer>();
//		for (Entry<Integer,Integer> entry : slotFrequencies.entrySet()) {
//			Slot realSlot = schedule.slots.get(entry.getKey());
//			slotFrequenciesX.put(schedule.slot2name(realSlot.weekdayNr, realSlot.time, 1), entry.getValue());
//		}
//		for (int i=0; i<250; i++) {
//			if (slotFrequencies.containsKey(i)) {
//				Slot realSlot = schedule.slots.get(i);
//				System.out.println(slotFrequencies.get(i)+" - "+schedule.slot2name(realSlot.weekdayNr, realSlot.time, 1));				
//			}
//		}
		
		
// XXX
//		Map<Integer,Player> players = new HashMap<Integer,Player>();
//		players.putAll(XMLOps.readFromFile(players.getClass(), "FinalSchedulePlayers.xml"));
//		Schedule schedule = XMLOps.readFromFile(Schedule.class, "FinalSchedule.xml");		
//		
//		Map<Integer, Integer> playerLinkability = new HashMap<Integer, Integer>();
//		Map<Integer, Integer> playerLinkabilityX = new HashMap<Integer, Integer>();
//		for (Player player : players.values()) {
//			int feasibleGroups = 0;
//			int feasibleGroupsX = 0;
//			for (Slot slot : schedule.slots.values()) {
//				if (slot.groupVirtuallyAcceptsPlayer(player)) {
//					feasibleGroups++;
//				}
//				feasibleGroupsX += slot.pushPlayerAndKickOtherplayer(player).size();
//			}
//			playerLinkability.put(player.playerNr, feasibleGroups);
//			playerLinkabilityX.put(player.playerNr, feasibleGroupsX);
//		}
//		
//		System.out.println(playerLinkability.toString());
//		int totalLinkability = 0;
//		for (int pl : playerLinkability.values()) {
//			totalLinkability += pl;
//		}
//		System.out.println("Total = "+totalLinkability);
//		
//		System.out.println(playerLinkabilityX.toString());
//		int totalLinkabilityX = 0;
//		for (int pl : playerLinkabilityX.values()) {
//			totalLinkabilityX += pl;
//		}
//		System.out.println("TotalX = "+totalLinkabilityX);
		
		
// XXX
//		Map<Integer,Player> players = new HashMap<Integer,Player>();
//		players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers.xml"));	
//		Schedule schedule = new Schedule();
//		schedule.players.putAll(players);
//		for (Player player : schedule.players.values()) {
//			for (Slot slot : player.selectedSlots) {
//				schedule.slots.get(schedule.dayTimeCourt2slotId(slot.weekdayNr,slot.time,slot.courtNr)).players.put(player.playerNr, player);
//			}
//		}
//		schedule.write("scheduleTest.xlsx");
		
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
