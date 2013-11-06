import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.util.OpenBitSet;

public class BitSetBank {

	public static final String serialBitSetBankMap = "bitSetMap";
	public static final String outputSimPath = "similarities.txt";
	public static final String authorsMapPath = "results/apkSignatures3500.txt";

	public HashMap<String, AppVector> bitSetsHashMap;
	public HashMap<String, String> authorsMap;
	public double jaccardThreshold = .70;
	//public MySQLAccess mySQL;

	public BitSetBank() {
		this.bitSetsHashMap = new HashMap<String, AppVector>();
		this.authorsMap = new HashMap<String, String>();
	}

	public void add(String fileName, AppVector appVector) {
		if (!appVector.isEmpty()) {
			bitSetsHashMap.put(fileName, appVector);
		} else
			System.out.println("No bits set for: " + fileName
					+ ", Excluding package...");
	}

	public void readFromSerial() {
		readFromSerial("");
	}

	public void readFromSerial(String filePath) {
		try {
			if (filePath == null || filePath.isEmpty())
				filePath = serialBitSetBankMap;

			FileInputStream fis = new FileInputStream(filePath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			bitSetsHashMap = (HashMap<String, AppVector>) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readAllFromSerial(String filePath) {
		try {
			if (filePath == null || filePath.isEmpty())
				return;
			
			if (filePath.endsWith(".ser"))
				readFromSerial(filePath);
			
			File dir = new File(filePath);
			if (dir.isFile())
				return;
			
			_readAllFromSerial(dir);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void _readAllFromSerial(File file) throws IOException, ClassNotFoundException {
		
		/* read all serial files in the folder */
		for (File entry : file.listFiles()) {
			
			if (entry.isDirectory()) {
				_readAllFromSerial(entry);
				continue;
			}
			
			if (entry.isFile() && !entry.getName().endsWith(".ser"))
				continue;
			
			FileInputStream fis = new FileInputStream(entry.getAbsoluteFile());
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, AppVector> buff = (HashMap<String, AppVector>) ois.readObject();
			ois.close();
			//System.out.println(buff.size());
			bitSetsHashMap.putAll(buff);
			//System.out.println(bitSetsHashMap.size());
		}
	}

	public String bitSetToString(OpenBitSet bitSet) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(bitSet);
			oos.close();
			byte[] bytes = baos.toByteArray();
			return (Hex.encodeHexString(bytes));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void loadAuthorsMap() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					authorsMapPath));
			String delimiter = "\\s+";
			String currentLine;
			String tokens[];

			while ((currentLine = in.readLine()) != null) {
				tokens = currentLine.split(delimiter);
				authorsMap.put(tokens[0], tokens[1]);
			}
			// Close buffered reader
			in.close();
			System.out.println(authorsMap.size());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void compareBitSetBank_KDtree(OpenBitSet x, OpenBitSet y, kdtreeCompare kdtree) {
//		OpenBitSet logicVector, contentVector;
//		double jSimX, jSimY;
//		HashMap<String, AppVector> buff = (HashMap<String, AppVector>) bitSetsHashMap.clone();
//
//		try {
//			for (Iterator<Map.Entry<String, AppVector>> iter1 = buff.entrySet().iterator(); iter1.hasNext();) {
//				Map.Entry<String, AppVector> entry1 = iter1.next();
//				logicVector = entry1.getValue().LogicVector;
//				contentVector = entry1.getValue().ContentVector;
//				iter1.remove();
//				
//				if (x == null) {
//					x = logicVector;
//					y = contentVector;
//					continue;
//				}
//				if (y == null) { 
//					y = logicVector;
//					continue;
//				}
//				
//				/* Calculate distance between X, Y sand App */
//				jSimX = this.JaccardSim(x, logicVector);
//				jSimY = this.JaccardSim(y, contentVector);
//				//jSimY = this.JaccardSim(y, logicVector);
//				
//				/*insert code for KD-tree*/
//				kdtree.insertKdtree(entry1.getKey(), jSimX, jSimY);
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	/*
	 * Finds the two base apps that produce the least correlation among the apps.
	 */
//	public String[] findVectorsLeastCorrelation(int size) {
//		PearsonsCorrelation pCorr = new PearsonsCorrelation();
//		String[] result = new String[2];
//		OpenBitSet x, y;
//		double min = 1.1, absCorrelation;
//		double[] xArr = new double[size], yArr = new double[size];
//		
//		for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap.entrySet().iterator(); iter1.hasNext();) {
//			Map.Entry<String, AppVector> xEntry = iter1.next();
//			x = xEntry.getValue().LogicVector;
//			xArr = this.getJaccardArray(x, xArr, true);
//		
//			for (Iterator<Map.Entry<String, AppVector>> iter2 = bitSetsHashMap.entrySet().iterator(); iter2.hasNext();) {
//				Map.Entry<String, AppVector> yEntry = iter2.next();
//				//if (xEntry == yEntry)
//				//	continue;
//
//				y = yEntry.getValue().ContentVector;
//				xArr = this.getJaccardArray(y, yArr, false);
//				
//				absCorrelation = Math.abs(pCorr.correlation(xArr, yArr));
//				
//				if(min > absCorrelation) {
//					min = absCorrelation;
//					result[0] = xEntry.getKey();
//					result[1] = yEntry.getKey();
//					this.plotAndCompareBitSetBank(this.bitSetsHashMap.get(result[0]).LogicVector, this.bitSetsHashMap.get(result[1]).ContentVector, result[0] + "   and   " + result[1]);
//				}
//				System.out.println(xEntry.getKey () + "   " + yEntry.getKey() + "   Absolute Correlation:  " + absCorrelation);
//			}
//		}
//		return result;
//	}
	/*
	 * Gets array containing all the Jaccard distances from base app X.
	 */
	public double[] getJaccardArray(OpenBitSet x, double[] arr, boolean isLogic) {
		OpenBitSet bitSet1;
		int i = 0;
		for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap.entrySet().iterator(); iter1.hasNext();) {
			Map.Entry<String, AppVector> entry1 = iter1.next();
			bitSet1 = isLogic ? entry1.getValue().LogicVector : entry1.getValue().ContentVector;
			
			arr[i] = this.JaccardSim(x, bitSet1);
			System.out.println(arr[i] + "  " + i);
			i++;

		}
		return arr;
	}
	/*
	 * Finds the app that produces the greatest variance; 
	 */
	public String findVectorWithMaxVariance(boolean isLogic) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		double jSimX, max = 0.0;
		String maxApp = "";
		OpenBitSet x, y, bitSet1;

		for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap.entrySet().iterator(); iter1.hasNext();) {
			Map.Entry<String, AppVector> maxEntry = iter1.next();
			x = isLogic ? maxEntry.getValue().LogicVector : maxEntry.getValue().ContentVector;
		
			for (Iterator<Map.Entry<String, AppVector>> iter2 = bitSetsHashMap.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry<String, AppVector> entry1 = iter2.next();
				bitSet1 = isLogic ? entry1.getValue().LogicVector : entry1.getValue().ContentVector;
				
				jSimX = this.JaccardSim(x, bitSet1);
				
				stats.addValue(jSimX);
			}
			double variance = stats.getVariance();
			if (max < variance) {
				max = variance;
				maxApp = maxEntry.getKey();
			}
			
			//System.out.println("VARIANCE: " + variance);
			if (variance > 0.015) {
				String yKey = findMostDistant(x, isLogic);
				y = isLogic ? bitSetsHashMap.get(yKey).LogicVector : bitSetsHashMap.get(yKey).ContentVector ;
			//	plotAndCompareBitSetBank(x, y, "X:" + maxEntry.getKey() + "   Y:" + yKey + "   V:" + variance);
			}
			
			stats.clear();
		}
		String str = isLogic ? "LOGIC" : "CONTENT";
		System.out.println(str + " MAX__VARIANCE: " + max);
		return maxApp;
	}
	
	/*
	 * Finds the most distant (dissimilar) app to the bit vector passed.
	 */
	public String findMostDistant(OpenBitSet x, boolean isLogic) {
		OpenBitSet bitSet1;
		double jSimX, min = 1.0;
		String minKey = "";
		
		for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap.entrySet().iterator(); iter1.hasNext();) {
			Map.Entry<String, AppVector> entry1 = iter1.next();
			bitSet1 = isLogic ? entry1.getValue().LogicVector : entry1.getValue().ContentVector;

			jSimX = this.JaccardSim(x, bitSet1);
			if (min > jSimX) {
				min = jSimX;
				minKey = entry1.getKey();
			}
		}
		return minKey;
	}
	
