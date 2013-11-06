import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;



public class WhiteListGenerator {

	public long dirCount;
	public long indivdualMethodCount;
	public long totalCount;
	public HashMap<String, Integer> whiteListHashMap;
	public HashMap<String, Integer> contentHashMap;
	public File folder;

	private static final String separatorSign = "/";
	private static final String whiteListLibraries = "whitelist_libraries.txt";


	public HashMap<String, Integer> whiteListLibsHashMap;
	public int featuresCount;
	public String extension = "";
	
	
	public WhiteListGenerator() {

		this.whiteListHashMap = new HashMap<String, Integer>();
		this.whiteListLibsHashMap = new HashMap<String, Integer>();
		this.contentHashMap = new HashMap<String, Integer>();
		this.loadWhitelistLibs();
		this.totalCount = 0;
		this.dirCount = 0;

	}
	
	public void generateWhiteList(ApkDisassembler ad) {
		
		long startTime = System.currentTimeMillis();
				
		File currentDir;
		
		while((currentDir = ad.disassembleNextFile()) != null){
			System.out.println(currentDir.getName());
			this.apkDirectoryTraversal(currentDir);
			this.apkContentTraversal(currentDir);
			try {
				FileUtils.deleteDirectory(new File(currentDir.getAbsolutePath()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		long endTime = System.currentTimeMillis();
		
		
		System.out.println("Total classes in Hash: "+ this.whiteListHashMap.size());


		System.out.println("Total time: " + (endTime - startTime)
				+ " ms for " + this.totalCount + " files");
		System.out.println("Time Per Apk " + (endTime - startTime)
				/ (double) this.dirCount + " ms/file");
		
		this.printHashMap(contentHashMap, "content");
		this.printHashMap(whiteListHashMap, "wl");
		
		long endTime2 = System.currentTimeMillis();
		
		System.out.println("Time to Print File " + (endTime2 - endTime) + " ms");

	}
	
	public void listFilesForFolder(File folder, int folderNameLength) {
		for (File fileEntry : folder.listFiles()) {
			if (isWhitelisted(fileEntry.getAbsolutePath().substring(folderNameLength))) {
				//System.out.println("WHITELISTED: " + fileEntry.getPath());
				continue;
			}

			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry,  folderNameLength);
			} else {
				try {
					this.parseDelimitedFile(fileEntry.getAbsolutePath());

				} catch (Exception e) {
					System.out.println(folder.getName() +" Failed Decompilation");
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listFilesForFolder(File folder) {
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.getAbsolutePath().contains("smali")) {
				//System.out.println("WHITELISTED: " + fileEntry.getPath());
				continue;
			}

			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				try {
					String ext = FilenameUtils.getExtension(fileEntry.getName());
					Integer count = contentHashMap.get(ext);
					if (count == null) {
						contentHashMap.put(ext, 1);
					}
					else {
						contentHashMap.put(ext, count + 1);
					}

				} catch (Exception e) {
					System.out.println(folder.getName() +" Failed Decompilation");
					e.printStackTrace();
				}
			}
		}
	}
	
	//Traverses the decompiled folder that was produced from an APK
	public void apkDirectoryTraversal(File folder) {
		try {
			
			File fileEntry = new File(folder.getAbsoluteFile() + "/smali/");
			int folderNameLength = fileEntry.getAbsolutePath().length();
			this.dirCount++;
			
			listFilesForFolder(fileEntry, folderNameLength);

		} catch (Exception e) {
			System.out.println(folder.getName() +" Failed Decompilation");
			e.printStackTrace();
		}
	}
	
	
	//Traverses the decompiled folder that was produced from an APK
	public void apkContentTraversal(File folder) {
		try {
			
			//File fileEntry = new File(folder.getAbsoluteFile() + "/smali/");
			//int folderNameLength = fileEntry.getAbsolutePath().length();
			this.dirCount++;
			
			listFilesForFolder(folder);

		} catch (Exception e) {
			System.out.println(folder.getName() +" Failed Decompilation");
			e.printStackTrace();
		}
	}
	
	private void parseDelimitedFile(String filePath)
			throws Exception {

		FileReader fr = new FileReader(filePath);
		BufferedReader br = new BufferedReader(fr);
		String currentRecord;
		String delimiter = "\\s+";
		String tokens[];
		String token;

		while ((currentRecord = br.readLine()) != null) {
				tokens = currentRecord.split(delimiter);
				token = tokens[tokens.length - 1];
				this.totalCount++;
				
				
				Integer count = whiteListHashMap.get(token);
				if (count == null) {
					whiteListHashMap.put(token, 1);
				}
				else {
					whiteListHashMap.put(token, count + 1);
				}
				
				break;
		}
		
		br.close();
		fr.close();

	}
	
	public void printHashMap(HashMap<String, Integer> map, String type) {

		try {

			ValueComparator bvc =  new ValueComparator(map);
	        TreeMap<String,Integer> sorted_map = new TreeMap<String,Integer>(bvc);
	        sorted_map.putAll(map);
			
	        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	        
	        //Creates outputLogs Directory if it Does not Exist
	        File directory = new File("outputLogs/");
			directory.mkdirs();
			
	        File file = new File("outputLogs/" + type + "_" + timeStamp + ".txt");
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);

			for (Iterator<Map.Entry<String, Integer>> iter1 = sorted_map.entrySet().iterator(); iter1.hasNext();) {
				Map.Entry<String, Integer> entry1 = iter1.next();
				bw.write(entry1.getKey() + " " + entry1.getValue() + "\n");

			}

			bw.close();
			fw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Boolean isWhitelisted(String fileEntryName) {
		if (whiteListLibsHashMap.containsKey(fileEntryName)) {
			return true;
		}
		return false;
	}
	
	private void loadWhitelistLibs() {
		try {

			BufferedReader in = new BufferedReader(new FileReader(whiteListLibraries));
			String str;
			
			while ((str = in.readLine()) != null) {
				str.replace("/", separatorSign);
				whiteListLibsHashMap.put(str, featuresCount);
				featuresCount++;
			}
			// Close buffered reader
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

class ValueComparator implements Comparator<String> {

    Map<String, Integer> base;
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
 //   @Override
	public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}


