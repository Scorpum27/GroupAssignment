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
	Map<Integer,Player> players;
	Boolean isFrozen = false;
	String category = "empty";
	
	Slot(){	
		this.players = new HashMap<Integer,Player>();
	}
	
	Slot(Integer weekday, Integer time){	
		this.weekdayNr = weekday;
		this.time = time;
		this.players = new HashMap<Integer,Player>();
	}

	Slot(Integer weekday, Integer time, Integer courtNr){	
		this.weekdayNr = weekday;
		this.time = time;
		this.courtNr = courtNr;
		this.players = new HashMap<Integer,Player>();
	}

	Slot(Integer slotId, Integer weekday, Integer time, Integer courtNr){	
		this.slotId	= slotId;
		this.weekdayNr = weekday;
		this.time = time;
		this.courtNr = courtNr;
		this.players = new HashMap<Integer,Player>();
	}
	
	public Slot clone() {
		Slot copy = new Slot(this.weekdayNr, this.time, this.courtNr);
		copy.slotId = this.slotId;
		copy.isFrozen = this.isFrozen;
		copy.category = this.category;
		for (Player player : this.players.values()) {
			copy.players.put(player.playerNr,player.clone());
		}
		return copy;
	}
	
//	public Boolean groupVirtuallyAcceptsPlayer(Player player) {
//		if (! player.isADesiredSlot(this)) {
//			return false;
//		}
//		for (Slot slot : player.selectedSlots) {
//			if (this.weekdayNr == slot.weekdayNr) {
//				return false;
//			}
//		}
//		for (Player otherPlayer : this.players.values()) {
//			int ageDiff = Math.abs(player.age-otherPlayer.age);
//			int classDiff = Math.abs(player.strength-otherPlayer.strength);
//			if (ageDiff > player.maxAgeDiff  ||  ageDiff > otherPlayer.maxAgeDiff	||				// Default > 3.0
//				classDiff > player.maxClassDiff  ||  classDiff > otherPlayer.maxClassDiff) {		// Default > 2.0
//				return false;
//			}
//		}
//		return true;
//	}
	
	public void addPlayer(int playerNr, Player player) {
		for (int otherPlayerNr : this.players.keySet()) {
			if (playerNr==otherPlayerNr) {
				System.out.println("XXXXXXXXXXXXXXXXX CAUTION: A player already exists in this slot with same playerNr = "+otherPlayerNr);
			}
		}
		this.players.put(playerNr, player);
	}
	
	public Boolean acceptsPlayer(Player player, int strategy) {
		// loop to ensure that a player is not assigned two slots on the same weekday if he wants to train more than once
		// this is always a condition irrespective of the strategy
		if (this.players.containsKey(player.playerNr)) {
			System.out.println("--------------------------------- Yes, it tried it !!");
			return false;
		}
		if (this.isFrozen) {
			return false;
		}
		for (Slot slot : player.selectedSlots) {
			if (this.weekdayNr == slot.weekdayNr) {
				return false;
			}
		}
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1 because considering case where another player is to be added
		if (player.maxGroupSize < this.players.size()+1) {
			return false;
		}
		for (Player otherPlayer : this.players.values()) {
			if (otherPlayer.maxGroupSize < this.players.size()+1) {
				return false;
			}
		}

		if (!this.category.equals("empty") && !player.category.equals(this.category)) {
			return false;
		}

		// STRATEGY 0: add player only in an already activated slot (i.e. that has already been added a player)
		// max 4 players/group; class/age constraints; only desired slots;
		if (strategy == 0) {
			if (this.players.size()>=this.limitingPlayerMaxGroupSize()) {
				return false;
			}
			if (this.players.size()==0) {
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
			if (this.players.size()>=this.limitingPlayerMaxGroupSize()) {
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
			if (this.players.size()>=this.limitingPlayerMaxGroupSize()) {
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
			if (this.players.size()>=this.limitingPlayerMaxGroupSize()) {
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
			if (this.players.size()>=5) {
				return false;
			}
			return true;
		}
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
		} else {
			return "Unkwn";
		}
	}

	public List<Player> pushPlayerAndKickOtherplayer(Player player) {
		Map<Player,Double> kickoutCandidates = new HashMap<Player,Double>();
		List<Player> kickoutCandidatesList = new ArrayList<Player>();
		// return initialized, but empty list if the group already features this player
		if (this.players.containsKey(player.playerNr)) {
			System.out.println("--------------------------------- Yes, it tried it !!");
			return kickoutCandidatesList;
		}
		// check that desired slot
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1 because considering case where another player is to be added
		if (! player.isADesiredSlot(this) || player.maxGroupSize < this.players.size()) {
			// returns empty list indicating that no possibility of a push and kick
			return kickoutCandidatesList;
		}
		// check for every playerOut if new player could be added if playerOut is kicked out 
		for (Player playerOut : this.players.values()) {
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

	public boolean isCompatibleWithPlayer(Player player) {
		if (this.players.containsKey(player.playerNr)) {
			System.out.println("--------------------------------- Yes, it tried it !!");
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
		if (player.maxGroupSize < this.players.size()+1) {
			return false;
		}
		for (Player otherPlayer : this.players.values()) {
			if (otherPlayer.maxGroupSize < this.players.size()+1) {
				return false;
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
			return 1000;			
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
			average += player.maxGroupSize/this.players.size();
		}
		return average;
	}

	public double sumMaxPlayerGroupSize() {
		double sum = 0.0;
		for (Player player : this.players.values()) {
			sum += player.maxGroupSize/this.players.size();
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
			System.out.println("--------------------------------- Yes, it tried it !!");
			return kickoutCandidatesList;
		}
		// check that desired slot
		// check that group is not too large for player's maxGroupSize and the other players' maxGroupSize
		// +1 because considering case where another player is to be added
		if (! player.isADesiredSlot(this) || player.maxGroupSize < this.players.size()) {
			// returns empty list indicating that no possibility of a push and kick
			return kickoutCandidatesList;
		}
		// check for every playerOut if new player could be added if playerOut is kicked out 
		for (Player playerOut : this.players.values()) {
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
				average += player.maxGroupSize/(this.players.size()-1);
			}
		}
		return average;
	}
	

	
}
