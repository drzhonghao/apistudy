import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.PackageProperties;
import org.apache.poi.ooxml.util.*;


import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.util.IOUtils;

import java.io.*;
import java.net.URI;

/**
 * Provides handy methods to work with OOXML packages
 */
public final class PackageHelper {

    public static OPCPackage open(InputStream is) throws IOException {
        try {
            return OPCPackage.open(is);
        } catch (InvalidFormatException e){
            throw new POIXMLException(e);
        }
    }

    /**
     * Clone the specified package.
     *
     * @param   pkg   the package to clone
     * @param   file  the destination file
     * @return  the cloned package
     */
    public static OPCPackage clone(OPCPackage pkg, File file) throws OpenXML4JException, IOException {

        String path = file.getAbsolutePath();

        OPCPackage dest = OPCPackage.create(path);
        PackageRelationshipCollection rels = pkg.getRelationships();
        for (PackageRelationship rel : rels) {
            PackagePart part = pkg.getPart(rel);
            PackagePart part_tgt;
            if (rel.getRelationshipType().equals(PackageRelationshipTypes.CORE_PROPERTIES)) {
                copyProperties(pkg.getPackageProperties(), dest.getPackageProperties());
                continue;
            }
            dest.addRelationship(part.getPartName(), rel.getTargetMode(), rel.getRelationshipType());
            part_tgt = dest.createPart(part.getPartName(), part.getContentType());

            OutputStream out = part_tgt.getOutputStream();
            IOUtils.copy(part.getInputStream(), out);
            out.close();

            if(part.hasRelationships()) {
                copy(pkg, part, dest, part_tgt);
            }
        }
        dest.close();

        //the temp file will be deleted when JVM terminates
        new File(path).deleteOnExit();
        return OPCPackage.open(path);
    }

    /**
     * Recursively copy package parts to the destination package
     */
    private static void copy(OPCPackage pkg, PackagePart part, OPCPackage tgt, PackagePart part_tgt) throws OpenXML4JException, IOException {
        PackageRelationshipCollection rels = part.getRelationships();
        if(rels != null) for (PackageRelationship rel : rels) {
            PackagePart p;
            if(rel.getTargetMode() == TargetMode.EXTERNAL){
                part_tgt.addExternalRelationship(rel.getTargetURI().toString(), rel.getRelationshipType(), rel.getId());
                //external relations don't have associated package parts
                continue;
            }
            URI uri = rel.getTargetURI();

            if(uri.getRawFragment() != null) {
                part_tgt.addRelationship(uri, rel.getTargetMode(), rel.getRelationshipType(), rel.getId());
                continue;
            }
            PackagePartName relName = PackagingURIHelper.createPartName(rel.getTargetURI());
            p = pkg.getPart(relName);
            part_tgt.addRelationship(p.getPartName(), rel.getTargetMode(), rel.getRelationshipType(), rel.getId());

            PackagePart dest;
            if(!tgt.containPart(p.getPartName())){
                dest = tgt.createPart(p.getPartName(), p.getContentType());
                OutputStream out = dest.getOutputStream();
                IOUtils.copy(p.getInputStream(), out);
                out.close();
                copy(pkg, p, tgt, dest);
            }
        }
    }

    /**
     * Copy core package properties
     *
     * @param src source properties
     * @param tgt target properties
     */
    private static void copyProperties(PackageProperties src, PackageProperties tgt) {
        tgt.setCategoryProperty(src.getCategoryProperty());
        tgt.setContentStatusProperty(src.getContentStatusProperty());
        tgt.setContentTypeProperty(src.getContentTypeProperty());
        tgt.setCreatorProperty(src.getCreatorProperty());
        tgt.setDescriptionProperty(src.getDescriptionProperty());
        tgt.setIdentifierProperty(src.getIdentifierProperty());
        tgt.setKeywordsProperty(src.getKeywordsProperty());
        tgt.setLanguageProperty(src.getLanguageProperty());
        tgt.setRevisionProperty(src.getRevisionProperty());
        tgt.setSubjectProperty(src.getSubjectProperty());
        tgt.setTitleProperty(src.getTitleProperty());
        tgt.setVersionProperty(src.getVersionProperty());
    }
}
