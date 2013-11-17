import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.lucene.util.OpenBitSet;


public class DecompileTester {
	
	public static File inputFolder;
	public static File vsFolder;
	public static File outputFolder;
	public static int divisor = 0;
	public static int sectionNumber = 0;
	public static int apkBufferLength = 500;

	
	  private static Options createOptions() {
		    Options options = new Options();
		    options.addOption("h", "help", false, "print this message and exit");
			options.addOption("i", "input", true, "input apk to test");
			options.addOption("v", "vs", true, "apk to test against");
			options.addOption("mc", "mainComponent", false, "extract main component from package name");
			options.addOption("ma", "mainActivity", false, "extract main activity from package");
			options.addOption("w", "whitelist", false, "generate whitelist for files; output class file is whitelist-classes.txt");
		    
		    return options;
	  }
	  
	  private static void showHelp(Options options) {
		  	System.out.println("Current working directory : " + System.getProperty("user.dir"));
		    HelpFormatter h = new HelpFormatter();
		    h.printHelp("help", options);
		    System.exit(-1);
		  }
	
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		
		boolean decompileAPK = false;
		boolean mcEnable = false;
		boolean maEnable = false;
		boolean wlEnable = false;
		boolean vsEnable = false;
		
		Options options = createOptions();
		try {
			// create the command line parser
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);
			String inputPath = cmd.getOptionValue("i"); 
			
			//Get the input and output paths. Default to output /tmp if no path present 
			if (inputPath != null)
				inputFolder = new File(inputPath);
			else{
				System.out.println("Please specify an input apk folder");
				showHelp(options);
			}
			
			String vsPath = cmd.getOptionValue("v"); 
			 
			if (vsPath != null){
				vsFolder = new File(vsPath);
				vsEnable = true;
			}
		
			
			 if(inputFolder.exists() && inputFolder.isDirectory()){
				decompileAPK = false;
			 }else if(inputFolder.exists() && inputFolder.isFile()){
				decompileAPK = true;
			 }else{
				System.out.println("Invalid input");
				showHelp(options);
			 }
				 

			if( cmd.hasOption( "mc" ) ) {
				mcEnable = true;
			}

			if( cmd.hasOption( "ma" ) ) {
				maEnable = true;
			}

			if( cmd.hasOption( "w" ) ) {
				wlEnable = true;
			}
			
			outputFolder = new File("tmp");
			
				 
		} catch (ParseException e1) {
			e1.printStackTrace();
			showHelp(options);
		}
		
		ApkDisassembler ad = new ApkDisassembler(inputFolder.getAbsolutePath(), outputFolder.getAbsolutePath());
		SmaliParser sp = new SmaliParser();
		BitSetBank bsb = new BitSetBank();
		File currentDir;
		
		if (decompileAPK){
			currentDir = ad.disassembleIndividualFile(inputFolder);
		}else
			currentDir = inputFolder;
		
		if (mcEnable){
			String mcDirPath = sp.getMainPackageName(currentDir);
			if(mcDirPath != null){
			File mcDir = sp.toMainFolder(currentDir.getAbsolutePath(), mcDirPath);
				if(mcDir != null){
					if (wlEnable)
						System.out.print("WhiteListed and ");
					System.out.println("Main Component From Package Folder");
					System.out.println("=========================");
					OpenBitSet logicVector = new OpenBitSet(sp.logicFeaturesCount);
					OpenBitSet contentVector = new OpenBitSet(sp.contentFeaturesCount);
					AppVector appVector = new AppVector(logicVector, contentVector);
					int folderNameLength = currentDir.getAbsolutePath().length();
					sp.listFilesForFolder(mcDir, logicVector, folderNameLength, wlEnable);
					System.out.println("Number of Bits set: " + logicVector.cardinality());
					System.out.println(sp.recognizedHashMap);
				}else
					System.out.println("Main Component From Package not Found");
			}else
				System.out.println("Main Component From Package not Found");
		}
		
		if (maEnable){
			File maDir = sp.toMainActivityFolder(currentDir.getAbsolutePath());
			if(maDir != null){
				if (wlEnable)
					System.out.print("WhiteListed and ");
				System.out.println("Main Activity Folder");
				System.out.println("=========================");
				OpenBitSet logicVector = new OpenBitSet(sp.logicFeaturesCount);
				int folderNameLength = currentDir.getAbsolutePath().length();
				sp.listFilesForFolder(maDir, logicVector, folderNameLength, wlEnable);
				System.out.println("Number of Bits set: " + logicVector.cardinality());
				System.out.println(sp.recognizedHashMap);
			}else
				System.out.println("Main Activity not Found");
		}
		
		if(vsEnable){
			if (wlEnable){
				System.out.print("WhiteListed ");
			}
			
			OpenBitSet logicVector1 = new OpenBitSet(sp.logicFeaturesCount);
			OpenBitSet contentVector1 = new OpenBitSet(sp.contentFeaturesCount);
			OpenBitSet logicVector2 = new OpenBitSet(sp.logicFeaturesCount);
			OpenBitSet contentVector2 = new OpenBitSet(sp.contentFeaturesCount);
			AppVector appVector = new AppVector(logicVector1, contentVector1);
			sp.apkDirectoryTraversal(currentDir, logicVector1, contentVector1, wlEnable);
			
			 if(inputFolder.exists() && inputFolder.isDirectory()){
				 currentDir = vsFolder;
			 }else if(inputFolder.exists() && inputFolder.isFile()){
				 currentDir = ad.disassembleIndividualFile(vsFolder);
			 }
			
			AppVector appVector2 = new AppVector(logicVector2, contentVector2);
			sp.apkDirectoryTraversal(currentDir, logicVector2, contentVector2, wlEnable);
			//System.out.println(sp.recognizedHashMap);
			
			double jsim = bsb.JaccardSim(logicVector1, logicVector2);
			System.out.println(jsim + " " + logicVector1.cardinality() + " " + logicVector2.cardinality());
		}
		
		if(!maEnable && !mcEnable && !vsEnable){
//			if (wlEnable){
//				System.out.print("WhiteListed ");
//			}
//			
//			System.out.println("Results");
//			System.out.println("=========================");

			OpenBitSet logicVector = new OpenBitSet(sp.logicFeaturesCount);
			OpenBitSet contentVector = new OpenBitSet(sp.contentFeaturesCount);
			AppVector appVector = new AppVector(logicVector, contentVector);
			
			sp.apkDirectoryTraversal(currentDir, logicVector, contentVector, wlEnable);
			printMap(sp.recognizedHashMap);
			
//			System.out.println("\nNumber of Logic Bits set: " + logicVector.cardinality());
		}
		
		//bsb.add(currentDir.getName(), bitSet);
		
		long endTime = System.currentTimeMillis();
		
		
		//System.out.println("\nTotal time: " + (endTime - startTime) + " ms");
		
	}
	
	public static void printMap(HashMap<String, Long> map) {
			for (Iterator<Map.Entry<String, Long>> iter1 = map.entrySet().iterator(); iter1.hasNext();) {
				Map.Entry<String, Long> entry1 = iter1.next();
				System.out.println(entry1.getKey());

			}
	}
	
	public static void printMap2(HashMap<String, Integer> map) {
		for (Iterator<Map.Entry<String, Integer>> iter1 = map.entrySet().iterator(); iter1.hasNext();) {
			Map.Entry<String, Integer> entry1 = iter1.next();
			System.out.println(entry1.getKey());

		}
}
}