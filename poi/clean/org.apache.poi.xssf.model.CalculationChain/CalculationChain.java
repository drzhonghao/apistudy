import org.apache.poi.xssf.model.*;


import static org.apache.poi.ooxml.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcChain;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CalcChainDocument;

/**
 * The cells in a workbook can be calculated in different orders depending on various optimizations and
 * dependencies. The calculation chain object specifies the order in which the cells in a workbook were last calculated.
 *
 * @author Yegor Kozlov
 */
public class CalculationChain extends POIXMLDocumentPart {
    private CTCalcChain chain;

    public CalculationChain() {
        super();
        chain = CTCalcChain.Factory.newInstance();
    }

    /**
     * @since POI 3.14-Beta1
     */
    public CalculationChain(PackagePart part) throws IOException {
        super(part);
        readFrom(part.getInputStream());
    }
    
    public void readFrom(InputStream is) throws IOException {
        try {
            CalcChainDocument doc = CalcChainDocument.Factory.parse(is, DEFAULT_XML_OPTIONS);
            chain = doc.getCalcChain();
        } catch (XmlException e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }
    public void writeTo(OutputStream out) throws IOException {
        CalcChainDocument doc = CalcChainDocument.Factory.newInstance();
        doc.setCalcChain(chain);
        doc.save(out, DEFAULT_XML_OPTIONS);
    }

    @Override
    protected void commit() throws IOException {
        PackagePart part = getPackagePart();
        OutputStream out = part.getOutputStream();
        writeTo(out);
        out.close();
    }


    public CTCalcChain getCTCalcChain(){
        return chain;
    }

    /**
     * Remove a formula reference from the calculation chain
     * 
     * @param sheetId  the sheet Id of a sheet the formula belongs to.
     * @param ref  A1 style reference to the cell containing the formula.
     */
    public void removeItem(int sheetId, String ref){
        //sheet Id of a sheet the cell belongs to
        int id = -1;
        CTCalcCell[] c = chain.getCArray();

        for (int i = 0; i < c.length; i++){
            //If sheet Id  is omitted, it is assumed to be the same as the value of the previous cell.
            if(c[i].isSetI()) id = c[i].getI();

            if(id == sheetId && c[i].getR().equals(ref)){
                if(c[i].isSetI() && i < c.length - 1 && !c[i+1].isSetI()) {
                    c[i+1].setI(id);
                }
                chain.removeC(i);
                break;
            }
        }
    }
}
