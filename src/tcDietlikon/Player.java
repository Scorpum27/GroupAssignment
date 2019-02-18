package tcDietlikon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player {
	
	String name;
	Integer playerNr;
	String notes;
	Integer age;
	Integer nSlots;
	Integer maxGroupSize;
	Integer strength;
	List<Slot> desiredSlots;
	List<Slot> selectedSlots;
	Integer maxAgeDiff;
	Integer maxClassDiff;
	List<Integer> linkablePlayers;
	Integer placementRound;
	
	public Player clone() {
		Player copy = new Player();
		copy.name = this.name;
		copy.playerNr = this.playerNr;
		copy.notes = this.notes;
		copy.age = this.age;
		copy.nSlots = this.nSlots;
		copy.maxGroupSize = this.maxGroupSize;
		copy.strength = this.strength;
		copy.maxAgeDiff = this.maxAgeDiff;
		copy.maxClassDiff = this.maxClassDiff;
		copy.placementRound = this.placementRound;
		for (Integer linkablePlayerNr : this.linkablePlayers) {
			copy.linkablePlayers.add(linkablePlayerNr);
		}
		for (Slot desiredSlot : desiredSlots) {
			copy.desiredSlots.add(desiredSlot);
		}
		for (Slot selectedSlot : selectedSlots) {
			copy.selectedSlots.add(selectedSlot);
		}
		return copy;
	}
	
	public Player(){
		this.desiredSlots = new ArrayList<Slot>();
		this.selectedSlots = new ArrayList<Slot>();
		this.linkablePlayers = new ArrayList<Integer>();
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
	public boolean canMoveThisSlot2OtherSlot(Slot thisSlot, Slot otherslot) {
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
		return true;
	}

	public boolean isCompatibleWithOtherPlayer(Player otherPlayer) {
			int ageDiff = Math.abs(this.age - otherPlayer.age);
			int classDiff = Math.abs(this.strength - otherPlayer.strength);
			if (ageDiff > this.maxAgeDiff || ageDiff > otherPlayer.maxAgeDiff || // Default > 3.0
				classDiff > this.maxClassDiff || classDiff > otherPlayer.maxClassDiff) { // Default > 2.0
					return false;
			}
			else {
				return true;				
			}
	}
	
}

