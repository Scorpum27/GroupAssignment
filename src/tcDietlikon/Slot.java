package tcDietlikon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Slot {

	int weekdayNr = 0;
		// 1 = Monday; 2 = Tuesday; ...
	int time = 0;
		// 8 = 0800-0900; 16 = 1600-1700; etc.
	int courtNr;
	int slotId;
	Map<Integer,Player> players = new HashMap<Integer,Player>();
	List<Player> desirablePlayers = new ArrayList<Player>();
	Boolean isFrozen = false;
	String category = "empty";
	
	Slot(){	
	}
	
	Slot(Integer weekday, Integer time){	
		this.weekdayNr = weekday;
		this.time = time;
	}

	Slot(Integer weekday, Integer time, Integer courtNr){	
		this.weekdayNr = weekday;
		this.time = time;
		this.courtNr = courtNr;
	}

	Slot(Integer slotId, Integer weekday, Integer time, Integer courtNr){	
		this.slotId	= slotId;
		this.weekdayNr = weekday;
		this.time = time;
		this.courtNr = courtNr;
	}
	
	public Slot clone() {
		Slot copy = new Slot(this.weekdayNr, this.time, this.courtNr);
		copy.slotId = this.slotId;
		copy.isFrozen = this.isFrozen;
		copy.category = this.category;
		for (Player player : this.players.values()) {
			copy.players.put(player.playerNr,player.clone());
		}
		for (Player player : this.desirablePlayers) {
			copy.desirablePlayers.add(player);
		}
		return copy;
	}
	
	public int getSize() {
		int size = 0;
		for (Player player : this.players.values()) {
			size += player.getSize();
		}
		return size;
	}
	
	// usually to check if player could be added to a group if it were pushed there (no size constraint bc during push procedure another palyer would be kicked out)
	public Boolean groupVirtuallyAcceptsPlayer(Player player, Schedule schedule) {
		if (this.isFrozen) {
			return false;
		}
		if (this.players.containsKey(player.playerNr)) {
			return false;
		}
		if (! player.isADesiredSlot(this)) {
			return false;
		}
		if (!this.category.equals("empty") && !player.category.equals(this.category)) {
			return false;
		}
		if (player.hasSlotOnSameDay(this, schedule)) {
			return false;
		}
		for (Player otherPlayer : this.players.values()) {
			int ageDiff = Math.abs(player.age-otherPlayer.age);
			int classDiff = Math.abs(player.strength-otherPlayer.strength);
			if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
				classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
				return false;
			}
		}
		return true;
	}
	
	public void addPlayer(int playerNr, Player player) {
		for (int otherPlayerNr : this.players.keySet()) {
			if (playerNr==otherPlayerNr) {
				System.out.println("XXXXXXXXXXXXXXXXX CAUTION: A player already exists in this slot with same playerNr = "+otherPlayerNr);
			}
		}
		this.players.put(playerNr, player);
	}
	
	public Boolean acceptsPlayer(Player player, int strategy, boolean allowOverfullGroups, Schedule schedule, boolean includingMustHavePeers) {
		// loop to ensure that a player is not assigned two slots on the same weekday if he wants to train more than once
		// this is always a condition irrespective of the strategy
		if (this.players.containsKey(player.playerNr)) {
			return false;
		}
		if (this.isFrozen) {
			return false;
		}
		if (player.hasSlotOnSameDay(this, schedule)) {
			return false;
		}
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1/nMustHavePeers because considering case where (an)other player(s) is/are to be added
		if (!allowOverfullGroups) {
			int nPlayersToAdd;
			if (includingMustHavePeers) {
				nPlayersToAdd = player.getSize() + player.frozenSameGroupPeers.size();
			}
			else {
				nPlayersToAdd = player.getSize();
			}
			if (player.maxGroupSize < this.getSize() + nPlayersToAdd) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				if (otherPlayer.maxGroupSize < this.getSize() + nPlayersToAdd) {
					return false;
				}
			}		
		}

		if (!this.category.equals("empty") && !player.category.equals(this.category)) {
			return false;
		}
		// a Saturday slot can only be assigned if the player has a desired slot on Saturdays ("...at least as close as two hours to this slot." could be implemented)
		if (this.weekdayNr==6) {
			// standard version
			if (! player.isADesiredSlot(this)) {
				return false;
			}
			// close desired slots version
//			boolean saturdayPermitted = false;
//			for (Slot slot : player.desiredSlots) {
//				if (slot.weekdayNr==6 && Math.abs(slot.time-this.time)<=2) {
//					return true;
//				}
//				else {
//					continue;
//				}
//			}
//			
//			if (saturdayPermitted) {
//				return true;
//			}
//			else {
//				return false;
//			}
		}
		// STRATEGY 0: add player only in an already activated slot (i.e. that has already been added a player)
		// max 4 players/group; class/age constraints; only desired slots;
		if (strategy == 0) {
			if (this.getSize()>=this.limitingPlayerMaxGroupSize()) {
				return false;
			}
			if (this.getSize()==0) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				int ageDiff = Math.abs(player.age-otherPlayer.age);
				int classDiff = Math.abs(player.strength-otherPlayer.strength);
				if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
					classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
					return false;
				}
			}
			if (! player.isADesiredSlot(this)) {
				return false;
			}
			return true;
