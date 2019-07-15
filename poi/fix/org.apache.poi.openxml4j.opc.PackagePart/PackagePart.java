

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.RelationshipSource;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.openxml4j.opc.internal.ContentType;


public abstract class PackagePart implements Comparable<PackagePart> , RelationshipSource {
	protected OPCPackage _container;

	protected PackagePartName _partName;

	protected ContentType _contentType;

	private boolean _isRelationshipPart;

	private boolean _isDeleted;

	private PackageRelationshipCollection _relationships;

	protected PackagePart(OPCPackage pack, PackagePartName partName, ContentType contentType) throws InvalidFormatException {
		this(pack, partName, contentType, true);
	}

	protected PackagePart(OPCPackage pack, PackagePartName partName, ContentType contentType, boolean loadRelationships) throws InvalidFormatException {
		_partName = partName;
		_contentType = contentType;
		_container = pack;
		_isRelationshipPart = this._partName.isRelationshipPartURI();
		if (loadRelationships) {
			loadRelationships();
		}
	}

	public PackagePart(OPCPackage pack, PackagePartName partName, String contentType) throws InvalidFormatException {
		this(pack, partName, new ContentType(contentType));
	}

	public PackageRelationship findExistingRelation(PackagePart packagePart) {
		return null;
	}

	public PackageRelationship addExternalRelationship(String target, String relationshipType) {
		return addExternalRelationship(target, relationshipType, null);
	}

	public PackageRelationship addExternalRelationship(String target, String relationshipType, String id) {
		if (target == null) {
			throw new IllegalArgumentException(("target is null for type " + relationshipType));
		}
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		if ((_relationships) == null) {
		}
		URI targetURI;
		try {
			targetURI = new URI(target);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(("Invalid target - " + e));
		}
		return _relationships.addRelationship(targetURI, TargetMode.EXTERNAL, relationshipType, id);
	}

	public PackageRelationship addRelationship(PackagePartName targetPartName, TargetMode targetMode, String relationshipType) {
		return addRelationship(targetPartName, targetMode, relationshipType, null);
	}

	public PackageRelationship addRelationship(PackagePartName targetPartName, TargetMode targetMode, String relationshipType, String id) {
		if (targetPartName == null) {
			throw new IllegalArgumentException("targetPartName");
		}
		if (targetMode == null) {
			throw new IllegalArgumentException("targetMode");
		}
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		if ((this._isRelationshipPart) || (targetPartName.isRelationshipPartURI())) {
			throw new InvalidOperationException("Rule M1.25: The Relationships part shall not have relationships to any other part.");
		}
		if ((_relationships) == null) {
		}
		return _relationships.addRelationship(targetPartName.getURI(), targetMode, relationshipType, id);
	}

	public PackageRelationship addRelationship(URI targetURI, TargetMode targetMode, String relationshipType) {
		return addRelationship(targetURI, targetMode, relationshipType, null);
	}

	public PackageRelationship addRelationship(URI targetURI, TargetMode targetMode, String relationshipType, String id) {
		if (targetURI == null) {
			throw new IllegalArgumentException("targetPartName");
		}
		if (targetMode == null) {
			throw new IllegalArgumentException("targetMode");
		}
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		if ((this._isRelationshipPart) || (PackagingURIHelper.isRelationshipPartURI(targetURI))) {
			throw new InvalidOperationException("Rule M1.25: The Relationships part shall not have relationships to any other part.");
		}
		if ((_relationships) == null) {
		}
		return _relationships.addRelationship(targetURI, targetMode, relationshipType, id);
	}

	public void clearRelationships() {
		if ((_relationships) != null) {
			_relationships.clear();
		}
	}

	public void removeRelationship(String id) {
		if ((this._relationships) != null)
			this._relationships.removeRelationship(id);

	}

	public PackageRelationshipCollection getRelationships() throws InvalidFormatException {
		return getRelationshipsCore(null);
	}

	public PackageRelationship getRelationship(String id) {
		return this._relationships.getRelationshipByID(id);
	}

	public PackageRelationshipCollection getRelationshipsByType(String relationshipType) throws InvalidFormatException {
		return getRelationshipsCore(relationshipType);
	}

	private PackageRelationshipCollection getRelationshipsCore(String filter) throws InvalidFormatException {
		if ((_relationships) == null) {
			this.throwExceptionIfRelationship();
		}
		return new PackageRelationshipCollection(_relationships, filter);
	}

	public boolean hasRelationships() {
		return (!(this._isRelationshipPart)) && (((_relationships) != null) && ((_relationships.size()) > 0));
	}

	public boolean isRelationshipExists(PackageRelationship rel) {
		return (rel != null) && ((_relationships.getRelationshipByID(rel.getId())) != null);
	}

	public PackagePart getRelatedPart(PackageRelationship rel) throws InvalidFormatException {
		if (!(isRelationshipExists(rel))) {
			throw new IllegalArgumentException(((("Relationship " + rel) + " doesn't start with this part ") + (_partName)));
		}
		URI target = rel.getTargetURI();
		if ((target.getFragment()) != null) {
			String t = target.toString();
			try {
				target = new URI(t.substring(0, t.indexOf('#')));
			} catch (URISyntaxException e) {
				throw new InvalidFormatException(("Invalid target URI: " + target));
			}
		}
		PackagePartName relName = PackagingURIHelper.createPartName(target);
		return null;
	}

	public InputStream getInputStream() throws IOException {
		InputStream inStream = this.getInputStreamImpl();
		if (inStream == null) {
			throw new IOException(("Can't obtain the input stream from " + (_partName.getName())));
		}
		return inStream;
	}

	public OutputStream getOutputStream() {
		OutputStream outStream;
		outStream = null;
		return outStream;
	}

	private void throwExceptionIfRelationship() throws InvalidOperationException {
		if (this._isRelationshipPart)
			throw new InvalidOperationException("Can do this operation on a relationship part !");

	}

	void loadRelationships() throws InvalidFormatException {
		if (((this._relationships) == null) && (!(this._isRelationshipPart))) {
			this.throwExceptionIfRelationship();
		}
	}

	public PackagePartName getPartName() {
		return _partName;
	}

	public String getContentType() {
		return _contentType.toString();
	}

	public ContentType getContentTypeDetails() {
		return _contentType;
	}

	public void setContentType(String contentType) throws InvalidFormatException {
		if ((_container) == null) {
			_contentType = new ContentType(contentType);
		}else {
			_container.unregisterPartAndContentType(_partName);
			_contentType = new ContentType(contentType);
		}
	}

	public OPCPackage getPackage() {
		return _container;
	}

	public boolean isRelationshipPart() {
		return this._isRelationshipPart;
	}

	public boolean isDeleted() {
		return _isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this._isDeleted = isDeleted;
	}

	public long getSize() {
		return -1;
	}

	@Override
	public String toString() {
		return (("Name: " + (this._partName)) + " - Content Type: ") + (this._contentType);
	}

	@Override
	public int compareTo(PackagePart other) {
		if (other == null)
			return -1;

		return PackagePartName.compare(this._partName, other._partName);
	}

	protected abstract InputStream getInputStreamImpl() throws IOException;

	protected abstract OutputStream getOutputStreamImpl();

	public abstract boolean save(OutputStream zos) throws OpenXML4JException;

	public abstract boolean load(InputStream ios) throws InvalidFormatException;

	public abstract void close();

	public abstract void flush();

	public void clear() {
	}
}

