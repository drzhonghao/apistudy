import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.xssf.usermodel.extensions.XSSFHeaderFooter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter;

/**
 * Odd page header value. Corresponds to odd printed pages. 
 * Odd page(s) in the sheet may not be printed, for example, if the print area is specified to be 
 * a range such that it falls outside an odd page's scope.
 *
 */
public class XSSFOddHeader extends XSSFHeaderFooter implements Header{

    /**
     * Create an instance of XSSFOddHeader from the supplied XML bean
     * @see XSSFSheet#getOddHeader()
     * @param headerFooter
     */
    protected XSSFOddHeader(CTHeaderFooter headerFooter) {
        super(headerFooter);
    }
    
    /**
     * Get the content text representing this header
     * @return text
     */
    @Override
    public String getText() {
        return getHeaderFooter().getOddHeader();
    }
    
    /**
     * Set a text for the header. If null unset the value
     * @see XSSFHeaderFooter to see how to create a string with Header/Footer Formatting Syntax
     * @param text - a string representing the header. 
     */
    @Override
    public void setText(String text) {
    	if(text == null) {
    		getHeaderFooter().unsetOddHeader();
    	} else {
    		getHeaderFooter().setOddHeader(text);
    	}
    }
}
