package tcDietlikon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Slot {

	Integer weekdayNr = 0;
		// 1 = Monday; 2 = Tuesday; ...
	Integer time = 0;
		// 8 = 0800-0900; 16 = 1600-1700; etc.
	Integer courtNr;
	Integer slotId;
	Map<Integer,Player> players;
	
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
		for (Player player : this.players.values()) {
			copy.players.put(player.playerNr,player.clone());
		}
		return copy;
	}
	
	public Boolean groupVirtuallyAcceptsPlayer(Player player) {
		if (! player.isADesiredSlot(this)) {
			return false;
		}
		for (Slot slot : player.selectedSlots) {
			if (this.weekdayNr == slot.weekdayNr) {
				return false;
			}
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
	
	public Boolean acceptsPlayer(Player player, int strategy) {
		// loop to ensure that a player is not assigned two slots on the same weekday if he wants to train more than once
		// this is always a condition irrespective of the strategy
		for (Slot slot : player.selectedSlots) {
			if (this.weekdayNr == slot.weekdayNr) {
				return false;
			}
		}
		// STRATEGY 0: add player only in an already activated slot (i.e. that has already been added a player)
		// max 4 players/group; class/age constraints; only desired slots;
		if (strategy == 0) {
			if (this.players.size()>=4) {
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
			if (this.players.size()>=4) {
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
			if (this.players.size()>=4) {
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
			if (this.players.size()>=4) {
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
		if (! player.isADesiredSlot(this)) {
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
		// sort the kick-out candidates by the compatibility that they leave for the group i.e. how small the max. class and age difference is
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
		return kickoutCandidatesList;
	}

	public boolean isCompatibleWithPlayer(Player player) {
		for (Player otherPlayer : this.players.values()) {
			int ageDiff = Math.abs(player.age - otherPlayer.age);
			int classDiff = Math.abs(player.strength - otherPlayer.strength);
			if (ageDiff > player.maxAgeDiff || ageDiff > otherPlayer.maxAgeDiff || // Default > 3.0
				classDiff > player.maxClassDiff || classDiff > otherPlayer.maxClassDiff) { // Default > 2.0
				return false;
			}
		}
		if (!player.isADesiredSlot(this)) {
			return false;
		}
		return true;
	}
}
