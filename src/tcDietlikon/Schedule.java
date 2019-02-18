package tcDietlikon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Schedule {

	Map<Integer,Slot> slots = new HashMap<Integer,Slot>();
	int nCourts = 0;
	int nDays = 0;
	int firstHour = 0;
	int lastHour = 0;
	
	public Schedule() {
	}
	
	public Schedule(int nCourts, int nDays, int firstHour, int lastHour) {
		this.nCourts = nCourts;
		this.nDays = nDays;
		this.firstHour = firstHour;
		this.lastHour = lastHour;
		int slotId = 1;
		for (int court=1; court<=nCourts; court++) {
			for (int day=1; day<=nDays; day++) {
				for (int hour=firstHour; hour<=lastHour; hour++) {
					slots.put(slotId, new Slot(slotId, day, hour, court));
					slotId++;
				}
			}
		}
	}

	public Schedule(int nCourts, int nDays, int firstHour, int lastHour, String courtScheduleFile) {
		this.nCourts = nCourts;
		this.nDays = nDays;
		this.firstHour = firstHour;
		this.lastHour = lastHour;
		int slotId = 1;
		for (int court=1; court<=nCourts; court++) {
			for (int day=1; day<=nDays; day++) {
				for (int hour=firstHour; hour<=lastHour; hour++) {
					slots.put(slotId, new Slot(slotId, day, hour, court));
					slotId++;
				}
			}
		}
	}
	
	
	
	public Schedule(String courtScheduleFile) throws EncryptedDocumentException, InvalidFormatException, IOException {
		
		// load court schedules from excel file
		Workbook workbook = WorkbookFactory.create(new File(courtScheduleFile));
		Sheet courtSheet = workbook.getSheetAt(0);	// maybe use the apache sl instead of ss import if it does not work!!		
		DataFormatter dataFormatter = new DataFormatter();
		
		Row headerRow = courtSheet.getRow(1);
//		System.out.println(headerRow.toString());
//		for (int i=0; i<10; i++) {
//			System.out.println(dataFormatter.formatCellValue(headerRow.getCell(i)));
//		}
		this.nCourts = Integer.parseInt(dataFormatter.formatCellValue(headerRow.getCell(0)));
		this.firstHour = Integer.parseInt(dataFormatter.formatCellValue(headerRow.getCell(3)));
		this.lastHour = Integer.parseInt(dataFormatter.formatCellValue(headerRow.getCell(6)));
		this.nDays = Integer.parseInt(dataFormatter.formatCellValue(headerRow.getCell(9)));
		
		Integer firstHourRow = -1;
		for (int r=2; r<=100; r++) {
			if (dataFormatter.formatCellValue(courtSheet.getRow(r).getCell(0)).equals(Integer.toString(this.firstHour))) {
				firstHourRow = r;
				break;
			}
		}
		if (firstHourRow == -1) {
			System.out.println("CAUTION: First daily hour slot cannot be found in the court schedule excel file. Aborting...");
			System.exit(0);
		}

		int slotId = 1;
		for (Integer d=1; d<=this.nDays; d++) {
			for (int r=firstHourRow; r<=firstHourRow+(this.lastHour-this.firstHour); r++) {
				Integer h = Integer.parseInt(dataFormatter.formatCellValue(courtSheet.getRow(r).getCell(0)));
				for (Integer c=1; c<=this.nCourts; c++) {
					if (dataFormatter.formatCellValue(courtSheet.getRow(r).getCell((d-1)*this.nCourts+c)).equals("x")) {
						this.slots.put(slotId, new Slot(slotId, d, h, c));
						System.out.println("Day/Hour/Court = "+d+"/"+h+"/"+c);
						slotId++;						
					}
				}
			}
		}

	}
	
	
	
	public boolean placePlayer(Player player, int strategy) {
	// make rules when placing player
		
		// ORDER OF PLACING PLAYERS
		// 1st RUN: Try to place players within their desired slots (block court 3!)
		// 2nd RUN: Open court 3 for desired slot placements
		// 3rd RUN: Place players in not desired slots but obeying placement rules
		// 4th RUN: Place players without respecting slot preferences and with loosened placement rules
		// 5th RUN: No slot preference and no placement rules except max. 4 players/group
		
		// CONSTRAINTS WHEN PLACING PLAYERS
		// - same player may want to play twice --> separate loops for each desired slot
		// - use court 1 and 2 before using court 3
		// - only four participants, and five if really necessary
		// - max age difference
		// - max class difference
		
		if (strategy > 0) {
			Boolean higherStrategySuccessful = placePlayer(player, strategy-1);
			if (higherStrategySuccessful) {
				return true;
			}
		}
		
		
		for (int s=1; s<=player.nSlots; s++) {
			for (Slot slot : this.slots.values()) {
//				if ((strategy == 0 || strategy == 1) && slot.courtNr == 3) {
//					continue;
//				}
//				if (slot.courtNr == 3) {
//					continue;
//				}
				if (slot.acceptsPlayer(player, strategy)) {
					slot.players.put(player.playerNr,player);
					player.selectedSlots.add(new Slot(slot.slotId, slot.weekdayNr, slot.time, slot.courtNr));
					player.placementRound = strategy;
					return true;
				}
			}			
		}
		return false;
		
	}

	public int slot2row(int time) {
		return 2+(time-this.firstHour)*7;
	}

	public int slot2col(int day, int court) {
		return (day-1)*10 + (court-1)*this.nCourts;
	}

	public String slot2name(int day, int time, int court) {
		String slotName = (Slot.dayNr2Name(day))+" - "+time+"h - Court "+court;
		return slotName;
	}

	public void write(String fileName) throws IOException {
		Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
		Sheet sheet = workbook.createSheet("Junioren Sommertraining");

		List<Row> rows = new ArrayList<Row>();
		for (int r=0; r<=10+7*(this.lastHour-this.firstHour+1); r++) {
			rows.add(sheet.createRow(r));
		}
		
		int refRowNr;
		int refColNr;
		
		Font slotTitleFont = workbook.createFont();
		slotTitleFont.setBold(true);
		CellStyle slotTitleCellStyle = workbook.createCellStyle();
		slotTitleCellStyle.setFont(slotTitleFont);
		
		for (int time=this.firstHour; time<=this.lastHour; time++) {
			refRowNr = this.slot2row(time);
			Row row = rows.get(refRowNr);
//			row.setRowStyle(slotTitleCellStyle);
			for (int day=1; day<=this.nDays; day++) {
				for (int court=1; court<=this.nCourts; court++) {
					refColNr = this.slot2col(day, court);
					Cell cell = row.createCell(refColNr);
					cell.setCellValue(this.slot2name(day,time,court));
					cell.setCellStyle(slotTitleCellStyle);
				}
			}
		}
		
		CellStyle rightBorderCellStyle = workbook.createCellStyle();
		rightBorderCellStyle.setBorderRight(BorderStyle.MEDIUM);
		
		for (Slot slot : this.slots.values()) {
			refColNr = this.slot2col(slot.weekdayNr, slot.courtNr);
			refRowNr = this.slot2row(slot.time);
			int playerNr = 1;
			for (Player player : slot.players.values()) {
				Row playerRow = rows.get(refRowNr + playerNr);
				Cell nameCell = playerRow.createCell(refColNr);
				nameCell.setCellValue(player.name + " (" + player.linkablePlayers.size() + ")");
				Cell classCell = playerRow.createCell(refColNr + 1);
				classCell.setCellValue("R" + player.strength);
				Cell ageCell = playerRow.createCell(refColNr + 2);
				ageCell.setCellValue(player.age);
				ageCell.setCellStyle(rightBorderCellStyle);
				playerNr++;
			}
		}
	  
		// Resize all columns to fit the content size
		for (int i = 0; i <= 50; i++) {
			sheet.autoSizeColumn(i);
		}

		// Make header after resizing so that first column is not super wide due to long title of first headerCell
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		Row headerRow = rows.get(0);
		Cell titleCell = headerRow.createCell(0);
		titleCell.setCellValue("Tennisschule Cyrill Keller - Junioren, Sommertraining");
		titleCell.setCellStyle(headerCellStyle);

		FileOutputStream fileOut = new FileOutputStream(fileName);
		workbook.write(fileOut);
		fileOut.close();
		workbook.close();
	}

	public static Schedule initializeSchedule(Map<Integer, Player> players, List<Player> playersSortedByPossibleCombinations, String courtScheduleFile) throws EncryptedDocumentException, InvalidFormatException, IOException {
		
		Schedule schedule = new Schedule(courtScheduleFile); 	// initializes schedule with slots when courts are free (see excel file)
		int unsuccessfulPlacements = 0;
		for (Player player : playersSortedByPossibleCombinations) {
			Boolean successfulPlacement = schedule.placePlayer(player, 2); // use strategy 2 to consider only desired slots and all constraints
			// maybe place a player in fullest possible group instead of smaller ones!
			if (!successfulPlacement) {
				unsuccessfulPlacements++;
			}
		}
		for (Player player : players.values()) {
			if (Arrays.asList(3, 4, 5).contains(player.placementRound)) {
				System.out.println("Strategy for player " + player.playerNr + " = " + player.placementRound);
			}
		}
		System.out.println("Number of unsuccessful placements = " + unsuccessfulPlacements);
		return schedule;
	}

	
	public void refine() {
		for (int groupSize : Arrays.asList(1,2,3,4)) {
			this.breakUpGroups(groupSize);
		}
		int maxPullLevel = 5;
		for (int rounds=0; rounds<3; rounds++) {
			for (int groupSize : Arrays.asList(1,2,3)) {
//				this.extendSmallGroups(groupSize, maxPullLevel);
			}
		}
		int maxDesirableGroupSize = 4;
//		this.shrinkOverfullGroups(maxDesirableGroupSize);
	}



	private void shrinkOverfullGroups(int maxDesirableGroupSize) {
		int pushLevel = 3;
		List<Integer> slotIdList = new ArrayList<Integer>();
		slotIdList.addAll(this.slots.keySet());
		for (Integer slotId : slotIdList) {
			Slot slot = this.slots.get(slotId);
			if (slot.players.size()>4) {
//				System.out.println("Trying to shrink slot = "+slotId);
				for (Integer playerNr : slot.players.keySet()) {
					boolean pushSuccessful = this.pushSinglePlayer(playerNr, slot.slotId, pushLevel);
					if (pushSuccessful && slot.players.size()<=4) {
						break;
					}
				}
			}
		}
	}

	private void breakUpGroups(int groupSize) {
//		System.out.println("Attempting break-ups of groups with size="+groupSize);
		// instead of // for (Slot timeslot : this.slots.values()) { // make list of slotNrs to be checked
		// to avoid concurrentModException when modifying the schedule in this.pushPlayers
		List<Integer> slotIds = new ArrayList<Integer>();
		for (Integer slotId : this.slots.keySet()) {
			slotIds.add(slotId);
		}
		for (Integer slotId : slotIds) {
			if (!this.slots.containsKey(slotId)) {
				continue;
			}
			Slot slot = this.slots.get(slotId);
			if (slot.players.size() == groupSize) {
				// changes this (schedule) directly within method
//				System.out.println("Attempting break-up of slot: "+Slot.dayNr2Name(slot.weekdayNr)+" "+slot.time+"h Court"+slot.courtNr);
				Boolean pushSuccessful = this.pushPlayers(slot.slotId);
				if (pushSuccessful) {
//					System.out.println("All players of this slot relocated successfully");
				} else {
//					System.out.println("Push failed for this slot.");
				}
			}
		}
	}
	
	

	public Schedule clone() {
		Schedule copy = new Schedule();
		for (Entry<Integer, Slot> entry : this.slots.entrySet()) {
			copy.slots.put(entry.getKey(), entry.getValue().clone());
		}
		copy.firstHour = this.firstHour;
		copy.lastHour = this.lastHour;
		copy.nCourts = this.nCourts;
		copy.nDays = this.nDays;
		return copy;
	}
	
	public boolean pushPlayers(Integer slotId) {
		// work on a copy of the current schedule to avoid worsening the current working one
		Schedule pushedSchedule = this.clone();
		// make a reference to exactly the slot to be broken up in the copied schedule
		// try to push all players individually to other slots
		for (Player player :  pushedSchedule.slots.get(slotId).players.values()) {
			// define reach of a push
			int pushLevel = 6;
			boolean pushSuccessful = pushedSchedule.pushSinglePlayer(player.playerNr, slotId, pushLevel);
			
			// the following block is activated for conditional pushs i.e. where single pushs can only be performed if all players can be relocated
			// for the default case, however, any feasible push is executed immediately!
// ---
//			// if any of the pushes cannot be performed, the entire slot break up is cancelled bc leaving smaller groups is worse than
//			// leaving bigger ones also if they cannot be broken up. Instead, they may be extended with other additional players for sizes 1 or 2
//			// --> the pushedSchedule is not used and the original one remains as active schedule
//			if (!pushSuccessful) {
//				return false;
//			}
// ---
		}
		// if all pushes were successful, take pushedSchedule and set as active schedule for further refinement process
		this.copyFromSchedule(pushedSchedule);			
		return true;
	}



	// can change actual schedule within this method
	// if it fails, just return false and the calling code will consider the push attempt obsolete and will keep using the old code
	// IMPORTANT: The underlying schedule is only modified (synced) if a push can successfully be performed. Else, it remains unaltered!
	public boolean pushSinglePlayer(Integer playerNr, Integer slotId, Integer pushLevel) {
		
		Schedule tempSchedule = this.clone();
		
		// if too many levels of the push tree have been attempted
//		System.out.println("Push level = "+pushLevel);
		if (pushLevel == 0) {
//			System.out.println("Failed to push within pushLevel limits.");
			return false;
		}
		
		// build reference to exactly the slot and player to be pushed
		Integer slotSize = this.slots.get(slotId).players.size();
//		Player player = slot.players.get(playerNr);
		
		// try to push to potential other slots (put slots into a list that is shuffled for random order of attempting slots)
		Boolean pushSuccessful = false;
		if (slotSize==1 || slotSize==2) {
			for (int pushGroupSize : Arrays.asList(3,4,2,1)) {
				pushSuccessful = tempSchedule.pushSinglePlayerToGroupWithSizeX(slotId, playerNr, pushGroupSize, pushLevel);				
				if (pushSuccessful) {
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
		}
		else if (slotSize==3) {
			for (int pushGroupSize : Arrays.asList(3,4,2)) {
				pushSuccessful = tempSchedule.pushSinglePlayerToGroupWithSizeX(slotId, playerNr, pushGroupSize, pushLevel);				
				if (pushSuccessful) {
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
		}
		else if (slotSize==4) {
			for (int pushGroupSize : Arrays.asList(3)) {
				pushSuccessful = tempSchedule.pushSinglePlayerToGroupWithSizeX(slotId, playerNr, pushGroupSize, pushLevel);				
				if (pushSuccessful) {
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
		}
		else if (slotSize==5) {
			for (int pushGroupSize : Arrays.asList(3,4)) {
				if (pushGroupSize==3) {
//					System.out.println("Attempting to push player from G5 into a G3");					
				}
				else {
//					System.out.println("Attempting to push player from G5 into a G4");
				}
				pushSuccessful = tempSchedule.pushSinglePlayerToGroupWithSizeX(slotId, playerNr, pushGroupSize, pushLevel);				
				if (pushSuccessful) {
//					System.out.println("Push successful!");
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
//			System.out.println("Push attempts were unsuccessful!");
		}
		
		// if the player could not be pushed to any other slot
		return false;
	}
	

	public Boolean pushSinglePlayerToGroupWithSizeX(Integer slotId, Integer playerNr, int pushGroupSize, int pushLevel) {
		
		// make a temporary copy, which is modified in the process --> If successfully modified, the initial schedule is synced with the modified one
		// this avoids messing up the initial schedule or ending in a concurrentModificationException due to implicit referencing
		Schedule workingSchedule = this.clone();
		
		// build reference to exactly the slot and player to be pushed
		Slot slot = workingSchedule.slots.get(slotId);
		Player player;
		// through the implicit loops a player might no more be within a group (return false instead of proceeding with a null pointer)
		if (slot.players.containsKey(playerNr)) {
			player = slot.players.get(playerNr);			
		}
		else {
			return false;
		}
		
		List<Map.Entry<Integer,Slot>> slotList = new ArrayList<Map.Entry<Integer,Slot>>(workingSchedule.slots.entrySet());
		Collections.shuffle(slotList);
		for(Map.Entry<Integer,Slot> entry: slotList) {
			Slot otherslot = entry.getValue();
			// do not push to parallel slot (same slot or slot at same day and time on another court)
			if (otherslot.isSameTimeAndDay(slot)) {
				continue;
			}
			if (otherslot.players.size()!=pushGroupSize) {
				continue;
			}
			if (otherslot.players.size() < 4) {
				if (otherslot.acceptsPlayer(player, 2)) {
					otherslot.players.put(player.playerNr,player);
					slot.players.remove(player.playerNr);
					System.out.println("Managed to shift a larger group into a G<4");
					this.copyFromSchedule(workingSchedule);
					return true;
				}
			}
			else if (otherslot.players.size() == 4) {
				// if all other 4 players do not accept this player, it might be an option to kick out another player (-> push) before adding new player
				// kick out obviously only possible if consecutive push is possible for the latter!
				if (otherslot.groupVirtuallyAcceptsPlayer(player)) {
					// remove player from old slot and push into new one to make it 5 players in that group
					slot.players.remove(player.playerNr);
					otherslot.players.put(player.playerNr,player);
//					System.out.println("Made a group of 5!");					
					// have too many players now --> must push one to another group now!
					Iterator<Player> otherPlayerIter = otherslot.players.values().iterator();
					while(otherPlayerIter.hasNext()) {
						Player otherPlayer = otherPlayerIter.next();
						if (player.playerNr != otherPlayer.playerNr) {
//							System.out.println("Push Level = "+pushLevel);
							boolean pushSuccessful = workingSchedule.pushSinglePlayer(otherPlayer.playerNr, otherslot.slotId, pushLevel-1);
							// if push successful, pushSinglePlayer will have modified schedule
							if (pushSuccessful) {
								this.copyFromSchedule(workingSchedule);
//								System.out.println("Insertion was success");
								return true;
							}
							// if push of that player fails, try next player
							else {
//								System.out.println("Insertion was no success. Trying to relocate next player in group.");
								continue;
							}
						}
						else {
//							System.out.println("Insertion was no success. Was trying to relocate same player.");
						}
					}
					slot.players.put(player.playerNr,player);
					otherslot.players.remove(player.playerNr);
//					System.out.println("Reversed group of 5!");
				}
			}
		}
		return false;
	}

	public void extendSmallGroups(int groupSize, int maxPullLevel) {
//		System.out.println("Attempting filling of groups with size="+groupSize);
		// instead of // for (Slot timeslot : this.slots.values()) { // make list of slotNrs to be checked
		// to avoid concurrentModException when modifying the schedule in this.pushPlayers
		List<Integer> slotIds = new ArrayList<Integer>();
		for (Integer slotId : this.slots.keySet()) {
			slotIds.add(slotId);
		}
		for (Integer slotId : slotIds) {
			if (!this.slots.containsKey(slotId)) {
				continue;
			}
			Slot originalSlot = this.slots.get(slotId);
			if (originalSlot.players.size() == groupSize) {
				// -------------------------------------------------
				// changes this (schedule) directly within method
//				System.out.println("Attempting filling of slot: "+Slot.dayNr2Name(originalSlot .weekdayNr)+" "+originalSlot .time+"h Court"+originalSlot .courtNr);
				
				Schedule pulledSchedule = this.clone();
				boolean pullSuccessul = pulledSchedule.pullPlayers(slotId, maxPullLevel, groupSize, false);
				if (pullSuccessul) {
					this.copyFromSchedule(pulledSchedule);
//					System.out.println("Slot successfully filled with other players");
				}
				else {
//					System.out.println("Filling failed for this slot.");					
				}
			}
		}
	}

	private boolean pullPlayers(Integer slotId, int maxPullLevel, int groupSize, boolean hardSuccessCondition) {
		// make a temporary copy, which is modified in the process --> If successfully modified, the initial schedule is synced with the modified one
		// this avoids messing up the initial schedule or ending in a concurrentModificationException due to implicit referencing
		Schedule pulledSchedule = this.clone();

		// build reference to exactly the slot and player to be pulled
		boolean pullSuccess = false;
		if(groupSize == 1) {
			// if single player slot can be broken up, return for sure p_continue=0.0
			pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 1, 0.0, false);
			if (pullSuccess) {
				this.copyFromSchedule(pulledSchedule);
				return pullSuccess;
			}
			else {
				// if double player slot is broken up leaving a single player in the group, repeat the process for the new single group with p=0.9
				// the result is probably again another single player group, but it shuffles around the players to open up new options for other redistributions
				pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 2, 0.9, false);				
				if (pullSuccess) {
					this.copyFromSchedule(pulledSchedule);
					return pullSuccess;
				}
				else {
					return pullSuccess;
				}
			}
		}
		else if(groupSize == 2) {
			// if single player slot can be broken up, return for sure p_continue=0.0
			pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 1, 0.0, false);
			if (pullSuccess) {
				this.copyFromSchedule(pulledSchedule);
				return pullSuccess;
			}
			else {
				pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 2, 0.9, false);
				if (pullSuccess) {
					this.copyFromSchedule(pulledSchedule);
					return pullSuccess;
				}
				else {
					// if a group of 2 pulls a player from a group of 4, it is only allowed if the group of now 3 can be restored to 4 again in consecutive pulls
					// --> hardSuccessCondition = true and try consecutive pulls with p=1.0
					pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 4, 1.0, true);
					if (pullSuccess) {
						this.copyFromSchedule(pulledSchedule);
						return pullSuccess;
					}
					else {
						return pullSuccess;
					}
				}
			}
			
		}
		else if(groupSize == 3) {
			pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 1, 0.0, false);
			if (pullSuccess) {
				this.copyFromSchedule(pulledSchedule);
				return pullSuccess;
			}
			else {
				pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 2, 1.0, false);
				if (pullSuccess) {
					this.copyFromSchedule(pulledSchedule);
					return pullSuccess;
				}
				else {
					// if a group of 3 pulls a player from a group of 4, generally no hardSuccessCondition is required as a new group of 4 results inherently
					// it is only true if the argument was given by a group of 2 pulling from a group of 4 and therefore yielding a new group of 3 (this one)
					// it adds diversity by shuffling the larger groups and may even fill up the reduced group to 4 again in a consecutive step
					// p=0.99 to allow several consecutive search levels of trying to fill up reduced group of now 3 players
					pullSuccess = pulledSchedule.pullNextPlayer(slotId, maxPullLevel, 4, 0.99, hardSuccessCondition);
					if (pullSuccess) {
						this.copyFromSchedule(pulledSchedule);
						return pullSuccess;
					}
					else {
						return pullSuccess;
					}
				}
			}
			
		}
		else {
			return false;			
		}
	}
	
	public boolean pullNextPlayer(Integer slotId, Integer pullLevel, int pullSlotSize, double pContinuePull, boolean hardSuccessCondition) {
		
		// if too many levels of the pull tree have been attempted
		if (pullLevel == 0) {
//			System.out.println("Failed to pull within pullLevel limits.");
			return false;
		}
		
		Slot slot = this.slots.get(slotId);
		
		// try to pull from potential other slots (put slots into a list that is shuffled for random order of attempting slots)
		List<Integer> otherSlotIds = new ArrayList<Integer>();
		for (Integer otherSlotId : this.slots.keySet()) {
			otherSlotIds.add(otherSlotId);
		}
		Collections.shuffle(otherSlotIds);
		for (Integer otherslotId : otherSlotIds) {
			if (!this.slots.containsKey(otherslotId)) {
				continue;
			}
			Slot otherslot = this.slots.get(otherslotId);
			// do not push to parallel slot (same slot or slot at same day and time on another court)
			if (otherslot.isSameTimeAndDay(slot)) {
				continue;
			}
			if (otherslot.players.size() == pullSlotSize) {
				for (Player player : otherslot.players.values()) {
					if (slot.acceptsPlayer(player, 2)) {
						Schedule initialBackupSchedule = this.clone();
						slot.players.put(player.playerNr, player);
						otherslot.players.remove(player.playerNr);
						if (pContinuePull < new Random().nextDouble()) {
							Schedule pulledSchedule = this.clone();
							boolean consecutivePullSuccessul = pulledSchedule.pullPlayers(otherslot.slotId, pullLevel-1, otherslot.players.size(), hardSuccessCondition);
							if (consecutivePullSuccessul) {
								this.copyFromSchedule(pulledSchedule);
							}
							// in case a player was pulled from a group of four into a group of two
							// --> Pull is only permitted if the subsequent pull into the reduced group of three is successful to make it a group of four again
							if (hardSuccessCondition && !consecutivePullSuccessul) {
								// restore backupSchedule before pulling from group of four and continue to next player instead of returning
								this.copyFromSchedule(initialBackupSchedule);
								continue;
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private void copyFromSchedule(Schedule scheduleToCopyFrom) {
		this.slots.clear();
		for (Slot slot : scheduleToCopyFrom.slots.values()) {
			this.slots.put(slot.slotId, slot.clone());
		}
	}

	public void calculateEfficiency() {
		Map<Integer,Integer> sizeBins = new HashMap<Integer,Integer>(6);
		for (int i=0; i<6; i++) {
			sizeBins.put(i, 0);
		}
		for (Slot slot : this.slots.values()) {
			int size = slot.players.size();
			if (size > 5) {
				sizeBins.put(5,sizeBins.get(5)+1);
			}
			else {
				sizeBins.put(size,sizeBins.get(size)+1);
			}
		}
		System.out.println(sizeBins.toString());
	}

}
