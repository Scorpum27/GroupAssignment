package tcDietlikon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
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
			Player player = new Player(name, playerNr, age, strength, nSlots, maxGroupSize, true);
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

	public static Map<Integer, Player> loadPlayers(String file, boolean considerMustHavePeerWishes, boolean mergeMustBePeers2OnePlayer)
			throws EncryptedDocumentException, InvalidFormatException, IOException {

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
        		// do this in case spelling of name accidentially has space at beginning or end
        		if (name.startsWith(" ")) {
        			name = name.substring(1, name.length());
        		}
        		if (name.endsWith(" ")) {
        			name = name.substring(0, name.length()-1);
        		}
        		String mustHavePeerCellContent = dataFormatter.formatCellValue(row.getCell(5));
        		String[] mustHavePeers = new String[0];
        		if (!mustHavePeerCellContent.equals("")) {
        			mustHavePeers = mustHavePeerCellContent.split(",");        			
        		}
        		String playerNotes = dataFormatter.formatCellValue(row.getCell(6));
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
//        			strength = createUniformDistribution(7,9);
//        			category = "default";
//        			maxAgeDiff = 3;
//        			maxClassDiff = 2;
        		}
        		else if (strengthString.equals("G")){
        			strength = 21;
        			category = "G";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
//        			strength = createUniformDistribution(7,9);
//        			category = "default";
//        			maxAgeDiff = 3;
//        			maxClassDiff = 2;
        		}
        		
        		else if (strengthString.equals("O")){
        			strength = 22;
        			category = "O";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
//        			strength = createUniformDistribution(7,9);
//        			category = "default";
//        			maxAgeDiff = 3;
//        			maxClassDiff = 2;
        		}
        		else if (strengthString.equals("R")){
        			strength = 23;
        			category = "R";
        			maxAgeDiff = 100;
        			maxClassDiff = 0;
//        			strength = createUniformDistribution(7,9);
//        			category = "default";
//        			maxAgeDiff = 3;
//        			maxClassDiff = 2;
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
        			System.out.println(r);
        			maxGroupSize = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(4)));        			
        		}
        		
