import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.util.OpenBitSet;


public class SmaliParser {

	private static final String manifestName = "AndroidManifest.xml";
	private static final String packageIndicator = "package=\"";
	private static final String separatorSign = "/";
	/* Store the main activity and package name of an app */
	public String mainActivity = null;
	public String mainPackage = null;

	private static final String contentMapPath = "content_extensions.txt";
	private static final String logicMapPath = "smali-methods.txt";
	private static final String outputFeaturePath = "testRun.txt";
	private static final String whiteListLibraries = "whitelist_librariesDB.txt";
	private static final String permissionsMapPath = "permissions.txt";
	
	public static final int contentBitSize = 32;

	public HashMap<String, Integer> featuresHashMap;
	public HashMap<String, Long> recognizedHashMap;
	public HashMap<String, Long> unRecognizedHashMap;
	public HashMap<String, Integer> whiteListHashMap;
	public HashMap<String, Integer> contentHashMap;
	public HashMap<String, Integer> permissionsHashMap;
	public int bitSetsCount;
	public long recCount;
	public long unRecCount;
	public long totalCount;
	public long indivdualMethodCount;
	public double jaccardThreshold = .70;
	public long dirCount;
	public int logicFeaturesCount;
	public int contentFeaturesCount;
	public int filesInContentVector;
	public int failedApk;

	


	public SmaliParser() {
		this.contentHashMap = new HashMap<String, Integer>();
		this.featuresHashMap = new HashMap<String, Integer>();
		this.recognizedHashMap = new HashMap<String, Long>();
		this.unRecognizedHashMap = new HashMap<String, Long>();
		this.whiteListHashMap = new HashMap<String, Integer>();
		this.permissionsHashMap = new HashMap<String, Integer>();
		this.recCount = 0;
		this.unRecCount = 0;
		this.totalCount = 0;
		this.indivdualMethodCount = 0;
		this.bitSetsCount = 0;
		this.dirCount = 0;
		this.failedApk = 0;
		this.logicFeaturesCount = 0; //keeps track of the total number of feature vectors for logic vector.
		this.contentFeaturesCount = 0;	//keeps track of the total number of feature vectors for content vector. 
		this.loadLogicHashMap();
		this.loadContentHashMap();
		this.loadWhitelistLibs();
		this.loadPermissionsHashMap();
								
	}

//	public static void main(String[] args) {
//		long startTime = System.currentTimeMillis();
//		SmaliParser smaliParser = new SmaliParser();
//		//smaliParser.topLevelTraversal(smaliParser.folder);
//		long endSmaliParseTime = System.currentTimeMillis();
//
//		double recPercent = 100 * (double) smaliParser.recognizedHashMap.size()
//				/ (double) smaliParser.indivdualMethodCount;
//		double unRecPercent = 100
//				* (double) smaliParser.unRecognizedHashMap.size()
//				/ (double) smaliParser.indivdualMethodCount;
//		double recCoverage = 100
//				* (double) smaliParser.recognizedHashMap.size()
//				/ (double) smaliParser.featuresHashMap.size();
//		double unRecCoverage = 100
//				* (double) smaliParser.unRecognizedHashMap.size()
//				/ (double) smaliParser.featuresHashMap.size();
//
//		System.out.println("Recognized: "
//				+ smaliParser.recognizedHashMap.size() + " or " + recPercent
//				+ "%");
//		System.out.println("Unrecognized: "
//				+ smaliParser.unRecognizedHashMap.size() + " or "
//				+ unRecPercent + "%");
//		System.out.println("Recognized Coverage: " + recCoverage + "%");
//		System.out.println("Unrecognized Coverage: " + unRecCoverage + "%");
//		System.out.println("Total Features in Hash: "
//				+ smaliParser.featuresHashMap.size());
//		System.out.println("Total Distinct Methods Found: "
//				+ smaliParser.indivdualMethodCount);
//		System.out.println("Total Methods Parsed: " + smaliParser.totalCount
//				+ "\n");
//
//		System.out.println("\nTotal time: " + (cmpEndTime - startTime)
//				+ " ms for " + bitSetHashSize + " bitSets");
//		System.out.println("Parse time: " + (endSmaliParseTime - startTime)
//				+ " ms or " + (double) (endSmaliParseTime - startTime)
//				/ (double) bitSetHashSize + " ms/bitSet");
//		System.out.println("Comparison time: " + (cmpEndTime - cmpStartTime)
//				+ " ms or " + (double) (cmpEndTime - cmpStartTime)
//				/ (double) bitSetHashSize + " ms/bitSet");
//
//	}