	/*
	 * Create a scatter plot of the data comparing it to X and Y. X - logicVector, Y - contentVector
	 */
//	public XYSeriesCollection plotAndCompareBitSetBank(OpenBitSet x,
//			OpenBitSet y, String title) {
//
//		XYSeries series = new XYSeries("Android Apps");
//		OpenBitSet logicBitSet, contentBitSet;
//		double jSimX, jSimY;
//		HashMap<String, String> revMap = new HashMap<String,String>();
//		ArrayList<String> phonegapList = new ArrayList<String>();
//
//		for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap
//				.entrySet().iterator(); iter1.hasNext();) {
//			Map.Entry<String, AppVector> entry1 = iter1.next();
//			logicBitSet = entry1.getValue().LogicVector;
//			contentBitSet = entry1.getValue().ContentVector;
//			// Take first two apps as base X and Y
//			if (x == null) {
//				x = logicBitSet;
//				y = contentBitSet;
//				continue;
//			}
//			if (y == null) {
//				y = logicBitSet;
//				continue;
//			}
//			
//			if (entry1.getKey().contains("phonegap")){
//			// Calculate distance between X, Y and each app
//			jSimX = this.JaccardSim(x, logicBitSet);
//			jSimY = this.JaccardSim(y, contentBitSet);
//			//jSimY = this.JaccardSim(y, logicBitSet);
//			series.add(jSimX, jSimY);
//			
//			
//			if (entry1.getKey().contains("phonegap")){
//				phonegapList.add(entry1.getKey() + " " + jSimX + " "+ jSimY);
//			}
//			
//			String str = revMap.get(jSimX + " " + jSimY);
//			if (str == null) {
//				revMap.put(jSimX + " " + jSimY,entry1.getKey());
//			}
//			else {
//				revMap.put(jSimX + " " + jSimY, entry1.getKey() + " " + str);
//			}
//			
//			}
////			if (jSimX > 0.0)
////				System.out.println(entry1.getKey() + "--->(X,Y) = (" + jSimX
////					+ " , " + jSimY + ")");
//		}
//
////		for (String str : phonegapList){
////			System.out.println(str);
////		}
//		System.out.println("phonegap apps: " + phonegapList.size());
//		
//		XYSeriesCollection seriesCollection = new XYSeriesCollection();
//		seriesCollection.addSeries(series);
//		
//		PlotResults plotter = new PlotResults();
//		plotter.ScatterPlot(seriesCollection, title, revMap);
//		return seriesCollection;
//	}
	/*
	 * Calculates the Jaccard distance using 2 base apps and outputs to default location.
	 */
	public void compareBitSetBank(OpenBitSet x, OpenBitSet y) {
		this.compareBitSetBank(x, y, null);
	}

