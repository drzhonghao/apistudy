import org.apache.poi.xssf.extractor.*;


import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;

/**
 * Helper class to extract text from an OOXML Excel file
 */
public class XSSFExcelExtractor extends POIXMLTextExtractor 
       implements org.apache.poi.ss.extractor.ExcelExtractor {
    public static final XSSFRelation[] SUPPORTED_TYPES = new XSSFRelation[] {
        XSSFRelation.WORKBOOK, XSSFRelation.MACRO_TEMPLATE_WORKBOOK,
        XSSFRelation.MACRO_ADDIN_WORKBOOK, XSSFRelation.TEMPLATE_WORKBOOK,
        XSSFRelation.MACROS_WORKBOOK
    };

    private Locale locale;
    private XSSFWorkbook workbook;
    private boolean includeSheetNames = true;
    private boolean formulasNotResults;
    private boolean includeCellComments;
    private boolean includeHeadersFooters = true;
    private boolean includeTextBoxes = true;

    public XSSFExcelExtractor(OPCPackage container) throws XmlException, OpenXML4JException, IOException {
        this(new XSSFWorkbook(container));
    }
    public XSSFExcelExtractor(XSSFWorkbook workbook) {
        super(workbook);
        this.workbook = workbook;
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            System.err.println("Use:");
            System.err.println("  XSSFExcelExtractor <filename.xlsx>");
            System.exit(1);
        }

        try (OPCPackage pkg = OPCPackage.create(args[0]);
             POIXMLTextExtractor extractor = new XSSFExcelExtractor(pkg)) {
            System.out.println(extractor.getText());
        }
    }

    /**
     * Should sheet names be included? Default is true
     */
    public void setIncludeSheetNames(boolean includeSheetNames) {
        this.includeSheetNames = includeSheetNames;
    }
    /**
     * Should we return the formula itself, and not
     *  the result it produces? Default is false
     */
    public void setFormulasNotResults(boolean formulasNotResults) {
        this.formulasNotResults = formulasNotResults;
    }
    /**
     * Should cell comments be included? Default is false
     */
    public void setIncludeCellComments(boolean includeCellComments) {
        this.includeCellComments = includeCellComments;
    }
    /**
     * Should headers and footers be included? Default is true
     */
    public void setIncludeHeadersFooters(boolean includeHeadersFooters) {
        this.includeHeadersFooters = includeHeadersFooters;
    }
    /**
     * Should text within textboxes be included? Default is true
     * @param includeTextBoxes True if textboxes should be included, false if not.
     */
    public void setIncludeTextBoxes(boolean includeTextBoxes){
        this.includeTextBoxes = includeTextBoxes;
    }
    /**
     * What Locale should be used for formatting numbers (based
     *  on the styles applied to the cells)
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }


    /**
     * Retrieves the text contents of the file
     */
    public String getText() {
        DataFormatter formatter;
        if(locale == null) {
            formatter = new DataFormatter();
        } else  {
            formatter = new DataFormatter(locale);
        }

        StringBuilder text = new StringBuilder(64);
        for(Sheet sh : workbook) {
            XSSFSheet sheet = (XSSFSheet) sh;
            if(includeSheetNames) {
                text.append(sheet.getSheetName()).append("\n");
            }

            // Header(s), if present
            if(includeHeadersFooters) {
                text.append(
                        extractHeaderFooter(sheet.getFirstHeader())
                        );
                text.append(
                        extractHeaderFooter(sheet.getOddHeader())
                        );
                text.append(
                        extractHeaderFooter(sheet.getEvenHeader())
                        );
            }

            // Rows and cells
            for (Object rawR : sheet) {
                Row row = (Row)rawR;
                for(Iterator<Cell> ri = row.cellIterator(); ri.hasNext();) {
                    Cell cell = ri.next();

                    // Is it a formula one?
                    if(cell.getCellType() == CellType.FORMULA) {
                        if (formulasNotResults) {
                            String contents = cell.getCellFormula();
                            checkMaxTextSize(text, contents);
                            text.append(contents);
                        } else {
                            if (cell.getCachedFormulaResultType() == CellType.STRING) {
                                handleStringCell(text, cell);
                            } else {
                                handleNonStringCell(text, cell, formatter);
                            }
                        }
                    } else if(cell.getCellType() == CellType.STRING) {
                        handleStringCell(text, cell);
                    } else {
                        handleNonStringCell(text, cell, formatter);
                    }

                    // Output the comment, if requested and exists
                    Comment comment = cell.getCellComment();
                    if(includeCellComments && comment != null) {
                        // Replace any newlines with spaces, otherwise it
                        //  breaks the output
                        String commentText = comment.getString().getString().replace('\n', ' ');
                        checkMaxTextSize(text, commentText);
                        text.append(" Comment by ").append(comment.getAuthor()).append(": ").append(commentText);
                    }

                    if(ri.hasNext()) {
                        text.append("\t");
                    }
                }
                text.append("\n");
            }
            
            // add textboxes
            if (includeTextBoxes){
                XSSFDrawing drawing = sheet.getDrawingPatriarch();
                if (drawing != null) {
                    for (XSSFShape shape : drawing.getShapes()){
                        if (shape instanceof XSSFSimpleShape){
                            String boxText = ((XSSFSimpleShape)shape).getText();
                            if (boxText.length() > 0){
                                text.append(boxText);
                                text.append('\n');
                            }
                        }
                    }
                }
            }
            // Finally footer(s), if present
            if(includeHeadersFooters) {
                text.append(
                        extractHeaderFooter(sheet.getFirstFooter())
                        );
                text.append(
                        extractHeaderFooter(sheet.getOddFooter())
                        );
                text.append(
                        extractHeaderFooter(sheet.getEvenFooter())
                        );
            }
        }

        return text.toString();
    }

    private void handleStringCell(StringBuilder text, Cell cell) {
        String contents = cell.getRichStringCellValue().getString();
        checkMaxTextSize(text, contents);
        text.append(contents);
    }

    private void handleNonStringCell(StringBuilder text, Cell cell, DataFormatter formatter) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        if (type == CellType.NUMERIC) {
            CellStyle cs = cell.getCellStyle();

            if (cs != null && cs.getDataFormatString() != null) {
                String contents = formatter.formatRawCellContents(
                        cell.getNumericCellValue(), cs.getDataFormat(), cs.getDataFormatString());
                checkMaxTextSize(text, contents);
                text.append(contents);
                return;
            }
        }

        // No supported styling applies to this cell
        String contents = ((XSSFCell)cell).getRawValue();
        if (contents != null) {
            checkMaxTextSize(text, contents);
            text.append(contents);
        }
    }

    private String extractHeaderFooter(HeaderFooter hf) {
        return ExcelExtractor._extractHeaderFooter(hf);
    }
}
