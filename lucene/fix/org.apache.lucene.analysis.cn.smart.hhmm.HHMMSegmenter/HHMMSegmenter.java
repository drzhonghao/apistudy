

import java.util.List;
import org.apache.lucene.analysis.cn.smart.Utility;
import org.apache.lucene.analysis.cn.smart.hhmm.SegToken;


public class HHMMSegmenter {
	private static int[] getCharTypes(String sentence) {
		int length = sentence.length();
		int[] charTypeArray = new int[length];
		for (int i = 0; i < length; i++) {
			charTypeArray[i] = Utility.getCharType(sentence.charAt(i));
		}
		return charTypeArray;
	}

	public List<SegToken> process(String sentence) {
		return null;
	}
}

