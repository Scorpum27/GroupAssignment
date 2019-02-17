package tcDietlikon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.DataFormatter;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.ss.usermodel.WorkbookFactory;

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
			if (new Random().nextDouble() < 0.9) {
				maxGroupSize = 4;
			}
			else {
				maxGroupSize = 3;
			}
			Player player = new Player(name, playerNr, age, strength, nSlots, maxGroupSize);
			player.maxAgeDiff = 3;
			player.maxClassDiff = 2;
			player.notes = notes;
			player.desiredSlots = createNormalDistTimeSlots();
			players.put(playerNr, player);
		}
		return players;
	}
	
	public static List<Slot> createNormalDistTimeSlots() {
		Integer nSlots;	// how many days the player can play
		Double rSlots = new Random().nextDouble();
		if (rSlots < 0.2) {
			nSlots = 1;
		}
		else if (rSlots< 0.8){
			nSlots = 2;
		}
		else {
			nSlots = 3;
		}
		List<Slot> slots = new ArrayList<Slot>();
		for (int n=1; n<=nSlots; n++) {
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
		}
	}

	public static boolean checkForSameSlots(Player player, Player otherPlayer) {
		for (Slot slot: player.desiredSlots) {
			for (Slot otherSlot : otherPlayer.desiredSlots) {
				if (slot.isSameTimeAndDay(otherSlot)) {
					return true;
				}
			}
		}
		return false;
	}

	public static List<Player> sortByPossibleCombinations(Map<Integer, Player> players) {
		List<Player> sortedPlayers = new ArrayList<Player>();
		for (Player player : players.values()) {
			if (sortedPlayers.size()==0) {
				sortedPlayers.add(player);
			}
			else {
				int rank = 0;
				for (Player otherPlayer : sortedPlayers) {
					if (player.linkablePlayers.size()<otherPlayer.linkablePlayers.size()) {
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
		for (Player player: sortedPlayers) {
			System.out.println(player.linkablePlayers.size());
		}
		
		return sortedPlayers;
	}

	public static Map<Integer, Player> loadPlayers(String file) throws EncryptedDocumentException, InvalidFormatException, IOException {
		
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
        		// have to parse player strength as it is given with prefix "R" or "N"
        		String strengthString = dataFormatter.formatCellValue(row.getCell(1));
        		Integer strength;
        		if (strengthString.contains("R")) {
        			strength = Integer.parseInt(strengthString.substring(1));
        		}
        		else if (strengthString.contains("N")) {
        			strength = -5+Integer.parseInt(strengthString.substring(1));
        		}
        		else {
        			System.out.println("This player does not have a valid rank. Assigning R9 strength to player "+name);
        			strength = 9;
        		}
        		Integer age = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(2)));
        		Integer nSlots = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(3)));
        		Integer maxGroupSize = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(4)));

        		Player newPlayer = new Player(name, playerNr, age, strength, nSlots, maxGroupSize);
        		newPlayer.maxAgeDiff = 3;
        		newPlayer.maxClassDiff = 2;
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
	




}



