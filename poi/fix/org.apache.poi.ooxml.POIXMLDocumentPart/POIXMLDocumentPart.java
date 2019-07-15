

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLFactory;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.exceptions.PartAlreadyExistsException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xwpf.usermodel.XWPFRelation;


public class POIXMLDocumentPart {
	private static final POILogger logger = POILogFactory.getLogger(POIXMLDocumentPart.class);

	private String coreDocumentRel = PackageRelationshipTypes.CORE_DOCUMENT;

	private PackagePart packagePart;

	private POIXMLDocumentPart parent;

	private Map<String, POIXMLDocumentPart.RelationPart> relations = new LinkedHashMap<>();

	private boolean isCommited = false;

	public boolean isCommited() {
		return isCommited;
	}

	public void setCommited(boolean isCommited) {
		this.isCommited = isCommited;
	}

	public static class RelationPart {
		private final PackageRelationship relationship;

		private final POIXMLDocumentPart documentPart;

		RelationPart(PackageRelationship relationship, POIXMLDocumentPart documentPart) {
			this.relationship = relationship;
			this.documentPart = documentPart;
		}

		public PackageRelationship getRelationship() {
			return relationship;
		}

		@SuppressWarnings("unchecked")
		public <T extends POIXMLDocumentPart> T getDocumentPart() {
			return ((T) (documentPart));
		}
	}

	private int relationCounter;

	int incrementRelationCounter() {
		(relationCounter)++;
		return relationCounter;
	}

	int decrementRelationCounter() {
		(relationCounter)--;
		return relationCounter;
	}

	int getRelationCounter() {
		return relationCounter;
	}

	public POIXMLDocumentPart(OPCPackage pkg) {
		this(pkg, PackageRelationshipTypes.CORE_DOCUMENT);
	}

	public POIXMLDocumentPart(OPCPackage pkg, String coreDocumentRel) {
		this(POIXMLDocumentPart.getPartFromOPCPackage(pkg, coreDocumentRel));
		this.coreDocumentRel = coreDocumentRel;
	}

	public POIXMLDocumentPart() {
	}

	public POIXMLDocumentPart(PackagePart part) {
		this(null, part);
	}

	public POIXMLDocumentPart(POIXMLDocumentPart parent, PackagePart part) {
		this.packagePart = part;
		this.parent = parent;
	}

	protected final void rebase(OPCPackage pkg) throws InvalidFormatException {
		PackageRelationshipCollection cores = packagePart.getRelationshipsByType(coreDocumentRel);
		if ((cores.size()) != 1) {
			throw new IllegalStateException((((("Tried to rebase using " + (coreDocumentRel)) + " but found ") + (cores.size())) + " parts of the right type"));
		}
		packagePart = packagePart.getRelatedPart(cores.getRelationship(0));
	}

	public final PackagePart getPackagePart() {
		return packagePart;
	}

	public final List<POIXMLDocumentPart> getRelations() {
		List<POIXMLDocumentPart> l = new ArrayList<>();
		for (POIXMLDocumentPart.RelationPart rp : relations.values()) {
			l.add(rp.getDocumentPart());
		}
		return Collections.unmodifiableList(l);
	}

	public final List<POIXMLDocumentPart.RelationPart> getRelationParts() {
		List<POIXMLDocumentPart.RelationPart> l = new ArrayList<>(relations.values());
		return Collections.unmodifiableList(l);
	}

	public final POIXMLDocumentPart getRelationById(String id) {
		POIXMLDocumentPart.RelationPart rp = getRelationPartById(id);
		return rp == null ? null : rp.getDocumentPart();
	}

	public final POIXMLDocumentPart.RelationPart getRelationPartById(String id) {
		return relations.get(id);
	}

	public final String getRelationId(POIXMLDocumentPart part) {
		for (POIXMLDocumentPart.RelationPart rp : relations.values()) {
			if ((rp.getDocumentPart()) == part) {
				return rp.getRelationship().getId();
			}
		}
		return null;
	}

