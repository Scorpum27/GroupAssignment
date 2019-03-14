package tcDietlikon;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class ScheduleWriter {

	XSSFWorkbook defaultWorkbook;
	Schedule schedule;
	CellStyle chapterCellStyle;
	List<XSSFCellStyle> cellStyles;
	XSSFCellStyle nSlotsXCellStyle;
	XSSFCellStyle sectionTitleStyle;
	CellStyle rightBorderCellStyle;
	CellStyle rightBorderCellStyleLight;
	CellStyle leftBorderCellStyle;
	XSSFCellStyle whiteCellStyle;
	XSSFCellStyle undesiredSlotStyle;
	XSSFCellStyle slotTitleCellStyle;
	CellStyle headerCellStyle;
	CellStyle headerCellStyle2;
	CellStyle leftAlignCellStyle;
	
	
	public ScheduleWriter() {
	}
	
	public ScheduleWriter(Schedule schedule) {
		this();
		this.schedule = schedule;
	}
	
	public void write(String fileName) throws IOException {
		
		XMLOps.writeToFile(this.schedule, "FinalSchedule.xml");
		XMLOps.writeToFile(this.schedule.players, "FinalSchedulePlayers.xml");
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		this.loadStylesAndWorkbook(workbook);
		XSSFSheet sheet = this.defaultWorkbook.createSheet("Wintertraining");

		List<Row> rows = new ArrayList<Row>();
		for (int r=0; r<=10+9*(this.schedule.lastHour-this.schedule.firstHour+1); r++) {
			rows.add(sheet.createRow(r));
		}
		
		int refRowNr;
		int refColNr;

		for (int time=this.schedule.firstHour; time<=this.schedule.lastHour; time++) {
			refRowNr = this.schedule.slot2row(time);
			Row row = rows.get(refRowNr);
			row.setRowStyle(slotTitleCellStyle);
			for (int day=1; day<=this.schedule.nDays; day++) {
				for (int court=1; court<=this.schedule.nCourts; court++) {
					refColNr = this.schedule.slot2col(day, court);
					Cell slotCell = row.createCell(refColNr);
					if (this.schedule.slots.containsKey(this.schedule.dayTimeCourt2slotId(day, time, court))) {
						slotCell.setCellValue(this.schedule.slot2name(day,time,court)+" ("+this.schedule.dayTimeCourt2slotId(day, time, court)+")");
					}
					else {
						slotCell.setCellValue("Tennishalle Halsrüti AG");
						// mark the cells that are not available for the tennis school as "blocked"
						for (int s=1; s<=8; s++) {
							Row blockedCellRow = rows.get(refRowNr+s);
							Cell blockedCell;
							if (blockedCellRow.getCell(refColNr)==null) {
								blockedCell = blockedCellRow.createCell(refColNr);
							}
							else {
								blockedCell = blockedCellRow.getCell(refColNr);
							}
							blockedCell.setCellValue("------  gesperrt  ------");
						}
					}
					slotCell.setCellStyle(slotTitleCellStyle);
				}
			}
		}
		
		for (Slot slot : this.schedule.slots.values()) {
			refColNr = this.schedule.slot2col(slot.weekdayNr, slot.courtNr);
			refRowNr = this.schedule.slot2row(slot.time);
			int playerNr = 1;
			for (Player playerUnit : slot.players.values()) {
				// a playerUnit may contain more than one player
				// if so, make a list of subProfiles that must be listed individually in the slot
				// else, subprofiles list is just the single player in the unit i.e. the unit itself
				List<Player> subProfiles = new ArrayList<Player>();
				if (playerUnit.subPlayerProfiles.size()==0) {
					subProfiles.add(playerUnit);
				}
				else {
					subProfiles.addAll(playerUnit.subPlayerProfiles);
				}
				for (Player player : subProfiles) {
					Row playerRow = rows.get(refRowNr + playerNr);
					Cell nameCell = playerRow.createCell(refColNr);
					// make sure to mark undesirable slots!
					if (slot.isFrozen) {
						nameCell.setCellValue("(**) "+player.name + " (" + player.linkablePlayers.size() + ")");
					}				
					else if (!player.isADesiredSlot(slot)) {
						nameCell.setCellStyle(undesiredSlotStyle);
						nameCell.setCellValue("(*) "+player.name + " (" + player.linkablePlayers.size() + ")");
					}
					// make sure to mark undesirable slots!
					else {
						// if the players are mustBeTogetherPeers, mark them with PP (Peer Player). else, just standard name format
						if (subProfiles.size()>1) {
							nameCell.setCellValue("(PP) "+player.name + " (" + playerUnit.linkablePlayers.size() + ")");							
						}
						else {
							nameCell.setCellValue(player.name + " (" + playerUnit.linkablePlayers.size() + ")");							
						}
						XSSFCellStyle newColorCellStyle = this.defaultWorkbook.createCellStyle();
						newColorCellStyle.cloneStyleFrom(nameCell.getCellStyle());
//					XSSFColor efficiencyColor = new XSSFColor(new Color(255,255,255));
						Color efficiencyColor = new Color(255,255,0);
						if (slot.category.equals("TC")) {
							if (Arrays.asList(7,8).contains(slot.getSize())) {
								efficiencyColor = new Color(0,128,0);		// green for very good usage of player tolerance
							}
							else if (Arrays.asList(5,6).contains(slot.getSize())){
								efficiencyColor = new Color(107,142,35);	// olive for good usage of player tolerance
							}
							else if (Arrays.asList(4).contains(slot.getSize())){
								efficiencyColor = new Color(154,205,50);	// light green for satisfying usage of player tolerance
							}
							else if (Arrays.asList(2,3).contains(slot.getSize())){
								efficiencyColor = new Color(255,255,0);	// yellow for bad usage of player tolerance
							}
							else if (Arrays.asList(1).contains(slot.getSize())){
								efficiencyColor = new Color(255,140,0);	// orange for very bad usage of player tolerance
							}
						}
						else {
							if (slot.getSize() > player.maxGroupSize) {
								efficiencyColor = new Color(32,178,170);	// blue for too full groups
							}
							else if (slot.getSize()==player.maxGroupSize) {
								efficiencyColor = new Color(0,128,0);
							}
							else if (slot.getSize()==player.maxGroupSize-1){
								efficiencyColor = new Color(154,205,50);	// light green for good usage of player tolerance
							}
							else if (slot.getSize()==player.maxGroupSize-2){
								efficiencyColor = new Color(255,255,0);	// yellow for bad usage of player tolerance
							}
							else if (slot.getSize()<=player.maxGroupSize-3){
								efficiencyColor = new Color(255,140,0);	// orange for very bad usage of player tolerance
							}						
						}
						newColorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
						newColorCellStyle.setFillForegroundColor(new XSSFColor(efficiencyColor));
						newColorCellStyle.setFillBackgroundColor(new XSSFColor(efficiencyColor));
						nameCell.setCellStyle(newColorCellStyle);
					}
					Cell maxGroupSizeCell = playerRow.createCell(refColNr + 1);
					maxGroupSizeCell.setCellValue(player.maxGroupSize);
					Cell classCell = playerRow.createCell(refColNr + 2);
					if (!player.category.equals("default")) 												{ classCell.setCellValue(player.category); }
					else if (player.category.equals("default") && 0<player.strength && player.strength<10) 	{ classCell.setCellValue("R" + player.strength); }
					else if (player.category.equals("default") && -4<player.strength && player.strength<1) 	{ classCell.setCellValue("N" + (4+player.strength)); }
					else 																					{ classCell.setCellValue("?default?"); }
					Cell ageCell = playerRow.createCell(refColNr + 3);
					ageCell.setCellValue(player.age);
					ageCell.setCellStyle(rightBorderCellStyleLight);
					playerNr++;
				}
			}
		}
	  
		// make list of players with nSlots not satisfied
		// --> for every player (maybe multiple times) make an entry with its name, groupsize strength and slots etc after saturday!
		int col = 72;	// make a colum with unsatisfied players after 6 days (3 courts w/ 4 columns) at 12 columns each
		int rr = 2;	// start at third row for list of unsatisfied players
		Row rrow;
		Cell nameCell;
		Cell classCell;
		Cell ageCell;
		Cell maxGroupSizeCell;
		for (Player playerUnit : this.schedule.players.values()) {
			int nUnsatisfiedSlots = playerUnit.nSlots-playerUnit.selectedSlots.size();
			if (nUnsatisfiedSlots>0) {
				// if playerUnit consists of several players, must note an undesired slot for all subPlayerProfiles
				// if so, make a list of subProfiles that must be listed individually
				// else, subprofiles list is just the single player in the unit i.e. the unit itself
				List<Player> subProfiles = new ArrayList<Player>();
				if (playerUnit.subPlayerProfiles.size()==0) {
					subProfiles.add(playerUnit);
				}
				else {
					subProfiles.addAll(playerUnit.subPlayerProfiles);
				}
				for (Player player : subProfiles) {
					if (sheet.getLastRowNum()<rr) {
						rrow = sheet.createRow(rr);
					}
					else {
						rrow = sheet.getRow(rr);					
					}
					nameCell = rrow.createCell(col+2);
					nameCell.setCellValue(player.age + " - " + player.name);
					nameCell.setCellStyle(undesiredSlotStyle);
					Cell borderCell = rrow.createCell(col+3);
					borderCell.setCellStyle(leftBorderCellStyle);
					classCell = rrow.createCell(col);
					classCell.setCellValue(player.strength2string());					
					classCell.setCellStyle(slotTitleCellStyle);
//				ageCell = rrow.createCell(col+1);
//				ageCell.setCellValue(player.age);
					maxGroupSizeCell = rrow.createCell(col+1);
					maxGroupSizeCell.setCellValue(player.maxGroupSize+"er");
					maxGroupSizeCell.setCellStyle(slotTitleCellStyle);
					rr++;
					for (int n=1; n<=nUnsatisfiedSlots; n++) {
						for (Slot slot : player.desiredSlots) {
							Row slotRow;
							if (sheet.getLastRowNum()<rr) {
								slotRow = sheet.createRow(rr);
							}
							else {
								slotRow = sheet.getRow(rr);							
							}
							Cell dayCell = slotRow.createCell(col);
							dayCell.setCellValue(Slot.dayNr2Name(slot.weekdayNr));
							Cell timeCell = slotRow.createCell(col+1);
							timeCell.setCellValue(slot.time);
							Cell whiteCell = slotRow.createCell(col+2);
							whiteCell.setCellValue("");
							whiteCell.setCellStyle(timeCell.getCellStyle());
							borderCell = slotRow.createCell(col+3);
							borderCell.setCellStyle(leftBorderCellStyle);
							rr++;
						}
					}
					
				}
				
			}
		}
				
		// Resize all columns to fit the content size
		for (int i = 0; i <= 250; i++) {
			sheet.autoSizeColumn(i);
		}
		
		// make lines to separate days (thick) and courts (thin)
		// figure out till where the lines should be drawn
		int maxRowCellNr = 0;
		for (int r=2; r<62; r++) {
			Row row = sheet.getRow(r);
			if (row.getLastCellNum()>maxRowCellNr) {
				maxRowCellNr = row.getLastCellNum();
			}
		}
		for (int r=2; r<=sheet.getLastRowNum(); r++) {
			Row row = sheet.getRow(r);
			for (int c=0; c<=maxRowCellNr; c++) {
				Cell cell;
				if (row.getCell(c)==null) {
					cell = row.createCell(c);
					if ((r-2)%9==0) {
						cell.setCellStyle(slotTitleCellStyle);
					}
				}
				else {
					cell = row.getCell(c);					
				}
				if ((c+1)%12==0) {
					CellStyle newCellStyle = this.defaultWorkbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THICK);
					cell.setCellStyle(newCellStyle);
				}
				else if ((c+1-4)%12==0) {
					CellStyle newCellStyle = this.defaultWorkbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THIN);
					cell.setCellStyle(newCellStyle);
				}
				else if ((c+1-8)%12==0) {
					CellStyle newCellStyle = this.defaultWorkbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THIN);
					cell.setCellStyle(newCellStyle);
				}
			}
		}

		// Make header after resizing so that first column is not super wide due to long title of first headerCell
		Row headerRow = rows.get(0);
		Cell titleCell = headerRow.createCell(0);
		titleCell.setCellValue("Tennisschule Cyrill Keller - Wintertraining");
		titleCell.setCellStyle(headerCellStyle);
		Cell totalSlotsCell = headerRow.createCell(6);
		totalSlotsCell.setCellValue("Total Gruppen (unter Vorbehalt nicht vollständig gesetzter Spieler) = "+this.schedule.totalUsedSots());
		totalSlotsCell.setCellStyle(headerCellStyle2);

		FileOutputStream fileOut = new FileOutputStream(fileName);
		this.defaultWorkbook.write(fileOut);
		fileOut.close();
		this.defaultWorkbook.close();
	}

	public void writeProposal(String proposalFileName) throws IOException {
		
		// make lists of unsatisfied players (undesirable or too few assignments)
		List<Player> undesirablyPlacedPlayers = new ArrayList<Player>();
		List<Player> notEnoughSlotsPlayers = new ArrayList<Player>();
		for (Player player : this.schedule.players.values()) {
			if (player.undesirablePlacements.size()>0) {
				undesirablyPlacedPlayers.add(player);
			}
			if (player.nSlots-player.selectedSlots.size()>0) {
				notEnoughSlotsPlayers.add(player);
			}
		}
		
		// for unsatisfied players, find sub-optimal slots that violate certain constraints, but conform to the others
		for (Player player : undesirablyPlacedPlayers) {
			// try groups with free spaces - not desired slot time, but all other constraints fulfilled (age, class, maxGroupSize, category(comes autom. w/ class))
			// start at player.maxGroupSize-1 and go down one by one (may stop at 1 or already at 2)
			for (int currentSlotSize=player.maxGroupSize-1; currentSlotSize>=1; currentSlotSize--) {
				slotLoop:
				for (Slot slot : this.schedule.slots.values()) {
					if (player.maxGroupSize < slot.getSize() + 1) {
						continue;
					}
					for (Player otherPlayer : slot.players.values()) {
						if (!player.isCompatibleWithOtherPlayer(otherPlayer)
								|| otherPlayer.maxGroupSize < slot.getSize() + 1) {
							continue slotLoop;
						}

					}
					// if code arrives here, can add slot as proposed slot
					String remark = "Zeit/Tag nicht als Prio angegeben, aber alle anderen Bedingungen erfüllt. ";
					for (Slot desiredSlot : player.desiredSlots) {
						if (desiredSlot.weekdayNr==slot.weekdayNr) {
							remark += "Der Tag wurde als Wunschtag angegeben, aber nicht die Zeit.";
							break;
						}
					}
					player.postProposedSlots.put(slot, remark);
				}			
			}
			// try groups with desired slot time, but age violation
			for (int currentSlotSize = player.maxGroupSize - 1; currentSlotSize >= 1; currentSlotSize--) {
				slotLoop:
				for (Slot slot : this.schedule.slots.values()) {
					if (!player.isADesiredSlot(slot)) {
						continue;
					}
					if (player.maxGroupSize < slot.getSize() + 1) {
						continue;
					}
					for (Player otherPlayer : slot.players.values()) {
						int ageDiff = Math.abs(player.age - otherPlayer.age);
						int classDiff = Math.abs(player.strength - otherPlayer.strength);
						// see that age difference constraint is loosened by two years
						if (ageDiff > player.maxAgeDiff + 2 || ageDiff > otherPlayer.maxAgeDiff + 2
								|| classDiff > player.maxClassDiff || classDiff > otherPlayer.maxClassDiff
								|| player.playerNr == otherPlayer.playerNr
								|| otherPlayer.maxGroupSize < slot.getSize() + 1) {
							continue slotLoop;
						}
					}
					// if code arrives here, can add slot as proposed slot
					String remark = "Zeit/Tag als Prio angegeben, aber Bedingungen zum Altersunterschied nicht erfüllt. ";
					int maxAgeDiffThisSlot = 0;
					for (Player otherPlayer : slot.players.values()) {
						int ageDiff = Math.abs(player.age - otherPlayer.age);
						if (ageDiff > maxAgeDiffThisSlot) {
							maxAgeDiffThisSlot = ageDiff;
						}
					}
					remark += "Maximale Altersdifferenz in dieser Gruppe wäre " + maxAgeDiffThisSlot + " Jahre.";
					player.postProposedSlots.put(slot, remark);
				}
				// make sure proposed slot is not one of the already undesirable ones :)
			}
		}
		for (Player player : notEnoughSlotsPlayers) {
			// try groups with free spaces - not desired slot time, but all other constraints fulfilled (age, class, maxGroupSize, category(comes autom. w/ class))
			// start at player.maxGroupSize-1 and go down one by one (may stop at 1 or already at 2)
			for (int currentSlotSize=player.maxGroupSize-1; currentSlotSize>=1; currentSlotSize--) {
				slotLoop:
				for (Slot slot : this.schedule.slots.values()) {
					if (player.maxGroupSize < slot.getSize() + 1) {
						continue;
					}
					for (Player otherPlayer : slot.players.values()) {
						if (!player.isCompatibleWithOtherPlayer(otherPlayer)
								|| otherPlayer.maxGroupSize < slot.getSize() + 1) {
							continue slotLoop;
						}

					}
					// if code arrives here, can add slot as proposed slot
					String remark = "Zeit/Tag nicht als Prio angegeben, aber alle anderen Bedingungen erfüllt. ";
					for (Slot desiredSlot : player.desiredSlots) {
						if (desiredSlot.weekdayNr==slot.weekdayNr) {
							remark += "Der Tag wurde als Wunschtag angegeben, aber nicht die Zeit.";
							break;
						}
					}
					player.postProposedSlots.put(slot, remark);
				}			
			}
			// try groups with desired slot time, but age violation
			for (int currentSlotSize = player.maxGroupSize - 1; currentSlotSize >= 1; currentSlotSize--) {
				slotLoop:
				for (Slot slot : this.schedule.slots.values()) {
					if (!player.isADesiredSlot(slot)) {
						continue;
					}
					if (player.maxGroupSize < slot.getSize() + 1) {
						continue;
					}
					for (Player otherPlayer : slot.players.values()) {
						int ageDiff = Math.abs(player.age - otherPlayer.age);
						int classDiff = Math.abs(player.strength - otherPlayer.strength);
						// see that age difference constraint is loosened by two years
						if (ageDiff > player.maxAgeDiff + 2 || ageDiff > otherPlayer.maxAgeDiff + 2
								|| classDiff > player.maxClassDiff || classDiff > otherPlayer.maxClassDiff
								|| player.playerNr == otherPlayer.playerNr
								|| otherPlayer.maxGroupSize < slot.getSize() + 1) {
							continue slotLoop;
						}
					}
					// if code arrives here, can add slot as proposed slot
					String remark = "Zeit/Tag als Prio angegeben, aber Bedingungen zum Altersunterschied nicht erfüllt. ";
					int maxAgeDiffThisSlot = 0;
					for (Player otherPlayer : slot.players.values()) {
						int ageDiff = Math.abs(player.age - otherPlayer.age);
						if (ageDiff > maxAgeDiffThisSlot) {
							maxAgeDiffThisSlot = ageDiff;
						}
					}
					remark += "Maximale Altersdifferenz in dieser Gruppe wäre " + maxAgeDiffThisSlot + " Jahre.";
					player.postProposedSlots.put(slot, remark);
				}
				// make sure proposed slot is not one of the already undesirable ones :)
			}
		}
		
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		// start with actual excel file here
		XSSFWorkbook workbook = new XSSFWorkbook();
		this.loadStylesAndWorkbook(workbook);
		XSSFSheet sheet = this.defaultWorkbook.createSheet("PlayerOverview");
		List<Row> rows = new ArrayList<Row>();
		for (int r=0; r<=5000; r++) {
			rows.add(sheet.createRow(r));
		}
		int refRowNr = 0;

		List<Integer> processedPlayers = new ArrayList<Integer>();
		Row chapterTitleRow;
		Cell chapterTitleCell;
		
		int category;
		String chapterTitle;
		refRowNr += 4;
		category = 0; // "notEnoughSlots";
		chapterTitle = "Einzuteilende Spieler";
		chapterTitleRow = rows.get(refRowNr);
		chapterTitleCell = chapterTitleRow.createCell(0);
		chapterTitleCell.setCellValue(chapterTitle);
		chapterTitleCell.setCellStyle(chapterCellStyle);
		refRowNr++;
		if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(notEnoughSlotsPlayers)==0) {
			Cell m1 = rows.get(refRowNr).createCell(0);
			Cell m2 = rows.get(refRowNr).createCell(1);
			m1.setCellValue("keine");
			m2.setCellValue("-");
			m1.setCellStyle(leftAlignCellStyle);
			m2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
		}
		else {
			for (Player player : notEnoughSlotsPlayers) {
				refRowNr = this.writePlayer2Table(player, refRowNr, category, chapterTitle, rows);
				
				// check if another same player profile must be placed here!!
				// XXX ...
				processedPlayers.add(player.playerNr);
			}			
		}
		refRowNr +=4;
		category = 1; // "undesirableSlots";
		chapterTitle = "Umzuteilende Spieler";
		chapterTitleRow = rows.get(refRowNr);
		chapterTitleCell = chapterTitleRow.createCell(0);
		chapterTitleCell.setCellValue(chapterTitle);
		chapterTitleCell.setCellStyle(chapterCellStyle);
		refRowNr++;
		if (PlayerUtils.getNumberOfIndividualPlayersFromPlayerList(undesirablyPlacedPlayers)==0) {
			Cell m1 = rows.get(refRowNr).createCell(0);
			Cell m2 = rows.get(refRowNr).createCell(1);
			m1.setCellValue("keine");
			m2.setCellValue("-");
			m1.setCellStyle(leftAlignCellStyle);
			m2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
		}
		else {
			for (Player player : undesirablyPlacedPlayers) {
				refRowNr = this.writePlayer2Table(player, refRowNr, category, chapterTitle, rows);
				// check if another same player profile must be placed here!!
				// XXX ...
				processedPlayers.add(player.playerNr);
			}			
		}
		refRowNr +=4;
		category = 2; // "desirableSlots";
		chapterTitle = "Zugeteilte Spieler";
		chapterTitleRow = rows.get(refRowNr);
		chapterTitleCell = chapterTitleRow.createCell(0);
		chapterTitleCell.setCellValue(chapterTitle);
		chapterTitleCell.setCellStyle(chapterCellStyle);
		refRowNr++;
		for (Player player : this.schedule.players.values()) {
			if (processedPlayers.contains(player.playerNr)) {
				continue;
			}
			if (player.nSlots - player.selectedSlots.size() > 0) {
				System.out.println(
						"CAUTION: Player is left in desirable category although it does not have all slots filled! It is player with ID="
								+ player.playerNr);
			}
			if (player.undesirablePlacements.size() > 0) {
				System.out.println(
						"CAUTION: Player is left in desirable category although it has undesirable slots! It is player with ID="
								+ player.playerNr);
			}
			refRowNr = this.writePlayer2Table(player, refRowNr, category, chapterTitle, rows);

			// check if another same player profile must be placed here!!
			// XXX ...
			processedPlayers.add(player.playerNr);		
		}
		
		// Resize all columns to fit the content size
		for (int i = 0; i <= 2; i++) {
//			sheet.autoSizeColumn(i);
			sheet.setColumnWidth(i, 6700);
		}
		
		// Make header after resizing so that first column is not super wide due to long title of first headerCell
		Font headerFont = this.defaultWorkbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());
		CellStyle headerCellStyle = this.defaultWorkbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		Row headerRow = rows.get(0);
		Cell titleCell = headerRow.createCell(0);
		titleCell.setCellValue("Tennisschule Cyrill Keller - Wintertraining Übersicht & Vorschläge");
		titleCell.setCellStyle(headerCellStyle);

		FileOutputStream fileOut = new FileOutputStream(proposalFileName);
		this.defaultWorkbook.write(fileOut);
		fileOut.close();
		this.defaultWorkbook.close();
		
	}

	private void loadStylesAndWorkbook(XSSFWorkbook workbook) {

		this.defaultWorkbook = workbook; // new HSSFWorkbook() for generating `.xls` file
		
		Font headerFont = defaultWorkbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());
		this.headerCellStyle = defaultWorkbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		
		Font headerFont2 = defaultWorkbook.createFont();
		headerFont2.setBold(true);
		headerFont2.setFontHeightInPoints((short) 14);
		this.headerCellStyle2 = defaultWorkbook.createCellStyle();
		headerCellStyle2.setFont(headerFont2);
		
		Font chapterFont = defaultWorkbook.createFont();
		chapterFont.setBold(true);
		chapterFont.setFontHeightInPoints((short) 18);
		this.chapterCellStyle = defaultWorkbook.createCellStyle();
		chapterCellStyle.setFont(chapterFont);
		
		this.cellStyles = new ArrayList<XSSFCellStyle>();
		List<Color> cellColors = Arrays.asList(new Color(255,99,71), new Color(255,140,0), new Color(0,128,0));
		for (Color color : cellColors) {
			Font playerFont = defaultWorkbook.createFont();
			playerFont.setBold(true);
			XSSFCellStyle playerCellStyle = defaultWorkbook.createCellStyle();
			playerCellStyle.setBorderTop(BorderStyle.THIN);
			playerCellStyle.setBorderBottom(BorderStyle.THIN);
			playerCellStyle.setFont(playerFont);
			playerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			XSSFColor slotCellColor = new XSSFColor(color);
			playerCellStyle.setFillBackgroundColor(slotCellColor);
			playerCellStyle.setFillForegroundColor(slotCellColor);
			cellStyles.add(playerCellStyle);
		}
		
		this.nSlotsXCellStyle = defaultWorkbook.createCellStyle();
		nSlotsXCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor nSlotsXCellColor = new XSSFColor(new Color(221,235,247));
		nSlotsXCellStyle.setFillBackgroundColor(nSlotsXCellColor);
		nSlotsXCellStyle.setFillForegroundColor(nSlotsXCellColor);
		nSlotsXCellStyle.setAlignment(HorizontalAlignment.LEFT);
		
		Font sectionTitleFont = defaultWorkbook.createFont();
		sectionTitleFont.setBold(true);
		this.sectionTitleStyle = defaultWorkbook.createCellStyle();
		sectionTitleStyle.setBorderTop(BorderStyle.THIN);
		sectionTitleStyle.setBorderBottom(BorderStyle.THIN);
		sectionTitleStyle.setFont(sectionTitleFont);
		
		Font slotTitleFont = defaultWorkbook.createFont();
		slotTitleFont.setBold(true);
		this.slotTitleCellStyle = defaultWorkbook.createCellStyle();
		slotTitleCellStyle.setBorderTop(BorderStyle.THIN);
		slotTitleCellStyle.setFont(slotTitleFont);
		slotTitleCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor slotTitleCellColor = new XSSFColor(new Color(220,220,220));
		slotTitleCellStyle.setFillBackgroundColor(slotTitleCellColor);
		slotTitleCellStyle.setFillForegroundColor(slotTitleCellColor);
		
		this.whiteCellStyle = defaultWorkbook.createCellStyle();
		whiteCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor whiteCellColor = new XSSFColor(new Color(255,255,255));
		whiteCellStyle.setFillBackgroundColor(whiteCellColor);
		whiteCellStyle.setFillForegroundColor(whiteCellColor);
		
		this.undesiredSlotStyle = defaultWorkbook.createCellStyle();
		undesiredSlotStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor undesiredCellColor = new XSSFColor(new Color(255,99,71));
		undesiredSlotStyle.setFillBackgroundColor(undesiredCellColor);
		undesiredSlotStyle.setFillForegroundColor(undesiredCellColor);
		
		this.rightBorderCellStyle = defaultWorkbook.createCellStyle();
		rightBorderCellStyle.setBorderRight(BorderStyle.THICK);
		this.rightBorderCellStyleLight = defaultWorkbook.createCellStyle();
		rightBorderCellStyleLight.setBorderRight(BorderStyle.THIN);
		
		this.leftBorderCellStyle = defaultWorkbook.createCellStyle();
		leftBorderCellStyle.setBorderLeft(BorderStyle.THICK);
		
		this.leftAlignCellStyle = defaultWorkbook.createCellStyle();
		leftAlignCellStyle.setAlignment(HorizontalAlignment.LEFT);;
		
	}

	private int writePlayer2Table(Player playerUnit, int refRowNr, int category, String chapterTitle, List<Row> rows) {
		
		// playerUnit may consist of several players
		// if so, make a list of subProfiles that must be listed individually
		// else, subprofiles list is just the single player in the unit i.e. the unit itself
		List<Player> subProfiles = new ArrayList<Player>();
		if (playerUnit.subPlayerProfiles.size()==0) {
			subProfiles.add(playerUnit);
		}
		else {
			subProfiles.addAll(playerUnit.subPlayerProfiles);
		}
		for (Player player : subProfiles) {
			Cell nameCell1 = rows.get(refRowNr).createCell(0);
			Cell nameCell2 = rows.get(refRowNr).createCell(1);
			nameCell1.setCellValue("Name");
			nameCell2.setCellValue(player.name);
			nameCell1.setCellStyle(cellStyles.get(category));
			nameCell2.setCellStyle(cellStyles.get(category));
			refRowNr++;
			Cell remarkCell1 = rows.get(refRowNr).createCell(0);
			Cell remarkCell2 = rows.get(refRowNr).createCell(1);
			remarkCell1.setCellValue("Spielerbemerkungen");
			if (player.samePersonPlayerProfiles.size()>0) {
				String samePlayerProfiles = "Gleicher Spieler hat noch andere Profile ";
				for (int s : player.samePersonPlayerProfiles) {
					Player samePlayer = this.schedule.players.get(s);
					for (Slot slot : samePlayer.selectedSlots) {
						samePlayerProfiles += "("+Slot.dayNr2Name(slot.weekdayNr)+"-"+slot.time+"h-Court"+slot.courtNr+")";
					}
				}
				remarkCell2.setCellValue(player.notes + "\r\n" + samePlayerProfiles);
			}
			else {
				remarkCell2.setCellValue(player.notes);
			}
			remarkCell2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
			Cell ageCell1 = rows.get(refRowNr).createCell(0);
			Cell ageCell2 = rows.get(refRowNr).createCell(1);
			ageCell1.setCellValue("Alter");
			ageCell2.setCellValue(player.age);
			ageCell2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
			Cell strengthCell1 = rows.get(refRowNr).createCell(0);
			Cell strengthCell2 = rows.get(refRowNr).createCell(1);
			strengthCell1.setCellValue("Spielstärke / Kategorie");
			strengthCell2.setCellValue(player.strength2string());
			strengthCell2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
			Cell sizeCell1 = rows.get(refRowNr).createCell(0);
			Cell sizeCell2 = rows.get(refRowNr).createCell(1);
			sizeCell1.setCellValue("Max. Gruppengrösse");
			sizeCell2.setCellValue(player.maxGroupSize);
			sizeCell2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
			Cell nSlotsCell1 = rows.get(refRowNr).createCell(0);
			Cell nSlotsCell2 = rows.get(refRowNr).createCell(1);
			nSlotsCell1.setCellValue("# Gewünschte Trainings");
			nSlotsCell2.setCellValue(player.nSlots);
			nSlotsCell2.setCellStyle(leftAlignCellStyle);
			refRowNr++;
			Cell nSlotsXCell1 = rows.get(refRowNr).createCell(0);
			Cell nSlotsXCell2 = rows.get(refRowNr).createCell(1);
			nSlotsXCell1.setCellValue("# Zu-/Umzuteilende Trainings");
			if (category == 0 || category == 1) {
				nSlotsXCell2.setCellValue(player.nSlots - player.selectedSlots.size() + player.undesirablePlacements.size());
			}
			if (category == 2) {
				nSlotsXCell2.setCellValue(0);
			}
			nSlotsXCell1.setCellStyle(nSlotsXCellStyle);
			nSlotsXCell2.setCellStyle(nSlotsXCellStyle);			
			refRowNr++;
			
			Cell selected1 = rows.get(refRowNr).createCell(0);
			Cell selected2 = rows.get(refRowNr).createCell(1);
			selected1.setCellValue("Zugeteilte Trainings");
			selected2.setCellValue("Bemerkungen");
			selected1.setCellStyle(sectionTitleStyle);
			selected2.setCellStyle(sectionTitleStyle);
			refRowNr++;
			
			if (player.selectedSlots.size()==0) {
				Cell m1 = rows.get(refRowNr).createCell(0);
				Cell m2 = rows.get(refRowNr).createCell(1);
				m1.setCellValue("keine");
				m2.setCellValue("-");
				m1.setCellStyle(leftAlignCellStyle);
				m2.setCellStyle(leftAlignCellStyle);
				refRowNr++;
			}
			else {
				for (Slot slot : player.selectedSlots) {
					String training = Slot.dayNr2Name(slot.weekdayNr) + " - " + slot.time + "h - Court " + slot.courtNr
							+ " - G" + slot.getSize();
					String remark = "";
					if (category == 1) { // undesired player category
						remark = "Unerwünschter Slot";
					} else {
						remark = "OK";
					}
					Cell slotCell = rows.get(refRowNr).createCell(0);
					Cell remarkCell = rows.get(refRowNr).createCell(1);
					slotCell.setCellValue(training);
					remarkCell.setCellValue(remark);
					refRowNr++;
				}
			}
			
			Cell desired1 = rows.get(refRowNr).createCell(0);
			Cell desired2 = rows.get(refRowNr).createCell(1);
			desired1.setCellValue("Wunschtermine");
			desired1.setCellStyle(sectionTitleStyle);
			desired2.setCellStyle(sectionTitleStyle);
			refRowNr++;
			
			if (player.desiredSlots.size()==0) {
				Cell m1 = rows.get(refRowNr).createCell(0);
				Cell m2 = rows.get(refRowNr).createCell(1);
				m1.setCellValue("keine");
				m2.setCellValue("-");
				m1.setCellStyle(leftAlignCellStyle);
				m2.setCellStyle(leftAlignCellStyle);
				refRowNr++;
			}
			else {
				for (Slot slot : player.desiredSlots) {
					String training = Slot.dayNr2Name(slot.weekdayNr)+" - "+slot.time+"h";
					String remark = "-";
					Cell slotCell = rows.get(refRowNr).createCell(0);
					Cell remarkCell = rows.get(refRowNr).createCell(1);
					slotCell.setCellValue(training);
					remarkCell.setCellValue(remark);
					refRowNr++;
				}
			}
			
			if (category==0 || category==1) {
				Cell alternative1 = rows.get(refRowNr).createCell(0);
				Cell alternative2 = rows.get(refRowNr).createCell(1);
				if (category==0) {
					alternative1.setCellValue("Vorschläge");				
				}
				if (category==1) {
					alternative1.setCellValue("Alternative Vorschläge");
				}
				alternative2.setCellValue("Bemerkungen");
				alternative1.setCellStyle(sectionTitleStyle);
				alternative2.setCellStyle(sectionTitleStyle);
				refRowNr++;
				
				if (player.postProposedSlots.size()==0) {
					Cell m1 = rows.get(refRowNr).createCell(0);
					Cell m2 = rows.get(refRowNr).createCell(1);
					m1.setCellValue("keine");
					m2.setCellValue("-");
					m1.setCellStyle(leftAlignCellStyle);
					m2.setCellStyle(leftAlignCellStyle);
					refRowNr++;
				}
				else {
					for (Entry<Slot,String> entry : player.postProposedSlots.entrySet()) {
						Slot slot = entry.getKey();
						String training = Slot.dayNr2Name(slot.weekdayNr)+" - "+slot.time+"h - Court "+slot.courtNr+" - G"+slot.getSize();
						String remark = entry.getValue();
						Cell slotCell = rows.get(refRowNr).createCell(0);
						Cell remarkCell = rows.get(refRowNr).createCell(1);
						slotCell.setCellValue(training);
						remarkCell.setCellValue(remark);
						refRowNr++;
					}
				}
			}
		}
		
		return refRowNr;
	}
	
}