	private void parseDelimitedFile(String filePath, OpenBitSet bitSet)
			throws Exception {

		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		String currentRecord;
		String delimiter = "\\s+";
		String tokens[];
		String token;
		
		//System.out.println(filePath);

		while ((currentRecord = br.readLine()) != null) {
			if (currentRecord.contains("invoke")) {
				tokens = currentRecord.split(delimiter);
				token = tokens[tokens.length - 1];

				//if (featuresHashMap.containsKey(token)) 
				//	bitSet.fastSet(featuresHashMap.get(token));
				if (featuresHashMap.containsKey(token)) {
					bitSet.fastSet(featuresHashMap.get(token));
					if (!recognizedHashMap.containsKey(token)) {
						//bw.write("R: " + token + "\n");
						indivdualMethodCount++;
						recognizedHashMap.put(token, (long)featuresHashMap.get(token));
					}

				} else {
					if (!unRecognizedHashMap.containsKey(token)) {
						//bw.write("U: " + token + "\n");
						indivdualMethodCount++;
						unRecognizedHashMap.put(token, indivdualMethodCount);
					}
				}
			}
		}
		br.close();
		fr.close();

	}
	
	public void parseDelimitedFile(String filePath, OpenBitSet bitSet, boolean debug)
			throws Exception {

		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		String currentRecord;
		String delimiter = "\\s+";
		String tokens[];
		String token;

		//File file = new File(outputFeaturePath);

		// if file doesnt exists, then create it
		//if (!file.exists()) {
		//	file.createNewFile();
		//}

		//FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
		//BufferedWriter bw = new BufferedWriter(fw);

		while ((currentRecord = br.readLine()) != null) {
			if (currentRecord.contains("invoke")) {
				tokens = currentRecord.split(delimiter);
				token = tokens[tokens.length - 1];
				this.totalCount++;

				// if (token.startsWith("Landroid")){
				if (featuresHashMap.containsKey(token)) {
					bitSet.fastSet(featuresHashMap.get(token));
					if (!recognizedHashMap.containsKey(token)) {
						//bw.write("R: " + token + "\n");
						indivdualMethodCount++;
						recognizedHashMap.put(token, (long)featuresHashMap.get(token));
					}

				} else {
					if (!unRecognizedHashMap.containsKey(token)) {
						//bw.write("U: " + token + "\n");
						indivdualMethodCount++;
						unRecognizedHashMap.put(token, indivdualMethodCount);
					}
				}
				// }

			}
		}

		//bw.close();
		br.close();
		//fw.close();
		fr.close();
	}

