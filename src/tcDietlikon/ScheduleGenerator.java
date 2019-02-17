
//  QUESTIONS
//   1. Wie schlimm ist die Benutzung des mittleren Felds?
//		- kein Problem
//   2. Was ist die minimale Spieleranzahl pro Gruppe?
//		- Privat/Halbprivat siehe Anmeldung
//   3. Was ist schlimmer, eine 5er-Gruppe oder wenn ein Spielzeitwunsch nicht berücksichtigt werden kann?
//		- Tel --> Make proposals --> dann 5er-Gruppe
//	 4. Verfügbare Courts? Innen/aussen?
//		- 3!
//	 5. Spätester Trainingsbeginn
//		- 1 Siehe Courtschedule
//   6. Unterschiede zum Wintertraining (um das Programm globaler zu machen)?
//		- Im Winter generell bis 2000 - dann Fixplätze vermietet (selten Ausnahmen)
//	 7. Lieber 3 oder 5 Spieler in einer Gruppe?
//		- 5 Spieler = letzte Option
//	 8. Lieber 3-3-3 oder 1-4-4 und dann den Einer irgendwo verschachteln
//		- 4er auffüllen wichtig generell

// Leute organisieren sich vorher und wollen alle in 3er Gruppe


//  IDEAS
//   1. May combine age and class difference in differenceFunction
//   2. May place stronger class more easily in a weaker group or vice versa?
//   3. Manual inputs for very strong players
//	 4. Player placement: May try first days, where the player can start playing early (not that important )
//	 5. Place player in biggest already existing group instead of first best group with lowest strategy!!
//	 6. General possibility for placing manually players
//	 7. Recalculate linkable players to a group after groups have been formed
//		--> use to decide e.g. whether player in G3 should be moved to a G2 making it a G3
//		--> do the push if overall linkability is improved
//	 8. After stagnation of improvement, reshuffle groups by shifting players from one to another (may implement some measure to keep linkability!)
//		--> 	[G2->G1]; [G3->G2]; [G4->G3];


//  CAUTION
//   1. schedule.placePlayer() never assigns player to court3 by design. can be changed anytime!


// - Add player attributes e.g. max player strength difference, group size constraints
// - Add current group size constraint of a schedule depending on its players
// - Adapt reassigning process with constraints and preferences

package tcDietlikon;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class ScheduleGenerator {

	public static void main(String[] args) throws IOException, EncryptedDocumentException, InvalidFormatException {
		
	// create the players (or load them)
		int nPlayers = 200;
		Map<Integer,Player> players = PlayerUtils.createPlayers(nPlayers);
		String playerRegistrationFile = "Template_Tennischule_Einteilung.xlsx";
//		Map<Integer,Player> players = PlayerUtils.loadPlayers(playerRegistrationFile);
		for (Player p : players.values()) {
//			System.out.println("Player = "+p.notes);
			for (Slot slot : p.desiredSlots) {
//				System.out.println("Weekday = "+ts.weekdayNr.toString());
//				System.out.println("TimeSlot = "+ts.slotNr.toString());				
			}
		}
		
		
		
	// for each player find all other players that can be assigned to the same group
		PlayerUtils.findLinkablePlayers(players);
		List<Player> playersSortedByPossibleCombinations = PlayerUtils.sortByPossibleCombinations(players);
				
	// create and fill in initial schedule (may follow specific strategies here instead of just filling in randomly)
		String courtScheduleFile = "Belegung_TennishalleDietlikon.xlsx";
		Schedule schedule = Schedule.initializeSchedule(players, playersSortedByPossibleCombinations, courtScheduleFile);
		
	// refine schedule to be more efficient
		System.out.println("Schedule efficiency BEFORE refinement:");
		schedule.calculateEfficiency();
		schedule.refine();
		System.out.println("Schedule efficiency AFTER refinement:");
		schedule.calculateEfficiency();
		
		// groups with two or less members must be reassigned to other groups or filled with members from other groups
		// for reassignment: check to which players (--> basket) a player can be assigned and try to put into that group
		// --> might need to remove another player (remove that one with the most combination possibilities to players not within this group)
		
	
		
	// write schedule
		schedule.write("Sommertraining_Einteilung.xlsx");

	}

}