	public final POIXMLDocumentPart.RelationPart addRelation(String relId, POIXMLRelation relationshipType, POIXMLDocumentPart part) {
		PackageRelationship pr = this.packagePart.findExistingRelation(part.getPackagePart());
		if (pr == null) {
			PackagePartName ppn = part.getPackagePart().getPartName();
			String relType = relationshipType.getRelation();
			pr = packagePart.addRelationship(ppn, TargetMode.INTERNAL, relType, relId);
		}
		addRelation(pr, part);
		return new POIXMLDocumentPart.RelationPart(pr, part);
	}

	private void addRelation(PackageRelationship pr, POIXMLDocumentPart part) {
		relations.put(pr.getId(), new POIXMLDocumentPart.RelationPart(pr, part));
		part.incrementRelationCounter();
	}

	protected final void removeRelation(POIXMLDocumentPart part) {
		removeRelation(part, true);
	}

	protected final boolean removeRelation(POIXMLDocumentPart part, boolean removeUnusedParts) {
		String id = getRelationId(part);
		return removeRelation(id, removeUnusedParts);
	}

	protected final void removeRelation(String partId) {
		removeRelation(partId, true);
	}

	private final boolean removeRelation(String partId, boolean removeUnusedParts) {
		POIXMLDocumentPart.RelationPart rp = relations.get(partId);
		if (rp == null) {
			return false;
		}
		POIXMLDocumentPart part = rp.getDocumentPart();
		part.decrementRelationCounter();
		getPackagePart().removeRelationship(partId);
		relations.remove(partId);
		if (removeUnusedParts) {
			if ((part.getRelationCounter()) == 0) {
				try {
					part.onDocumentRemove();
				} catch (IOException e) {
					throw new POIXMLException(e);
				}
				getPackagePart().getPackage().removePart(part.getPackagePart());
			}
		}
		return true;
	}

