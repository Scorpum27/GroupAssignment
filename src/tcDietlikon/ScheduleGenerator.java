
//  QUESTIONS
//   
//   1. Unterschiede zum Wintertraining (um das Programm globaler zu machen)?
//		- Im Winter generell bis 2000 - dann Fixplätze vermietet (selten Ausnahmen)
//	 2. Max Age Difference? (Maybe combine with class difference)
//		- 
//	 3. Viele Privatstunden? Können die ev. flexibel am Schluss eingefügt werden? Haben die Prio in der Setzung?
//		- 
//	 4. Sind Mi/Fr wirklich viiiel beliebter?
//		- 

// nTot = 
// TX = one category!
// 10-15% undesired slots


// IDEAS & TUNING
// XXX DATA-SET --> make list of most wanted slots and fill in accordingly maybe rather place players first in desirable slots!!! 
//		--> Check impact of reverse linkability order with new TC player sets
//		--> in initial place players
//		--> in currentlyOptimalReceiverSlot
//		--> or completely random?
// XXX Try to explicitly push players with undesired slots! --> may just change their current undesired slot to another slot and then be combinable!!
//	 --> source of problem may be: undesired placement takes place into a G4, then it can only be shifted if it comes out of G4, which is only possible into G3/G4
//	 --> may loosen placement rules at the end or not put in undesiredSlot at the start
//	 --> or at the start make an immediate push with another player --> and push those players who have highest linkability first!
// XXX Where do the differences between actual total and desired total come from?
//   --> check from cumulative slot size if not all desired nSlots are covered
//   --> do the same thing for the players!
//	 --> do both the above in compliance --> check before, intermediate, final!
//   --> possibly a player is put in a slot where it already is!! -> check compliance after initialPlacement: if it has two slots on same day, can lead to failure
// XXX Dijkstra strategies!!
// - strategies to shuffle players into unused slots to open them up! (maybe whole groups?)
// - stagnation: reshuffle groups by shifting players from one to another improving their overall linkability or age/class differences --> then refine() again?
// - pull procedures
// - manual inputs for very strong players

// TUNING
// - pushLevel --> make list of most wanted slots and fill in accordingly maybe rather place players first in desirable slots!!! 
// - try pushGroupSize only until size (pushGroupSize<thisSlotSizeBeforePush-1) or just (pushGroupSize<thisSlotSizeBeforePush)
// - use 4 as pushGroup size in arrays or not
// - break or shift/push first? (current optimal = shift&push, then break)

package tcDietlikon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class ScheduleGenerator {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, EncryptedDocumentException, InvalidFormatException {
	
		int pushLevel = 4;
		boolean createNewPlayerSet = true;
		boolean useFixedSlotFile = false;
		int initialPlacementStrategy = 2;
		boolean doNotLoadSelectedSlots = true;

	// create or load players
		Map<Integer,Player> players = new HashMap<Integer,Player>();
		// create random players with a reasonable distribution
		if (createNewPlayerSet) {
			int nPlayers = 200;
			players = PlayerUtils.createPlayers(nPlayers);
			XMLOps.writeToFile(players, "samplePlayers.xml");
		}
		else {
			players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers_BENCHMARK.xml"));
			if (doNotLoadSelectedSlots) { for (Player player : players.values()) {player.selectedSlots.clear();} }
		}
	// load players from actual TCD registration form
		// String playerRegistrationFile = "Template_Tennischule_Einteilung.xlsx";
		// Map<Integer,Player> players = PlayerUtils.loadPlayers(playerRegistrationFile);
	// load sample players from file

	// for each player find all other players that can be assigned to the same group
		PlayerUtils.findLinkablePlayers(players);
		
	// create and fill in initial schedule (may follow specific strategies here instead of just filling in randomly)
		String courtScheduleFile = "Belegung_TennishalleDietlikon.xlsx";
		String fixedGroupsFile = "Fixe_Gruppen.xlsx";
		Schedule schedule = Schedule.initializeSchedule(players, courtScheduleFile, initialPlacementStrategy, fixedGroupsFile, useFixedSlotFile);
		schedule.verifyCompliance(players);

	// refine schedule to be more efficient
		schedule.calculateEfficiency(players, "Schedule efficiency BEFORE refinement:");
		schedule.refine(players, pushLevel);
		schedule.calculateEfficiency(players, "Schedule efficiency AFTER refinement:");
	
	// verify compliance of slot and player assignment -> slots feasible and players satisfied
		schedule.verifyCompliance(players);
		
	// write schedule
		schedule.write("Sommertraining_Einteilung.xlsx");

	}

}
