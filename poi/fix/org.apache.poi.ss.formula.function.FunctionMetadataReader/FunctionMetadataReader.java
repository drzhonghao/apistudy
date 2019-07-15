

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.IOUtils;


final class FunctionMetadataReader {
	private static final int MAX_RECORD_LENGTH = 100000;

	private static final String METADATA_FILE_NAME = "functionMetadata.txt";

	private static final String ELLIPSIS = "...";

	private static final Pattern TAB_DELIM_PATTERN = Pattern.compile("\t");

	private static final Pattern SPACE_DELIM_PATTERN = Pattern.compile(" ");

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[]{  };

	private static final String[] DIGIT_ENDING_FUNCTION_NAMES = new String[]{ "LOG10", "ATAN2", "DAYS360", "SUMXMY2", "SUMX2MY2", "SUMX2PY2" };

	private static final Set<String> DIGIT_ENDING_FUNCTION_NAMES_SET = new HashSet<>(Arrays.asList(FunctionMetadataReader.DIGIT_ENDING_FUNCTION_NAMES));

	public static FunctionMetadataRegistry createRegistry() {
		try {
			InputStream is = FunctionMetadataReader.class.getResourceAsStream(FunctionMetadataReader.METADATA_FILE_NAME);
			if (is == null) {
				throw new RuntimeException((("resource '" + (FunctionMetadataReader.METADATA_FILE_NAME)) + "' not found"));
			}
			try {
				try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					while (true) {
						String line = br.readLine();
						if (line == null) {
							break;
						}
						if (((line.length()) < 1) || ((line.charAt(0)) == '#')) {
							continue;
						}
						String trimLine = line.trim();
						if ((trimLine.length()) < 1) {
							continue;
						}
					} 
				}
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private static byte parseReturnTypeCode(String code) {
		if ((code.length()) == 0) {
			return Ptg.CLASS_REF;
		}
		return FunctionMetadataReader.parseOperandTypeCode(code);
	}

	private static byte[] parseOperandTypeCodes(String codes) {
		if ((codes.length()) < 1) {
			return FunctionMetadataReader.EMPTY_BYTE_ARRAY;
		}
		if (FunctionMetadataReader.isDash(codes)) {
			return FunctionMetadataReader.EMPTY_BYTE_ARRAY;
		}
		String[] array = FunctionMetadataReader.SPACE_DELIM_PATTERN.split(codes);
		int nItems = array.length;
		if (FunctionMetadataReader.ELLIPSIS.equals(array[(nItems - 1)])) {
			nItems--;
		}
		byte[] result = IOUtils.safelyAllocate(nItems, FunctionMetadataReader.MAX_RECORD_LENGTH);
		for (int i = 0; i < nItems; i++) {
			result[i] = FunctionMetadataReader.parseOperandTypeCode(array[i]);
		}
		return result;
	}

	private static boolean isDash(String codes) {
		if ((codes.length()) == 1) {
			switch (codes.charAt(0)) {
				case '-' :
					return true;
			}
		}
		return false;
	}

	private static byte parseOperandTypeCode(String code) {
		if ((code.length()) != 1) {
			throw new RuntimeException((("Bad operand type code format '" + code) + "' expected single char"));
		}
		switch (code.charAt(0)) {
			case 'V' :
				return Ptg.CLASS_VALUE;
			case 'R' :
				return Ptg.CLASS_REF;
			case 'A' :
				return Ptg.CLASS_ARRAY;
		}
		throw new IllegalArgumentException((((("Unexpected operand type code '" + code) + "' (") + ((int) (code.charAt(0)))) + ")"));
	}

	private static void validateFunctionName(String functionName) {
		int len = functionName.length();
		int ix = len - 1;
		if (!(Character.isDigit(functionName.charAt(ix)))) {
			return;
		}
		while (ix >= 0) {
			if (!(Character.isDigit(functionName.charAt(ix)))) {
				break;
			}
			ix--;
		} 
		if (FunctionMetadataReader.DIGIT_ENDING_FUNCTION_NAMES_SET.contains(functionName)) {
			return;
		}
		throw new RuntimeException((("Invalid function name '" + functionName) + "' (is footnote number incorrectly appended)"));
	}

	private static int parseInt(String valStr) {
		try {
			return Integer.parseInt(valStr);
		} catch (NumberFormatException e) {
			throw new RuntimeException((("Value '" + valStr) + "' could not be parsed as an integer"));
		}
	}
}

