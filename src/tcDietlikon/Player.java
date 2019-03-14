package tcDietlikon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Player {
	
	String name;
	int playerNr;
	String notes = "";
	int age;
	int nSlots = 0;
	boolean slotNrSatisfied = true;
	int maxGroupSize = 0;
	int strength;
	String category = "default";
	int maxAgeDiff;
	int maxClassDiff;
	int worstPlacementRound = -1;
	double linkability = 0.0;
	boolean isFrozen = false;
	List<Slot> desiredSlots = new ArrayList<Slot>();
	List<Slot> selectedSlots = new ArrayList<Slot>();
	List<Integer> frozenSameGroupPeers = new ArrayList<Integer>();
	List<String> frozenSameGroupPeerStrings = new ArrayList<String>();
	List<Integer> linkablePlayers = new ArrayList<Integer>();
	List<Integer> samePersonPlayerProfiles = new ArrayList<Integer>();
	List<Slot> undesirablePlacements = new ArrayList<Slot>();
	List<Player> subPlayerProfiles = new ArrayList<Player>();
	// subPlayerProfiles is usually empty, but may be used to list peers who NEED to play together and are therefore combined in one player
	Map<Slot,String> postProposedSlots = new HashMap<Slot,String>();
	
	public Player clone() {
		Player copy = new Player(false);
		copy.name = this.name;
		copy.playerNr = this.playerNr;
		copy.notes = this.notes;
		copy.age = this.age;
		copy.nSlots = this.nSlots;
		copy.slotNrSatisfied = this.slotNrSatisfied;
		copy.maxGroupSize = this.maxGroupSize;
		copy.strength = this.strength;
		copy.maxAgeDiff = this.maxAgeDiff;
		copy.maxClassDiff = this.maxClassDiff;
		copy.category = this.category;
		copy.worstPlacementRound = this.worstPlacementRound;
		copy.linkability = this.linkability;
		copy.isFrozen = this.isFrozen;
		for (Integer linkablePlayerNr : this.linkablePlayers) {
			copy.linkablePlayers.add(linkablePlayerNr);
		}
		for (Integer samePersonNr : this.samePersonPlayerProfiles) {
			copy.samePersonPlayerProfiles.add(samePersonNr);
		}
		for (Integer mustHavePeer : this.frozenSameGroupPeers) {
			copy.frozenSameGroupPeers.add(mustHavePeer);
		}
		for (String mustHavePeerName : this.frozenSameGroupPeerStrings) {
			copy.frozenSameGroupPeerStrings.add(mustHavePeerName);
		}
		for (Slot desiredSlot : this.desiredSlots) {
			copy.desiredSlots.add(desiredSlot);
		}
		for (Slot selectedSlot : this.selectedSlots) {
			copy.selectedSlots.add(selectedSlot);
		}
		for (Slot undesirableSlot : this.undesirablePlacements) {
			copy.undesirablePlacements.add(undesirableSlot);
		}
		for (Entry<Slot,String> entry : this.postProposedSlots.entrySet()) {
			copy.postProposedSlots.put(entry.getKey(), entry.getValue());
		}
		for (Player player : this.subPlayerProfiles) {
			copy.subPlayerProfiles.add(player);
		}
		return copy;
	}
	
	public Player(boolean addItselfAsSubPlayerProfiles){
		if (addItselfAsSubPlayerProfiles) {
			this.subPlayerProfiles.add(this);			
		}
	}

	public Player(String name, boolean addItselfAsSubPlayerProfiles){
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
		this.name = name;
		// NOTE: super important that pointing to itself as subplayer profile must come at the very end
		// because if not a pointer is used, but a clone, one can make sure that all other constructor parameters have priorly been set and copied to the clone
		if (addItselfAsSubPlayerProfiles) {
			this.subPlayerProfiles.add(this);			
		}		
	}
	public Player(String name, Integer playerNr, Integer age, Integer strength, Integer nSlots, Integer maxGroupSize, boolean addItselfAsSubPlayerProfiles) {
		this.name = name;
		this.playerNr = playerNr;
		this.age = age;
		this.strength = strength;
		this.nSlots = nSlots;
		this.maxGroupSize = maxGroupSize;
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
		// NOTE: super important that pointing to itself as subplayer profile must come at the very end
		// because if not a pointer is used, but a clone, one can make sure that all other constructor parameters have priorly been set and copied to the clone
		if (addItselfAsSubPlayerProfiles) {
			this.subPlayerProfiles.add(this);			
		}
	}
	public Player(String name, Integer playerNr, Integer age, Integer strength, Integer nSlots, Integer maxGroupSize, String category,
			Integer maxAgeDiff, Integer maxClassDiff, boolean addItselfAsSubPlayerProfiles) {
		this.name = name;
		this.playerNr = playerNr;
		this.age = age;
		this.strength = strength;
		this.nSlots = nSlots;
		this.maxGroupSize = maxGroupSize;
		this.maxAgeDiff = maxAgeDiff;
		this.maxClassDiff = maxClassDiff;
		this.category = category;
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
		// NOTE: super important that pointing to itself as subplayer profile must come at the very end
		// because if not a pointer is used, but a clone, one can make sure that all other constructor parameters have priorly been set and copied to the clone
		if (addItselfAsSubPlayerProfiles) {
			this.subPlayerProfiles.add(this);			
		}
	}
	
	public int getSize() {
		return this.subPlayerProfiles.size();	// has several subProfiles and therefore carries and places more than one player at the time
	}

	public boolean isADesiredSlot(Slot timeslot) {
		for (Slot desiredSlot : this.desiredSlots) {
			if (desiredSlot.weekdayNr == timeslot.weekdayNr && desiredSlot.time == timeslot.time) {
				return true;
			}
		}
		return false;
	}
	
	public boolean removeSelectedSlot(Slot xSlot) {
		if (this.isFrozen) {
			System.out.println("CAUTION: Player did not allow slot removal as the player is frozen.");
			return false;
		}
		Iterator<Slot> slotIterMainProfile = this.selectedSlots.iterator();
		while (slotIterMainProfile.hasNext()) {
			Slot slot = slotIterMainProfile.next();
			if (slot.isSameTimeAndDay(xSlot)) {				
				slotIterMainProfile.remove();
				return true;
			}
		}
		return false;
	}
	
	public void addSelectedSlot(Slot xSlot) {
		this.selectedSlots.add(xSlot);
//		for (Player subplayer : this.subPlayerProfiles) {
//			subplayer.selectedSlots.add(xSlot);
//		}
	}

	// method to check if player can be moved from thisSlot to otherSlot
	// otherslot cannot be on a day, where player already has a training slot
	// but otherslot can be on the day, where thisSlot is dropped bc now the player is free on this day
	public boolean canMoveThisSlot2OtherSlot(Slot thisSlot, Slot otherslot, Schedule schedule) {
		for (Slot selectedSlot : this.selectedSlots) {
			// do not have to check slots on the day that we are dropping the training on bc it is free again
			if (selectedSlot.weekdayNr==thisSlot.weekdayNr) {
				continue;
			}
			// check if otherslot does not interfere with selected training slots on another day
			// if it does, player cannot be moved to that day a second time
			if (selectedSlot.weekdayNr == otherslot.weekdayNr) {
				return false;
			}
		}
		// repeat exactly the same story for all same player profiles (have to refer to subplayers here!) and their slots
		for (Player subplayer : this.subPlayerProfiles) {
			for (int playerNr : subplayer.samePersonPlayerProfiles) {
				Player samePlayer = schedule.players.get(playerNr);
				for (Slot selectedSlot : samePlayer.selectedSlots) {
					// do not have to check slots on the day that we are dropping the training on bc it is free again
					if (selectedSlot.weekdayNr==thisSlot.weekdayNr) {
						continue;
					}
					// check if otherslot does not interfere with selected training slots on another day
					// if it does, player cannot be moved to that day a second time
					if (selectedSlot.weekdayNr == otherslot.weekdayNr) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean isCompatibleWithOtherPlayer(Player otherPlayer) {
		// make sure not same player is put in a group twice
		if (this.playerNr == otherPlayer.playerNr) {
			return false;
		}
		int ageDiff = Math.abs(this.age - otherPlayer.age);
		int classDiff = Math.abs(this.strength - otherPlayer.strength);
		if (ageDiff > this.maxAgeDiff || ageDiff > otherPlayer.maxAgeDiff ||
				classDiff > this.maxClassDiff || classDiff > otherPlayer.maxClassDiff) { 	// Default > 2.0
			return false;
		}
		return true;
	}

	public boolean hasSlotOnSameDay(Slot slot, Schedule schedule) {
		for (Slot selectedSlot : this.selectedSlots) {
			if (slot.weekdayNr==selectedSlot.weekdayNr) {
				return true;
			}
		}
		// do the same thing for other player profiles of the same person
		for (Player subplayer : this.subPlayerProfiles) {
			for (int playerNr : subplayer.samePersonPlayerProfiles) {
				Player samePlayer = schedule.players.get(playerNr);
				for (Slot selectedSlot : samePlayer.selectedSlots) {
					if (slot.weekdayNr==selectedSlot.weekdayNr) {
						return true;
					}
				}
			}			
		}
		return false;
	}

	public String strength2string() {
		if (this.strength==20) {
			return "TC";					
		}
		else if (this.strength==21) {
			return "G";					
		}
		else if (this.strength==22) {
			return "O";					
		}
		else if (this.strength==23) {
			return "R";					
		}
		else if (1 <= this.strength && this.strength <= 9){
			return "R"+this.strength;
		}
		else if (-3 <= this.strength && this.strength <= 0) {
			return "N"+(4+this.strength);
		}
		else {
			return "??";
		}
	}

	public List<Integer> subplayerNrList() {
		List<Integer> subprofileNrList = new ArrayList<Integer>();
		for (Player subprofile : this.subPlayerProfiles) {
			subprofileNrList.add(subprofile.playerNr);
		}
		return subprofileNrList;
	}

	
	public void setClassFromSubprofiles() {
		double averageClass = 0.0;
		for (Player subplayer : this.subPlayerProfiles) {
    		averageClass += 1.0*subplayer.strength/this.subPlayerProfiles.size();
		}
		this.strength = (int) Math.round(averageClass);
	}
	
	
	public void setAgeFromSubprofiles() {
		double averageAge = 0.0;
		for (Player subplayer : this.subPlayerProfiles) {
    		averageAge += 1.0*subplayer.age/this.subPlayerProfiles.size();
		}
		this.age = (int) Math.round(averageAge);
	}
	
	
	public void setCategoryFromSubprofiles() {
		String cat = "";
		for (Player subplayer : this.subPlayerProfiles) {
			if (subplayer.category.equals("default")) {
				cat = "default";
    		}
    		else if (Arrays.asList("R","O","G").contains(subplayer.category)) {
    			if (!category.equals("default")) {
    				cat = subplayer.category;
    			}
    		}
    		else if (subplayer.category.equals("TC")) {
    			if (!Arrays.asList("default","R","O","G").contains(cat)) {
    				cat = "TC";
    			}
    		}
    		else {
    			System.out.println("This player has unknown category --> may jeopardize setting known category for merged group (PlayerUtils)");
    		}
		}
		this.category = cat;
	}
	
	
	public void setNameFromSubprofiles() {
		String mergedName = "";
		for (Player player : this.subPlayerProfiles) {
			mergedName += (player.name+"/");			
		}
		// have to remove last "/" from the concatenated name
		mergedName = mergedName.substring(0, mergedName.length()-1);
		this.name = mergedName;
	}
	
	
	public void setMinNSlotsFromSubprofiles() {
		int minSlots = Integer.MAX_VALUE;
		for (Player player : this.subPlayerProfiles) {
			if (player.nSlots<minSlots) {
				minSlots = player.nSlots;
			}
		}			
		this.nSlots = minSlots;
	}

	
	public void setMaxGroupSizeFromSubprofiles() {
    	int highestMaxGroupSize = 0;
		for (Player player : this.subPlayerProfiles) {
    		if (player.maxGroupSize>highestMaxGroupSize) {
    			highestMaxGroupSize = player.maxGroupSize;
    		}
		}
		this.maxGroupSize = highestMaxGroupSize;
	}


	public void setMaxClassDiffFromSubprofiles() {
		int lowestMaxClassDiff = Integer.MAX_VALUE;
		for (Player player : this.subPlayerProfiles) {
			if (player.maxClassDiff<lowestMaxClassDiff) {
				lowestMaxClassDiff = player.maxClassDiff;
			}
		}
		this.maxClassDiff = lowestMaxClassDiff;
	}

	
	public void setMaxAgeDiffFromSubprofiles() {
		int lowestMaxAgeDiff = Integer.MAX_VALUE;		
		for (Player player : this.subPlayerProfiles) {
			if (player.maxAgeDiff<lowestMaxAgeDiff) {
				lowestMaxAgeDiff = player.maxAgeDiff;
			}
		}			
		this.maxAgeDiff = lowestMaxAgeDiff;		
	}

	public void setWorstPlacementRound(int strategy) {
		this.worstPlacementRound = strategy;
		for (Player subplayer : this.subPlayerProfiles) {
			subplayer.worstPlacementRound = strategy;
		}
	}

	public void addUndesirablePlacement(Slot currentlyOptimalSlot) {
		this.undesirablePlacements.add(currentlyOptimalSlot);
		for (Player subplayer : this.subPlayerProfiles) {
			subplayer.undesirablePlacements.add(currentlyOptimalSlot);
		}
		
	}

	public boolean hasDesiredSlotOnSameDayAndSameTime(Slot desiredSlot, Schedule schedule) {
		for (Slot slot : this.desiredSlots) {
			if (desiredSlot.isSameTimeAndDay(slot)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasSelectedSlotOnSameDayAndSameTime(Slot selectedSlot, Schedule schedule) {
		for (Slot slot : this.selectedSlots) {
			if (selectedSlot.isSameTimeAndDay(slot)) {
				return true;
			}
		}
		return false;
	}
		
}

