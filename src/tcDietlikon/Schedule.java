package tcDietlikon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Schedule {

	Map<Integer,Slot> slots = new HashMap<Integer,Slot>();
	Map<Integer,Player> players = new HashMap<Integer,Player>();
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
		for (int day=1; day<=nDays; day++) {
			for (int hour=firstHour; hour<=lastHour; hour++) {
				for (int court=1; court<=nCourts; court++) {
					int slotId = this.dayTimeCourt2slotId(day, hour, court);
					slots.put(slotId, new Slot(slotId, day, hour, court));
				}
			}
		}
	}

	public int dayTimeCourt2slotId(Integer day, Integer time, Integer court) {
		Integer slotId = 	(day-1)*(this.lastHour-this.firstHour+1)*(this.nCourts) + (time-this.firstHour)*(this.nCourts) + (court);
		return slotId;
	}
	
	public Integer slotId2day(Integer slotId) {
		Integer day = 0 + Math.floorDiv(slotId , (this.lastHour-this.firstHour+1)*(this.nCourts));
		return day;
	}
	
	public Integer slotId2time(Integer slotId) {
		Integer time = this.firstHour + Math.floorDiv(slotId % (this.lastHour-this.firstHour+1)*(this.nCourts), (this.nCourts));
		return time;
	}
	
	public Integer slotId2court(Integer slotId) {
		Integer court = 0 + (slotId % this.nCourts);
		// CAUTION: If slotId is a multiple of 3, the modulus is 0, which is to conform to court 3 --> make extra rule!
		if (court==0) {
			court = 3;
		}
		return court;
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

		for (Integer d=1; d<=this.nDays; d++) {
			for (int r=firstHourRow; r<=firstHourRow+(this.lastHour-this.firstHour); r++) {
				Integer h = Integer.parseInt(dataFormatter.formatCellValue(courtSheet.getRow(r).getCell(0)));
				for (Integer c=1; c<=this.nCourts; c++) {
					if (dataFormatter.formatCellValue(courtSheet.getRow(r).getCell((d-1)*this.nCourts+c)).equals("x")) {
						int slotId = this.dayTimeCourt2slotId(d, h, c);						
						this.slots.put(slotId, new Slot(slotId, d, h, c));
//						System.out.println("Day/Hour/Court = "+d+"/"+h+"/"+c);
					}
				}
			}
		}

	}
	
	
	public static Schedule initializeSchedule(Map<Integer, Player> players, String courtScheduleFile, int initialPlacementStrategy,
			String fixedGroupsFile, boolean useFixedSlotFile, boolean useFullSlotFilling)
			throws EncryptedDocumentException, InvalidFormatException, IOException {
		
		Schedule schedule = new Schedule(courtScheduleFile); 	// initializes schedule with slots when courts are free (see excel file)
		schedule.players.putAll(players);
		List<Player> playersSortedByPossibleCombinations = PlayerUtils.sortByPossibleCombinations(schedule.players);
		// load fixed groups and courts into a list of fixed slots
		if (useFixedSlotFile) {
			schedule.setFixedSlotsAndPlayers(fixedGroupsFile); 
		}
		// it can be that the same person is featured twice in the initial player list as they register e.g. for private and group lessons
		// --> mark the same person profiles so that one profile knows when the other one plays and does not make double day assignments
		schedule.makeSamePlayersReferences();
		
		
	// try initial placement of directly filling a min. number of players in the same slot (for a min. number always start filling least demanded slot)
		if (useFullSlotFilling) {
			List<Slot> slotsRankedByDemand = schedule.sortSlotsByPlayerDemand(players);	// lowest demand first (slots with same time/day come right after each other)
			List<Slot> slotsRankedByDemandReverse = new ArrayList<Slot>();
			for (Slot slot : slotsRankedByDemand) {
				slotsRankedByDemandReverse.add(0, slot);
			}
			System.out.println("slotsRankedByDemand.size() = "+slotsRankedByDemand.size());
			schedule.markSlotsWithDesirablePlayers(players);
			int maxGroupSizeOverall = 8;
			// take maxSize=8 players first and try to build full slots with 8 players
			// --> then take maxSize=7 players and try to build full slots with those 7 players
			// --> continue trying to fill entire slots at once down to single player slots (in every round start with the least demanded slot)
			// after trying to fill slots entirely in one shot, loosen condition and allow one space to remain free
			// after that, advance to two free spaces and so on
			// note (as can be seen in the actual method slot.fillWithPlayers()) that only players of the same maxGroupSize are put together for these placements
			// denotes how many spaces are allowed to remain free for a successful fill
//		for (int fullnessGoalPlacementRound = 0; fullnessGoalPlacementRound<=maxGroupSizeOverall-1; fullnessGoalPlacementRound++) {
			for (int fullnessGoalPlacementRound = 0; fullnessGoalPlacementRound<=1; fullnessGoalPlacementRound++) {	// XXX <=0 means only directly full groups
				// denotes players that can be filled (maxGroupSize)
				for (int maxGroupSizePlacementRound=maxGroupSizeOverall; maxGroupSizePlacementRound>3; maxGroupSizePlacementRound--) {
					// fill to at least one player!
					if ( maxGroupSizePlacementRound-fullnessGoalPlacementRound < 1) {
						continue;
					}
					List<Slot> rankedSlots = new ArrayList<Slot>();
					if (fullnessGoalPlacementRound==0) {
						rankedSlots = slotsRankedByDemand;
					}
					else {
						rankedSlots = slotsRankedByDemandReverse;
					}
					for (Slot slot : slotsRankedByDemand) {
						if (slot.getSize()>0) {	// only fill into empty slots
							continue;
						}
						boolean fillSuccessful = slot.fillWithPlayers(fullnessGoalPlacementRound, maxGroupSizePlacementRound, schedule);
						// in the above method, the algorithm tries to place mustBeTogetherPeers together
						// if it fails, the single players can still be pushed individually below and have not yet been set frozen together in the mutual group
					}
				}
			}
		}
		
		int unsuccessfulPlacements = 0;
		// Fill in players with max group size 4 before more limited group sizes: G4 before G3 before G2 before G1 (players with maxGroupSize G"X")
		for (int maxGroupSizePlacementRound : Arrays.asList(8,7,6,5,4,3,2,1)) {
			for (Player player : playersSortedByPossibleCombinations) { // for (Player player : PlayerUtils.reversePlayerList(playersSortedByPossibleCombinations)) {
				// place player in this round only if it has the corresponding maxGroupSize. Else, wait until it hits correct groupSizeCategory
				if (player.maxGroupSize!=maxGroupSizePlacementRound) {
					continue;
				}
				// use strategy 2 or lower to consider only desired slots and all other constraints
				Integer unsuccessfulPlacementsThisPlayer = schedule.placePlayer(player, 2, initialPlacementStrategy);	// XXX 2 instead of 3
				// in the above method, the algorithm tries to place mustBeTogetherPeers together
				// if it fails, the single players can still be pushed individually below and have not yet been set frozen together in the mutual group
				// if player could not be assigned to enough slots with placement strategies, this is a problem
				// --> try to make a push and reassign another player in order to fit both slots
				// --> make sure an undesirable placement (in schedule.placePlayer) is always marked within the player
				// try the push for all undesirable placements
				
				// first all slots which have been assigned in unsatisfying manner
				for (Slot undesirableSlot : player.undesirablePlacements) {
					Schedule tempSchedule = schedule.clone();
					boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
							player.playerNr, false, undesirableSlot.slotId, undesirableSlot.clone(), 5, false, 0, false);	// XXX maybe set TRUE here to overfill
					if (pushSuccessful) {
						System.out.println("Managed to push player from undesirable slot into desirable one");
						schedule.copyFromSchedule(tempSchedule);
						player = schedule.players.get(player.playerNr);
					}					
				}
				
				
//				check if this is really not possible to push any players! Make comments and prints
				
				// then as many slots as have not yet been assigned for this player
				int nSlotsNotYetAssigned = player.nSlots-player.selectedSlots.size();
				for (int n=1; n<=nSlotsNotYetAssigned; n++) {
					Schedule tempSchedule = schedule.clone();
					boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
							player.playerNr, true, null, null, 5, false, 0, false);	// XXX maybe set TRUE here to overfill
					if (pushSuccessful) {
						System.out.println("Managed to push place player by reassigning other one");
						schedule.copyFromSchedule(tempSchedule);
						player = schedule.players.get(player.playerNr);
					}					
				}
				// then try the push for all failed placements (not even undesired one)
//				for ()
				// --> either loosen placement strategy and try again or assign manually at the end
				// in any case, the player is marked so that the unsatisfied players can be tracked!
				int nSlotsStillNotYetAssigned = schedule.players.get(player.playerNr).nSlots-schedule.players.get(player.playerNr).selectedSlots.size();
				if (nSlotsStillNotYetAssigned > 0) {
					unsuccessfulPlacements += nSlotsStillNotYetAssigned;
					// System.out.println("Number of unsuccessful placements = " + unsuccessfulPlacements);
					player.slotNrSatisfied = false;
					for (Player subplayer : player.subPlayerProfiles) {
						subplayer.slotNrSatisfied = false;
					}
				}
//				if (unsuccessfulPlacements > 0) {
//					unsuccessfulPlacements += unsuccessfulPlacementsThisPlayer;
//					player.slotNrSatisfied = false;
//				}
			}
		}
		for (Player player : schedule.players.values()) {
			if (Arrays.asList(3, 4, 5).contains(player.worstPlacementRound)) {
				System.out.println("Strategy for player " + player.playerNr + " = " + player.worstPlacementRound);
			}
		}
		return schedule;
	}
	
	private void makeSamePlayersReferences() {
		// it can be that the same person is featured twice in the initial player list as they register e.g. for private and group lessons
		// --> mark the same person profiles so that one profile knows when the other one plays and does not make double day assignments
		for (Player player : players.values()) {
			for (Player subPlayer : player.subPlayerProfiles) {
				for (Player otherPlayer : players.values()) {
					if (player.equals(otherPlayer)) {
						continue;
					}
					for (Player otherSubPlayer : otherPlayer.subPlayerProfiles) {
						if (otherSubPlayer.name.equals(subPlayer.name)) {
							// CAUTION: add otherPlayer and not otherSubPlayer bc the latter does not carry the up-to-date selectedSlots while otherPlayer does
							subPlayer.samePersonPlayerProfiles.add(otherPlayer.playerNr);
							// add only for this player --> this way can avoid double adding when same player combo is hit by switching inner and outer loop
						}
					}
				}				
			}
		}
	}

	public void markSlotsWithDesirablePlayers(Map<Integer, Player> players) {
		for (Slot slot : this.slots.values()) {
			for (Player player : players.values()) {
				if (player.isADesiredSlot(slot)) {
					slot.desirablePlayers.add(player);
				}
			}
		}
		return;
	}

	public List<Slot> sortSlotsByPlayerDemand(Map<Integer, Player> players) {
		Schedule tempSchedule = new Schedule(this.nCourts, this.nDays, this.firstHour, this.lastHour);
		Map<Integer,Integer> slotFrequencies = new HashMap<Integer,Integer>();
		// count player demand for a slot at a specific day/time -> choose court1 for a slotId
		// --> the other slots at the same time/day on other courts will have the same demand and will be added later (copied demand from court1)
		for (Player player : players.values()) {
			for (Slot slot : player.desiredSlots) {
				int slotId = tempSchedule.dayTimeCourt2slotId(slot.weekdayNr, slot.time, 1);
				if (slotFrequencies.containsKey(slotId)) {
					slotFrequencies.put(slotId, slotFrequencies.get(slotId)+player.subPlayerProfiles.size());
					// add not just one for frequency, but amount of subprofiles attached to this player
				}
				else {
					slotFrequencies.put(slotId, player.subPlayerProfiles.size());
				}
			}
		}
		System.out.println(slotFrequencies.toString());
		// sort the slots by their demand frequency
		// --> LEAST demanded slot first
		List<Integer> sortedSlotIds = new ArrayList<Integer>();
		for (Entry<Integer,Integer> entry : slotFrequencies.entrySet()) {
			int slotId = entry.getKey();
			int freq = entry.getValue();
			if (sortedSlotIds.size()==0) {
				sortedSlotIds.add(slotId);
				continue;
			}
			else {
				int position = 0;
				for (Integer sortedSlotId : sortedSlotIds) {
					if (freq < slotFrequencies.get(sortedSlotId)) {	// XXX
						sortedSlotIds.add(position, slotId);
						position++;					
						break;
					}
					position++;
					if (position==sortedSlotIds.size()) {
						sortedSlotIds.add(slotId);
						break;
					}
				}							
			}
		}
		System.out.println(sortedSlotIds.toString());
		// add also other slots at same time/day as they have the same demand
		// --> make sure those slots and the initial one from court 1 actually exist
		List<Slot> sortedSlots = new ArrayList<Slot>();
		for (Integer tempId : sortedSlotIds) {
			int day = this.slotId2day(tempId);
			int time = this.slotId2time(tempId);
			// check what day/time this sorted slot conforms to
			for (int c=1; c<=this.nCourts; c++) {
				// find all slots (courts) that exist in the schedule on the same time/day
				int preciseId = this.dayTimeCourt2slotId(day, time, c);
				if (this.slots.containsKey(preciseId)) {
					Slot slot = this.slots.get(preciseId);
					sortedSlots.add(slot);					
				}
			}
		}
		
		for (Slot slot : sortedSlots) {
			System.out.println(slot.slotId);			
		}
		return sortedSlots;
	}

	private void setFixedSlotsAndPlayers(String fixedGroupsFile) throws EncryptedDocumentException, InvalidFormatException, IOException {
		
		// FOR OLD VERSION SEE BEFORE 14.03.2019 (GitHub)
		
		// load court schedules from excel file
		Workbook workbook = WorkbookFactory.create(new File(fixedGroupsFile));
		Sheet fixedSlotsSheet = workbook.getSheetAt(0);
		DataFormatter dataFormatter = new DataFormatter();

		// LOAD
		for (int r = 1; r <= fixedSlotsSheet.getLastRowNum(); r++) {
			if (!dataFormatter.formatCellValue(fixedSlotsSheet.getRow(r).getCell(0)).equals("")) {
				// read slot with d,t,c
				int day = Integer.parseInt(dataFormatter.formatCellValue(fixedSlotsSheet.getRow(r).getCell(0)));
				int time = Integer.parseInt(dataFormatter.formatCellValue(fixedSlotsSheet.getRow(r).getCell(1)));
				int court = Integer.parseInt(dataFormatter.formatCellValue(fixedSlotsSheet.getRow(r).getCell(2)));
				Slot slot = this.slots.get(this.dayTimeCourt2slotId(day, time, court));
				slot.isFrozen = true;
				// System.out.println("Adding frozen slot "+slot.weekdayNr+" "+slot.time+" "+slot.courtNr);
				
				// now go through all players placed in this fixed slot and add them as a player union!
				List<Player> thisFixedSlotPlayerUnion = new ArrayList<Player>();
				for (int rp = r; rp <= fixedSlotsSheet.getLastRowNum(); rp++) {
					// after the first player in the slot (rp>r) go down players for this slot. stop when new slot starts and therefore player cell is empty
					if (rp>r && !dataFormatter.formatCellValue(fixedSlotsSheet.getRow(rp).getCell(0)).equals("")) {
						break;
					}
					else {
						String fixedPlayerName = dataFormatter.formatCellValue(fixedSlotsSheet.getRow(rp).getCell(3));
						Player fixedPlayer = new Player(fixedPlayerName, false); // does not need reference to itself as will be placed in union as subplayer
						fixedPlayer.playerNr = rp;
						fixedPlayer.nSlots=1;
						// no need to add slot to desired/selectedSlots as it is added to the mergedPlayer below
						// fixedPlayer.desiredSlots.add(slot);	
						// fixedPlayer.selectedSlots.add(slot);
						fixedPlayer.isFrozen = true;
						thisFixedSlotPlayerUnion.add(fixedPlayer);
						// System.out.println("Adding player "+fixedPlayer.name+" "+fixedPlayer.playerNr);
					}
				}
				Player mergedPlayer = new Player(false);
				for (Player subPlayer : thisFixedSlotPlayerUnion) {
					mergedPlayer.subPlayerProfiles.add(subPlayer);
					subPlayer.maxGroupSize = thisFixedSlotPlayerUnion.size();
				}
				mergedPlayer.desiredSlots.add(slot);
				mergedPlayer.addSelectedSlot(slot);
				mergedPlayer.setNameFromSubprofiles();
				mergedPlayer.playerNr = PlayerUtils.searchHighestPlayerNr(players)+1;
				mergedPlayer.nSlots = 1;
				mergedPlayer.maxGroupSize = thisFixedSlotPlayerUnion.size();
				mergedPlayer.isFrozen = true;
				slot.addPlayer(mergedPlayer.playerNr, mergedPlayer);
				this.players.put(mergedPlayer.playerNr, mergedPlayer);
			}
			else {
				continue;
			}
		}
	}

	public Integer placePlayer(Player player, int strategy, int initialPlacementStrategy) {
	// make rules when placing player
		
		// ORDER OF PLACING PLAYERS
		// 1st RUN: Try to place players within their desired slots
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
			Integer unsuccessfulPlacementsThisPlayerAndRound = placePlayer(player, strategy-1, initialPlacementStrategy);
			if (unsuccessfulPlacementsThisPlayerAndRound==0) {
				return 0; // the number of desired slots has been assigned to the player. the initial placement of this player is complete
			}
			else {
				// do not return yet and try to assign with the current strategy level. if this is not possible either, will have to move one strategy level higher!
				// the below routine will now be attempted
			}
		}
		
		// INITIAL PLAYER PLACEMENT
		// a] G4 before G3 before G2 before G1 (players with maxGroupSize G"X")
		//		--> between two same GX, first choose the one of more rare/frequent category (usually G,O,R), then choose the one with lower/higher linkability
		// b] Fill a G3 first into a G3 before a G4
		// c] Fill into largest possible group! (Strategy 0 makes sure only activated slots are used if feasible i.e. min. 1 player)
		// d] Maybe use Monday, Tuesday, Thursday first (see real registrations if Wednesday & Friday are really much more desirable)
		// e] Use earliest slots every day!
		//
		
		Integer unsuccessfulPlacementsThisPlayerAndRound = 0;
		// try to assign to player as many slots as it still needs to satisfy its number of desired lessons (#desiredSlots-#alreadySelectedSlots)
		int nrOfNotYetAssignedSlots = player.nSlots-player.selectedSlots.size();
		for (int s=1; s<=nrOfNotYetAssignedSlots; s++) {	// 		for (int s=1; s<=player.nSlots-player.selectedSlots.size(); s++) {
//			if (initialPlacementStrategy==3) {
//				continue;
//			}
			Slot currentlyOptimalSlot = null;
//					List<Slot> currentlyLargestFeasibleSlots = new ArrayList<Slot>();
			// b] Fill in a G3 first into a G3 before a G4 --> start by filling into groups with same maxGroupSize.
			//    If not possible, advance to groups with higher capacity.
			for (int receiverGroupSizeLimit=player.maxGroupSize; receiverGroupSizeLimit<=8; receiverGroupSizeLimit++) {
				for (Slot slot : this.slots.values()) {
					if (slot.isFrozen) {
						continue;
					}
					// b] Find limiting player for maxGroupSize. Make sure to fill in group only if permitted by current
					//    placement round of receiverGroupSizeLimit with the conditions above
					if (receiverGroupSizeLimit != slot.limitingPlayerMaxGroupSize()) {
						continue;
					}
					if (slot.acceptsPlayer(player, strategy, false, this, true)) {
						// if first acceptable slot found
						if (currentlyOptimalSlot == null) {
							currentlyOptimalSlot = slot;
						}
						// if can be added to a fuller slot
						if (slot.getSize()>currentlyOptimalSlot.getSize()) {
							currentlyOptimalSlot = slot;
							continue;
						}
						// if same full slot, check if other slot properties are better
						else if(slot.getSize()==currentlyOptimalSlot.getSize()) {
							// if day is preferable (usually Mo/Tu/Th > Fr > We) to keep high demand days as available as possible!
							// --> or maybe it is exactly opposite
							if (slot.isPreferableDayToOtherSlot(currentlyOptimalSlot,initialPlacementStrategy)) {
								currentlyOptimalSlot = slot;
							}
							else if (slot.dayIsSamePreferenceAsOtherSlot(currentlyOptimalSlot)) {
								// the earlier, the better --> or maybe opposite!
								if (initialPlacementStrategy==1 && slot.time<currentlyOptimalSlot.time) {
									currentlyOptimalSlot = slot;
								}
								else if (initialPlacementStrategy==2 && slot.time>currentlyOptimalSlot.time) {
									currentlyOptimalSlot = slot;
								}
								else if (slot.time==currentlyOptimalSlot.time){
									// beneficial if not having to use many parallel courts
									if (slot.courtNr<currentlyOptimalSlot.courtNr) {
										currentlyOptimalSlot=slot;
									}
									else {
										continue;
									}
								}
								else {
									continue;
								}
							}
							else {
								continue;
							}
						}
						// slot is not better than current one
						else {
							continue;
						}
					}
				}				
			}
			if (currentlyOptimalSlot != null) {
				if (currentlyOptimalSlot.category.equals("empty")) {
					currentlyOptimalSlot.category = player.category;
				}
				currentlyOptimalSlot.addPlayer(player.playerNr, player);
				player.addSelectedSlot(currentlyOptimalSlot);
				if (strategy>player.worstPlacementRound) {
					player.setWorstPlacementRound(strategy);
				}
				if (strategy>2) {
					player.addUndesirablePlacement(currentlyOptimalSlot);
				}
				// if player has mustHavePeers, set to frozen and do all addings for the must have peers!
				if (player.frozenSameGroupPeers.size() > 0) {
					player.isFrozen = true;
					for (int mustHavePeerNr : player.frozenSameGroupPeers) {
						Player mustHavePeer = this.players.get(mustHavePeerNr);
						mustHavePeer.isFrozen = true;
						currentlyOptimalSlot.addPlayer(mustHavePeer.playerNr, mustHavePeer);
						mustHavePeer.addSelectedSlot(currentlyOptimalSlot);
						if (strategy>mustHavePeer.worstPlacementRound) {
							mustHavePeer.setWorstPlacementRound(strategy);				
						}
						if (strategy>2) {
							mustHavePeer.addUndesirablePlacement(currentlyOptimalSlot);
						}
					}					
				}
				// ---
				continue;				
			}
			else {
				unsuccessfulPlacementsThisPlayerAndRound++;
				// No need to place player in undesirable slot here, bc a higher number strategy will allow to do so when the loop moves one strategy higher
				// it will inherently move one number higher bc the output of this method is higher than 0 indicating that not all desired nSlots could be assigned
				
				// X may delete this entire thing
				// place the unsuccessful player in an early monday slot on a not active court for reassignement
//				for (int day=0; day<=4; day++) {
//					Slot slot = this.slots.get(1+day*3);
//					if (slot.getSize()>=8) {
//						continue;
//					}
//					else {
//						slot.addPlayer(player.playerNr, player);
//						player.addSelectedSlot(slot);
//						player.undesirablePlacements.add(slot);
//					}
//				}
			}
		}
		return unsuccessfulPlacementsThisPlayerAndRound;
	}

	

	public int slot2row(int time) {
		return 2+(time-this.firstHour)*9;
	}

	public int slot2col(int day, int court) {
		return (day-1)*this.nCourts*4 + (court-1)*4;
	}

	public String slot2name(int day, int time, int court) {
		String slotName = (Slot.dayNr2Name(day))+" - "+time+"h - Court "+court;
		return slotName;
	}

	
	
	
	public Schedule clone() {
		Schedule copy = new Schedule();
		for (Entry<Integer, Slot> slot : this.slots.entrySet()) {
			copy.slots.put(slot.getKey(), slot.getValue().clone());
		}
		for (Entry<Integer, Player> player : this.players.entrySet()) {
			copy.players.put(player.getKey(), player.getValue().clone());
		}
		copy.firstHour = this.firstHour;
		copy.lastHour = this.lastHour;
		copy.nCourts = this.nCourts;
		copy.nDays = this.nDays;
		return copy;
	}

	
	public void refine(Map<Integer, Player> players, int pushLevel) {
		// BRAKE UP stands for shifting and pushing players from one group to another group with the goal to fill up groups
		// -> do both simultaneously (for every slot first try shifting, then try pushing)
		// SHIFTING (moving to a group with a free slot)
		// 1. shift to a larger or same size group
		//		a. attempt first shifting from small groups right up to full size=4 groups,
		//		   then go down again with sender size in case larger groups have changed meanwhile and new shifts/pushs are possible!
		//			--> (groupsize = 1,2,3,4,3,2,1)
		//		b. largest receiver first
		//		c. if two receiver candidates have same size, shift to group, where average maxGSize is closer to the maxGSize of the player to be shifted
		// 2. shift to group one size smaller
		//		a. if maxGSize(shifterPlayer)<maxGSize(average(receiverGroup)), shift if:
		//			- sum(maxGSize(receiverGroup)) <= sum(maxGSize(senderGroup))
		//			- min(maxGSize(newGroup)=>GSize(shifterPlayer)
		//		b. if maxGSize(shifterPlayer)>=maxGSize(average(receiverGroup)), shift if:
		//			- sum(maxGSize(receiverGroup)) >= sum(maxGSize(senderGroup)) 
		// 
		// PUSHING (pushing into full group and kicking out another player)
		// 1. push to any size group
		//		a. largest first
		//		b. consecutive pushs into full groups until an ordinary shift can be performed into a group with an open space
		//			--> This shift must be permitted by the shifting rules above


		for (int groupSize : Arrays.asList(1,2,3,4,5,6,7,8,7,6,5,4,3,2,1)) {	// optimal strategy = 1,2,3,4,3,2,1
			System.out.println("SHIFT/PUSH Strategy: Shift/Push Group Size = "+groupSize);
			this.shiftAndPush(groupSize, pushLevel);
		}
		this.calculateEfficiency(this.players, "Schedule efficiency INTERMEDIATE:");
		this.verifyCompliance(players);
		for (int groupSize : Arrays.asList(1,2,3,4,5,6,7,8,7,6,5,4,3,2,1)) {	// optimal strategy = 1,2,3,4,3,2,1
			System.out.println("BREAK Strategy: Shift/Push Group Size = "+groupSize);
			this.breakUpGroups(groupSize, pushLevel);
		}
		this.postOptimization();

//		int maxPullLevel = 5;
//		for (int rounds=0; rounds<3; rounds++) {
//			for (int groupSize : Arrays.asList(1,2,3)) {
//				this.extendSmallGroups(groupSize, maxPullLevel);
//			}
//		}
		// int maxDesirableGroupSize = 4;
		// this.shrinkOverfullGroups(maxDesirableGroupSize);
		
		// we change the schedule and take notice in the player selectedSlots
		// we do this within cloned schedules that refer internally to their players list
		// however, the initially loaded players do not take notice this way and are not up-to-date with their selectedSlots
		// therefore, they are synced with the refined schedule here
		// --> empty original players list and insert the same but updated players from the refined schedule
		players.clear();
		players.putAll(this.players);
	}


	private void postOptimization() {
		// SLOT ASSIGNMENT as for initial placement where it is also permitted to assign to empty slots
		// first try to reassign undesirable slots, then try to assign unassigned slot requests of individual players
		// use this list work-around to avoid ConcurrentModificationException
		List<Integer> playerNrsList = new ArrayList<Integer>();
		playerNrsList.addAll(this.players.keySet());
		for (int playerNr : playerNrsList) {
			Player player = this.players.get(playerNr);
			if (player.isFrozen) {
				continue;
			}
			// first all slots which have been assigned in unsatisfying manner
			for (Slot undesirableSlot : player.undesirablePlacements) {
				// System.out.println("Trying 1");
				Schedule tempSchedule = this.clone();
				boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
						player.playerNr, false, undesirableSlot.slotId, undesirableSlot.clone(), 5, false, 0, false);
				if (pushSuccessful) {
					System.out.println("Managed to push player from undesirable slot into desirable one");
					this.copyFromSchedule(tempSchedule);
					player = this.players.get(player.playerNr);
				}					
			}
			// then as many slots as have not yet been assigned for this player
			int nSlotsNotYetAssigned = player.nSlots-player.selectedSlots.size();
			for (int n=1; n<=nSlotsNotYetAssigned; n++) {
				// System.out.println("Trying 2");
				Schedule tempSchedule = this.clone();
				boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
						player.playerNr, true, null, null, 5, false, 0, false);
				if (pushSuccessful) {
					System.out.println("Managed to push place player by reassigning other one");
					this.copyFromSchedule(tempSchedule);
					player = this.players.get(player.playerNr);
				}					
			}
		}
		
		// MERGE GROUPS (and unplaced player requests)
		// - unplaced slots
		// - undesired slots
		// - any slots
		for (Slot slot : this.slots.values()) {
			// first try to merge two already assigned groups
			for (Slot otherSlot1 : this.slots.values()) {
				if (otherSlot1.getSize()==0) {
					continue;
				}
				for (Slot otherSlot2 : this.slots.values()) {
					if (otherSlot2.getSize()==0) {
						continue;
					}
					// slot may actually by one of the two otherslots. this is also possible w/o modification as it results in a push/pull of an entire slot
					// this is the only case where a not empty slot can be used as a merger location. else, the slot already has players and cannot take another two groups
					if (slot.getSize()!=0 && !slot.equals(otherSlot1) && !slot.equals(otherSlot2)) {
						continue;
					}
					if (otherSlot1.equals(otherSlot2)) {
						continue;
					}
					if (slot.mergerFeasible(otherSlot1, otherSlot2, this)) {
						System.out.println("Merging two slots with sizes "+otherSlot1.getSize()+"/"+otherSlot2.getSize());
						// add slot, player and mark slot with category if it is not the same slot
						if (!slot.equals(otherSlot1)) {
							for (Player player : otherSlot1.players.values()) {
								slot.players.put(player.playerNr, player);
								player.removeSelectedSlot(otherSlot1);
								player.addSelectedSlot(slot);
							}
							otherSlot1.players.clear();
							otherSlot1.category = "empty";
							slot.category = slot.players.values().iterator().next().category;	// assign slot with category of its first player
						}
						if (!slot.equals(otherSlot2)) {
							for (Player player : otherSlot2.players.values()) {
								slot.players.put(player.playerNr, player);
								player.removeSelectedSlot(otherSlot2);
								player.addSelectedSlot(slot);
							}
							otherSlot2.players.clear();
							otherSlot2.category = "empty";
							slot.category = slot.players.values().iterator().next().category;	// assign slot with category of its first player
						}
						break;
					}
					else {
						continue;
					}
				}
			}
			// then try to merge a group with an unassigned player slot
			for (Player player : this.players.values()) {
				int nUnplaced = player.nSlots - player.selectedSlots.size();
				for (int n=1; n<=nUnplaced; n++) {
					for (Slot otherSlot2 : this.slots.values()) {
						// merger receiver slot must be empty except if merger takes place into on of the two source slots (otherSlot1/2)
						if (slot.getSize()!=0 && !slot.equals(otherSlot2)) {
							continue;
						}
						if (slot.mergerFeasible(player, otherSlot2, this)) {
							System.out.println("Merging a slots with unassigned player slot. Slot size = "+otherSlot2.getSize());
							// add slot, player and mark slot with category if it is not the same slot
							slot.players.put(player.playerNr, player);
							player.addSelectedSlot(slot);
							slot.category = player.category;	// assign slot with category of its first player
							if (!slot.equals(otherSlot2)) {
								for (Player player2 : otherSlot2.players.values()) {
									slot.players.put(player2.playerNr, player2);
									player.removeSelectedSlot(otherSlot2);
									player.addSelectedSlot(slot);
								}
								otherSlot2.players.clear();
								otherSlot2.category = "empty";
								slot.category = slot.players.values().iterator().next().category;	// assign slot with category of its first player
							}
							break;
						}
						else {
							continue;
						}
					}					
				}
			}
		}
		
		
		// GROUP SIZE OPTIMIZATION (may make groups of 5 at the very end)
		// -> do this for undesirable, unassigned slots and slots with maxGroupSze=4 but only 1 or 2 players --> note the min. receiver slot size of 4!
		boolean allowOverfullGroups = true;
		for (Player player : this.players.values()) {
			if (player.maxGroupSize!=4) {
				continue;
			}
			// this time allow shifting to form a group of 5
			for (Slot undesirableSlot : player.undesirablePlacements) {
				// System.out.println("Trying 1");
				Schedule tempSchedule = this.clone();
				boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
						player.playerNr, false, undesirableSlot.slotId, undesirableSlot.clone(), 5, false, 4, allowOverfullGroups);
				if (pushSuccessful) {
					System.out.println("Managed to push player from undesirable slot into desirable one");
					this.copyFromSchedule(tempSchedule);
					player = this.players.get(player.playerNr);
				}					
			}
			// ---
			int nSlotsNotYetAssigned = player.nSlots-player.selectedSlots.size();
			for (int n=1; n<=nSlotsNotYetAssigned; n++) {
				// System.out.println("Trying 2");
				Schedule tempSchedule = this.clone();
				boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
						player.playerNr, true, null, null, 5, false, 4, allowOverfullGroups);
				if (pushSuccessful) {
					System.out.println("Managed to push place player by reassigning other one");
					this.copyFromSchedule(tempSchedule);
					player = this.players.get(player.playerNr);
				}					
			}
		}
		// now for groups with maxGroupSize=4 players, but only 1 or two of them
		// make list of slot id references so that loop can be performed with an external list and not actual slots which would give a concurrentModExcpetion
		List<Integer> slotIdList = new ArrayList<Integer>();
		slotIdList.addAll(this.slots.keySet());
		for (int slotId : slotIdList) {
			Slot slot = this.slots.get(slotId);
			if (slot.getSize()>1) {		// XXX assign only single players to already full G4s or also groups with two players
				continue;
			}
			for (Player player : slot.players.values()) {
				if (player.maxGroupSize!=4) {
					continue;
				}
				Schedule tempSchedule = this.clone();
				boolean pushSuccessful = tempSchedule.shiftAndPushSinglePlayer(
						player.playerNr, false, slot.slotId, slot.clone(), 5, false, 4, allowOverfullGroups);
				if (pushSuccessful) {
					System.out.println("Managed to post optimize a G4 player");
					this.copyFromSchedule(tempSchedule);
				}												
			}
		}
	}

	@SuppressWarnings("unused")
	@Deprecated
	private void shrinkOverfullGroups(int maxDesirableGroupSize) {
		int pushLevel = 3;
		List<Integer> slotIdList = new ArrayList<Integer>();
		slotIdList.addAll(this.slots.keySet());
		for (Integer slotId : slotIdList) {
			Slot slot = this.slots.get(slotId);
			if (slot.isFrozen) {
				continue;
			}
			if (slot.getSize()>4) {
//				System.out.println("Trying to shrink slot = "+slotId);
				for (Integer playerNr : slot.players.keySet()) {
					boolean pushSuccessful = this.pushSinglePlayer(playerNr, slot.slotId, pushLevel);
					if (pushSuccessful && slot.getSize()<=4) {
						break;
					}
				}
			}
		}
	}

	// BREAK STRATEGY
	// - break groups if they feature a specific group size
	// - try to move every player individually out of this group
	// - push players to groups with minimum size thisSize-1 (try size 3 first, than push into size 4, then smaller sizes)
	// - if receiver is size<4, try shift. if it does not work, don't try a push, just continue
	// - if receiver is size=4, try pushing until a shift into a G3 is possible or pushLevels are exhausted
	
	// SHIFT & PUSH STRATEGY
		// - process groups if they feature a specific group size
		// - try to move every player individually out of this group
		// - move players to groups with minimum size thisSize-1 (3,4,2,1)
		// - if shift is not possible, immediately try push (this may lead to small groups being shifted to smaller groups instead of pushed to larger ones)
		// Currently: final shift after pushs is to be better than initial push
		// --> New: First try pushs with min groupSize conditions of final shift of 4, than 3, ..., as long as final push can be better than initial shift!
	
	private void breakUpGroups(int groupSize, int pushLevel) {
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
			if (slot.isFrozen) {
				continue;
			}
			if (slot.getSize() == groupSize) {
				// the following loop makes sure groups larger than already 4 players are only considered (-> see Version before 17h 26.02.2019)
				// if they are TennisCross (TC, strength=20) groups with a maxGroupSize=8
				// if a player is not a TC player, we do not consider this group above size 3 bc useless

				// changes this (schedule) directly within method
				Boolean pushSuccessful = this.pushPlayers(slot.slotId, pushLevel);
				if (pushSuccessful) {
					System.out.println("Sender Group Size = "+groupSize);
					
				} else {
//					System.out.println("Push failed for this slot.");
				}
			}
		}
	}
	
	
	
	public boolean pushPlayers(Integer slotId, int pushLevel) {
		// work on a copy of the current schedule to avoid worsening the current working one
		Schedule pushedSchedule = this.clone();
		// make a reference to exactly the slot to be broken up in the copied schedule
		// try to push all players individually to other slots
		boolean minimumOneSuccessfulPush = false;
		for (Player player :  pushedSchedule.slots.get(slotId).players.values()) {
			if (player.isFrozen) {
				continue;
			}
			// define reach of a push
			boolean pushSuccessful = pushedSchedule.pushSinglePlayer(player.playerNr, slotId, pushLevel);
			if (pushSuccessful) {
				minimumOneSuccessfulPush = true;
				if (player.getSize()>1) {
					System.out.println("Successfully breakShifted a playerUnion: "+player.subPlayerProfiles.get(0).name+"/"+player.subPlayerProfiles.get(1).name);
				}
			}
			// the following block is activated for conditional pushs i.e. where single pushs can only be performed if all players can be relocated
			// for the default case, however, any feasible push is executed immediately and the block below is not necessary
			// ---
			// if any of the pushes cannot be performed, the entire slot break up is cancelled bc leaving smaller groups is worse than
			// leaving bigger ones also if they cannot be broken up. Instead, they may be extended with other additional players for sizes 1 or 2
			// --> the pushedSchedule is not used and the original one remains as active schedule
			// if (!pushSuccessful) {
			// 	return false;
			// }
			// ---
		}
		// if all pushes were successful, take pushedSchedule and set as active schedule for further refinement process
		if (minimumOneSuccessfulPush) {
			this.copyFromSchedule(pushedSchedule);			
		}
		// may update players with selected slots here if problems arise
		return minimumOneSuccessfulPush;
	}

	// can change actual schedule within this method
	// if it fails, just return false and the calling code will consider the push attempt obsolete and will keep using the old code
	// IMPORTANT: The underlying schedule is only modified (synced) if a push can successfully be performed. Else, it remains unaltered!
	public boolean pushSinglePlayer(Integer playerNr, Integer slotId, Integer pushLevel) {
		
		Schedule tempSchedule = this.clone();
		

		// if too many levels of the push tree have been attempted
		if (pushLevel == 0) {
			// System.out.println("Failed to push within pushLevel limits.");
			return false;
		}
		
		// build reference to exactly the slot and player to be pushed
		Slot slot = this.slots.get(slotId);
		Integer slotSize = slot.getSize();
		
		// try to push to potential other slots (put slots into a list that is shuffled for random order of attempting slots)
		List<Integer> pushGroupSizeList = new ArrayList<Integer>();
		Boolean pushSuccessful = false;
		if (slotSize==1 || slotSize==2) {
			if (slot.category.equals("TC")) {
				pushGroupSizeList = Arrays.asList(7,8,6,5,4,3,2,1);
			}
			else {
				pushGroupSizeList = Arrays.asList(3,4,2,1);
			}
			for (int pushGroupSize : pushGroupSizeList) { // 
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
			if (slot.category.equals("TC")) {
				pushGroupSizeList = Arrays.asList(7,8,6,5,4,3,2);
			}
			else {
				pushGroupSizeList = Arrays.asList(3,4,2);
			}
			for (int pushGroupSize : pushGroupSizeList) { // 
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
			if (slot.category.equals("TC")) {
				pushGroupSizeList = Arrays.asList(7,8,6,5,4,3);
			}
			else {
				return false; // pushGroupSizeList = Arrays.asList(3);	// old version is worse: Arrays.asList(3,4)
			}
			for (int pushGroupSize : pushGroupSizeList) { // 
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
			if (slot.category.equals("TC")) {
				pushGroupSizeList = Arrays.asList(7,8,6,5,4);
			}
			else {
				pushGroupSizeList = Arrays.asList(3,4);
			}
			for (int pushGroupSize : pushGroupSizeList) { // 
				// if (pushGroupSize==3) {System.out.println("Attempting to push player from G5 into a G3");}
				// else {System.out.println("Attempting to push player from G5 into a G4");}
				pushSuccessful = tempSchedule.pushSinglePlayerToGroupWithSizeX(slotId, playerNr, pushGroupSize, pushLevel);				
				if (pushSuccessful) {
					// System.out.println("Push successful!");
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
			// System.out.println("Push attempts were unsuccessful!");
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
			// an empty slot can take any player cat. If there already is a player, the cat. must conform to existing players and inherently the slot cat.
			if (!otherslot.category.equals("empty") && !player.category.equals(otherslot.category)) {
				continue;
			}
			if (otherslot.isFrozen) {
				continue;
			}
			// only push to groups with desired size
			if (otherslot.getSize()!=pushGroupSize) {
				continue;
			}
			// do not push to same slot again!
			if (otherslot.slotId==slot.slotId) {
				continue;
			}
			// only if otherslot does not interfere with other slots selected by player
			// (note that on the day the slot is dropped another slot can be chosen --> the day is not blocked as for other selected slots)
			if (!player.canMoveThisSlot2OtherSlot(slot, otherslot, workingSchedule)) {
				continue;
			}
			if (otherslot.getSize() < 4) {
				if (otherslot.isCompatibleWithPlayer(player, false)) {
					System.out.println("Receiver Slot Size ==> "+otherslot.getSize());
					player.removeSelectedSlot(slot);
					player.addSelectedSlot(otherslot);
					slot.players.remove(player.playerNr);
					otherslot.addPlayer(player.playerNr,player);
					if (slot.getSize()==0) {
						slot.category = "empty";
					}
					if (otherslot.category.equals("empty")) {
						otherslot.category = player.category;
					}

					this.copyFromSchedule(workingSchedule);
					return true;
				}
			}
			else if (otherslot.getSize() == 4) {
				// if all other 4 players do not accept this player, it might be an option to kick out another player (-> push) before adding new player
				// kick out obviously only possible if consecutive push is possible for the latter!
				if (! player.isADesiredSlot(otherslot)) {
					continue;
				}
				// check which individual players could be kicked out to make the slot compatible with the new player
				// the returned list is in order of highest resulting compatibility (first candidate makes slot most compatible with new player)
				List<Player> playersToBeKickedOut = otherslot.pushPlayerAndKickOtherplayer(player);
				if (playersToBeKickedOut.size() > 0) {
					// attempting to push player in other slot
					// see below: if no other player could be pushed out of otherslot successfully and a group of 5 would result, the initial push is reversed
					slot.players.remove(player.playerNr);
					player.removeSelectedSlot(slot);
					otherslot.addPlayer(player.playerNr,player);
					if (otherslot.category.equals("empty")) {
						otherslot.category = player.category;
					}
					player.addSelectedSlot(otherslot);
					for (Player playerToBeKickedOut : playersToBeKickedOut) {
						boolean pushSuccessful = workingSchedule.pushSinglePlayer(playerToBeKickedOut.playerNr, otherslot.slotId, pushLevel-1);
						// if push successful, pushSinglePlayer will have modified schedule
						if (pushSuccessful) {
							this.copyFromSchedule(workingSchedule);
							return true;
						}
						else {
							continue;
						}
					}
					// if code has arrived here: reverse the initial push bc no other player could be pushed out from the now group of 5!
					player.removeSelectedSlot(otherslot);
					player.addSelectedSlot(slot);
					otherslot.players.remove(player.playerNr);
					if (otherslot.getSize()==0) {
						otherslot.category = "empty";
					}
					slot.addPlayer(player.playerNr,player);
				}
			}
		}

		return false;
	}
	
	private void shiftAndPush(int groupSize, int pushLevel) {
		List<Integer> slotIds = new ArrayList<Integer>();
		for (Integer slotId : this.slots.keySet()) {
			slotIds.add(slotId);
		}
		for (Integer slotId : slotIds) {
			if (!this.slots.containsKey(slotId)) {
				continue;
			}
			Slot slot = this.slots.get(slotId);
			if (slot.isFrozen) {
				continue;
			}
			if (slot.getSize() == groupSize) {
				// the following loop makes sure groups larger than already 3 players are only considered (-> see Version before 17h 26.02.2019)
				// if they are TennisCross (TC, strength=20) groups with a maxGroupSize=8
				// if a player is not a TC player, we do not consider this group above size 3 bc useless

				// changes this (schedule) directly within method
				Boolean pushSuccessful = this.shiftAndPushPlayers(slot.slotId, pushLevel);
				if (pushSuccessful) {
					System.out.println("Sender Slot Size "+groupSize);
				} else {
				}
			}
		}
	}


	public boolean shiftAndPushPlayers(Integer slotId, int pushLevel) {
		// A SHIFT IS NOTHING BUT A PUSH WITH PUSHLEVEL=1 bc
		// it cannot proceed into a consecutive push, but has only one level to check possible receiver slots
		
		// Work on a temporary copy of the current schedule to avoid worsening the current working one
		// if the new work is successful, the schedule can be updated with the temp and improved copy
		Schedule pushedSchedule = this.clone();

		boolean minimumOneSuccessfulPush = false;
		// Make a reference to exactly the slot to be broken up in the copied schedule
		// try to push all players individually to other slots
		for (Player player :  pushedSchedule.slots.get(slotId).players.values()) {
			if (player.isFrozen) {
				continue;
			}
			// a shift is nothing but a push with level=1 (see above commentary) this is nothing but the first push attempt
			// define reach of a push
			// need to mark a copy of the initial pusher slot for comparing potential final shifts and deciding whether pushes actually are an improvement
			Slot initialPusherSlotCopy = this.slots.get(slotId).clone();
			boolean pushSuccessful;
			// last argument is false indicating that this is an initial call and not a call after attempting a push
			pushSuccessful = pushedSchedule.shiftAndPushSinglePlayer(player.playerNr, false, slotId,
					initialPusherSlotCopy, pushLevel, false, 0, false);
			if (pushSuccessful) {
				minimumOneSuccessfulPush = true;
				if (player.getSize()>1) {
					System.out.println("Successfully shifted a playerUnion: "+player.subPlayerProfiles.get(0).name+"/"+player.subPlayerProfiles.get(1).name);
				}
			}
			// the following block is activated for conditional pushs i.e. where single pushs can only be performed if all players can be relocated
			// for the default case, however, any feasible push is executed immediately and the block below is not necessary
			// ---
			// if any of the pushes cannot be performed, the entire slot break up is cancelled bc leaving smaller groups is worse than
			// leaving bigger ones also if they cannot be broken up. Instead, they may be extended with other additional players for sizes 1 or 2
			// --> the pushedSchedule is not used and the original one remains as active schedule
			// if (!pushSuccessful) {
			// 	return false;
			// }
			// ---
		}
		// if all pushes were successful, take pushedSchedule and set as active schedule for further refinement process
		if (minimumOneSuccessfulPush) {
			this.copyFromSchedule(pushedSchedule);			
		}
		// may update players with selected slots here if problems arise
		return minimumOneSuccessfulPush;
	}
	


	
	// can change actual schedule within this method
		// if it fails, just return false and the calling code will consider the push attempt obsolete and will keep using the old code
		// IMPORTANT: The underlying schedule is only modified (synced) if a push can successfully be performed. Else, it remains unaltered!
		public boolean shiftAndPushSinglePlayer(Integer playerNr, boolean playerHasNotYetBeenPlaced, Integer slotId, Slot initialPusherSlotCopy,
				Integer pushLevel, boolean methodCalledAfterPush, int finalShiftGroupSizeMinimum, boolean allowOverfullGroups) {
			
			Schedule tempSchedule = this.clone();
			
//			System.out.println("Attempting player = "+playerNr);
			
			// if too many levels of the push tree have been attempted
			if (pushLevel == 0) {
//				System.out.println("Failed to push within pushLevel limits.");
				return false;
			}
			
			
			int thisSlotSizeBeforePush;
			// CAUTION: if method was called after a push, the group has been added a player and is one size larger than originally
			// we need the size before the push to decide upon the max size of the receiver group below -> therefore reduce current size by one
			
			if (playerHasNotYetBeenPlaced) {
				thisSlotSizeBeforePush = 0;
			}
			else {
				if (methodCalledAfterPush) {
					thisSlotSizeBeforePush = this.slots.get(slotId).getSize()-this.players.get(playerNr).getSize();
				}
				else {
					thisSlotSizeBeforePush = this.slots.get(slotId).getSize();
				}				
			}
			

			List<Integer> pushGroupSizeList = new ArrayList<Integer>();
			
			if (this.players.get(playerNr).category.equals("TC")) {
				pushGroupSizeList.addAll(Arrays.asList(7,8,6,5,4,3,2,1));
			}
			else {
				pushGroupSizeList.addAll(Arrays.asList(3,2,1));	// old version is worse: Arrays.asList(3,2,1);
			}
			
			// if this is an initial player placement pushs (resp. the final shifts) can also be performed into an empty slot
			// this is helpful if there was an unsuccessful placement of a player, but it could be pushed into a group successfully
			// from where another player is pushed into a possibly empty slot
			if (playerHasNotYetBeenPlaced) {
				pushGroupSizeList.add(0);
			}
			if (allowOverfullGroups) {
				pushGroupSizeList.clear();
				pushGroupSizeList.add(4);	// note that only G4 groups can be overfilled this way
			}
			
			Boolean pushSuccessful = false;
			for (int pushGroupSize : pushGroupSizeList) {
				// shifting/pushing is possible to groups with a minimum size of    Size(receiverGroup) >= Size(senderGroup)-1
				if (pushGroupSize < finalShiftGroupSizeMinimum) {
					break;
				}
				if (pushGroupSize<thisSlotSizeBeforePush) {	// alternative: (pushGroupSize<thisSlotSizeBeforePush-1)
					continue;
				}
				pushSuccessful = tempSchedule.shiftAndPushSinglePlayerToGroupWithSizeX(slotId, playerHasNotYetBeenPlaced, initialPusherSlotCopy, playerNr,
						pushGroupSize, pushLevel, methodCalledAfterPush, finalShiftGroupSizeMinimum, allowOverfullGroups);
				if (pushSuccessful) {
					this.copyFromSchedule(tempSchedule);
					return true;
				} else {
					continue;
				}
			}
			
			// if the player could not be pushed to any other slot
			return false;
		}
	

	
	
	public Boolean shiftAndPushSinglePlayerToGroupWithSizeX(Integer slotId, boolean playerHasNotYetBeenPlaced, 
			Slot initialPusherSlotCopy, Integer playerNr, int pushGroupSize,
			int pushLevel, boolean methodCalledAfterPush, int finalShiftMinGroupSize, boolean allowOverfullGroups) {
		
		// make a temporary copy, which is modified in the process --> If successfully modified, the initial schedule is synced with the modified one
		// this avoids messing up the initial schedule or ending in a concurrentModificationException due to implicit referencing
		Schedule workingSchedule = this.clone();
		
//		System.out.println("Attempting shift/push into groupSize = "+pushGroupSize);

		// build reference to exactly the slot and player to be pushed
		Slot slot = null;
		Player player;
		if (!playerHasNotYetBeenPlaced) {
			slot = workingSchedule.slots.get(slotId);
			// through the implicit loops a player might no more be within a group (return false instead of proceeding with a null pointer)
			if (slot.players.containsKey(playerNr)) {
				player = slot.players.get(playerNr);			
			}
			else {
				return false;
			}
		}
		else {
			// super important that referring to the working schedule
			player = workingSchedule.players.get(playerNr);
		}
		
		// try all slots for a shift and chose the optimal one
		Slot optimalReceiverSlot = null;
		
		List<Map.Entry<Integer,Slot>> slotList = new ArrayList<Map.Entry<Integer,Slot>>(workingSchedule.slots.entrySet());
		Collections.shuffle(slotList);
		for(Map.Entry<Integer,Slot> entry: slotList) {
			Slot otherslot = entry.getValue();
			// an empty slot can take any player cat. If there already is a player, the cat. must conform to existing players and inherently the slot cat.
			if (!otherslot.category.equals("empty") && !player.category.equals(otherslot.category)) {
				continue;
			}
			if (otherslot.isFrozen) {
				continue;
			}
			// do not push to same slot again!
			if (!playerHasNotYetBeenPlaced && otherslot.slotId==slot.slotId) {
				continue;
			}
			// only push to groups with desired size
			if (otherslot.getSize()!=pushGroupSize) {
				continue;
			}
//			System.out.println("Other slot has desired push group size = "+pushGroupSize);
			if (playerHasNotYetBeenPlaced) {
//				System.out.println("Checking if otherSlot accepts the new player (that has not yet been placed)");
				if(!otherslot.acceptsPlayer(player, 2, allowOverfullGroups, workingSchedule, false)) {
					continue;
				}
			}
			else {
				// only if otherslot does not interfere with other slots selected by player, which are on the same day --> never two trainings on one day
				// (note that on the day the slot is dropped another slot can be chosen --> the day is not blocked as for other selected slots)
				if (!player.canMoveThisSlot2OtherSlot(slot, otherslot, workingSchedule)) {
					continue;
				}				
			}
			// only shift or push into desired slots
			if (! player.isADesiredSlot(otherslot)) {
				continue;
			}
//			System.out.println("Is a desired slot and accepts the new player");
			// %%% SHIFT %%% attempt the shift (only if new group accepts player and there is a free space)
			// the initialPusherSlotCopy is the benchmark to decide if the shift/push leads to an actual improvement of the entire schedule
			// if there is only a shift without previous push, the reference slot is obviously the current sending slot from where the player originates
			// if the shift comes after a push, a copy of the originating slot is handed over through the method as initialPusherSlotCopy and is to be compared
			if (otherslot.isCompatibleWithPlayer(player, allowOverfullGroups) && shiftIsFormallyPermitted(initialPusherSlotCopy,player,otherslot)) {
				if (optimalReceiverSlot==null) {
					optimalReceiverSlot = otherslot;
				}
				// maybe two receiver slots are found. choose the one with better overall improvement
				// --> shift to group, where average maxGSize is closer to the maxGSize of the player to be shifted
				else if (allowOverfullGroups || otherslot.isPreferrableReceiverSlotTo(optimalReceiverSlot, player)) {
					optimalReceiverSlot = otherslot;
				}
				else {
					continue;
				}
			}
		}
		// if a feasible slot has been found, shift the player to the optimal one and return
		// if no feasible receiver slot has been found for a shift, the procedure for a push is initiated below
		if (optimalReceiverSlot!=null) {
			System.out.println("Receiver Slot Size ==> "+optimalReceiverSlot.getSize());
			if (!playerHasNotYetBeenPlaced) {
				slot.players.remove(player.playerNr);				
			}
			optimalReceiverSlot.addPlayer(player.playerNr,player);
			if (!playerHasNotYetBeenPlaced) {
				player.removeSelectedSlot(slot);				
			}
			player.addSelectedSlot(optimalReceiverSlot);
			if (optimalReceiverSlot.category.equals("empty")) {
				optimalReceiverSlot.category = player.category;
			}
			if (!playerHasNotYetBeenPlaced) {
				if (slot.getSize()==0) {
					slot.category = "empty";
				}				
			}
			this.copyFromSchedule(workingSchedule);
			return true;
		}
		else {
			// do nothing and let code perform the the push attempt routine below
		}
		// %%% PUSH %%% attempt the push (if there is no free space, but another player could be kicked out so that new remaining group accepts new player)
			for(Map.Entry<Integer,Slot> entry: slotList) {
				Slot otherslot = entry.getValue();
				// an empty slot can take any player cat. If there already is a player, the cat. must conform to existing players and inherently the slot cat.
				if (!otherslot.category.equals("empty") && !player.category.equals(otherslot.category)) {
					continue;
				}
				if (otherslot.isFrozen) {
					continue;
				}
				// do not push to same slot again!
				if (!playerHasNotYetBeenPlaced && otherslot.slotId==slot.slotId) {
					continue;
				}
				// only push to groups with minimum size
				// -> Assume very low success likelihood to initiate push into a group smaller than sender group
				//    as it has already been attempted to push those priorly!
				if (otherslot.getSize() < pushGroupSize) {
					continue;
				}
				// only shift or push into desired slots
				if (! player.isADesiredSlot(otherslot)) {
					continue;
				}
				// only if otherslot does not interfere with other slots selected by player, which are on the same day --> never two trainings on one day
				// (note that on the day the slot is dropped another slot can be chosen --> the day is not blocked as for other selected slots)
				if (playerHasNotYetBeenPlaced) {
					if(!otherslot.groupVirtuallyAcceptsPlayer(player, workingSchedule)) {
						continue;
					}
				}
				else {
					if (!player.canMoveThisSlot2OtherSlot(slot, otherslot, workingSchedule)) {
						continue;
					}				
				}
//			List<Player> playersToBeKickedOut = otherslot.pushPlayerAndKickOtherplayer(player);
				// push is only permitted if new player fits into new group as good as the kicked out player has (no worsening condition)
				List<Player> playersToBeKickedOut = otherslot.feasibleKickoutPlayers(player);
				if (playersToBeKickedOut.size() > 0) {
					// attempting to push player in other slot
					// see below: if no other player could be pushed out of otherslot successfully and a group of 5 would result, the initial push is reversed
					if (!playerHasNotYetBeenPlaced) {
						slot.players.remove(player.playerNr);						
						player.removeSelectedSlot(slot);
					}
					otherslot.addPlayer(player.playerNr,player);
					player.addSelectedSlot(otherslot);
					if (otherslot.category.equals("empty")) {
						otherslot.category = player.category;
					}
					for (Player playerToBeKickedOut : playersToBeKickedOut) {
						boolean pushSuccessful = false;
//						if (methodCalledAfterPush) {
//							pushSuccessful = workingSchedule.shiftAndPushSinglePlayer(playerToBeKickedOut.playerNr, otherslot.slotId,
//									initialPusherSlotCopy, pushLevel-1, true, finalShiftMinGroupSize);							
//						}
//						else {
//							for (finalShiftMinGroupSize=3; finalShiftMinGroupSize>=Math.min(initialPusherSlotCopy.getSize()-1,3); finalShiftMinGroupSize--) {							
//								pushSuccessful = workingSchedule.shiftAndPushSinglePlayer(playerToBeKickedOut.playerNr, otherslot.slotId,
//														initialPusherSlotCopy, pushLevel-1, true, finalShiftMinGroupSize);
//								if (pushSuccessful) {
//									break;
//								}
//								else {
//									continue;
//								}
//							}
//						}
						// here playerHasNotYetBeenPlaced must always be false bc we are reassigning a kickout player!
						pushSuccessful = workingSchedule.shiftAndPushSinglePlayer(playerToBeKickedOut.playerNr, false, otherslot.slotId,
								initialPusherSlotCopy, pushLevel-1, true, pushGroupSize, allowOverfullGroups);
						// if push successful, pushSinglePlayer will have modified schedule
						if (pushSuccessful) {
							this.copyFromSchedule(workingSchedule);
							return true;
						}
						else {
							continue;
						}
					}
					// if code has arrived here: reverse the initial push bc no other player could be pushed out from the now group of 5!
					player.removeSelectedSlot(otherslot);
					if (!playerHasNotYetBeenPlaced) {
						player.addSelectedSlot(slot);						
					}
					otherslot.players.remove(player.playerNr);
					if (!playerHasNotYetBeenPlaced) {
						slot.addPlayer(player.playerNr,player);						
					}
					if (otherslot.getSize()==0) {
						otherslot.category = "empty";
					}
				}
			}
		return false;
	}

	private boolean shiftIsFormallyPermitted(Slot slot, Player player, Slot otherslot) {

		// if there is no initial slot (bc this may be an initial placement), any placement is better than no placement, so just returns true
		if (slot == null) {
			return true;
		}
		
		// if receiver group is same size or larger, shift is always formally permitted (as long as other constraints in the calling method can be respected)
		if (otherslot.getSize()>=slot.getSize()) {
			return true;
		}
		else if (otherslot.getSize()==slot.getSize()-1) {
			// shift to group one size smaller is formally permitted if:
			//		a. if maxGSize(shifterPlayer)<maxGSize(average(receiverGroup)), shift if:
			//			- sum(maxGSize(receiverGroup)) <= sum(maxGSize(senderGroup))
			//			- min(maxGSize(receiverGroup)=>GSize(shifterPlayer)
			//		b. if maxGSize(shifterPlayer)>=maxGSize(average(receiverGroup)), shift if:
			//			- sum(maxGSize(receiverGroup)) >= sum(maxGSize(senderGroup))
			if (player.maxGroupSize < otherslot.averageMaxPlayerGroupSize()
					&& otherslot.sumMaxPlayerGroupSize() <= slot.sumMaxPlayerGroupSize()-player.maxGroupSize
					&& otherslot.minMaxPlayerGroupSize() >= player.maxGroupSize) {
				return true;
			}
			else if(player.maxGroupSize >= otherslot.averageMaxPlayerGroupSize()
					&& otherslot.sumMaxPlayerGroupSize() >= slot.sumMaxPlayerGroupSize()-player.maxGroupSize ) {
				return true;				
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
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
			if (originalSlot.isFrozen) {
				continue;
			}
			if (originalSlot.getSize() == groupSize) {
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
		if (slot.isFrozen) {
			return false;
		}
		
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
			if (otherslot.isFrozen) {
				continue;
			}
			// do not push to parallel slot (same slot or slot at same day and time on another court)
			if (otherslot.isSameTimeAndDay(slot)) {
				continue;
			}
			if (otherslot.getSize() == pullSlotSize) {
				for (Player player : otherslot.players.values()) {
					if (slot.acceptsPlayer(player, 2, false, this, false)) {
						Schedule initialBackupSchedule = this.clone();
						slot.addPlayer(player.playerNr, player);
						player.addSelectedSlot(slot);
						otherslot.players.remove(player.playerNr);
						player.removeSelectedSlot(otherslot);
						if (pContinuePull < new Random().nextDouble()) {
							Schedule pulledSchedule = this.clone();
							boolean consecutivePullSuccessul = pulledSchedule.pullPlayers(otherslot.slotId, pullLevel-1, otherslot.getSize(), hardSuccessCondition);
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
		this.players.clear();
		for (Player player : scheduleToCopyFrom.players.values()) {
			this.players.put(player.playerNr, player.clone());
		}
	}

	public void calculateEfficiency(Map<Integer,Player> players, String printPhrase) {

		System.out.println(printPhrase);
		
		// make bins of groups according to size
		Map<Integer,Integer> groupSizeBins = new HashMap<Integer,Integer>(10);
		for (int i=0; i<10; i++) {
			groupSizeBins.put(i, 0);
		}
		for (Slot slot : this.slots.values()) {
			int size = slot.getSize();
			if (size > 8) {
				groupSizeBins.put(9,groupSizeBins.get(9)+1);
			}
			else {
				groupSizeBins.put(size,groupSizeBins.get(size)+1);
			}
		}
		System.out.println("Actual group sizes bins:  "+groupSizeBins.toString());

		Map<Integer,Integer> playerGroupSizeBins = new HashMap<Integer,Integer>(10);
		for (Entry<Integer, Integer> entry : groupSizeBins.entrySet()) {
			playerGroupSizeBins.put(entry.getKey(), entry.getKey()*entry.getValue());
		}
		System.out.println("Actual player group sizes:"+playerGroupSizeBins.toString());


		
		// make bins of desired maxGroupSize by players
		Map<Integer,Integer> playerMaxSizeBins = new HashMap<Integer,Integer>(8);
		for (int i=0; i<=8; i++) {
			playerMaxSizeBins.put(i, 0);
		}
		for (Player player : players.values()) {
				int maxGroupSizeOfPlayer = player.maxGroupSize;
//				System.out.println("maxGroupSizeOfPlayer: "+maxGroupSizeOfPlayer);
//				System.out.println("player.nSlots: "+player.nSlots);
				playerMaxSizeBins.put(maxGroupSizeOfPlayer, playerMaxSizeBins.get(maxGroupSizeOfPlayer)+player.nSlots*player.getSize());
		}
		System.out.println("Desired max. group sizes: "+playerMaxSizeBins.toString());
		
		// check how many players are in a group below their max group size
		Map<Integer,Integer> playerEfficiencyBins = new HashMap<Integer,Integer>(8);
		for (int i=0; i<=8; i++) {
			playerEfficiencyBins.put(i, 0);
		}
//		Map<Integer,Integer> groupEfficiencyBins = new HashMap<Integer,Integer>(8);
//		for (int i=0; i<=7; i++) {
//			groupEfficiencyBins.put(i, 0);
//		}
		for (Slot slot : this.slots.values()) {
			int slotSize = slot.getSize();
			int limitingMaxGroupSize = 8;
			for (Player player : slot.players.values()) {
				int maxGroupSizeOfPlayer = player.maxGroupSize;
				int diff = maxGroupSizeOfPlayer-slotSize;
				if (diff<0) {
					continue;
				}
				playerEfficiencyBins.put(diff,playerEfficiencyBins.get(diff)+player.getSize());
				if (maxGroupSizeOfPlayer<limitingMaxGroupSize) {
					limitingMaxGroupSize=maxGroupSizeOfPlayer;
				}
			}
//			if (slotSize>0) {
//				int remainingSpaceInGroup = limitingMaxGroupSize-slotSize;				
//				groupEfficiencyBins.put(remainingSpaceInGroup,playerEfficiencyBins.get(remainingSpaceInGroup)+1);
//			}
		}
//		System.out.println("Unused player tolerance:  "+groupEfficiencyBins.toString());
		System.out.println("Spaces remaining free:    "+playerEfficiencyBins.toString());

		this.totalUsedSots();
		
	}

	public int totalUsedSots() {
		// check how many slots have been used (where an actual lecture is held)
		int nSlotsUsed = 0;
		for (Slot slot : this.slots.values()) {
			if (slot.getSize()>0) {
				nSlotsUsed++;
			}
		}
		System.out.println("TOTAL number of slots required = "+nSlotsUsed);
		return nSlotsUsed;
	}

	public void verifyCompliance(Map<Integer,Player> players) {
		
//		Check if rules have actually been obeyed:
//		- no other slot on same day
//		- successful placement in desired slots
//		- compatibility with other players
		
		// no other slot on same day for each player --> check also referenced samePlayerProfiles here!
		for (Player player : players.values()) {
			for (Slot thisSlot : player.selectedSlots) {
				// have to check samePlayerReferences of all its subprofiles!
				for (Player subplayer : player.subPlayerProfiles) {
					for (int subPlayerSamePlayerReferenceNr : subplayer.samePersonPlayerProfiles) {
						Player subPlayerSamePlayerReference = this.players.get(subPlayerSamePlayerReferenceNr);
						for (Slot otherSlot : subPlayerSamePlayerReference.selectedSlots) {
							if (thisSlot.slotId==otherSlot.slotId) {
//						System.out.println("SLOTS THE SAME - NO WAY!!!!");
								continue;
							}
//					else {
//						System.out.println("It proceeds ...!");
//					}
							else if (thisSlot.weekdayNr==otherSlot.weekdayNr) {
								System.out.println("ERROR: Two slots on same day!");
							}
//					else if (thisSlot.weekdayNr==otherSlot.weekdayNr) {
//						System.out.println("ERROR: Two slots on same day!");
//					}
							else {
//						System.out.println("SameDaySlot OK!");
							}
						}
					}
				}
			}
		}
		
		// successful placement in desired slots
		for (Player player : players.values()) {
			for (Slot slot : player.selectedSlots) {
				boolean slotIsFeasibleSlot = false;
				for (Slot feasibleSlot : player.desiredSlots) {
					if (slot.isSameTimeAndDay(feasibleSlot)) {
						slotIsFeasibleSlot = true;
						break;
					}
				}
				if (!slotIsFeasibleSlot) {
					System.out.println("ERROR: Slot is not featured within initially desired slots!");
				}
				else {
//					System.out.println("FeasibleSlot OK!");
				}
			}
		}
		
		// compatibility with other players
		for (Slot slot : this.slots.values()) {
			if (slot.isFrozen) {
				continue;
			}
			for (Player thisPlayer : slot.players.values()) {
				for (Player otherPlayer : slot.players.values()) {
					if (thisPlayer.equals(otherPlayer)) {
						continue;
					}
					else if (!thisPlayer.isCompatibleWithOtherPlayer(otherPlayer)) {
						System.out.println("ERROR: Two players are not compatible!");
					}
					else {
//						System.out.println("PlayerCompatibility OK!");
					}
				}
			}
		}
		
		// check if players have been assigned enough slots
		Map<Integer,Integer> unsatisfiedTrainingFrequencyBins = new HashMap<Integer,Integer>(5);
		for (int i=0; i<=4; i++) {
			unsatisfiedTrainingFrequencyBins.put(i, 0);
		}
		for (Player player : this.players.values()) {
			for (Player subplayer : player.subPlayerProfiles) {
				if (subplayer.selectedSlots.size()<subplayer.nSlots) {
					int diff = subplayer.nSlots-subplayer.selectedSlots.size();
					if (diff > 4) {
						unsatisfiedTrainingFrequencyBins.put(4,unsatisfiedTrainingFrequencyBins.get(4)+1);
					}
					else {
						unsatisfiedTrainingFrequencyBins.put(diff,unsatisfiedTrainingFrequencyBins.get(diff)+1);
					}
				}
			}
		}
		System.out.println("unsatisfiedTrainingFrequencyBins:  "+unsatisfiedTrainingFrequencyBins.toString());
		
		int totPlayerSlots = 0;
		for (Player player : this.players.values()) {
			totPlayerSlots += player.selectedSlots.size()*player.subPlayerProfiles.size();				
		}
		System.out.println("Number of playerSlots by players:  "+totPlayerSlots);
		int totSlotPlayers = 0;
		for (Slot slot : this.slots.values()) {
			totSlotPlayers += slot.getSize();
		}
		System.out.println("Number of playerSlots by slots:  "+totSlotPlayers);
	}

	public void cleanUp() {
		for (Player player : this.players.values()) {
			for (Player subplayer : player.subPlayerProfiles) {
				for (Slot desiredSlot : player.desiredSlots) {
					if (!subplayer.hasDesiredSlotOnSameDayAndSameTime(desiredSlot, this)) {
						subplayer.desiredSlots.add(desiredSlot);
					}
				}
				for (Slot selectedSlot : player.selectedSlots) {
					if (!subplayer.hasSelectedSlotOnSameDayAndSameTime(selectedSlot, this)) {
						subplayer.selectedSlots.add(selectedSlot);
					}
				}
			}
		}
		
	}

}