	public void parseManifestXML(String filePath, OpenBitSet bitSet) {
		FileReader fr = null;
		BufferedReader br = null;
		String currentRecord = null;
		Boolean activityNotFound = true, packageNotFound = true;
		String nearestName = null;
		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);	
			while ((currentRecord = br.readLine()) != null) {
				/* Get permission. */
				if (currentRecord.contains("<uses-permission") && currentRecord.contains("android:name=\"")) {
					currentRecord = getNameAttr(currentRecord);
					if (permissionsHashMap.containsKey(currentRecord)) {
						bitSet.fastSet(permissionsHashMap.get(currentRecord));
						continue;
					}
				}
				/* Get main activity name. */
				if (activityNotFound && currentRecord.contains("<activity") && currentRecord.contains("android:name=\""))
					nearestName = getNameAttr(currentRecord);
				else if (currentRecord.contains("android.intent.action.MAIN")) {
					activityNotFound = false;
				}
				/* Get main package of the app. */
				if(packageNotFound) {
					mainPackage = extractPackageName(currentRecord);
					if (mainPackage != null)
						packageNotFound = false;
				}
				//TODO: possibly get features
				//TODO: possibly get intents
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fr != null && br != null) {
				try {
					br.close();
					fr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		/* Main activity found. */
		if(!activityNotFound)
			mainActivity = nearestName;
	}
	public String getNameAttr(String currentRecord){
		currentRecord = currentRecord.substring(currentRecord.indexOf("android:name=\"") + 14);
		currentRecord = currentRecord.substring(0, currentRecord.indexOf('"'));
		return currentRecord;
	}
	
	private void loadLogicHashMap() {
		try {
			InputStream is = getClass().getResourceAsStream(logicMapPath);
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader in = new BufferedReader(isr);
//			BufferedReader in = new BufferedReader(new FileReader(logicMapPath));
			String str;
			
			while ((str = in.readLine()) != null) {
				featuresHashMap.put(str, logicFeaturesCount);
				logicFeaturesCount++;
			}
			// Close buffered reader
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void loadContentHashMap() {
		try {
			InputStream is = getClass().getResourceAsStream(contentMapPath);
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader in = new BufferedReader(isr);
			//BufferedReader in = new BufferedReader(new FileReader(contentMapPath));
			String str;
			
			while ((str = in.readLine()) != null) {
				Integer count = contentHashMap.get(str);
				if (count == null) {
					contentHashMap.put(str, contentFeaturesCount);
					contentFeaturesCount++;
				}
			}
			
			contentFeaturesCount = contentFeaturesCount*contentBitSize;
			// Close buffered reader
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void loadWhitelistLibs() {
		try {
			InputStream is = getClass().getResourceAsStream(whiteListLibraries);
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader in = new BufferedReader(isr);
//			BufferedReader in = new BufferedReader(new FileReader(whiteListLibraries));
			String str;
			
			while ((str = in.readLine()) != null) {
				Integer count = whiteListHashMap.get(str);
				if (count == null) {
					str.replace("/", separatorSign);
					whiteListHashMap.put(str, contentFeaturesCount);
					contentFeaturesCount++;
				}
			}
			// Close buffered reader
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loadPermissionsHashMap(String filePath) {
		try {
			InputStream is = getClass().getResourceAsStream(filePath);
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader in = new BufferedReader(isr);
//			BufferedReader in = new BufferedReader(new FileReader(filePath));
			String str;

			while ((str = in.readLine()) != null) {
				str.replace("/", separatorSign);
				permissionsHashMap.put(str, contentFeaturesCount);
				contentFeaturesCount++;
			}
			
			// Close buffered reader
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	private void loadPermissionsHashMap() {
		loadPermissionsHashMap(permissionsMapPath);
	}
	
	private Boolean isWhitelisted(String fileEntryName) {
		if (whiteListHashMap.containsKey(fileEntryName)) {
			return true;
		}
		return false;
	}

	//Traverses root folder housing all the decompiled apk folders
//	public void apkRootTraversal(File folder, BitSetBank bsb) {
//		try {
//
//			for (File fileEntry : folder.listFiles()) {
//				dirCount++;
//
//				if (fileEntry.isDirectory()) {
//					
//					String packageName = getMainPackageName(folder);
//
//					if (packageName == null)
//						continue;
//					
//					fileEntry = toMainFolder(packageName);
//					if (fileEntry == null)
//						continue;
//
//					OpenBitSet bitSet = new OpenBitSet(this.featuresCount);
//					listFilesForFolder(fileEntry, bitSet);
//					bsb.add(fileEntry.getName(), bitSet);
//
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	//Traverses the decompiled folder that was produced from an APK
	public void apkDirectoryTraversal(File folder, OpenBitSet lVector, OpenBitSet cVector) {
		int[] contentCount = new int[contentHashMap.size()];
		String absPath = null;
		
		absPath = folder.getAbsolutePath();
		/* Parse AndroidManifest.xml. */
		this.parseManifestXML(absPath + separatorSign + manifestName, cVector);
		
		try {
			for (File fileEntry : folder.listFiles()) {
				/* Go into folder named "smali" */
				if (fileEntry.getAbsolutePath().endsWith("smali")) {
					
					int folderNameLength = absPath.length();
					searchAdLibs(fileEntry, lVector, folderNameLength);
					File mainComp = toMainFolder(absPath, mainPackage);
					//System.out.println("MAIN COMP:" + mainComp.getAbsolutePath());
					//System.out.println("MAIN COMP:" + mainActivity);
					
					listFilesForFolder(mainComp, lVector, folderNameLength);
					
					File mainActivity = toMainActivityFolder(absPath);
					
					if (mainActivity != null &&
						!mainActivity.getAbsolutePath().equals(mainComp.getAbsolutePath())){
						listFilesForFolder(mainComp, lVector, folderNameLength);
						//System.out.println("MAIN ACTIVITY:" + mainActivity.getAbsolutePath());
					}

					
				}else if (fileEntry.isDirectory()){
					listContentForFolder(fileEntry,contentCount);
				}
				//Else fileEntry is a file & do nothing
			}
		} catch (Exception e) {
			System.out.println(folder.getName() + " failed decompilation");
			failedApk++;
			e.printStackTrace();
			return;
		}
		
		createContentBitVector(cVector, contentCount);
		
	}
	
	//Traverses the decompiled folder that was produced from an APK. This method is used by
	//apkTester to test individual apks
	public void apkDirectoryTraversal(File folder, OpenBitSet lVector, OpenBitSet cVector,  boolean whiteListEnable) {
		
		int[] contentCount = new int[contentHashMap.size()];
		String absPath = null;
		
		absPath = folder.getAbsolutePath();
		/* Parse AndroidManifest.xml. */
		this.parseManifestXML(absPath + separatorSign + manifestName, cVector);
		
		try {
			for (File fileEntry : folder.listFiles()) {
				/* Go into folder named "smali" */
				if (fileEntry.getAbsolutePath().endsWith("smali")) {
					
					int folderNameLength = absPath.length();
					searchAdLibs(fileEntry, lVector, folderNameLength);
					File mainComp = toMainFolder(absPath, mainPackage);
					//System.out.println("MAIN COMP:" + mainComp.getAbsolutePath());
					//System.out.println("MAIN COMP:" + mainActivity);
					
					listFilesForFolder(mainComp, lVector, folderNameLength,true);
					
					File mainActivity = toMainActivityFolder(absPath);
					
					if (mainActivity != null &&
						!mainActivity.getAbsolutePath().equals(mainComp.getAbsolutePath())){
						listFilesForFolder(mainComp, lVector, folderNameLength, true);
						//System.out.println("MAIN ACTIVITY:" + mainActivity.getAbsolutePath());
					}

					
				}else if (fileEntry.isDirectory()){
					listContentForFolder(fileEntry,contentCount);
				}
				//Else fileEntry is a file & do nothing
			}
		} catch (Exception e) {
			failedApk++;
			e.printStackTrace();
			return;
		}
		
		createContentBitVector(cVector, contentCount);
		
	}

	public void listContentForFolder(File folder, int[] contentCount) {
		String ext = null;
		Integer idx = 0;
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listContentForFolder(fileEntry, contentCount);
			} else {
				try {
					ext = FilenameUtils.getExtension(fileEntry.getName());
					idx = contentHashMap.get(ext);
					if (idx != null)
						contentCount[idx]++;
					
				} catch (Exception e) {
					System.out.println(fileEntry.getAbsolutePath() + " Failed Content Count");
					System.out.println("idx = " + idx +  " ext = " + ext + " hashSize = " + contentHashMap.size() + " arraySize = " +contentCount.length);
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	public void searchAdLibs(File folder, OpenBitSet bitSet, int folderNameLength)  {
		for (File fileEntry : folder.listFiles()) {
			if (isWhitelisted(fileEntry.getAbsolutePath().substring(folderNameLength))) {
				//bitSet.fastSet(whiteListHashMap.get(fileEntry.getAbsolutePath().substring(folderNameLength)));
			//	System.out.println("WHITELISTED: " + fileEntry.getPath());
				continue;
			}

			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry, bitSet, folderNameLength);
			}
		}
	}
	
	public void listFilesForFolder(File folder, OpenBitSet bitSet, int folderNameLength) {
		for (File fileEntry : folder.listFiles()) {
			if (isWhitelisted(fileEntry.getAbsolutePath().substring(folderNameLength))) {
				//bitSet.fastSet(whiteListHashMap.get(fileEntry.getAbsolutePath().substring(folderNameLength)));
			//	System.out.println("WHITELISTED: " + fileEntry.getPath());
				continue;
			}

			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry, bitSet, folderNameLength);
			} else {
				try {
					this.parseDelimitedFile(fileEntry.getAbsolutePath(), bitSet);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listFilesForFolder(File folder, OpenBitSet bitSet, int folderNameLength, boolean whiteListEnable) {
		for (File fileEntry : folder.listFiles()) {
			if (whiteListEnable && isWhitelisted(fileEntry.getAbsolutePath().substring(folderNameLength))) {
				//bitSet.fastSet(whiteListHashMap.get(fileEntry.getAbsolutePath().substring(folderNameLength)));
				//System.out.println("WHITELISTED: " + fileEntry.getAbsolutePath().substring(folderNameLength));
				continue;
			}

			if (fileEntry.isDirectory()) {
				//System.out.println("\t" + fileEntry.getAbsolutePath().substring(folderNameLength));
				listFilesForFolder(fileEntry, bitSet, folderNameLength, whiteListEnable);
			} else {
				try {
					//System.out.println("\t\t" + fileEntry.getName());
					this.parseDelimitedFile(fileEntry.getAbsolutePath(), bitSet, true);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}


	/*
	 * check Manifest.xml, if has get package name else return NULL means it is
	 * not a smali folder
	 */
	public String getMainPackageName(File folder) {
		
		String buff;
		BufferedReader in = null;

		try {
			// FileInputStream fs = new FileInputStream(manifestName);
			in = new BufferedReader(new FileReader(folder.getAbsolutePath()
					+ separatorSign + manifestName));

			while ((buff = in.readLine()) != null) {

				buff = extractPackageName(buff);
				if (buff != null) {
					in.close();
					return buff;
				}
			}

		} catch (Exception e) {
			// do nothing
		}

		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String extractPackageName(String line) {

		int index = line.indexOf(packageIndicator);
		if (index == -1)
			return null;
		else
			index += packageIndicator.length(); // move to the start of the name

		int endIndex = line.indexOf("\"", index);
		if (endIndex == -1)
			return null;

		return line.substring(index, endIndex);
	}
	
	/* get to the folder next to "com" */
	public String getToDestinationLevel(String rootPath, String packageName) {
		String ret = getFolderWithCom(packageName);
		if (ret != null)
			return ret;
		//System.out.println(rootPath);
		return getFolderWithoutCom(rootPath, packageName);
	}
	
	public String getFolderWithCom(String packageName) {
		int index = packageName.indexOf("com");		// start of "com"
		if (index == -1)
			return null;
		int end = packageName.indexOf(".", index + 4);	// include the next level folder of "com"
		
		if (end != -1)
			return packageName.substring(0, end);
		else
			return packageName;
	}
	
	public String getFolderWithoutCom(String rootPath, String packageName) {
		String[] folders = packageName.split("\\.");
		String path = rootPath + separatorSign + "smali";
		String ret = "";
		int bound = (folders.length <= 2) ? (folders.length - 1) : 2;

		for (int i = 0; i < bound; i++) {
			if (ret.isEmpty())
				ret = folders[i];
			else
				ret = ret + "." + folders[i];
			if (isComLevel(path))
				return ret + "." + folders[i+1];
			path = path + separatorSign + folders[i];
			//ret = ret + "." + folders[i + 1];
		}
		
		return ret;
	}
	
	public boolean isComLevel(String path) {
		File f = new File(path);
		for (File entry : f.listFiles())
			if (entry.getName().equals("com"))
				return true;
		return false;
	}

	/*
	 * go to the main component folder as the name indicated return the
	 * destination folder object
	 */
	public File toMainFolder(String rootPath, String packageName) {
		File tmp = new File(rootPath + separatorSign + "smali"
				+ separatorSign + getToDestinationLevel(rootPath, packageName).replace(".", separatorSign));

		if (tmp.exists())
			return tmp;
		else
			return null;
	}
	
	private String getMainActivityPath(File folder) throws IOException {
		
		String buff;
		String nearestName = null;
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(folder.getAbsolutePath() + separatorSign + manifestName));

			while ((buff = in.readLine()) != null) {

				/* if the line is activity attributes, get the name of this attribute */
				if (buff.contains("<activity") && buff.contains("android:name=\""))
					nearestName = extractPackageName(buff, "android:name=\"");
				else if (buff.contains("android.intent.action.MAIN")) {
					in.close();
					return nearestName;
				}
			}

		} catch (Exception e) {
			// do nothing
		}

		in.close();
		return null;
	}
	
	static private String extractPackageName(String line, String sign) {

		int index = line.indexOf(sign);
		if (index == -1)
			return null;
		else
			index += sign.length();	// move to the start of the name

		int endIndex = line.indexOf("\"", index);
		if (endIndex == -1)
			return null;

		return line.substring(index, endIndex);
	}
	
	/* MODIFIED
	 * return File object if can locate main activity folder, otherwise null */
	public File toMainActivityFolder(String rootPath) {
		
		/*String activityName = null;
		
		try {
			activityName = getMainActivityPath(folder);
			//if (activityName != null)
				//System.out.println(activityName);
		} catch (Exception e) {
			
		}
			
		if (activityName == null || activityName.startsWith(".") || !activityName.contains("."))
			return null;

		File tmp = new File(folder.getAbsolutePath() + separatorSign + "smali" + separatorSign + activityName.substring(0, activityName.lastIndexOf(".")).replace(".", separatorSign));
		
		if (tmp.exists()){
			//System.out.println(tmp.getAbsolutePath());
			return tmp;
		}else
			return null;*/
		
		/* main activity is in the folder of main component */
		if (mainActivity == null || mainActivity.startsWith(".") || !mainActivity.contains("."))
			return null;
		
		String temp = mainActivity.substring(0, mainActivity.lastIndexOf("."));	//eliminate activity name to get folder path
		
		File ret = new File(rootPath + separatorSign + "smali" + separatorSign + 
				getToDestinationLevel(rootPath, temp).replace(".", separatorSign));
		
		if (ret.exists()){
			return ret;
		}else
			return null;
	}
	
	void createContentBitVector(OpenBitSet cVector, int [] contentCount) {
		int endBitIdx = 0;
		for (int i = 0; i < contentCount.length; i++) {
				if (contentCount[i] == 0)
					continue;
				else if (contentCount[i] >= contentBitSize)
					endBitIdx = i*contentBitSize + contentBitSize;
				else
					endBitIdx = i*contentBitSize + contentCount[i];
			
				cVector.set(i*contentBitSize, endBitIdx-1);
			}
	
	}

}
