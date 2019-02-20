package tcDietlikon;


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class reads and writes XML files, that do not have a MATSim specific Reader and Writer
* The methods are self-explanatory by their name
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class XMLOps {
	
	public static void writeToFile(Object object, String fileName) throws FileNotFoundException {
		String encodingType = Charset.defaultCharset().toString(); // Examples: "windows-1252", "UTF-8", Charset.defaultCharset().toString();

		if (fileName.contains("/")) {
			String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
			new File(filePath).mkdirs();
			System.out.println("Creating File Path: " + filePath);
		}
		
		FileOutputStream fos = new FileOutputStream(fileName);		
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!

		xstream.toXML(object, fos);
		System.out.println("Written to: " + fileName);
	}
	
	
	
	public static <T extends Object> T readFromFile(Class<T> type, String fileName) throws FileNotFoundException {	
		String encodingType = Charset.defaultCharset().toString(); // Examples: "windows-1252", "UTF-8", Charset.defaultCharset().toString();

		FileInputStream fis = new FileInputStream(fileName);
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		
		//XStream.setupDefaultSecurity(xstream);
		//xstream.allowTypes(new Class[] {type});
		Object object = xstream.fromXML(fis);
		System.out.println("Loaded: " + fileName);
		return type.cast(object);
	}
	
}
