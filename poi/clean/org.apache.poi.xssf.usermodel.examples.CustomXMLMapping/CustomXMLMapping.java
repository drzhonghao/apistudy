import org.apache.poi.xssf.usermodel.examples.*;


import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.extractor.XSSFExportToXml;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFMap;

import java.io.ByteArrayOutputStream;

/**
 * Print all custom XML mappings registered in the given workbook
 */
public class CustomXMLMapping {

    public static void main(String[] args) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(args[0]);
             XSSFWorkbook wb = new XSSFWorkbook(pkg)) {
            for (XSSFMap map : wb.getCustomXMLMappings()) {
                XSSFExportToXml exporter = new XSSFExportToXml(map);

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                exporter.exportToXML(os, true);
                String xml = os.toString("UTF-8");
                System.out.println(xml);
            }
        }
    }
}
