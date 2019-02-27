package tcDietlikon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;


public class PlayerUtils {

	public static Map<Integer,Player> createPlayers(int nPlayers) {
		Map<Integer,Player> players = new HashMap<Integer,Player>();
		for (int p=1; p<=nPlayers; p++) {
			String notes = "This player is an exemplary player with number " + Integer.toString(p);
			Integer playerNr = p;
			String name = "Player_" + Integer.toString(p);
			double meanStrength = 6.5;
			double stdDevPlayerStrength = 3.0;
			// mean strength=R6/R7 with stdDev of 3 classes
			Double exactStrength = new Random().nextGaussian()*stdDevPlayerStrength+meanStrength;
				// see below how stdDev for strengths above R6 is halved to squeeze in interval between R6-R9

//			System.out.println("This player's exact category before rounding = "+exactStrength);
			if (exactStrength < 1.0) {
				exactStrength = 1.0;
			}
			else if (exactStrength > 6.0) {
				exactStrength = 6.0 + 0.5*(exactStrength-6.0);
				if (exactStrength > 9.0) {
					exactStrength = 9.0;
				}
			}
			Integer strength = (int) Math.round(exactStrength);
			Integer age = (int) Math.round(new Random().nextGaussian()*3.5+14.0);
			if (age<6) {
				age = 6;
			}
			else if (age > 17) {
				age = 18;	// does not make difference after age of 18 -> ages above are equivalent to 18
			}
			Integer nSlots;
			if (new Random().nextDouble() < 0.9) {
				nSlots = 1;
			}
			else {
				nSlots = 2;
			}
			Integer maxGroupSize;
			double rand = new Random().nextDouble();
			
//			maxGroupSize = 4;
			
			if (rand < 0.83) {
				maxGroupSize = 4;
			}
//			else if (rand < 0.85) {
//				maxGroupSize = 3;
//			}
			else if (rand < 0.91) {
				maxGroupSize = 2;
			}
			else {
				maxGroupSize = 1;
			}
			Player player = new Player(name, playerNr, age, strength, nSlots, maxGroupSize);
			player.maxAgeDiff = 3;
			player.maxClassDiff = 2;
			player.notes = notes;
			player.desiredSlots = createNormalDistTimeSlots(nSlots);
			players.put(playerNr, player);
			// make special categories here and adapt player if they fall into special category
			double rS = new Random().nextDouble();
			if (rS<0.40) {
				player.category="default";
			}
			else if(rS<0.52) {
				player.category = "TC";
				player.strength = 20;
				player.maxAgeDiff = 100;
				player.maxClassDiff = 0;
				player.maxGroupSize = 8;
			}
			else if(rS<0.68) {
				player.category = "G";
				player.strength = 21;
				player.maxAgeDiff = 100;
				player.maxClassDiff = 0;
				player.maxGroupSize = 4;
			}
			else if(rS<0.84) {
				player.category = "O";
				player.strength = 22;
				player.maxAgeDiff = 100;
				player.maxClassDiff = 0;
				player.maxGroupSize = 4;
			}
			else if (rS<=1.00) {
				player.category = "R";
				player.strength = 23;
				player.maxAgeDiff = 100;
				player.maxClassDiff = 0;
				player.maxGroupSize = 4;
			}
		}
		return players;
	}
	
	public static List<Slot> createNormalDistTimeSlots(Integer minAvailableDays) {
		Integer nAvailableDays;	// how many days the player can play
		do {
			Double rSlots = new Random().nextDouble();
			if (rSlots < 0.2) {
				nAvailableDays = 1;
			}
			else if (rSlots< 0.6){
				nAvailableDays = 2;
			}
			else if (rSlots < 0.9){
				nAvailableDays = 3;
			}
			else {
				nAvailableDays = 4;
			}
		}while (nAvailableDays<minAvailableDays);
		List<Slot> slots = new ArrayList<Slot>();
		for (int n=1; n<=nAvailableDays; n++) {
			Integer weekday = 0;
			// choose random weekday (make wednesday and friday more frequent)
			while (weekday == 0 || containsWeekday(slots, weekday).equals(true)) {
				// pWednesday = 0.35
				// pFriday = 0.20;
				// pMonTuesThursday = 0.15
				Double r = new Random().nextDouble();
				if (r < 0.35) {
					weekday = 3;
				}
				else if (r < 0.55) {
					weekday = 5;
				}
				else if (r < 0.70) {
					weekday = 1;
				}
				else if (r < 0.85) {
					weekday = 2;
				}
				else {
					weekday = 4;
				}
			}
			double meanEarliestTime = 17.5;
			double stdDevEarliestTime = 1.5;
			int lastSlot = 21;
			double exactEarliestTime = (new Random().nextGaussian())*stdDevEarliestTime+meanEarliestTime;
			if (exactEarliestTime < 17.5) {
				exactEarliestTime = 17.5-0.5*(17.5-exactEarliestTime);
			}
			if (exactEarliestTime < 8.0) {
				exactEarliestTime = 8.0;
			}
			else if (exactEarliestTime > 21.0){
				exactEarliestTime = 21.0;
			}
			int firstSlot = (int) Math.round(exactEarliestTime);
			if (weekday == 3) {
				firstSlot -= 4;
			}
			else if (weekday == 5) {
				firstSlot -= 2;
			}
			// choose slots according to weekday, where Wednesdays more probable to start at lunch and Friday after 1500
			for (int s=firstSlot; s<=lastSlot; s++) {
				Slot slot = new Slot(weekday, s);
				slots.add(slot);
			}
		}
		
		return slots;
	}


