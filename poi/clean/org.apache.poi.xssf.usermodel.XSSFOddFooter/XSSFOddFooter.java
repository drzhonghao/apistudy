import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.xssf.usermodel.extensions.XSSFHeaderFooter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter;

/**
 * Odd page footer value. Corresponds to odd printed pages.
 * Odd page(s) in the sheet may not be printed, for example, if the print area is specified to be 
 * a range such that it falls outside an odd page's scope.
 *
 */
public class XSSFOddFooter extends XSSFHeaderFooter implements Footer{

    /**
     * Create an instance of XSSFOddFooter from the supplied XML bean
     * @see XSSFSheet#getOddFooter()
     * @param headerFooter
     */
    protected XSSFOddFooter(CTHeaderFooter headerFooter) {
        super(headerFooter);
    }
    
    /**
     * Get the content text representing the footer
     * @return text
     */
    @Override
    public String getText() {
        return getHeaderFooter().getOddFooter();
    }
    
    /**
     * Set a text for the footer. If null unset the value.
     * @see XSSFHeaderFooter to see how to create a string with Header/Footer Formatting Syntax
     * @param text - a string representing the footer. 
     */
    @Override
    public void setText(String text) {
    	if(text == null) {
    		getHeaderFooter().unsetOddFooter();
    	} else {
    		getHeaderFooter().setOddFooter(text);
    	}
    }
}
