package tcDietlikon;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class holds different methods to log events during the simulation and evolution
* The default method used is Log.write(...)
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {

	// if not declared otherwise, the default log file is given as:
	protected static String defaultLogFile = "LogDefault.txt";
	protected static String defaultEvoLogFile = "LogEvo.txt";
	
	// default method
	public static void write(String comment) throws IOException {
		write(defaultLogFile, comment);
	}

	public static void writeEvo(String comment) throws IOException {
		write(defaultEvoLogFile, comment);
	}
	
	public static void writeAndDisplay(String comment) throws IOException {
		System.out.println(comment);
		write(defaultLogFile, comment);
	}	

	public static void write(String f, String comment) throws IOException {
		// TimeZone tz = TimeZone.getTimeZone("EST"); // or PST, MID, etc ...
		// Date now = new Date();
		// DateFormat df = new SimpleDateFormat ("yyyy.mm.dd hh:mm:ss ");
		// df.setTimeZone(tz);
		// String currentTime = df.format(now);

		FileWriter aWriter = new FileWriter(f, true);
		// aWriter.write(currentTime + " " + s + "\n");
		aWriter.write((new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()) + " |  " + comment + "\r\n");
		aWriter.flush();
		aWriter.close();
	}
	
	public static void writeSameLine(String f, String comment) throws IOException {
		FileWriter aWriter = new FileWriter(f, true);
		// aWriter.write(currentTime + " " + s + "\n");
		aWriter.write(comment);
		aWriter.flush();
		aWriter.close();
	}
	
	public static void write1(String comment) {
		
		Logger logger = Logger.getLogger("MyLog");
		FileHandler fh;

		try {

			// This block configure the logger with handler and formatter
			fh = new FileHandler("C:Users/Sascha/eclipse-workspace/MATSim-Workspace/MasterThesis/zurich_1pm/Evolution/Population/PopulationEvolutionLog.log");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			// the following statement is used to log any messages
			logger.info(comment);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//logger.info("Hi How r u?");
	}

	public static void writeSameLine(String comment) throws IOException {
		writeSameLine(defaultLogFile, comment);
	}
	
	public static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}

}
