package tcDietlikon;

import java.util.ArrayList;
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
	List<Slot> desiredSlots = new ArrayList<Slot>();
	List<Slot> selectedSlots = new ArrayList<Slot>();
	List<Integer> linkablePlayers = new ArrayList<Integer>();
	List<Slot> undesirablePlacements = new ArrayList<Slot>();
	List<Integer> samePersonPlayerProfiles = new ArrayList<Integer>();
	Map<Slot,String> postProposedSlots = new HashMap<Slot,String>();
	
	public Player clone() {
		Player copy = new Player();
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
		for (Integer linkablePlayerNr : this.linkablePlayers) {
			copy.linkablePlayers.add(linkablePlayerNr);
		}
		for (Integer samePersonNr : this.samePersonPlayerProfiles) {
			copy.samePersonPlayerProfiles.add(samePersonNr);
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
		return copy;
	}
	
	public Player(){
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
	}

	public Player(String name){
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
		this.name = name;
	}
	public Player(String name, Integer playerNr, Integer age, Integer strength, Integer nSlots, Integer maxGroupSize) {
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
	}
	public Player(String name, Integer playerNr, Integer age, Integer strength, Integer nSlots, Integer maxGroupSize, String category) {
		this.name = name;
		this.playerNr = playerNr;
		this.age = age;
		this.strength = strength;
		this.nSlots = nSlots;
		this.maxGroupSize = maxGroupSize;
		this.category = category;
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
		this.undesirablePlacements = new ArrayList<Slot>();
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
		Iterator<Slot> slotIter = this.selectedSlots.iterator();
		while (slotIter.hasNext()) {
			Slot slot = slotIter.next();
			if (slot.isSameTimeAndDay(xSlot)) {				
				slotIter.remove();
				return true;
			}
		}
		return false;
	}
	
	public void addSelectedSlot(Slot xSlot) {
		this.selectedSlots.add(xSlot);
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
		// repeat exactly the same story for all same player profiles and their slots
		for (int playerNr : this.samePersonPlayerProfiles) {
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
		for (int playerNr : this.samePersonPlayerProfiles) {
			Player samePlayer = schedule.players.get(playerNr);
			for (Slot selectedSlot : samePlayer.selectedSlots) {
				if (slot.weekdayNr==selectedSlot.weekdayNr) {
					return true;
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
	
}

