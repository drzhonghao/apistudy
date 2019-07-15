import org.apache.poi.xssf.binary.XSSFBParseException;
import org.apache.poi.xssf.binary.XSSFBUtils;
import org.apache.poi.xssf.binary.*;


import org.apache.poi.util.Internal;

/**
 * @since 3.16-beta3
 */
@Internal
class XSSFBRichStr {

    public static XSSFBRichStr build(byte[] bytes, int offset) throws XSSFBParseException {
        byte first = bytes[offset];
        boolean dwSizeStrRunExists = (first >> 7 & 1) == 1;//first bit == 1?
        boolean phoneticExists = (first >> 6 & 1) == 1;//second bit == 1?
        StringBuilder sb = new StringBuilder();

        int read = XSSFBUtils.readXLWideString(bytes, offset+1, sb);
        //TODO: parse phonetic strings.
        return new XSSFBRichStr(sb.toString(), "");
    }

    private final String string;
    private final String phoneticString;

    XSSFBRichStr(String string, String phoneticString) {
        this.string = string;
        this.phoneticString = phoneticString;
    }

    public String getString() {
        return string;
    }
}
