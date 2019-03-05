
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
// make list of most wanted slots and fill in accordingly maybe rather place players random/first desirable (initialPlacePlayers, currentlyOptimalReceiverSlot)
// XXX explicitly push players with undesired slots! --> may just change their current undesired slot to another slot and then be combinable!!
// IF player cannot be placed
// 1. try to make an initial push/pull (can also push a player to a smaller or yet empty slot)
//		--> see if this strategy is good or rather do not use point 1 -> tend to push highly linkable ones first
//		--> could also wait until all players are placed and then take out of basket and try to assign by pushing!
// 2. If fails, do not place at beginning (keep in basket)
// 3. At the end, place undesired players again with smaller groups, groups of 5, and completely empty slots!
// 4. also try to put single (or double!) players into already full groups at the very end to push or to get groups of 5 if need be
//	 --> source of problem may be: undesired placement takes place into a G4, then it can only be shifted if it comes out of G4, which is only possible into G3/G4
//	 --> may loosen placement rules at the end or not put in undesiredSlot at the start
//	 --> or at the start make an immediate push with another player --> and push those players who have highest linkability first!
//   --> (1) Can also push another player to a yet empty slot or just a less filled slot!!!
//      (2) make pull procedures - maybe more efficient!!
//	 --> make pull and push at the same time --> make potential push candidates lists (kickout tree with branches memory) and the pulls at the same time
//			--> if any of the branches for push and pull meet, we can connect the two!
//	 --> at the end: can push unsuccessulPlacements also to groups of 5 or push unsuccessful players to group of 4 and then kickout to group of five!
// XXX Dijkstra strategies!!
// - strategies to shuffle players into unused slots to open them up! (maybe whole groups?)
// - stagnation: reshuffle groups by shifting players from one to another improving their overall linkability or age/class differences --> then refine() again?
// - pull procedures
// - manual inputs for very strong players
// - Problem: slots change with every push -> do not know if path actually exists


// TUNING
// - pushLevel --> make list of most wanted slots and fill in accordingly maybe rather place players first in desirable slots!!! 
// - try pushGroupSize only until size (pushGroupSize<thisSlotSizeBeforePush-1) or just (pushGroupSize<thisSlotSizeBeforePush)
// - use 4 as pushGroup size in arrays or not
// - break or shift/push first? (current optimal = shift&push, then break)


//Next:
// - shift entire groups to a single player or to a slot, where single player could go
// --> make this general by saying that could pull from several slots to an empty slot!! (maybe specifically pull players together that are linkable)
// - pull/push tree procedures
// PROPOSAL SECTION
//	--> dates for first adding to unfull groups, than G5
//	--> also make list of availability, linkable players


package tcDietlikon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class ScheduleGenerator {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, EncryptedDocumentException, InvalidFormatException {
	
		int pushLevel = 5;
		boolean createNewPlayerSet = false;
		boolean useFixedSlotFile = false;
		int initialPlacementStrategy = 2;
		boolean doNotLoadSelectedSlots = true;
		boolean loadPlayers = true;
		boolean useFullSlotFilling = false;

	// create or load players
		Map<Integer,Player> players = new HashMap<Integer,Player>();
		// create random players with a reasonable distribution
		if (!loadPlayers) {
			if (createNewPlayerSet) {
				int nPlayers = 200;
				players = PlayerUtils.createPlayers(nPlayers);
				XMLOps.writeToFile(players, "samplePlayers.xml");
			}
			else {
				// load sample players from file			
				players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers_BENCHMARK.xml")); // samplePlayers_BENCHMARK
				if (doNotLoadSelectedSlots) { for (Player player : players.values()) {player.selectedSlots.clear();} }
			}
		}
		else {
			// load players from actual TCD registration form
			String playerRegistrationFile = "PlayersTCDietlikonWinter2018.xlsx";
			players = PlayerUtils.loadPlayers(playerRegistrationFile);
		}

	// for each player find all other players that can be assigned to the same group
		PlayerUtils.findLinkablePlayers(players);
		
	// create and fill in initial schedule (may follow specific strategies here instead of just filling in randomly)
		String courtScheduleFile = "Belegung_TennishalleDietlikon.xlsx";
		String fixedGroupsFile = "Fixe_Gruppen.xlsx";
		Schedule schedule = Schedule.initializeSchedule(players, courtScheduleFile, initialPlacementStrategy, fixedGroupsFile, useFixedSlotFile, useFullSlotFilling);
		schedule.verifyCompliance(players);

	// refine schedule to be more efficient
		schedule.calculateEfficiency(players, "Schedule efficiency BEFORE refinement:");
		schedule.refine(players, pushLevel);
		schedule.calculateEfficiency(players, "Schedule efficiency AFTER refinement:");
	
	// verify compliance of slot and player assignment -> slots feasible and players satisfied
		schedule.verifyCompliance(players);
		
	// write schedule
		ScheduleWriter scheduleWriter = new ScheduleWriter(schedule);
		scheduleWriter.write("EinteilungenTennisschuleKeller_2018Winter.xlsx");
		scheduleWriter.writeProposal("PlayerOverview_FurtherOptimization.xlsx");
	}

}
