

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;


public abstract class TrecDocParser {
	public enum ParsePathType {

		GOV2,
		FBIS,
		FT,
		FR94,
		LATIMES;}

	public static final TrecDocParser.ParsePathType DEFAULT_PATH_TYPE = TrecDocParser.ParsePathType.GOV2;

	static final Map<TrecDocParser.ParsePathType, TrecDocParser> pathType2parser = new HashMap<>();

	static {
	}

	static final Map<String, TrecDocParser.ParsePathType> pathName2Type = new HashMap<>();

	static {
		for (TrecDocParser.ParsePathType ppt : TrecDocParser.ParsePathType.values()) {
			TrecDocParser.pathName2Type.put(ppt.name().toUpperCase(Locale.ROOT), ppt);
		}
	}

	private static final int MAX_PATH_LENGTH = 10;

	public static TrecDocParser.ParsePathType pathType(Path f) {
		int pathLength = 0;
		while (((f != null) && ((f.getFileName()) != null)) && ((++pathLength) < (TrecDocParser.MAX_PATH_LENGTH))) {
			TrecDocParser.ParsePathType ppt = TrecDocParser.pathName2Type.get(f.getFileName().toString().toUpperCase(Locale.ROOT));
			if (ppt != null) {
				return ppt;
			}
			f = f.getParent();
		} 
		return TrecDocParser.DEFAULT_PATH_TYPE;
	}

	public abstract DocData parse(DocData docData, String name, TrecContentSource trecSrc, StringBuilder docBuf, TrecDocParser.ParsePathType pathType) throws IOException;

	public static String stripTags(StringBuilder buf, int start) {
		return TrecDocParser.stripTags(buf.substring(start), 0);
	}

	public static String stripTags(String buf, int start) {
		if (start > 0) {
			buf = buf.substring(0);
		}
		return buf.replaceAll("<[^>]*>", " ");
	}

	public static String extract(StringBuilder buf, String startTag, String endTag, int maxPos, String[] noisePrefixes) {
		int k1 = buf.indexOf(startTag);
		if ((k1 >= 0) && ((maxPos < 0) || (k1 < maxPos))) {
			k1 += startTag.length();
			int k2 = buf.indexOf(endTag, k1);
			if ((k2 >= 0) && ((maxPos < 0) || (k2 < maxPos))) {
				if (noisePrefixes != null) {
					for (String noise : noisePrefixes) {
						int k1a = buf.indexOf(noise, k1);
						if ((k1a >= 0) && (k1a < k2)) {
							k1 = k1a + (noise.length());
						}
					}
				}
				return buf.substring(k1, k2).trim();
			}
		}
		return null;
	}
}