//			boolean hasLinkablePlayerAlreadyInGroup = false;
//			for (Player setPlayer : this.players.values()) {
//				if (player.linkablePlayers.contains(setPlayer)) {
//					hasLinkablePlayerAlreadyInGroup = true;
//					break;
//				}
//			}
//			if (hasLinkablePlayerAlreadyInGroup) {
//				return true;
//			}
//			else {
//				return false;				
//			}
		}
		// STRATEGY 1/2: max 4 players/group; class/age constraints; only desired slots
		if (strategy == 1 || strategy == 2) {
			if (this.getSize()>=this.limitingPlayerMaxGroupSize()) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				int ageDiff = Math.abs(player.age-otherPlayer.age);
				int classDiff = Math.abs(player.strength-otherPlayer.strength);
				if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
					classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
					return false;
				}
			}
			if (! player.isADesiredSlot(this)) {
				return false;
			}
			return true;
		}
		// STRATEGY 3: max 4 players/group; class/age constraints; also not desired slots
		if (strategy == 3) {
			if (this.getSize()>=this.limitingPlayerMaxGroupSize()) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				int ageDiff = Math.abs(player.age-otherPlayer.age);
				int classDiff = Math.abs(player.strength-otherPlayer.strength);
				if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
					classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
					return false;
				}
			}
			return true;
		}
		// STRATEGY 4: max 4 players/group; loosened placement constraints; also not desired slots
		if (strategy == 4) {
			if (this.getSize()>=this.limitingPlayerMaxGroupSize()) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				int ageDiff = Math.abs(player.age-otherPlayer.age);
				int classDiff = Math.abs(player.strength-otherPlayer.strength);
				if (ageDiff > player.maxAgeDiff+1  ||  ageDiff > otherPlayer.maxAgeDiff+1	||
					classDiff > player.maxClassDiff+1  ||  classDiff > otherPlayer.maxClassDiff+1) {
					return false;
				}
			}
			return true;
		}
		// STRATEGY 5: max 5 players/group; no placement constraints; also not desired slots
		if (strategy == 5) {
			if (this.getSize()>=5) {
				return false;
			}
			return true;
		}
		// STRATEGY 11: for checking if a slot would accept player (during push procedures)
		// -> note that the size constraint is not featured as we know that we will kick out another player after pushing this one -> size will not grow