//        		if (maxGroupSize>4) {
//        			maxGroupSize = 4;
//        		}

        		Player newPlayer = new Player(name, playerNr, age, strength, nSlots, maxGroupSize, category, maxAgeDiff, maxClassDiff, true);

        		if (considerMustHavePeerWishes && mustHavePeers.length>0) {
        			for (String mustHavePeerName : mustHavePeers) {
        				// make sure not to add spaces from the comma separation in the excel file e.g. in front of Sascha Mark in Cyrill Keller, Sascha Mark
        				if (mustHavePeerName.startsWith(" ")) {
        					mustHavePeerName = mustHavePeerName.substring(1,mustHavePeerName.length());
        				}
        				newPlayer.frozenSameGroupPeerStrings.add(mustHavePeerName);
        			}
        		}
        		players.put(playerCounter, newPlayer);
        		
        		for (int slotRowNr=r+1; slotRowNr<= r+6; slotRowNr++) {
        			if (slotRowNr>playerSheet.getLastRowNum()) {
        				break;
        			}
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
        
        if (considerMustHavePeerWishes) {
        	// this allows to handle mustBeTogetherPeers as one player, while else loop handles them separately (separately is much more complicated)
        	// if merge is enabled, create a new merged player with both merged ones in the subProfile
        	if (mergeMustBePeers2OnePlayer) {
        		// first go through all players and note down in a list of groups, which players must play together!
        		List<List<Integer>> mergedGroupsList = new ArrayList<List<Integer>>();
        		// go through players and merge with specified mustHavePeers
                // for this have to compare the desired peer names against all other players!
                for (Player player : players.values()) {
                	if (player.frozenSameGroupPeerStrings.size()==0) {
                		continue; // has no mustHavePeerWishes
                	}
                	// first just list all players that this player wants to be in a group with
                	List<Integer> thisPlayerMergerDesires = new ArrayList<Integer>();
                	thisPlayerMergerDesires.add(player.playerNr);
                	for (String playerName : player.frozenSameGroupPeerStrings) {
                		for (Player otherPlayer : players.values()) {
                			if (otherPlayer.name.equals(playerName)) {
                				thisPlayerMergerDesires.add(otherPlayer.playerNr);
                			}
                		}
                	}
                	// check if any of the players are already featured in a mergerGroup --> if yes, put all players in this group
                	// CAUTION: must make sure that a newly added player is not featured in another merger group already. if yes, do not add that specific player
                	boolean aPlayerIsAlreadyFeaturedInAMergerGroup = false;
                	for (int playerNr : thisPlayerMergerDesires) {
                		for (List<Integer> mergerGroup : mergedGroupsList) {
                			if (mergerGroup.contains(playerNr)) {
                				aPlayerIsAlreadyFeaturedInAMergerGroup = true;
                				// put all players in this group if they have not been featured in another mergerGroup
                				playerAddingLoop:
                				for (int playerNrX : thisPlayerMergerDesires) {
                					// add playerNr to group if not featured in this or any other group
                					for (List<Integer> mergerGroupX : mergedGroupsList) {
                						if (mergerGroupX.contains(playerNrX)) {
                							// if this player is added already in another mergerGroup, this is critical because cannot fulfill both groups
                							// that it has been desired to be merged into (other mergerGroup and current one. This must be checked!)
                							if (!mergerGroupX.equals(mergerGroup)) {
                								System.out.println("CAUTION: Player "+playerNrX+" cannot be merged with all merger desires! (PlayerUtils). Aborting ...");
                								System.exit(0);
                							}
                							continue playerAddingLoop;
                						}
                					}
                					// if not yet featured, add to merger group
                					mergerGroup.add(playerNrX);
                				}
                			}
                		}
                		// if no player has yet been placed in a merger group, add all desired peers of this and including this player as a new merger group
                		if (!aPlayerIsAlreadyFeaturedInAMergerGroup) {
                			mergedGroupsList.add(thisPlayerMergerDesires);
                		}
                	}
                }
                // at this point we have all groups of players that are to be merged --> now merge the actual players
                // make an average age, strength, category for the "merged" player and use only slots that have been desired by all players to be merged!
                for (List<Integer> mergerGroup : mergedGroupsList) {
                	// if mergedGroup contains of only a single player for some reason, must not merge single player as it is represented perfectly by itself
                	if (mergerGroup.size()==1) {
                		System.out.println("Single player in mergerGroup. Continuing with next mergerGroup.");
                		continue;                		
                	}
                	double averageAge = 0.0;
                	double averageStrength = 0.0;
                	int minSlots = Integer.MAX_VALUE;
                	int highestMaxGroupSize = 0;
                	int lowestMaxAgeDiff = Integer.MAX_VALUE;
                	int lowestMaxClassDiff = Integer.MAX_VALUE;
                	String mergedName = "";
                	String category = "";
                	for (int playerNr : mergerGroup) {
                		Player player = players.get(playerNr);
                		mergedName += (player.name+"/");
                		averageAge += 1.0*player.age/mergerGroup.size();
                		averageStrength += 1.0*player.strength/mergerGroup.size();
                		if (player.nSlots<minSlots) {
                			minSlots = player.nSlots;
                		}
                		if (player.maxAgeDiff<lowestMaxAgeDiff) {
                			lowestMaxAgeDiff = player.maxAgeDiff;
                		}
                		if (player.maxClassDiff<lowestMaxClassDiff) {
                			lowestMaxClassDiff = player.maxClassDiff;
                		}
                		if (player.maxGroupSize>highestMaxGroupSize) {
                			highestMaxGroupSize = player.maxGroupSize;
                		}
                		if (player.category.equals("default")) {
                			category = "default";
                		}
                		else if (Arrays.asList("R","O","G").contains(player.category)) {
                			if (!category.equals("default")) {
                				category = player.category;
                			}
                		}
                		else if (player.category.equals("TC")) {
                			if (!Arrays.asList("default","R","O","G").contains(category)) {
                				category = "TC";
                			}
                		}
                		else {
                			System.out.println("This player has unknown category --> may jeopardize setting known category for merged group (PlayerUtils)");
                		}
                	}
                	// have to remove last "/" from the concatenated name
                	mergedName = mergedName.substring(0, mergedName.length()-1);
                	int averageAgeRounded = (int) Math.round(averageAge);
                	int averageStrengthRounded = (int) Math.round(averageStrength);
                	// create merged player
                	int mergedPlayerNr = PlayerUtils.searchHighestPlayerNr(players)+1;
                	if (players.containsKey(mergedPlayerNr)) {
                		System.out.println("CAUTION: players already contains the new mergedPlayerNr="+mergedPlayerNr+" --> Aborting...");
                		System.exit(0);
                	}
                	// CAUTION: here, the player constructor receives a "false", so that the merged player does not point to itself automatically as it is
                	// not an actual player but only confines a player union
                	// --> else, a new physical player would be created with a name formed of a combination of its merger players
                	Player mergedPlayer = new Player(mergedName, mergedPlayerNr, averageAgeRounded, averageStrengthRounded,
                			minSlots, highestMaxGroupSize, category, lowestMaxAgeDiff, lowestMaxClassDiff, false);
                	// fit merged player with only the mutual desired slots from its individual merger players
                	// add all desired slots from first merger player and remove any of the slots if they are not explicitly featured in all other players
                	List<Slot> mutuallyDesiredSlots = new ArrayList<Slot>();
                	for (int playerNr : mergerGroup) {
                		Player player = players.get(playerNr);
                		if (mutuallyDesiredSlots.size()==0) {
                			mutuallyDesiredSlots.addAll(player.desiredSlots);
                		}
                		else {
                			Iterator<Slot> candidateSlotIter = mutuallyDesiredSlots.iterator();
                			mutualDesiredSlotsLoop:
                			while(candidateSlotIter.hasNext()) {
                				Slot candidateSlot = candidateSlotIter.next();
                				for (Slot playerSlot : player.desiredSlots) {
                					if (candidateSlot.isSameTimeAndDay(playerSlot)) {
                						continue mutualDesiredSlotsLoop;
                					}
                				}
                				// if code arrives here, the candidateSlot is not featured in another player's desired slots and therefore removed!
                				candidateSlotIter.remove();
                			}
                		}
                	}
                	// add new merged player to players, add individual merger players as subProfiles and remove the latter from the general players list!
                	if (mutuallyDesiredSlots.size()==0) {
                		// if no mutual slot could be found, a merged player is not of use...
                		System.out.println("CAUTION: mergedPlayer has no more mutual desired slots. Therefore, not merging corresponding players!");
                	}
                	else {
                		mergedPlayer.desiredSlots.addAll(mutuallyDesiredSlots);
                		players.put(mergedPlayer.playerNr, mergedPlayer);
                		for (int playerNr : mergerGroup) {
                			Player subplayerProfile = players.get(playerNr);
                			mergedPlayer.subPlayerProfiles.add(players.remove(subplayerProfile.playerNr));	// removal and adding in one :-)
                		}
                	}
                }
        	}
        	else {
        		// go through players and mark them with all their frozenMustHavePeers (mark both mutually)
                // for this have to compare the desired peer names against all other players!
                for (Player player : players.values()) {
                	for (String playerName : player.frozenSameGroupPeerStrings) {
                		for (Player otherPlayer : players.values()) {
                			if (otherPlayer.name.equals(playerName) && otherPlayer.frozenSameGroupPeerStrings.contains(player.name)) {
                				if (!player.frozenSameGroupPeers.contains(otherPlayer.playerNr) && !otherPlayer.frozenSameGroupPeers.contains(player.playerNr)) {
                					player.frozenSameGroupPeers.add(otherPlayer.playerNr);        					
                					otherPlayer.frozenSameGroupPeers.add(player.playerNr);        					
                				}
                				// now make sure that they only have mutual desired slots if they HAVE TO be in the same group
                				Iterator<Slot> slot1iter = player.selectedSlots.iterator();
                				slot1Loop:
                				while(slot1iter.hasNext()) {
                					Slot slot1 = slot1iter.next();
                					for (Slot slot2 : otherPlayer.desiredSlots) {
                						if (slot1.isSameTimeAndDay(slot2)) {
                							// good, this slot is also desired by other player
                							continue slot1Loop;
                						}
                					}
                					// if code arrives here, the slot was not found in the otherPlayer --> remove from desired slots
                					slot1iter.remove();
                				}
                				Iterator<Slot> slot2iter = otherPlayer.selectedSlots.iterator();
                				slot2Loop:
                				while(slot2iter.hasNext()) {
                					Slot slot2 = slot2iter.next();
                					for (Slot slot1 : player.desiredSlots) {
                						if (slot2.isSameTimeAndDay(slot1)) {
                							// good, this slot is also desired by other player
                							continue slot2Loop;
                						}
                					}
                					// if code arrives here, the slot was not found in the otherPlayer --> remove from desired slots
                					slot2iter.remove();
                				}
                			}
                		}
                	}
                	if (player.frozenSameGroupPeers.size()!=player.frozenSameGroupPeerStrings.size()) {
                		System.out.println("CAUTION: not all desired mustHavePeers could be found in the players list. Aborting ...");
                		System.exit(0);
                	}
                }
                // CAUTION: this is dangerous, but done anyways. if several players must be together, all of their ages and rankings are set to the same
                // to better allow accepting other players in their mutual slot
                for (Player player : players.values()) {
                	if (player.frozenSameGroupPeers.size()>0) {
                		double averageStrength = 0.0;
                		double averageAge = 0.0;
                		int nMustBeTogetherPeers = 1 + player.frozenSameGroupPeers.size();
                		averageStrength += 1.0*player.strength/nMustBeTogetherPeers;
                		averageAge+= 1.0*player.age/nMustBeTogetherPeers;
                		for (int mustHavePeerNr : player.frozenSameGroupPeers) {
                			Player mustHavePeer = players.get(mustHavePeerNr);
                			averageAge += 1.0*mustHavePeer.age/nMustBeTogetherPeers;
                			averageStrength += 1.0*mustHavePeer.strength/nMustBeTogetherPeers;
                		}
                		int averageAgeRounded = (int) Math.round(averageAge);
                		int averageStrengthRounded = (int) Math.round(averageStrength);
                		// now set same average age & strength for all mustBeTogetherPeers
                		player.age = averageAgeRounded;
                		player.strength = averageStrengthRounded;
                		for (int mustHavePeerNr : player.frozenSameGroupPeers) {
                			Player mustHavePeer = players.get(mustHavePeerNr);
                			mustHavePeer.age = averageAgeRounded;
                			mustHavePeer.strength = averageStrengthRounded;
                		}
                	}
                }
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
	
	private static Integer createUniformDistribution(int i, int j) {
		int diff = j-i;
		return i+(new Random().nextInt(diff+1));
	}

	public static List<Player> reversePlayerList(List<Player> players){
		List<Player> playersReverse = new ArrayList<Player>();
		for (Player player : players) {
			playersReverse.add(0, player);
		}
		return playersReverse;
	}

	public static List<Player> findOptimalPlayerCombination(List<Player> placedPlayers, List<Player> feasiblePlayers, int sizeGoal, double linkabilityMinimum, Schedule schedule) {
//		System.out.println("Trying to find an optimal combination of nFeasiblePlayers="+feasiblePlayers.size());
		
		List<Player> tempOptimalPlayers = new ArrayList<Player>();
		
		// try to add a new player to the list with a loop
		outerLoop:
		for (Player otherPlayerSingle : feasiblePlayers) {
			// check if otherPlayer has hard condition on mustHavePeers in same group: continue only if all those players are featured in the feasiblePlayersGroup
			// if continuing: have to add and check conditions for all must be together players --> make a new list of players for this!!
			List<Player> otherPlayers = new ArrayList<Player>();
			otherPlayers.add(otherPlayerSingle);
			if (otherPlayerSingle.frozenSameGroupPeers.size()>0) {
				for (int mustHavePlayerNr : otherPlayerSingle.frozenSameGroupPeers) {
					Player mustHavePeer = schedule.players.get(mustHavePlayerNr);
					if (!feasiblePlayers.contains(mustHavePeer)) {
						continue outerLoop;
					}
					// if OK, may add this mustHavePlayer. if any of the mustHavePeers is not feasible the otherPlayers array will be cleared anyways :)
					else {
						otherPlayers.add(mustHavePeer);						
					}
				}
			}
			// jump over players who will not be able to fulfill sizeGoal if placed in the group (--> they would be limiting)
			// just for completeness, actually only allowable players are in initial lot of feasible players (all have same maxGroupSize)
			// if the otherPlayers are a whole bunch of peers who want to player together, must check this condition for all of them
			for (Player otherPlayer : otherPlayers) {
				if (otherPlayer.maxGroupSize<sizeGoal) {
					continue outerLoop;
				}				
			}
			// if the code has arrived here, the otherPlayer is compatible with all already placed players
			// check now if slot already fulfills size goal (-otherPlayers.size() bc new players have not yet been added)
			if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(placedPlayers) ==
					sizeGoal-getNumberOfIndividualPlayersFromPlayerList(otherPlayers)) {
				double linkability = 0.0;
				for (Player player : placedPlayers) {
					linkability += player.linkability;
				}
				for (Player otherPlayer : otherPlayers) {
					linkability += otherPlayer.linkability;					
				}
				if (linkability<linkabilityMinimum) {
					linkabilityMinimum = linkability;
					tempOptimalPlayers.clear();
					for (Player player : placedPlayers) {
						tempOptimalPlayers.add(player);
					}
					for (Player otherPlayer : otherPlayers) {
						tempOptimalPlayers.add(otherPlayer);
					}
				}
			}
			else {
				List<Player> tempPlacedPlayers = new ArrayList<Player>();
				for (Player player : placedPlayers) {
					tempPlacedPlayers.add(player);
				}
				for (Player otherPlayer : otherPlayers) {
					tempPlacedPlayers.add(otherPlayer);					
				}
				// adapt feasiblePlayers here
				List<Player> tempFeasiblePlayers = new ArrayList<Player>();
				feasiblePlayerLoop:
				for (Player potentialFeasiblePlayer : feasiblePlayers) {
					// do not add otherPlayer to feasible list as it is already added to the group now
					for (Player otherPlayer : otherPlayers) {
						if (potentialFeasiblePlayer.equals(otherPlayer)) {
							continue feasiblePlayerLoop;
						}
						// add only those players who are ALSO feasible with the new player in the group (otherPayer)
						if (otherPlayer.isCompatibleWithOtherPlayer(potentialFeasiblePlayer)) {
							tempFeasiblePlayers.add(potentialFeasiblePlayer);
						}						
					}
				}
				if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(tempPlacedPlayers) +
						PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(tempFeasiblePlayers) < sizeGoal) {
					continue outerLoop;
				}
				tempPlacedPlayers = findOptimalPlayerCombination(tempPlacedPlayers, tempFeasiblePlayers, sizeGoal, linkabilityMinimum, schedule);
				if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(tempPlacedPlayers)>0) {
					// actually the lower than minLinkability condition is obsolete, bc if a size>0 array is returned, the linkMin had to be achieved anyways
					// XXX --> could check this with a condition :-)
					double linkability = 0.0;
					for (Player player : tempPlacedPlayers) {
						linkability += player.linkability;
					}
					if (linkability<linkabilityMinimum) {
						linkabilityMinimum = linkability;
						tempOptimalPlayers.clear();
						for (Player player : placedPlayers) {
							tempOptimalPlayers.add(player);
						}
						for (Player otherPlayer : otherPlayers) {
							tempOptimalPlayers.add(otherPlayer);							
						}
					}
				}
			}
		}
		// this will be empty array if no feasible combination has been found
		// and it will fulfill the sizeGoal if a feasible combination was found --> will return the optimal combination 
		return tempOptimalPlayers;
		
	}

	// accumulates number of players (counts individually players from subprofiles!)
	public static int getNumberOfIndividualPlayersFromPlayerList(List<Player> players) {
		int totalNrIndividualPlayers = 0;
		for (Player player : players) {
			totalNrIndividualPlayers += player.getSize();
		}
		return totalNrIndividualPlayers;
	}

	// accumulates number of players (counts individually players from subprofiles!)
		public static int getNumberOfIndividualPlayersFromPlayerMap(Map<Integer,Player> players) {
			int totalNrIndividualPlayers = 0;
			for (Player player : players.values()) {
				totalNrIndividualPlayers += player.getSize();
			}
			return totalNrIndividualPlayers;
		}

		public static int searchHighestPlayerNr(Map<Integer, Player> players) {
			int highestPlayerNr = 0;
			for (int playerNr : players.keySet()) {
				if (playerNr > highestPlayerNr) {
					highestPlayerNr = playerNr;
				}
			}
			return highestPlayerNr;
		}

		public static List<Player> makeSubplayerListFromPlayersMap(Map<Integer, Player> players) {
			List<Player> playerList = new ArrayList<Player>();
			for (Player player : players.values()) {
				if (player.subPlayerProfiles.size()==0) {
					playerList.add(player);
				}
				else {
					playerList.addAll(player.subPlayerProfiles);
				}
			}
			return playerList;
		}
		
		public static List<Player> makeSubplayerListFromPlayersList(List<Player> players) {
			List<Player> playerList = new ArrayList<Player>();
			for (Player player : players) {
				if (player.subPlayerProfiles.size()==0) {
					playerList.add(player);
				}
				else {
					playerList.addAll(player.subPlayerProfiles);
				}
			}
			return playerList;
		}

		public static boolean containsAPlayer(Map<Integer, Player> players, Player player) {
			for (Player singlePlayer : PlayerUtils.makeSubplayerListFromPlayersMap(players)) {
				if (player.subplayerNrList().contains(singlePlayer.playerNr)) {
					return true;
				}
			}
			return false;
		}

}



