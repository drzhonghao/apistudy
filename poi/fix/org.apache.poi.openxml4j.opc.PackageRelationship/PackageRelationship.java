

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;


public final class PackageRelationship {
	private static URI containerRelationshipPart;

	static {
		try {
			PackageRelationship.containerRelationshipPart = new URI("/_rels/.rels");
		} catch (URISyntaxException e) {
		}
	}

	public static final String ID_ATTRIBUTE_NAME = "Id";

	public static final String RELATIONSHIPS_TAG_NAME = "Relationships";

	public static final String RELATIONSHIP_TAG_NAME = "Relationship";

	public static final String TARGET_ATTRIBUTE_NAME = "Target";

	public static final String TARGET_MODE_ATTRIBUTE_NAME = "TargetMode";

	public static final String TYPE_ATTRIBUTE_NAME = "Type";

	private String id;

	private OPCPackage container;

	private String relationshipType;

	private PackagePart source;

	private TargetMode targetMode;

	private URI targetUri;

	public PackageRelationship(OPCPackage pkg, PackagePart sourcePart, URI targetUri, TargetMode targetMode, String relationshipType, String id) {
		if (pkg == null)
			throw new IllegalArgumentException("pkg");

		if (targetUri == null)
			throw new IllegalArgumentException("targetUri");

		if (relationshipType == null)
			throw new IllegalArgumentException("relationshipType");

		if (id == null)
			throw new IllegalArgumentException("id");

		this.container = pkg;
		this.source = sourcePart;
		this.targetUri = targetUri;
		this.targetMode = targetMode;
		this.relationshipType = relationshipType;
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PackageRelationship)) {
			return false;
		}
		PackageRelationship rel = ((PackageRelationship) (obj));
		return ((((this.id.equals(rel.id)) && (this.relationshipType.equals(rel.relationshipType))) && ((rel.source) != null ? rel.source.equals(this.source) : true)) && ((this.targetMode) == (rel.targetMode))) && (this.targetUri.equals(rel.targetUri));
	}

	@Override
	public int hashCode() {
		return ((((this.id.hashCode()) + (this.relationshipType.hashCode())) + ((this.source) == null ? 0 : this.source.hashCode())) + (this.targetMode.hashCode())) + (this.targetUri.hashCode());
	}

	public static URI getContainerPartRelationship() {
		return PackageRelationship.containerRelationshipPart;
	}

	public OPCPackage getPackage() {
		return container;
	}

	public String getId() {
		return id;
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public PackagePart getSource() {
		return source;
	}

	public URI getSourceURI() {
		if ((source) == null) {
			return PackagingURIHelper.PACKAGE_ROOT_URI;
		}
		return null;
	}

	public TargetMode getTargetMode() {
		return targetMode;
	}

	public URI getTargetURI() {
		if ((targetMode) == (TargetMode.EXTERNAL)) {
			return targetUri;
		}
		if (!(targetUri.toASCIIString().startsWith("/"))) {
			return PackagingURIHelper.resolvePartUri(getSourceURI(), targetUri);
		}
		return targetUri;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(((id) == null ? "id=null" : "id=" + (id)));
		sb.append(((container) == null ? " - container=null" : " - container=" + (container)));
		sb.append(((relationshipType) == null ? " - relationshipType=null" : " - relationshipType=" + (relationshipType)));
		sb.append(((source) == null ? " - source=null" : " - source=" + (getSourceURI().toASCIIString())));
		sb.append(((targetUri) == null ? " - target=null" : " - target=" + (getTargetURI().toASCIIString())));
		sb.append(((targetMode) == null ? ",targetMode=null" : ",targetMode=" + (targetMode)));
		return sb.toString();
	}
}