	public static Boolean containsWeekday(List<Slot> slots, Integer weekday) {
		for (Slot ts : slots) {
			if (ts.weekdayNr == weekday) {
				return true;
			}
		}
		return false;
	}

	public static void findLinkablePlayers(Map<Integer, Player> players) {
		for (Player player : players.values()) {
			for (Player otherPlayer : players.values()) {
				if (player.playerNr==otherPlayer.playerNr) {
					continue;
				}
				boolean playersShareSameDesiredSlots = PlayerUtils.checkForSameSlots(player, otherPlayer);
				if (playersShareSameDesiredSlots &&
						Math.abs(player.age-otherPlayer.age)<=player.maxAgeDiff &&
						Math.abs(player.age-otherPlayer.age)<=otherPlayer.maxAgeDiff &&
						Math.abs(player.strength-otherPlayer.strength)<=player.maxClassDiff && 
						Math.abs(player.strength-otherPlayer.strength)<=otherPlayer.maxClassDiff) {
							player.linkablePlayers.add(otherPlayer.playerNr);
				}
			}
//			if (player.maxGroupSize==1) {
//				player.linkability = 0.0;
//			}
//			else if (player.maxGroupSize==2) {
//				player.linkability = 0.33*player.linkablePlayers.size();
//			}
//			else if (player.maxGroupSize==3) {
//				player.linkability = 0.5*player.linkablePlayers.size();
//			}
//			else {
//				player.linkability = 1.0*player.linkablePlayers.size();
//			}
			player.linkability = 1.0*player.linkablePlayers.size();
		}
	}

	public static boolean checkForSameSlots(Player player, Player otherPlayer) {
		for (Slot slot: player.desiredSlots) {
			for (Slot otherSlot : otherPlayer.desiredSlots) {
//				System.out.println("Slot 1 Time = "+slot.time);
//				System.out.println("Slot 2 Time = "+otherSlot.time);
//				System.out.println("Slot 1 Day = "+slot.weekdayNr);
//				System.out.println("Slot 2 Day = "+otherSlot.weekdayNr);
//				System.out.println("Day Class = "+otherSlot.weekdayNr.getClass().toString());
//				if (slot.weekdayNr.equals(otherSlot.weekdayNr)) {	// slot.time==otherSlot.time &&
//				if (slot.weekdayNr==otherSlot.weekdayNr) {	// slot.time==otherSlot.time && 
////					System.out.println(slot.weekdayNr.getClass().toString());
//					System.out.println("Yes!");
//					return true;
//				}
//				else {
////					System.out.println("No! "+(slot.weekdayNr-otherSlot.weekdayNr));
//				}
				if (slot.isSameTimeAndDay(otherSlot)) {
//					System.out.println("Yes!");
					return true;
				}
				
			}
		}
		return false;
	}

	// sort players by their linkability (highest linkability has lowest rank and is first in list)
	public static List<Player> sortByPossibleCombinations(Map<Integer, Player> players) {
		List<Player> sortedPlayers = new ArrayList<Player>();
		for (Player player : players.values()) {
			if (sortedPlayers.size()==0) {
				sortedPlayers.add(player);
			}
			else {
				int rank = 0;
				for (Player otherPlayer : sortedPlayers) {
					if (player.linkability < otherPlayer.linkability) {
						break;
					}
					else {
						rank++;						
					}
					if (rank==sortedPlayers.size()) {
					}
				}
				sortedPlayers.add(rank, player);
				
			}
		}
//		for (Player player: sortedPlayers) {
//			System.out.println(player.linkablePlayers.size());
//		}
		
		return sortedPlayers;
	}

