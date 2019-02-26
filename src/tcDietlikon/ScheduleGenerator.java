
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
//		--> in initial place players
//		--> in currentlyOptimalReceiverSlot
// XXX DATA-SET may actually try lower push levels
// XXX DATA-SET may have to remove sender group size G4 again
// XXX Try to explicitly push players with undesired slots! --> may just change their current undesired slot to another slot and then be combinable!!
// XXX Dijkstra strategies!!
// - Where do the differences between actual total and desired total come from?
// - strategies to shuffle players into unused slots to open them up! (maybe whole groups?)
// - initial player placing --> maybe more random? (first best placement?)
// - stagnation: reshuffle groups by shifting players from one to another improving their overall linkability or age/class differences --> then refine() again?
// - pull procedures
// - manual inputs for very strong players
// - increase pushLevels for very long runs (maybe not even better!)
// - order of strategies (current optimal = shift&push, then break)
// - shift/push only until size, and not size-1 for possibly better performance (probably not better!)

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
		boolean createNewPlayerSet = false;
		boolean useFixedSlotFile = false;
		int initialPlacementStrategy = 2;

	// create or load players
		Map<Integer,Player> players = new HashMap<Integer,Player>();
		// create random players with a reasonable distribution
		if (createNewPlayerSet) {
			int nPlayers = 200;
			players = PlayerUtils.createPlayers(nPlayers);
			XMLOps.writeToFile(players, "samplePlayers.xml");
		}
		else {
			players.putAll(XMLOps.readFromFile(players.getClass(), "samplePlayers.xml"));			
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