	public final POIXMLDocumentPart getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return (packagePart) == null ? "" : packagePart.toString();
	}

	protected void commit() throws IOException {
	}

	protected final void onSave(Set<PackagePart> alreadySaved) throws IOException {
		if (this.isCommited) {
			return;
		}
		prepareForCommit();
		commit();
		alreadySaved.add(this.getPackagePart());
		for (POIXMLDocumentPart.RelationPart rp : relations.values()) {
			POIXMLDocumentPart p = rp.getDocumentPart();
			if (!(alreadySaved.contains(p.getPackagePart()))) {
				p.onSave(alreadySaved);
			}
		}
	}

	protected void prepareForCommit() {
		PackagePart part = this.getPackagePart();
		if (part != null) {
			part.clear();
		}
	}

	public final POIXMLDocumentPart createRelationship(POIXMLRelation descriptor, POIXMLFactory factory) {
		return createRelationship(descriptor, factory, (-1), false).getDocumentPart();
	}

	public final POIXMLDocumentPart createRelationship(POIXMLRelation descriptor, POIXMLFactory factory, int idx) {
		return createRelationship(descriptor, factory, idx, false).getDocumentPart();
	}

	protected final int getNextPartNumber(POIXMLRelation descriptor, int minIdx) {
		OPCPackage pkg = packagePart.getPackage();
		try {
			String name = descriptor.getDefaultFileName();
			if (name.equals(descriptor.getFileName(9999))) {
				PackagePartName ppName = PackagingURIHelper.createPartName(name);
				if (pkg.containPart(ppName)) {
					return -1;
				}else {
					return 0;
				}
			}
			int idx = (minIdx < 0) ? 1 : minIdx;
			int maxIdx = minIdx + (pkg.getParts().size());
			while (idx <= maxIdx) {
				name = descriptor.getFileName(idx);
				PackagePartName ppName = PackagingURIHelper.createPartName(name);
				if (!(pkg.containPart(ppName))) {
					return idx;
				}
				idx++;
			} 
		} catch (InvalidFormatException e) {
			throw new POIXMLException(e);
		}
		return -1;
	}

	public final POIXMLDocumentPart.RelationPart createRelationship(POIXMLRelation descriptor, POIXMLFactory factory, int idx, boolean noRelation) {
		try {
			PackagePartName ppName = PackagingURIHelper.createPartName(descriptor.getFileName(idx));
			PackageRelationship rel = null;
			PackagePart part = packagePart.getPackage().createPart(ppName, descriptor.getContentType());
			if (!noRelation) {
				rel = packagePart.addRelationship(ppName, TargetMode.INTERNAL, descriptor.getRelation());
			}
			if (!noRelation) {
			}
		} catch (PartAlreadyExistsException pae) {
			throw pae;
		} catch (Exception e) {
			throw new POIXMLException(e);
		}
		return null;
	}

	protected void read(POIXMLFactory factory, Map<PackagePart, POIXMLDocumentPart> context) throws OpenXML4JException {
		PackagePart pp = getPackagePart();
		if (pp.getContentType().equals(XWPFRelation.GLOSSARY_DOCUMENT.getContentType())) {
			POIXMLDocumentPart.logger.log(POILogger.WARN, ("POI does not currently support template.main+xml (glossary) parts.  " + "Skipping this part for now."));
			return;
		}
		POIXMLDocumentPart otherChild = context.put(pp, this);
		if ((otherChild != null) && (otherChild != (this))) {
			throw new POIXMLException("Unique PackagePart-POIXMLDocumentPart relation broken!");
		}
		if (!(pp.hasRelationships()))
			return;

		PackageRelationshipCollection rels = packagePart.getRelationships();
		List<POIXMLDocumentPart> readLater = new ArrayList<>();
		for (PackageRelationship rel : rels) {
			if ((rel.getTargetMode()) == (TargetMode.INTERNAL)) {
				URI uri = rel.getTargetURI();
				PackagePartName relName;
				if ((uri.getRawFragment()) != null) {
					relName = PackagingURIHelper.createPartName(uri.getPath());
				}else {
					relName = PackagingURIHelper.createPartName(uri);
				}
				final PackagePart p = packagePart.getPackage().getPart(relName);
				if (p == null) {
					POIXMLDocumentPart.logger.log(POILogger.ERROR, ("Skipped invalid entry " + (rel.getTargetURI())));
					continue;
				}
				POIXMLDocumentPart childPart = context.get(p);
				if (childPart == null) {
					childPart.parent = this;
					context.put(p, childPart);
					readLater.add(childPart);
				}
				addRelation(rel, childPart);
			}
		}
		for (POIXMLDocumentPart childPart : readLater) {
			childPart.read(factory, context);
		}
	}

	protected PackagePart getTargetPart(PackageRelationship rel) throws InvalidFormatException {
		return getPackagePart().getRelatedPart(rel);
	}

	protected void onDocumentCreate() throws IOException {
	}

	protected void onDocumentRead() throws IOException {
	}

	protected void onDocumentRemove() throws IOException {
	}

	@org.apache.poi.util.Internal
	@Deprecated
	public static void _invokeOnDocumentRead(POIXMLDocumentPart part) throws IOException {
		part.onDocumentRead();
	}

	private static PackagePart getPartFromOPCPackage(OPCPackage pkg, String coreDocumentRel) {
		PackageRelationship coreRel = pkg.getRelationshipsByType(coreDocumentRel).getRelationship(0);
		if (coreRel != null) {
			PackagePart pp = pkg.getPart(coreRel);
			if (pp == null) {
				throw new POIXMLException((("OOXML file structure broken/invalid - core document '" + (coreRel.getTargetURI())) + "' not found."));
			}
			return pp;
		}
		coreRel = pkg.getRelationshipsByType(PackageRelationshipTypes.STRICT_CORE_DOCUMENT).getRelationship(0);
		if (coreRel != null) {
			throw new POIXMLException("Strict OOXML isn't currently supported, please see bug #57699");
		}
		throw new POIXMLException("OOXML file structure broken/invalid - no core document found!");
	}
}