//		if (strategy == 11) {
//			for (Player otherPlayer : this.players.values()) {
//				int ageDiff = Math.abs(player.age-otherPlayer.age);
//				int classDiff = Math.abs(player.strength-otherPlayer.strength);
//				if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
//					classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
//					return false;
//				}
//			}
//			if (! player.isADesiredSlot(this)) {
//				return false;
//			}
//			
//			return true;
//		}
		// if has managed to pass all tests up to here, return true (= "player can be accepted into the group of this slot")
		return true;
	}
	
	
	public boolean isSameTimeAndDay(Slot slot) {
		if (this.time == slot.time &&
				this.weekdayNr == slot.weekdayNr) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static String dayNr2Name(int dayNr) {
		if (dayNr == 1) {
			return "Mo";
		} else if (dayNr == 2) {
			return "Di";
		} else if (dayNr == 3) {
			return "Mi";
		} else if (dayNr == 4) {
			return "Do";
		} else if (dayNr == 5) {
			return "Fr";
		} else if (dayNr == 6) {
			return "Sa";
		}
		else {
			return "Unkwn";
		}
	}

	public List<Player> pushPlayerAndKickOtherplayer(Player player) {
		Map<Player,Double> kickoutCandidates = new HashMap<Player,Double>();
		List<Player> kickoutCandidatesList = new ArrayList<Player>();
		// return initialized, but empty list if the group already features this player
		if (this.players.containsKey(player.playerNr)) {
			return kickoutCandidatesList;
		}
		// check that desired slot
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		if (! player.isADesiredSlot(this) || player.maxGroupSize < this.getSize()) {
			// returns empty list indicating that no possibility of a push and kick
			return kickoutCandidatesList;
		}
		// check for every playerOut if new player could be added if playerOut is kicked out 
		for (Player playerOut : this.players.values()) {
			if (playerOut.isFrozen) {
				continue;
			}
			// check compatibility of remaining other players (all players in group except the one that might be kicked out)
			boolean compatibleWithRemainingPlayers = true;
			int maxAge = player.age;
			int minAge = player.age;
			int maxClass = player.strength;
			int minClass = player.strength;
			for (Player otherPlayer : this.players.values()) {
				// all but the kick-out candidate
				if (otherPlayer.equals(playerOut)) {
					continue;
				}
				if (!player.isCompatibleWithOtherPlayer(otherPlayer)) {
					compatibleWithRemainingPlayers = false;
					break;
				}
				else {
					if (otherPlayer.age > maxAge) {
						maxAge = otherPlayer.age;
					}
					if (otherPlayer.age < minAge) {
						minAge = otherPlayer.age;
					}
					if (otherPlayer.strength > maxClass) {
						maxClass = otherPlayer.strength;
					}
					if (otherPlayer.strength < minClass) {
						minClass = otherPlayer.strength;
					}
					continue;
				}
			}
			if (compatibleWithRemainingPlayers) {
				kickoutCandidates.put(playerOut,1.0*(Math.abs(maxClass-minClass))+0.5*(Math.abs(maxAge-minAge)));
			}
		}
		// with a high probability sort the kick-out candidates by the compatibility that they leave for the group
		// i.e. how small the max. class and age difference is
		// with a low probability, keep the order as is
		if (new Random().nextDouble()<0.95) {
			for (Player kickoutPlayer : kickoutCandidates.keySet()) {
				if (kickoutCandidatesList.size()==0) {
					kickoutCandidatesList.add(kickoutPlayer);
				}
				else {
					int position = 0;
					int insertPosition = kickoutCandidatesList.size(); // initialize with last possible position in case loop does not hit worse candidate before
					for (Player sortedKickoutPlayer : kickoutCandidatesList) {
						if (kickoutCandidates.get(kickoutPlayer)<kickoutCandidates.get(sortedKickoutPlayer)) {
							insertPosition = position;
							break;
						}
						position++;
					}
					kickoutCandidatesList.add(insertPosition, kickoutPlayer);
				}
			}
		}
		else {
			kickoutCandidatesList.addAll(kickoutCandidates.keySet());
		}
		return kickoutCandidatesList;
	}

	public boolean isCompatibleWithPlayer(Player player, boolean allowOverfullGroups) {
		if (this.players.containsKey(player.playerNr)) {
			return false;
		}
		for (Player otherPlayer : this.players.values()) {
			int ageDiff = Math.abs(player.age - otherPlayer.age);
			int classDiff = Math.abs(player.strength - otherPlayer.strength);
			if (ageDiff > player.maxAgeDiff || ageDiff > otherPlayer.maxAgeDiff || // Default > 3.0
				classDiff > player.maxClassDiff || classDiff > otherPlayer.maxClassDiff) { // Default > 2.0
				return false;
			}
		}
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1 because considering case where another player is to be added
		if (!allowOverfullGroups) {
			if (player.maxGroupSize < this.getSize()+1) {
				return false;
			}
			for (Player otherPlayer : this.players.values()) {
				if (otherPlayer.maxGroupSize < this.getSize()+1) {
					return false;
				}
			}			
		}
		if (!player.isADesiredSlot(this)) {
			return false;
		}
		return true;
	}

	public int limitingPlayerMaxGroupSize() {
		int limitingMaxGroupSize = 8;
		for (Player player : this.players.values()) {
			if (player.maxGroupSize<limitingMaxGroupSize) {
				limitingMaxGroupSize = player.maxGroupSize;
			}
		}
		return limitingMaxGroupSize;
	}

	public boolean isPreferableDayToOtherSlot(Slot otherSlot, int strategy) {
		int thisSlotDayCategory = dayPlacementCategory(this.weekdayNr);
		int otherSlotDayCategory = dayPlacementCategory(otherSlot.weekdayNr);
		if (strategy==1) {
			if (thisSlotDayCategory > otherSlotDayCategory) {
				// higher category means lower demand e.g. Monday is not as popular as Wednesday
				return true;
			}
			else {
				return false;			
			}			
		}
		else if (strategy==2) {
			if (thisSlotDayCategory < otherSlotDayCategory) {
				// higher category means lower demand e.g. Monday is not as popular as Wednesday
				return true;
			}
			else {
				return false;			
			}
		}
		else {
			return this.isPreferableDayToOtherSlot(otherSlot, 1);
		}
	}

	public int dayPlacementCategory(Integer weekdayNr) {
		if (weekdayNr==3) {
			return 1; // Wednesday!
		}
		else if(weekdayNr==5) {
			return 2; // Friday
		}
		else if (Arrays.asList(1,2,4).contains(weekdayNr)) {
			return 3; // Monday, Tuesday, Thursday 
		}
		else {
			return 1000; // Saturday or unknown		
		}
	}

	public boolean dayIsSamePreferenceAsOtherSlot(Slot otherSlot) {
		int thisSlotDayCategory = dayPlacementCategory(this.weekdayNr);
		int otherSlotDayCategory = dayPlacementCategory(otherSlot.weekdayNr);
		if (thisSlotDayCategory == otherSlotDayCategory) {
			return true;
		}
		else {
			return false;			
		}
	}

	public double averageMaxPlayerGroupSize() {
		double average = 0.0;
		for (Player player : this.players.values()) {
			average += player.getSize()*player.maxGroupSize/this.getSize();
			// front factor player.getSize() is weight factor as player may have several subplayers
		}
		return average;
	}

	public double sumMaxPlayerGroupSize() {
		double sum = 0.0;
		for (Player player : this.players.values()) {
			sum += player.getSize()*player.maxGroupSize/this.getSize();
			// front factor player.getSize() is weight factor as player may have several subplayers
		}
		return sum;
	}

	public int minMaxPlayerGroupSize() {
		int min = Integer.MAX_VALUE;
		for (Player player: this.players.values()) {
			if (player.maxGroupSize<min) {
				min = player.maxGroupSize;
			}
		}
		return min;
	}

	public boolean isPreferrableReceiverSlotTo(Slot otherSlot, Player player) {
		double maxGSizeAverageThisSlot = this.averageMaxPlayerGroupSize();
		double maxGSizeAverageOtherSlot = otherSlot.averageMaxPlayerGroupSize();
		if (Math.abs(1.0*player.maxGroupSize-maxGSizeAverageThisSlot) <= Math.abs(1.0*player.maxGroupSize-maxGSizeAverageOtherSlot)) {
			return true;
		}
		else {
			return false;
		}
	}


	public List<Player> feasibleKickoutPlayers(Player player) {
		Map<Player,Double> kickoutCandidates = new HashMap<Player,Double>();
		List<Player> kickoutCandidatesList = new ArrayList<Player>();
		// return initialized, but empty list if the group already features this player
		if (this.players.containsKey(player.playerNr)) {
			return kickoutCandidatesList;
		}
		// check that desired slot
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1 because considering case where another player is to be added
		if (! player.isADesiredSlot(this) || player.maxGroupSize < this.getSize()) {
			// returns empty list indicating that no possibility of a push and kick
			return kickoutCandidatesList;
		}
		// check for every playerOut if new player could be added if playerOut is kicked out 
		for (Player playerOut : this.players.values()) {
			if (playerOut.isFrozen) {
				continue;
			}
			// check compatibility of remaining other players (all players in group except the one that might be kicked out)
			boolean compatibleWithRemainingPlayers = true;
			int maxAge = player.age;
			int minAge = player.age;
			int maxClass = player.strength;
			int minClass = player.strength;
			
			// in contrast to the light version of this class, pushPlayerAndKickOtherplayer:
			// a push is only permitted if the new player fits as good as/better than the player kicked out
			double playerOutGSizeDeviation = Math.abs(this.averageMaxGSizeWithoutPlayerOut(playerOut)-playerOut.maxGroupSize);
			double playerNewGSizeDeviation = Math.abs(this.averageMaxGSizeWithoutPlayerOut(playerOut)-player.maxGroupSize);
			
			if (playerNewGSizeDeviation>playerOutGSizeDeviation) {
				continue;
			}
			for (Player otherPlayer : this.players.values()) {
				// all but the kick-out candidate
				if (otherPlayer.equals(playerOut)) {
					continue;
				}
				if (!player.isCompatibleWithOtherPlayer(otherPlayer)) {
					compatibleWithRemainingPlayers = false;
					break;
				}
				else {
					if (otherPlayer.age > maxAge) {
						maxAge = otherPlayer.age;
					}
					if (otherPlayer.age < minAge) {
						minAge = otherPlayer.age;
					}
					if (otherPlayer.strength > maxClass) {
						maxClass = otherPlayer.strength;
					}
					if (otherPlayer.strength < minClass) {
						minClass = otherPlayer.strength;
					}
					continue;
				}
			}
			if (compatibleWithRemainingPlayers) {
				kickoutCandidates.put(playerOut,1.0*(Math.abs(maxClass-minClass))+0.5*(Math.abs(maxAge-minAge)));
			}
		}
		// with a high probability sort the kick-out candidates by the compatibility that they leave for the group
		// i.e. how small the max. class and age difference is
		// with a low probability, keep the order as is
		if (new Random().nextDouble()<0.95) {
			for (Player kickoutPlayer : kickoutCandidates.keySet()) {
				if (kickoutCandidatesList.size()==0) {
					kickoutCandidatesList.add(kickoutPlayer);
				}
				else {
					int position = 0;
					int insertPosition = kickoutCandidatesList.size(); // initialize with last possible position in case loop does not hit worse candidate before
					for (Player sortedKickoutPlayer : kickoutCandidatesList) {
						if (kickoutCandidates.get(kickoutPlayer)<kickoutCandidates.get(sortedKickoutPlayer)) {
							insertPosition = position;
							break;
						}
						position++;
					}
					kickoutCandidatesList.add(insertPosition, kickoutPlayer);
				}
			}
		}
		else {
			kickoutCandidatesList.addAll(kickoutCandidates.keySet());
		}
		return kickoutCandidatesList;
	}

	private double averageMaxGSizeWithoutPlayerOut(Player playerOut) {
		// find average of players' maxGroupSize with exception of the player which is potentially kicked out
		double average = 0.0;
		for (Player player : this.players.values()) {
			// do not consider kick-out candidate for average calculation
			if (player.playerNr == playerOut.playerNr) {
				continue;
			}
			else {
				average += player.getSize()*player.maxGroupSize/(this.getSize()-1);
			}
		}
		return average;
	}

	public boolean isFull() {
		int slotSize = this.getSize();
		for (Player player : this.players.values()) {
			if (player.maxGroupSize>=slotSize) {
				return true;
			}
		}
		return false;
	}


	public boolean fillWithPlayers(int fullnessGoalPlacementRound, int maxGroupSizePlacementRound, Schedule schedule) {
//		System.out.println("Trying to fill slot="+this.slotId);
		
		// find all players with the maxGroupSize as specified by loop in calling method (--> these may now be combined in filling in the slot)
		List<Player> feasiblePlayers = new ArrayList<Player>();
		for (Player player : this.desirablePlayers) {
//			System.out.println("player.maxGroupSize = "+player.maxGroupSize);
//			System.out.println("player.nSlots-player.selectedSlots.size() = "+(player.nSlots-player.selectedSlots.size()));
			if (player.maxGroupSize==maxGroupSizePlacementRound && player.selectedSlots.size()<player.nSlots && !player.hasSlotOnSameDay(this, schedule)) {
				feasiblePlayers.add(player);
			}
		}
		// find the combination of feasible players that fulfills the fullnessGoal with the least overall player linkability
		int sizeGoal = maxGroupSizePlacementRound-fullnessGoalPlacementRound;
		if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(feasiblePlayers) < sizeGoal) {
			return false;
		}
		List<Player> finalGroup = new ArrayList<Player>();
		finalGroup = PlayerUtils.findOptimalPlayerCombination(finalGroup, feasiblePlayers, sizeGoal, Double.MAX_VALUE, schedule);
		if (finalGroup.size()>0) {
			if (this.category.equals("empty")) {
				this.category = finalGroup.get(0).category;	// category of first player
			}
			for (Player player : finalGroup) {
				this.addPlayer(player.playerNr, player);
				player.addSelectedSlot(this);
				player.worstPlacementRound = 0;
				// set peers to frozen if minimum two of them successfully land in same group
				for (int mustHavePeerNr : player.frozenSameGroupPeers) {
					for (Player otherFinalGroupPlayer : finalGroup) {
						if (otherFinalGroupPlayer.playerNr==mustHavePeerNr) {
							player.isFrozen = true;
							otherFinalGroupPlayer.isFrozen = true;
						}
					}
				}
			}
			System.out.println("SUCCESS: GroupSize="+maxGroupSizePlacementRound+" / SpacesFree="+fullnessGoalPlacementRound);
			return true;
		}
		else {
			return false;			
		}
	}

	public boolean mergerFeasible(Slot otherSlot1, Slot otherSlot2, Schedule schedule) {
		// check that a player is not featured in both groups --> this would result in the player losing one assigned slot, which is not desired
		for (int playerNr1 : otherSlot1.players.keySet()) {
			for (int playerNr2 : otherSlot2.players.keySet()) {
				if (playerNr1==playerNr2) {
					return false;
				}
			}
		}
		
		// put all players into a list that would be merged
		List<Player> allPlayers = new ArrayList<Player>();
		for (Player player : otherSlot1.players.values()) {
			// avoid double training on same day
			if (player.canMoveThisSlot2OtherSlot(otherSlot1, this, schedule)) {
				allPlayers.add(player);				
			}
			else {
				return false;
			}
		}
		for (Player player : otherSlot2.players.values()) {
			// avoid double training on same day
			if (player.canMoveThisSlot2OtherSlot(otherSlot2, this, schedule)) {
				allPlayers.add(player);				
			}
			else {
				return false;
			}
		}
		
		// check compatibility of all players
		for (Player player : allPlayers) {
			// desired slot day/time
			if (!player.isADesiredSlot(this)) {
				return false;
			}
			for (Player otherPlayer : allPlayers) {
				if (player.equals(otherPlayer)) {
					continue;
				}
				// class & age
				if (!player.isCompatibleWithOtherPlayer(otherPlayer)) {
					return false;
				}
				// size
				// if maxG4 player, allow also G5 groups (see the -1 at the end allowing a G5 for player.maxGroupSize=4)
				if (player.maxGroupSize==4 && player.maxGroupSize< PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(allPlayers)-1) {
					return false;
				}
				else if (player.maxGroupSize!=4 && player.maxGroupSize< PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(allPlayers)) {
					return false;
				}
				// category
				if (!player.category.equals(otherPlayer.category)) {
					return false;
				}
			}
		}
		// if code arrives here, all players are compatible within that specific slot
		return true;
	}
	
	public boolean mergerFeasible(Player player1, Slot otherSlot2, Schedule schedule) {
		// check that a player is not featured in both groups --> this would result in the player losing one assigned slot, which is not desired
		for (int playerNr2 : otherSlot2.players.keySet()) {
			if (player1.playerNr==playerNr2) {
				return false;
			}
		}
		
		// put all players into a list that would be merged
		List<Player> allPlayers = new ArrayList<Player>();
		// avoid double training on same day
		if (player1.hasSlotOnSameDay(this, schedule)) {
			return false;
		}
		allPlayers.add(player1);
		for (Player player : otherSlot2.players.values()) {
			// avoid double training on same day
			if (player.canMoveThisSlot2OtherSlot(otherSlot2, this, schedule)) {
				allPlayers.add(player);				
			}
			else {
				return false;
			}
		}
		
		// check compatibility of all players
		for (Player player : allPlayers) {
			// desired slot day/time
			if (!player.isADesiredSlot(this)) {
				return false;
			}
			for (Player otherPlayer : allPlayers) {
				if (player.equals(otherPlayer)) {
					continue;
				}
				// class & age
				if (!player.isCompatibleWithOtherPlayer(otherPlayer)) {
					return false;
				}
				// size
				// if maxG4 player, allow also G5 groups (see the -1 at the end allowing a G5 for player.maxGroupSize=4)
				if (player.maxGroupSize==4 && player.maxGroupSize< PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(allPlayers)-1) {
					return false;
				}
				else if (player.maxGroupSize!=4 && player.maxGroupSize< PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(allPlayers)) {
					return false;
				}
				// category
				if (!player.category.equals(otherPlayer.category)) {
					return false;
				}
			}
		}
		// if code arrives here, all players are compatible within that specific slot
		return true;
	}
	
}
