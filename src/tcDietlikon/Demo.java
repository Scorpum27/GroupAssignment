package tcDietlikon;

import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

public class Demo {

	public static void main(String[] args) throws EncryptedDocumentException, InvalidFormatException, IOException {

		PlayerUtils.loadPlayers("Template_Tennischule_Einteilung.xlsx");

	}

}
