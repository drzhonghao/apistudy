

import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.analysis.cn.smart.AnalyzerProfile;
import org.apache.lucene.analysis.cn.smart.Utility;


class WordDictionary {
	private WordDictionary() {
	}

	private static WordDictionary singleInstance;

	public static final int PRIME_INDEX_LENGTH = 12071;

	private short[] wordIndexTable;

	private char[] charIndexTable;

	private char[][][] wordItem_charArrayTable;

	private int[][] wordItem_frequencyTable;

	public static synchronized WordDictionary getInstance() {
		if ((WordDictionary.singleInstance) == null) {
			WordDictionary.singleInstance = new WordDictionary();
			try {
				WordDictionary.singleInstance.load();
			} catch (IOException e) {
				String wordDictRoot = AnalyzerProfile.ANALYSIS_DATA_DIR;
				WordDictionary.singleInstance.load(wordDictRoot);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return WordDictionary.singleInstance;
	}

	public void load(String dctFileRoot) {
		String dctFilePath = dctFileRoot + "/coredict.dct";
		Path serialObj = Paths.get((dctFileRoot + "/coredict.mem"));
		if ((Files.exists(serialObj)) && (loadFromObj(serialObj))) {
		}else {
			try {
				wordIndexTable = new short[WordDictionary.PRIME_INDEX_LENGTH];
				charIndexTable = new char[WordDictionary.PRIME_INDEX_LENGTH];
				for (int i = 0; i < (WordDictionary.PRIME_INDEX_LENGTH); i++) {
					charIndexTable[i] = 0;
					wordIndexTable[i] = -1;
				}
				loadMainDataFromFile(dctFilePath);
				expandDelimiterData();
				mergeSameWords();
				sortEachItems();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
			saveToObj(serialObj);
		}
	}

	public void load() throws IOException, ClassNotFoundException {
		InputStream input = this.getClass().getResourceAsStream("coredict.mem");
		loadFromObjectInputStream(input);
	}

	private boolean loadFromObj(Path serialObj) {
		try {
			loadFromObjectInputStream(Files.newInputStream(serialObj));
			return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void loadFromObjectInputStream(InputStream serialObjectInputStream) throws IOException, ClassNotFoundException {
		try (final ObjectInputStream input = new ObjectInputStream(serialObjectInputStream)) {
			wordIndexTable = ((short[]) (input.readObject()));
			charIndexTable = ((char[]) (input.readObject()));
			wordItem_charArrayTable = ((char[][][]) (input.readObject()));
			wordItem_frequencyTable = ((int[][]) (input.readObject()));
		}
	}

	private void saveToObj(Path serialObj) {
		try (final ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(serialObj))) {
			output.writeObject(wordIndexTable);
			output.writeObject(charIndexTable);
			output.writeObject(wordItem_charArrayTable);
			output.writeObject(wordItem_frequencyTable);
		} catch (Exception e) {
		}
	}

	private int loadMainDataFromFile(String dctFilePath) throws IOException {
		int i;
		int cnt;
		int length;
		int total = 0;
		int[] buffer = new int[3];
		byte[] intBuffer = new byte[4];
		String tmpword;
		DataInputStream dctFile = new DataInputStream(Files.newInputStream(Paths.get(dctFilePath)));
		dctFile.close();
		return total;
	}

	private void expandDelimiterData() {
		int i;
		int cnt;
		i = 0;
	}

	private void mergeSameWords() {
		int i;
	}

	private void sortEachItems() {
		char[] tmpArray;
		int tmpFreq;
		for (int i = 0; i < (wordItem_charArrayTable.length); i++) {
			if (((wordItem_charArrayTable[i]) != null) && ((wordItem_charArrayTable[i].length) > 1)) {
				for (int j = 0; j < ((wordItem_charArrayTable[i].length) - 1); j++) {
					for (int j2 = j + 1; j2 < (wordItem_charArrayTable[i].length); j2++) {
						if ((Utility.compareArray(wordItem_charArrayTable[i][j], 0, wordItem_charArrayTable[i][j2], 0)) > 0) {
							tmpArray = wordItem_charArrayTable[i][j];
							tmpFreq = wordItem_frequencyTable[i][j];
							wordItem_charArrayTable[i][j] = wordItem_charArrayTable[i][j2];
							wordItem_frequencyTable[i][j] = wordItem_frequencyTable[i][j2];
							wordItem_charArrayTable[i][j2] = tmpArray;
							wordItem_frequencyTable[i][j2] = tmpFreq;
						}
					}
				}
			}
		}
	}

	private boolean setTableIndex(char c, int j) {
		int index = getAvaliableTableIndex(c);
		if (index != (-1)) {
			charIndexTable[index] = c;
			wordIndexTable[index] = ((short) (j));
			return true;
		}else
			return false;

	}

	private short getAvaliableTableIndex(char c) {
		int i = 1;
		return 0;
	}

	private short getWordItemTableIndex(char c) {
		int i = 1;
		return 0;
	}

	private int findInTable(short knownHashIndex, char[] charArray) {
		if ((charArray == null) || ((charArray.length) == 0))
			return -1;

		char[][] items = wordItem_charArrayTable[wordIndexTable[knownHashIndex]];
		int start = 0;
		int end = (items.length) - 1;
		int mid = (start + end) / 2;
		int cmpResult;
		while (start <= end) {
			cmpResult = Utility.compareArray(items[mid], 0, charArray, 1);
			if (cmpResult == 0)
				return mid;
			else
				if (cmpResult < 0)
					start = mid + 1;
				else
					if (cmpResult > 0)
						end = mid - 1;



			mid = (start + end) / 2;
		} 
		return -1;
	}

	public int getPrefixMatch(char[] charArray) {
		return getPrefixMatch(charArray, 0);
	}

	public int getPrefixMatch(char[] charArray, int knownStart) {
		short index = getWordItemTableIndex(charArray[0]);
		if (index == (-1))
			return -1;

		char[][] items = wordItem_charArrayTable[wordIndexTable[index]];
		int start = knownStart;
		int end = (items.length) - 1;
		int mid = (start + end) / 2;
		int cmpResult;
		while (start <= end) {
			cmpResult = Utility.compareArrayByPrefix(charArray, 1, items[mid], 0);
			if (cmpResult == 0) {
				while ((mid >= 0) && ((Utility.compareArrayByPrefix(charArray, 1, items[mid], 0)) == 0))
					mid--;

				mid++;
				return mid;
			}else
				if (cmpResult < 0)
					end = mid - 1;
				else
					start = mid + 1;


			mid = (start + end) / 2;
		} 
		return -1;
	}

	public int getFrequency(char[] charArray) {
		short hashIndex = getWordItemTableIndex(charArray[0]);
		if (hashIndex == (-1))
			return 0;

		int itemIndex = findInTable(hashIndex, charArray);
		if (itemIndex != (-1))
			return wordItem_frequencyTable[wordIndexTable[hashIndex]][itemIndex];

		return 0;
	}

	public boolean isEqual(char[] charArray, int itemIndex) {
		short hashIndex = getWordItemTableIndex(charArray[0]);
		return (Utility.compareArray(charArray, 1, wordItem_charArrayTable[wordIndexTable[hashIndex]][itemIndex], 0)) == 0;
	}
}

