package tcDietlikon;

import java.util.ArrayList;
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
	
	
	
}

