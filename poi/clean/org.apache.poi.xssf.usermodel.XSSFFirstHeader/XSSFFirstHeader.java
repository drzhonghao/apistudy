import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.xssf.usermodel.extensions.XSSFHeaderFooter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter;

/**
 * <p>
 * First page header content. Corresponds to first printed page.
 * The first logical page in the sheet may not be printed, for example, if the print area is specified to 
 * be a range such that it falls outside the first page's scope.
 * </p><p>
 * The first page header is activated by the "Different First" Header/Footer property for the sheet.
 * If this property is not set, the first page header is ignored.
 * </p><p>
 * Creating a first page header or footer sets this property by default, so all you need to do to
 * get an first page header or footer to display is to create one. Likewise, if both the first page
 * header and footer are usnset, then this property is unset, and the first page header and footer
 * are ignored.
 * </p>
 */
public class XSSFFirstHeader extends XSSFHeaderFooter implements Header{

    /**
     * Create an instance of XSSFFirstHeader from the supplied XML bean
     * @see XSSFSheet#getFirstHeader()
     * @param headerFooter
     */
    protected XSSFFirstHeader(CTHeaderFooter headerFooter) {
        super(headerFooter);
        headerFooter.setDifferentFirst(true);
    }
    
    /**
     * Get the content text representing this header
     * @return text
     */
    @Override
    public String getText() {
        return getHeaderFooter().getFirstHeader();
    }
    
    /**
     * Set a text for the header. If null unset the value. If unsetting this header results 
     * in no First Header, or footer for the sheet, the 'differentFirst' property is unset as well.
     *  
     * @see XSSFHeaderFooter to see how to create a string with Header/Footer Formatting Syntax
     * @param text - a string representing the header. 
     */
    @Override
    public void setText(String text) {
    	if(text == null) {
    		getHeaderFooter().unsetFirstHeader();
    		if (!getHeaderFooter().isSetFirstFooter()) {
    		    getHeaderFooter().unsetDifferentFirst();
    		}
    	} else {
    		getHeaderFooter().setFirstHeader(text);
    	}
    }

}
