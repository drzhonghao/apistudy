

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;
import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackageNamespaces;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public final class PackageRelationshipCollection implements Iterable<PackageRelationship> {
	private static final POILogger logger = POILogFactory.getLogger(PackageRelationshipCollection.class);

	private final TreeMap<String, PackageRelationship> relationshipsByID = new TreeMap<>();

	private final TreeMap<String, PackageRelationship> relationshipsByType = new TreeMap<>();

	private HashMap<String, PackageRelationship> internalRelationshipsByTargetName = new HashMap<>();

	private PackagePart relationshipPart;

	private PackagePart sourcePart;

	private PackagePartName partName;

	private OPCPackage container;

	private int nextRelationshipId = -1;

	PackageRelationshipCollection() {
	}

	public PackageRelationshipCollection(PackageRelationshipCollection coll, String filter) {
		this();
		for (PackageRelationship rel : coll.relationshipsByID.values()) {
			if ((filter == null) || (rel.getRelationshipType().equals(filter)))
				addRelationship(rel);

		}
	}

	public PackageRelationshipCollection(OPCPackage container) throws InvalidFormatException {
		this(container, null);
	}

	public PackageRelationshipCollection(PackagePart part) throws InvalidFormatException {
	}

	public PackageRelationshipCollection(OPCPackage container, PackagePart part) throws InvalidFormatException {
		if (container == null)
			throw new IllegalArgumentException("container needs to be specified");

		if ((part != null) && (part.isRelationshipPart()))
			throw new IllegalArgumentException("part");

		this.container = container;
		this.sourcePart = part;
		this.partName = PackageRelationshipCollection.getRelationshipPartName(part);
		if (((container.getPackageAccess()) != (PackageAccess.WRITE)) && (container.containPart(this.partName))) {
			relationshipPart = container.getPart(this.partName);
			parseRelationshipsPart(relationshipPart);
		}
	}

	private static PackagePartName getRelationshipPartName(PackagePart part) throws InvalidOperationException {
		PackagePartName partName;
		if (part == null) {
			partName = PackagingURIHelper.PACKAGE_ROOT_PART_NAME;
		}else {
			partName = part.getPartName();
		}
		return PackagingURIHelper.getRelationshipPartName(partName);
	}

	public void addRelationship(PackageRelationship relPart) {
		if (((relPart == null) || ((relPart.getId()) == null)) || (relPart.getId().isEmpty())) {
			throw new IllegalArgumentException("invalid relationship part/id");
		}
		relationshipsByID.put(relPart.getId(), relPart);
		relationshipsByType.put(relPart.getRelationshipType(), relPart);
	}

	public PackageRelationship addRelationship(URI targetUri, TargetMode targetMode, String relationshipType, String id) {
		if (id == null) {
			if ((nextRelationshipId) == (-1)) {
				nextRelationshipId = (size()) + 1;
			}
			do {
				id = "rId" + ((nextRelationshipId)++);
			} while ((relationshipsByID.get(id)) != null );
		}
		PackageRelationship rel = new PackageRelationship(container, sourcePart, targetUri, targetMode, relationshipType, id);
		addRelationship(rel);
		if (targetMode == (TargetMode.INTERNAL)) {
			internalRelationshipsByTargetName.put(targetUri.toASCIIString(), rel);
		}
		return rel;
	}

	public void removeRelationship(String id) {
		PackageRelationship rel = relationshipsByID.get(id);
		if (rel != null) {
			relationshipsByID.remove(rel.getId());
			relationshipsByType.values().remove(rel);
			internalRelationshipsByTargetName.values().remove(rel);
		}
	}

	public PackageRelationship getRelationship(int index) {
		if ((index < 0) || (index > (relationshipsByID.values().size())))
			throw new IllegalArgumentException("index");

		int i = 0;
		for (PackageRelationship rel : relationshipsByID.values()) {
			if (index == (i++))
				return rel;

		}
		return null;
	}

	public PackageRelationship getRelationshipByID(String id) {
		return relationshipsByID.get(id);
	}

	public int size() {
		return relationshipsByID.values().size();
	}

	public void parseRelationshipsPart(PackagePart relPart) throws InvalidFormatException {
		try {
			PackageRelationshipCollection.logger.log(POILogger.DEBUG, ("Parsing relationship: " + (relPart.getPartName())));
			Document xmlRelationshipsDoc = DocumentHelper.readDocument(relPart.getInputStream());
			Element root = xmlRelationshipsDoc.getDocumentElement();
			boolean fCorePropertiesRelationship = false;
			NodeList nodeList = root.getElementsByTagNameNS(PackageNamespaces.RELATIONSHIPS, PackageRelationship.RELATIONSHIP_TAG_NAME);
			int nodeCount = nodeList.getLength();
			for (int i = 0; i < nodeCount; i++) {
				Element element = ((Element) (nodeList.item(i)));
				String id = element.getAttribute(PackageRelationship.ID_ATTRIBUTE_NAME);
				String type = element.getAttribute(PackageRelationship.TYPE_ATTRIBUTE_NAME);
				if (type.equals(PackageRelationshipTypes.CORE_PROPERTIES))
					if (!fCorePropertiesRelationship)
						fCorePropertiesRelationship = true;
					else
						throw new InvalidFormatException("OPC Compliance error [M4.1]: there is more than one core properties relationship in the package !");


				Attr targetModeAttr = element.getAttributeNode(PackageRelationship.TARGET_MODE_ATTRIBUTE_NAME);
				TargetMode targetMode = TargetMode.INTERNAL;
				if (targetModeAttr != null) {
					targetMode = (targetModeAttr.getValue().toLowerCase(Locale.ROOT).equals("internal")) ? TargetMode.INTERNAL : TargetMode.EXTERNAL;
				}
				URI target = PackagingURIHelper.toURI("http://invalid.uri");
				String value = element.getAttribute(PackageRelationship.TARGET_ATTRIBUTE_NAME);
				try {
					target = PackagingURIHelper.toURI(value);
				} catch (URISyntaxException e) {
					PackageRelationshipCollection.logger.log(POILogger.ERROR, (("Cannot convert " + value) + " in a valid relationship URI-> dummy-URI used"), e);
				}
				addRelationship(target, targetMode, type, id);
			}
		} catch (Exception e) {
			PackageRelationshipCollection.logger.log(POILogger.ERROR, e);
			throw new InvalidFormatException(e.getMessage());
		}
	}

	public PackageRelationshipCollection getRelationships(String typeFilter) {
		return new PackageRelationshipCollection(this, typeFilter);
	}

	public Iterator<PackageRelationship> iterator() {
		return relationshipsByID.values().iterator();
	}

	public Iterator<PackageRelationship> iterator(String typeFilter) {
		ArrayList<PackageRelationship> retArr = new ArrayList<>();
		for (PackageRelationship rel : relationshipsByID.values()) {
			if (rel.getRelationshipType().equals(typeFilter))
				retArr.add(rel);

		}
		return retArr.iterator();
	}

	public void clear() {
		relationshipsByID.clear();
		relationshipsByType.clear();
		internalRelationshipsByTargetName.clear();
	}

	public PackageRelationship findExistingInternalRelation(PackagePart packagePart) {
		return internalRelationshipsByTargetName.get(packagePart.getPartName().getName());
	}

	@Override
	public String toString() {
		String str = (relationshipsByID.size()) + " relationship(s) = [";
		if ((partName) != null) {
			str += "," + (partName);
		}else {
			str += ",uri=null)";
		}
		return str + "]";
	}
}