	public static Map<Integer, Player> loadPlayers(String file) throws EncryptedDocumentException, InvalidFormatException, IOException {

		Date date = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		int year = calendar.get(Calendar.YEAR);
		
		// initialize to hold players in a map with a number each
		Map<Integer,Player> players = new HashMap<Integer,Player>();
		
		// load players from excel file
		Workbook workbook = WorkbookFactory.create(new File(file));
		Sheet playerSheet = workbook.getSheetAt(0);	// maybe use the apache sl instead of ss import if it does not work!!		
        DataFormatter dataFormatter = new DataFormatter();

        int playerCounter = 0;
        for (int r=0; r<=playerSheet.getLastRowNum(); r++) {
        	Row row = playerSheet.getRow(r);
        	Cell nameCell = row.getCell(0);
            String nameCellValue = dataFormatter.formatCellValue(nameCell);
        	if (nameCellValue.equals("")) {
        		continue;
        	}
        	else {
        		playerCounter++;
        		Integer playerNr = playerCounter;
        		String name = dataFormatter.formatCellValue(row.getCell(0));
        		// players have different categories
        		// if normal junior category: have to parse player strength as it is given with prefix "R" or "N"
        		// for slot categories instead of solely using player strength may change the following:
        		// - Slot.acceptsPlayer()
        		// - ...
        		String category;
        		String strengthString = dataFormatter.formatCellValue(row.getCell(1));
        		Integer strength;
        		Integer maxAgeDiff;
        		Integer maxClassDiff;
        		// Special groups TC, R, O, G:
        		//  --> age does not matter for special
        		//  --> can only be combined with same strength, which denotes the exact category (inside the category there is no differentiation)
        		// Normal Rx or Nx players have maxClassDiff=2 and maxAgeDiff=3
        		if (strengthString.equals("TC")) {
        			strength = 20;
        			category = "TC";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
        		}
        		else if (strengthString.equals("G")){
        			strength = 21;
        			category = "G";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
        		}
        		
        		else if (strengthString.equals("O")){
        			strength = 22;
        			category = "O";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
        		}
        		else if (strengthString.equals("R")){
        			strength = 23;
        			category = "R";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
        		}
        		else if (strengthString.contains("R")) {
        			strength = Integer.parseInt(strengthString.substring(1));
        			category = "default";
        			maxAgeDiff = 3;
        			maxClassDiff = 2;
        		}
        		else if (strengthString.contains("N")) {
        			strength = -4+Integer.parseInt(strengthString.substring(1));
        			category = "default";
        			maxAgeDiff = 3;
        			maxClassDiff = 2;
        		}
        		else {
        			System.out.println("This player does not have a valid rank. Assigning R9 strength to player "+name);
        			strength = 9;
        			category = "unknown";
        			maxAgeDiff = 3;
        			maxClassDiff = 2;
        		}
        		String ageString = dataFormatter.formatCellValue(row.getCell(2));
        		Integer age;
        		if (ageString.equals("E")) {
        			// set age=21, so that all adults can play together and possibly also with an 18-year-old by the 3-year difference
        			age = year - 21;
        		}
        		else {
        			age = Integer.parseInt(ageString);
        		}
        		Integer nSlots = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(3)));
        		Integer maxGroupSize;
        		if (strength==20) {
        			// XXX maybe this is not even necessary as the 8 should be typed into the registration table!
        			maxGroupSize = 8;
        		}
        		else {
        			maxGroupSize = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(4)));        			
        		}

        		Player newPlayer = new Player(name, playerNr, age, strength, nSlots, maxGroupSize, category);
        		newPlayer.maxAgeDiff = maxAgeDiff;
        		newPlayer.maxClassDiff = maxClassDiff;
        		players.put(playerCounter, newPlayer);
        		
        		for (int slotRowNr=r+1; slotRowNr<= r+6; slotRowNr++) {
        			Row slotRow = playerSheet.getRow(slotRowNr);
        			if (dataFormatter.formatCellValue(slotRow.getCell(0)).equals("") && !dataFormatter.formatCellValue(slotRow.getCell(1)).equals("")) {
        				int slotDayNr = Integer.parseInt(dataFormatter.formatCellValue(slotRow.getCell(1)));
        				int slotTimeStart = Integer.parseInt(dataFormatter.formatCellValue(slotRow.getCell(2)));
        				int slotTimeEnd = Integer.parseInt(dataFormatter.formatCellValue(slotRow.getCell(3)));
        				for (int slotNr=slotTimeStart; slotNr<=slotTimeEnd; slotNr++) {
        					newPlayer.desiredSlots.add(new Slot(slotDayNr, slotNr));
        				}
        			}
        			else {
        				break;
        			}
        		}
//        		System.out.println("Player name = "+name);
        	}
        	
        }
    	System.out.println("Loaded the following players:");
    	System.out.println("------ ------- ------- ------- -------");
        for (Player p : players.values()) {
        	System.out.println("Name = "+p.name);
        	System.out.println("Age = "+p.age);
        	System.out.println("PlayerNr = "+p.playerNr);
        	System.out.println("Class = "+p.strength);
        	System.out.println("MaxGroupSize = "+p.maxGroupSize);
        	System.out.println("Possible Slots:");
        	for (Slot slot : p.desiredSlots) {
        		System.out.println("Slot:  Day="+slot.weekdayNr+"  Time="+slot.time);
        	}
        	System.out.println("------ ------- ------- ------- -------");
        }
		
		return players;
	}
	
	public static List<Player> reversePlayerList(List<Player> players){
		List<Player> playersReverse = new ArrayList<Player>();
		for (Player player : players) {
			playersReverse.add(0, player);
		}
		return playersReverse;
	}



}