	/*
	 * Calculates the Jaccard distance using 2 base apps and outputs to custom location.
	 */
	public void compareBitSetBank(OpenBitSet x, OpenBitSet y, String outFilePath) {
		OpenBitSet logicBitSet, contentBitSet;
		
		double jSimX, jSimY;

		try {
			if (outFilePath == null || outFilePath.isEmpty())
				outFilePath = outputSimPath;
			File file = new File(outFilePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("NAME      X      Y\n");
			bw.write("==============================\n");

			for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap.entrySet().iterator(); iter1.hasNext();) {
				Map.Entry<String, AppVector> entry1 = iter1.next();
				logicBitSet = entry1.getValue().LogicVector;
				contentBitSet = entry1.getValue().ContentVector;
				iter1.remove();
				/* Take first two apps as base X and Y */
				if (x == null) { 
					x = logicBitSet;
					y = contentBitSet;
					continue;
				}
				if (y == null) {
					y = logicBitSet;
					continue;
				}
				/* Calculate distance between X, Y and each app */
				jSimX = this.JaccardSim(x, logicBitSet);
				jSimY = this.JaccardSim(y, contentBitSet);
				//jSimY = this.JaccardSim(y, logicBitSet);
				/* Write to file */
				bw.write(entry1.getKey() + "\t" + jSimX + "\t" + jSimY + "\n");
				System.out.println(entry1.getKey() + "--->(X,Y) = (" + jSimX + " , " + jSimY +")");
			}

			bw.close();
			fw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Calculates the Jaccard distance between each app pairwise.
	 */
	public void compareBitSetBank() {

		OpenBitSet bitSet1;
		OpenBitSet bitSet2;
		double jSim;

		try {

			File file = new File(outputSimPath);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);

			for (Iterator<Map.Entry<String, AppVector>> iter1 = bitSetsHashMap
					.entrySet().iterator(); iter1.hasNext();) {
				Map.Entry<String, AppVector> entry1 = iter1.next();
				bitSet1 = entry1.getValue().LogicVector;
				iter1.remove();
				bw.write("Jaccard Similarity for " + entry1.getKey()
						+ " against:\n");

				for (Iterator<Map.Entry<String, AppVector>> iter2 = bitSetsHashMap
						.entrySet().iterator(); iter2.hasNext();) {
					Map.Entry<String, AppVector> entry2 = iter2.next();
					bitSet2 = entry2.getValue().LogicVector;
					jSim = this.JaccardSim(bitSet1, bitSet2);
					bw.write("\t" + entry2.getKey() + " = " + jSim + "\n");

					if (jSim > jaccardThreshold
							&& isDifferentAuthors(entry1.getKey(),
									entry2.getKey()))
						System.out.println(entry1.getKey() + " vs "
								+ entry2.getKey() + " = " + jSim);

				}
			}

			bw.close();
			fw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Compares MD5 hash for each APK. Returns true if both are different
	public boolean isDifferentAuthors(String key1, String key2) {
		String hash1 = authorsMap.get("/proj/ds/encos/apk/" + key1 + ".apk");
		String hash2 = authorsMap.get("/proj/ds/encos/apk/" + key2 + ".apk");

		if (hash1 == null || hash2 == null) {
			System.out.print("NO MD5:");
			return true;
		}

		// if (!hash1.equals(hash2)){
		// System.out.println(hash1 + " " + hash2);
		// return true;
		// }
		// return false;

		// if (hash1.equals(hash2)){
		// System.out.println(hash1 + " " + hash2);
		// System.out.println(key1 + " " + key2);
		// return false;
		// }
		// return true;

		return (!hash1.equals(hash2));
	}

	public double JaccardSim(OpenBitSet bitSet1, OpenBitSet bitSet2) {

		OpenBitSet bitSetIntersect = (OpenBitSet) bitSet1.clone();
		OpenBitSet bitSetUnion = (OpenBitSet) bitSet1.clone();

		bitSetIntersect.intersect(bitSet2);
		bitSetUnion.union(bitSet2);

		double jaccardSim = (double) bitSetIntersect.cardinality()
				/ (double) bitSetUnion.cardinality();
		// System.out.println(bitSetIntersect.cardinality() + " " +
		// bitSetUnion.cardinality() + " " + jaccardSim);

		return jaccardSim;
	}
	
	public double JaccardSimLogic(String name1, String name2) {
		return JaccardSim(getBitSetLogicByName(name1), getBitSetLogicByName(name2));
	}
	
	public double JaccardSimContent(String name1, String name2) {
		return JaccardSim(getBitSetContentByName(name1), getBitSetContentByName(name2));
	}
	
	public OpenBitSet getBitSetLogicByName(String name) {
		return this.bitSetsHashMap.get(name).LogicVector;
	}
	
	public OpenBitSet getBitSetContentByName(String name) {
		return this.bitSetsHashMap.get(name).ContentVector;
	}

	public void writeToSerial(int d, int s, int partNumber) {
		FileOutputStream fos;
		try {
			if (d != 0){
				File directory = new File("outputLogs/" + serialBitSetBankMap +  "_d_" + d + "_s_" + s + "/");
				directory.mkdirs();
				fos = new FileOutputStream("outputLogs/" + serialBitSetBankMap +  "_d_" + d + "_s_" + s + "/" + "part_" + partNumber + ".ser");
			}else{
				//Creates outputLogs Directory if it Does not Exist
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		        File directory = new File("outputLogs/" + serialBitSetBankMap + "_" + timeStamp + "/");
				directory.mkdirs();				
				fos = new FileOutputStream("outputLogs/" + serialBitSetBankMap + "_" + timeStamp + "/" + "part_" + partNumber + ".ser");
			}
			
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.bitSetsHashMap);
			oos.close();
			
			//Creates new hashmap to write bitSet data of next iteration in main loop 
			this.bitSetsHashMap = new HashMap<String, AppVector>();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void writeToSerial() {
		FileOutputStream fos;
		try {
	        //Creates outputLogs Directory if it Does not Exist
	        File directory = new File("outputLogs/");
			directory.mkdirs();
			
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			fos = new FileOutputStream("outputLogs/" + serialBitSetBankMap + "_" + timeStamp + ".ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.bitSetsHashMap);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getSmaliMethodsFromBitVector(AppVector app) {
		OpenBitSet x = app.LogicVector;
		//bitSetsHashMap.values().get
		return null;
	}
	public ArrayList<String> getPermissionsFromBitVector(AppVector app) {
		
		return null;
	}
}
