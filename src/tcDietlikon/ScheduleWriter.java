package tcDietlikon;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ScheduleWriter {

	Schedule schedule;
	
	public ScheduleWriter() {
	}
	
	public ScheduleWriter(Schedule schedule) {
		this.schedule = schedule;
	}
	
	public void write(String fileName) throws IOException {
		
		XMLOps.writeToFile(this.schedule, "FinalSchedule.xml");
		XMLOps.writeToFile(this.schedule.players, "FinalSchedulePlayers.xml");
		
		XSSFWorkbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
		XSSFSheet sheet = workbook.createSheet("Junioren Sommertraining");

		List<Row> rows = new ArrayList<Row>();
		for (int r=0; r<=10+9*(this.schedule.lastHour-this.schedule.firstHour+1); r++) {
			rows.add(sheet.createRow(r));
		}
		
		int refRowNr;
		int refColNr;
		
		Font slotTitleFont = workbook.createFont();
		slotTitleFont.setBold(true);
		XSSFCellStyle slotTitleCellStyle = workbook.createCellStyle();
		slotTitleCellStyle.setBorderTop(BorderStyle.THIN);
		slotTitleCellStyle.setFont(slotTitleFont);
		slotTitleCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor slotTitleCellColor = new XSSFColor(new Color(220,220,220));
		slotTitleCellStyle.setFillBackgroundColor(slotTitleCellColor);
		slotTitleCellStyle.setFillForegroundColor(slotTitleCellColor);
		

		for (int time=this.schedule.firstHour; time<=this.schedule.lastHour; time++) {
			refRowNr = this.schedule.slot2row(time);
			Row row = rows.get(refRowNr);
			row.setRowStyle(slotTitleCellStyle);
			for (int day=1; day<=this.schedule.nDays; day++) {
				for (int court=1; court<=this.schedule.nCourts; court++) {
					refColNr = this.schedule.slot2col(day, court);
					Cell slotCell = row.createCell(refColNr);
					slotCell.setCellValue(this.schedule.slot2name(day,time,court)+" ("+this.schedule.dayTimeCourt2slotId(day, time, court)+")");
					slotCell.setCellStyle(slotTitleCellStyle);
//					Cell slotIdCell = row.createCell(refColNr+1);
//					slotIdCell.setCellValue("("+this.dayTimeCourt2slotId(day, time, court)+")");
				}
			}
		}

		XSSFCellStyle whiteCellStyle = workbook.createCellStyle();
		whiteCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor whiteCellColor = new XSSFColor(new Color(255,255,255));
		whiteCellStyle.setFillBackgroundColor(whiteCellColor);
		whiteCellStyle.setFillForegroundColor(whiteCellColor);
		
		XSSFCellStyle undesiredSlotStyle = workbook.createCellStyle();
		undesiredSlotStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFColor undesiredCellColor = new XSSFColor(new Color(255,99,71));
		undesiredSlotStyle.setFillBackgroundColor(undesiredCellColor);
		undesiredSlotStyle.setFillForegroundColor(undesiredCellColor);
		
		CellStyle rightBorderCellStyle = workbook.createCellStyle();
		rightBorderCellStyle.setBorderRight(BorderStyle.THICK);
		CellStyle rightBorderCellStyleLight = workbook.createCellStyle();
		rightBorderCellStyleLight.setBorderRight(BorderStyle.THIN);
		
		CellStyle leftBorderCellStyle = workbook.createCellStyle();
		leftBorderCellStyle.setBorderLeft(BorderStyle.THICK);
		
		for (Slot slot : this.schedule.slots.values()) {
			refColNr = this.schedule.slot2col(slot.weekdayNr, slot.courtNr);
			refRowNr = this.schedule.slot2row(slot.time);
			int playerNr = 1;
			for (Player player : slot.players.values()) {
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
					nameCell.setCellValue(player.name + " (" + player.linkablePlayers.size() + ")");
					XSSFCellStyle newColorCellStyle = workbook.createCellStyle();
					newColorCellStyle.cloneStyleFrom(nameCell.getCellStyle());
//					XSSFColor efficiencyColor = new XSSFColor(new Color(255,255,255));
					Color efficiencyColor = new Color(255,255,0);
					if (slot.category.equals("TC")) {
						if (Arrays.asList(7,8).contains(slot.players.size())) {
							efficiencyColor = new Color(0,128,0);		// green for very good usage of player tolerance
						}
						else if (Arrays.asList(5,6).contains(slot.players.size())){
							efficiencyColor = new Color(107,142,35);	// olive for good usage of player tolerance
						}
						else if (Arrays.asList(4).contains(slot.players.size())){
							efficiencyColor = new Color(154,205,50);	// light green for satisfying usage of player tolerance
						}
						else if (Arrays.asList(2,3).contains(slot.players.size())){
							efficiencyColor = new Color(255,255,0);	// yellow for bad usage of player tolerance
						}
						else if (Arrays.asList(1).contains(slot.players.size())){
							efficiencyColor = new Color(255,140,0);	// orange for very bad usage of player tolerance
						}
					}
					else {
						if (slot.players.size() > player.maxGroupSize) {
							efficiencyColor = new Color(32,178,170);	// blue for too full groups
						}
						else if (slot.players.size()==player.maxGroupSize) {
							efficiencyColor = new Color(0,128,0);
						}
						else if (slot.players.size()==player.maxGroupSize-1){
							efficiencyColor = new Color(154,205,50);	// light green for good usage of player tolerance
						}
						else if (slot.players.size()==player.maxGroupSize-2){
							efficiencyColor = new Color(255,255,0);	// yellow for bad usage of player tolerance
						}
						else if (slot.players.size()<=player.maxGroupSize-3){
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
	  
		// make list of players with nSlots not satisfied
		int col = 72;	// make a colum with unsatisfied players after 6 days (3 courts w/ 4 columns) at 12 columns each
		int rr = 2;	// start at third row for list of unsatisfied players
		Row rrow;
		Cell nameCell;
		Cell classCell;
		Cell ageCell;
		Cell maxGroupSizeCell;
		for (Player player : this.schedule.players.values()) {
			int nUnsatisfiedSlots = player.nSlots-player.selectedSlots.size();
			if (nUnsatisfiedSlots>0) {
				rrow = sheet.getRow(rr);
				nameCell = rrow.createCell(col+2);
				nameCell.setCellValue(player.age + " - " + player.name);
				nameCell.setCellStyle(undesiredSlotStyle);
				Cell borderCell = rrow.createCell(col+3);
				borderCell.setCellStyle(leftBorderCellStyle);
				classCell = rrow.createCell(col);
				if (player.strength==20) {
					classCell.setCellValue("TC");					
				}
				else if (player.strength==21) {
					classCell.setCellValue("G");					
				}
				else if (player.strength==22) {
					classCell.setCellValue("O");					
				}
				else if (player.strength==23) {
					classCell.setCellValue("R");					
				}
				else if (1 <= player.strength && player.strength <= 9){
					classCell.setCellValue("R"+player.strength);
				}
				else if (-3 <= player.strength && player.strength <= 0) {
					classCell.setCellValue("N"+(4+player.strength));
				}
				else {
					classCell.setCellValue("??");
				}
				classCell.setCellStyle(slotTitleCellStyle);
//				ageCell = rrow.createCell(col+1);
//				ageCell.setCellValue(player.age);
				maxGroupSizeCell = rrow.createCell(col+1);
				maxGroupSizeCell.setCellValue(player.maxGroupSize+"er");
				maxGroupSizeCell.setCellStyle(slotTitleCellStyle);
				rr++;
				for (int n=1; n<=nUnsatisfiedSlots; n++) {
					for (Slot slot : player.desiredSlots) {
						Row slotRow = sheet.getRow(rr);
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
		
		
		// for every player (maybe multiple times) make an entry with its name, groupsize strength and slots etc after saturday!
		
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
					CellStyle newCellStyle = workbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THICK);
					cell.setCellStyle(newCellStyle);
				}
				else if ((c+1-4)%12==0) {
					CellStyle newCellStyle = workbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THIN);
					cell.setCellStyle(newCellStyle);
				}
				else if ((c+1-8)%12==0) {
					CellStyle newCellStyle = workbook.createCellStyle();
					newCellStyle.cloneStyleFrom(cell.getCellStyle());
					newCellStyle.setBorderRight(BorderStyle.THIN);
					cell.setCellStyle(newCellStyle);
				}
			}
//			Iterator<Cell> cellIter = row.cellIterator();
//			int cellNr = 0;
//			while (cellIter.hasNext()) {
//				Cell cell = cellIter.next();
//				if ((cellNr+1)%12==0) {
//					CellStyle currentCellStyle = cell.getCellStyle();
//					currentCellStyle.setBorderRight(BorderStyle.THICK);
//				}
//				else if ((cellNr+1-4)%12==0) {
//					CellStyle currentCellStyle = cell.getCellStyle();
//					currentCellStyle.setBorderRight(BorderStyle.THIN);
//				}
//				else if ((cellNr+1-8)%12==0) {
//					CellStyle currentCellStyle = cell.getCellStyle();
//					currentCellStyle.setBorderRight(BorderStyle.THIN);
//				}
//				cellNr++;
//			}
		}

		// Make header after resizing so that first column is not super wide due to long title of first headerCell
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		Row headerRow = rows.get(0);
		Cell titleCell = headerRow.createCell(0);
		titleCell.setCellValue("Tennisschule Cyrill Keller - Junioren, Sommertraining");
		titleCell.setCellStyle(headerCellStyle);

		FileOutputStream fileOut = new FileOutputStream(fileName);
		workbook.write(fileOut);
		fileOut.close();
		workbook.close();
	}
	
}
