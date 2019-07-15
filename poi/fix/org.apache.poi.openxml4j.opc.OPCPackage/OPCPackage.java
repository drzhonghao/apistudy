

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JRuntimeException;
import org.apache.poi.openxml4j.exceptions.PartAlreadyExistsException;
import org.apache.poi.openxml4j.opc.ContentTypes;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartCollection;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageProperties;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.RelationshipSource;
import org.apache.poi.openxml4j.opc.StreamHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.apache.poi.openxml4j.opc.internal.ContentTypeManager;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.openxml4j.opc.internal.PartMarshaller;
import org.apache.poi.openxml4j.opc.internal.PartUnmarshaller;
import org.apache.poi.openxml4j.opc.internal.marshallers.DefaultMarshaller;
import org.apache.poi.openxml4j.opc.internal.marshallers.ZipPackagePropertiesMarshaller;
import org.apache.poi.openxml4j.opc.internal.unmarshallers.PackagePropertiesUnmarshaller;
import org.apache.poi.openxml4j.util.ZipEntrySource;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public abstract class OPCPackage implements Closeable , RelationshipSource {
	private static final POILogger logger = POILogFactory.getLogger(OPCPackage.class);

	protected static final PackageAccess defaultPackageAccess = PackageAccess.READ_WRITE;

	private final PackageAccess packageAccess;

	private PackagePartCollection partList;

	protected PackageRelationshipCollection relationships;

	protected final Map<ContentType, PartMarshaller> partMarshallers = new HashMap<>(5);

	protected final PartMarshaller defaultPartMarshaller = new DefaultMarshaller();

	protected final Map<ContentType, PartUnmarshaller> partUnmarshallers = new HashMap<>(2);

	protected PackagePropertiesPart packageProperties;

	protected ContentTypeManager contentTypeManager;

	protected boolean isDirty;

	protected String originalPackagePath;

	protected OutputStream output;

	OPCPackage(PackageAccess access) {
		this.packageAccess = access;
		final ContentType contentType = OPCPackage.newCorePropertiesPart();
		this.partUnmarshallers.put(contentType, new PackagePropertiesUnmarshaller());
		this.partMarshallers.put(contentType, new ZipPackagePropertiesMarshaller());
	}

	private static ContentType newCorePropertiesPart() {
		try {
			return new ContentType(ContentTypes.CORE_PROPERTIES_PART);
		} catch (InvalidFormatException e) {
			throw new OpenXML4JRuntimeException((("Package.init() : this exception should never happen, " + "if you read this message please send a mail to the developers team. : ") + (e.getMessage())), e);
		}
	}

	public static OPCPackage open(String path) throws InvalidFormatException {
		return OPCPackage.open(path, OPCPackage.defaultPackageAccess);
	}

	public static OPCPackage open(File file) throws InvalidFormatException {
		return OPCPackage.open(file, OPCPackage.defaultPackageAccess);
	}

	public static OPCPackage open(ZipEntrySource zipEntry) throws InvalidFormatException {
		return null;
	}

	public static OPCPackage open(String path, PackageAccess access) throws InvalidFormatException, InvalidOperationException {
		if ((path == null) || (path.trim().isEmpty())) {
			throw new IllegalArgumentException("'path' must be given");
		}
		File file = new File(path);
		if ((file.exists()) && (file.isDirectory())) {
			throw new IllegalArgumentException("path must not be a directory");
		}
		boolean success = false;
		return null;
	}

	public static OPCPackage open(File file, PackageAccess access) throws InvalidFormatException {
		if (file == null) {
			throw new IllegalArgumentException("'file' must be given");
		}
		if ((file.exists()) && (file.isDirectory())) {
			throw new IllegalArgumentException("file must not be a directory");
		}
		return null;
	}

	public static OPCPackage open(InputStream in) throws IOException, InvalidFormatException {
		return null;
	}

	public static OPCPackage openOrCreate(File file) throws InvalidFormatException {
		if (file.exists()) {
			return OPCPackage.open(file.getAbsolutePath());
		}else {
			return OPCPackage.create(file);
		}
	}

	public static OPCPackage create(String path) {
		return OPCPackage.create(new File(path));
	}

	public static OPCPackage create(File file) {
		if ((file == null) || ((file.exists()) && (file.isDirectory()))) {
			throw new IllegalArgumentException("file");
		}
		if (file.exists()) {
			throw new InvalidOperationException("This package (or file) already exists : use the open() method or delete the file.");
		}
		return null;
	}

	public static OPCPackage create(OutputStream output) {
		return null;
	}

	private static void configurePackage(OPCPackage pkg) {
		try {
			pkg.contentTypeManager.addContentType(PackagingURIHelper.createPartName(PackagingURIHelper.PACKAGE_RELATIONSHIPS_ROOT_URI), ContentTypes.RELATIONSHIPS_PART);
			pkg.contentTypeManager.addContentType(PackagingURIHelper.createPartName("/default.xml"), ContentTypes.PLAIN_OLD_XML);
			pkg.packageProperties.setCreatorProperty("Generated by Apache POI OpenXML4J");
			pkg.packageProperties.setCreatedProperty(Optional.of(new Date()));
		} catch (InvalidFormatException e) {
			throw new IllegalStateException(e);
		}
	}

	public void flush() {
		throwExceptionIfReadOnly();
		if ((this.packageProperties) != null) {
			this.packageProperties.flush();
		}
		this.flushImpl();
	}

	@Override
	public void close() throws IOException {
		if ((this.packageAccess) == (PackageAccess.READ)) {
			OPCPackage.logger.log(POILogger.WARN, "The close() method is intended to SAVE a package. This package is open in READ ONLY mode, use the revert() method instead !");
			revert();
			return;
		}
		if ((this.contentTypeManager) == null) {
			OPCPackage.logger.log(POILogger.WARN, "Unable to call close() on a package that hasn't been fully opened yet");
			revert();
			return;
		}
		if (((this.originalPackagePath) != null) && (!(this.originalPackagePath.trim().isEmpty()))) {
			File targetFile = new File(this.originalPackagePath);
			if ((!(targetFile.exists())) || (!(this.originalPackagePath.equalsIgnoreCase(targetFile.getAbsolutePath())))) {
				save(targetFile);
			}else {
				closeImpl();
			}
		}else
			if ((this.output) != null) {
				save(this.output);
				output.close();
			}

		this.contentTypeManager.clearAll();
	}

	public void revert() {
		revertImpl();
	}

	public void addThumbnail(String path) throws IOException {
		if ((path == null) || (path.isEmpty())) {
			throw new IllegalArgumentException("path");
		}
		String name = path.substring(((path.lastIndexOf(File.separatorChar)) + 1));
		try (FileInputStream is = new FileInputStream(path)) {
			addThumbnail(name, is);
		}
	}

	public void addThumbnail(String filename, InputStream data) throws IOException {
		if ((filename == null) || (filename.isEmpty())) {
			throw new IllegalArgumentException("filename");
		}
		String contentType = ContentTypes.getContentTypeFromFileExtension(filename);
		PackagePartName thumbnailPartName;
		try {
			thumbnailPartName = PackagingURIHelper.createPartName(("/docProps/" + filename));
		} catch (InvalidFormatException e) {
			String partName = "/docProps/thumbnail" + (filename.substring(((filename.lastIndexOf(".")) + 1)));
			try {
				thumbnailPartName = PackagingURIHelper.createPartName(partName);
			} catch (InvalidFormatException e2) {
				throw new InvalidOperationException((("Can't add a thumbnail file named '" + filename) + "'"), e2);
			}
		}
		if ((this.getPart(thumbnailPartName)) != null) {
			throw new InvalidOperationException((("You already add a thumbnail named '" + filename) + "'"));
		}
		PackagePart thumbnailPart = this.createPart(thumbnailPartName, contentType, false);
		this.addRelationship(thumbnailPartName, TargetMode.INTERNAL, PackageRelationshipTypes.THUMBNAIL);
		StreamHelper.copyStream(data, thumbnailPart.getOutputStream());
	}

	void throwExceptionIfReadOnly() throws InvalidOperationException {
		if ((packageAccess) == (PackageAccess.READ)) {
			throw new InvalidOperationException("Operation not allowed, document open in read only mode!");
		}
	}

	void throwExceptionIfWriteOnly() throws InvalidOperationException {
		if ((packageAccess) == (PackageAccess.WRITE)) {
			throw new InvalidOperationException("Operation not allowed, document open in write only mode!");
		}
	}

	public PackageProperties getPackageProperties() throws InvalidFormatException {
		this.throwExceptionIfWriteOnly();
		if ((this.packageProperties) == null) {
		}
		return this.packageProperties;
	}

	public PackagePart getPart(PackagePartName partName) {
		throwExceptionIfWriteOnly();
		if (partName == null) {
			throw new IllegalArgumentException("partName");
		}
		if ((partList) == null) {
			try {
				getParts();
			} catch (InvalidFormatException e) {
				return null;
			}
		}
		return partList.get(partName);
	}

	public ArrayList<PackagePart> getPartsByContentType(String contentType) {
		ArrayList<PackagePart> retArr = new ArrayList<>();
		for (PackagePart part : partList.sortedValues()) {
			if (part.getContentType().equals(contentType)) {
				retArr.add(part);
			}
		}
		return retArr;
	}

	public ArrayList<PackagePart> getPartsByRelationshipType(String relationshipType) {
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		ArrayList<PackagePart> retArr = new ArrayList<>();
		for (PackageRelationship rel : getRelationshipsByType(relationshipType)) {
			PackagePart part = getPart(rel);
			if (part != null) {
				retArr.add(part);
			}
		}
		Collections.sort(retArr);
		return retArr;
	}

	public List<PackagePart> getPartsByName(final Pattern namePattern) {
		if (namePattern == null) {
			throw new IllegalArgumentException("name pattern must not be null");
		}
		Matcher matcher = namePattern.matcher("");
		ArrayList<PackagePart> result = new ArrayList<>();
		for (PackagePart part : partList.sortedValues()) {
			PackagePartName partName = part.getPartName();
			if (matcher.reset(partName.getName()).matches()) {
				result.add(part);
			}
		}
		return result;
	}

	public PackagePart getPart(PackageRelationship partRel) {
		PackagePart retPart = null;
		ensureRelationships();
		for (PackageRelationship rel : relationships) {
			if (rel.getRelationshipType().equals(partRel.getRelationshipType())) {
				try {
					retPart = getPart(PackagingURIHelper.createPartName(rel.getTargetURI()));
				} catch (InvalidFormatException e) {
					continue;
				}
				break;
			}
		}
		return retPart;
	}

	public ArrayList<PackagePart> getParts() throws InvalidFormatException {
		throwExceptionIfWriteOnly();
		if ((partList) == null) {
			boolean hasCorePropertiesPart = false;
			boolean needCorePropertiesPart = true;
			partList = getPartsImpl();
			for (PackagePart part : new ArrayList<>(partList.sortedValues())) {
				if (ContentTypes.CORE_PROPERTIES_PART.equals(part.getContentType())) {
					if (!hasCorePropertiesPart) {
						hasCorePropertiesPart = true;
					}else {
						OPCPackage.logger.log(POILogger.WARN, ("OPC Compliance error [M4.1]: " + ("there is more than one core properties relationship in the package! " + "POI will use only the first, but other software may reject this file.")));
					}
				}
			}
		}
		return new ArrayList<>(partList.sortedValues());
	}

	public PackagePart createPart(PackagePartName partName, String contentType) {
		return this.createPart(partName, contentType, true);
	}

	PackagePart createPart(PackagePartName partName, String contentType, boolean loadRelationships) {
		throwExceptionIfReadOnly();
		if (partName == null) {
			throw new IllegalArgumentException("partName");
		}
		if ((contentType == null) || (contentType.isEmpty())) {
			throw new IllegalArgumentException("contentType");
		}
		if ((partList.containsKey(partName)) && (!(partList.get(partName).isDeleted()))) {
			throw new PartAlreadyExistsException((((("A part with the name '" + (partName.getName())) + "'") + " already exists : Packages shall not contain equivalent part names and package") + " implementers shall neither create nor recognize packages with equivalent part names. [M1.12]"));
		}
		if (contentType.equals(ContentTypes.CORE_PROPERTIES_PART)) {
			if ((this.packageProperties) != null) {
				throw new InvalidOperationException("OPC Compliance error [M4.1]: you try to add more than one core properties relationship in the package !");
			}
		}
		PackagePart part = this.createPartImpl(partName, contentType, loadRelationships);
		this.contentTypeManager.addContentType(partName, contentType);
		this.partList.put(partName, part);
		this.isDirty = true;
		return part;
	}

	public PackagePart createPart(PackagePartName partName, String contentType, ByteArrayOutputStream content) {
		PackagePart addedPart = this.createPart(partName, contentType);
		if (addedPart == null) {
			return null;
		}
		if (content != null) {
			try {
				OutputStream partOutput = addedPart.getOutputStream();
				if (partOutput == null) {
					return null;
				}
				partOutput.write(content.toByteArray(), 0, content.size());
				partOutput.close();
			} catch (IOException ioe) {
				return null;
			}
		}else {
			return null;
		}
		return addedPart;
	}

	protected PackagePart addPackagePart(PackagePart part) {
		throwExceptionIfReadOnly();
		if (part == null) {
			throw new IllegalArgumentException("part");
		}
		this.isDirty = true;
		return part;
	}

	public void removePart(PackagePart part) {
		if (part != null) {
			removePart(part.getPartName());
		}
	}

	public void removePart(PackagePartName partName) {
		throwExceptionIfReadOnly();
		if ((partName == null) || (!(this.containPart(partName)))) {
			throw new IllegalArgumentException("partName");
		}
		if (this.partList.containsKey(partName)) {
			this.partList.get(partName).setDeleted(true);
			this.removePartImpl(partName);
			this.partList.remove(partName);
		}else {
			this.removePartImpl(partName);
		}
		this.contentTypeManager.removeContentType(partName);
		if (partName.isRelationshipPartURI()) {
			URI sourceURI = PackagingURIHelper.getSourcePartUriFromRelationshipPartUri(partName.getURI());
			PackagePartName sourcePartName;
			try {
				sourcePartName = PackagingURIHelper.createPartName(sourceURI);
			} catch (InvalidFormatException e) {
				OPCPackage.logger.log(POILogger.ERROR, (("Part name URI '" + sourceURI) + "' is not valid ! This message is not intended to be displayed !"));
				return;
			}
			if (sourcePartName.getURI().equals(PackagingURIHelper.PACKAGE_ROOT_URI)) {
				clearRelationships();
			}else
				if (containPart(sourcePartName)) {
					PackagePart part = getPart(sourcePartName);
					if (part != null) {
						part.clearRelationships();
					}
				}

		}
		this.isDirty = true;
	}

	public void removePartRecursive(PackagePartName partName) throws InvalidFormatException {
		PackagePart relPart = this.partList.get(PackagingURIHelper.getRelationshipPartName(partName));
		PackagePart partToRemove = this.partList.get(partName);
		if (relPart != null) {
			PackageRelationshipCollection partRels = new PackageRelationshipCollection(partToRemove);
			for (PackageRelationship rel : partRels) {
				PackagePartName partNameToRemove = PackagingURIHelper.createPartName(PackagingURIHelper.resolvePartUri(rel.getSourceURI(), rel.getTargetURI()));
				removePart(partNameToRemove);
			}
		}
	}

	public void deletePart(PackagePartName partName) {
		if (partName == null) {
			throw new IllegalArgumentException("partName");
		}
		this.removePart(partName);
		this.removePart(PackagingURIHelper.getRelationshipPartName(partName));
	}

	public void deletePartRecursive(PackagePartName partName) {
		if ((partName == null) || (!(this.containPart(partName)))) {
			throw new IllegalArgumentException("partName");
		}
		PackagePart partToDelete = this.getPart(partName);
		this.removePart(partName);
		try {
			for (PackageRelationship relationship : partToDelete.getRelationships()) {
				PackagePartName targetPartName = PackagingURIHelper.createPartName(PackagingURIHelper.resolvePartUri(partName.getURI(), relationship.getTargetURI()));
				this.deletePartRecursive(targetPartName);
			}
		} catch (InvalidFormatException e) {
			OPCPackage.logger.log(POILogger.WARN, ((("An exception occurs while deleting part '" + (partName.getName())) + "'. Some parts may remain in the package. - ") + (e.getMessage())));
			return;
		}
		PackagePartName relationshipPartName = PackagingURIHelper.getRelationshipPartName(partName);
		if ((relationshipPartName != null) && (containPart(relationshipPartName))) {
			this.removePart(relationshipPartName);
		}
	}

	public boolean containPart(PackagePartName partName) {
		return (this.getPart(partName)) != null;
	}

	@Override
	public PackageRelationship addRelationship(PackagePartName targetPartName, TargetMode targetMode, String relationshipType, String relID) {
		if ((relationshipType.equals(PackageRelationshipTypes.CORE_PROPERTIES)) && ((this.packageProperties) != null)) {
			throw new InvalidOperationException("OPC Compliance error [M4.1]: can't add another core properties part ! Use the built-in package method instead.");
		}
		if (targetPartName.isRelationshipPartURI()) {
			throw new InvalidOperationException("Rule M1.25: The Relationships part shall not have relationships to any other part.");
		}
		ensureRelationships();
		PackageRelationship retRel = relationships.addRelationship(targetPartName.getURI(), targetMode, relationshipType, relID);
		this.isDirty = true;
		return retRel;
	}

	@Override
	public PackageRelationship addRelationship(PackagePartName targetPartName, TargetMode targetMode, String relationshipType) {
		return this.addRelationship(targetPartName, targetMode, relationshipType, null);
	}

	@Override
	public PackageRelationship addExternalRelationship(String target, String relationshipType) {
		return addExternalRelationship(target, relationshipType, null);
	}

	@Override
	public PackageRelationship addExternalRelationship(String target, String relationshipType, String id) {
		if (target == null) {
			throw new IllegalArgumentException("target");
		}
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		URI targetURI;
		try {
			targetURI = new URI(target);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(("Invalid target - " + e));
		}
		ensureRelationships();
		PackageRelationship retRel = relationships.addRelationship(targetURI, TargetMode.EXTERNAL, relationshipType, id);
		this.isDirty = true;
		return retRel;
	}

	@Override
	public void removeRelationship(String id) {
		if ((relationships) != null) {
			relationships.removeRelationship(id);
			this.isDirty = true;
		}
	}

	@Override
	public PackageRelationshipCollection getRelationships() {
		return getRelationshipsHelper(null);
	}

	@Override
	public PackageRelationshipCollection getRelationshipsByType(String relationshipType) {
		throwExceptionIfWriteOnly();
		if (relationshipType == null) {
			throw new IllegalArgumentException("relationshipType");
		}
		return getRelationshipsHelper(relationshipType);
	}

	private PackageRelationshipCollection getRelationshipsHelper(String id) {
		throwExceptionIfWriteOnly();
		ensureRelationships();
		return this.relationships.getRelationships(id);
	}

	@Override
	public void clearRelationships() {
		if ((relationships) != null) {
			relationships.clear();
			this.isDirty = true;
		}
	}

	public void ensureRelationships() {
		if ((this.relationships) == null) {
		}
	}

	@Override
	public PackageRelationship getRelationship(String id) {
		return this.relationships.getRelationshipByID(id);
	}

	@Override
	public boolean hasRelationships() {
		return (relationships.size()) > 0;
	}

	@Override
	public boolean isRelationshipExists(PackageRelationship rel) {
		for (PackageRelationship r : relationships) {
			if (r == rel) {
				return true;
			}
		}
		return false;
	}

	public void addMarshaller(String contentType, PartMarshaller marshaller) {
		try {
			partMarshallers.put(new ContentType(contentType), marshaller);
		} catch (InvalidFormatException e) {
			OPCPackage.logger.log(POILogger.WARN, (("The specified content type is not valid: '" + (e.getMessage())) + "'. The marshaller will not be added !"));
		}
	}

	public void addUnmarshaller(String contentType, PartUnmarshaller unmarshaller) {
		try {
			partUnmarshallers.put(new ContentType(contentType), unmarshaller);
		} catch (InvalidFormatException e) {
			OPCPackage.logger.log(POILogger.WARN, (("The specified content type is not valid: '" + (e.getMessage())) + "'. The unmarshaller will not be added !"));
		}
	}

	public void removeMarshaller(String contentType) {
		try {
			partMarshallers.remove(new ContentType(contentType));
		} catch (InvalidFormatException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeUnmarshaller(String contentType) {
		try {
			partUnmarshallers.remove(new ContentType(contentType));
		} catch (InvalidFormatException e) {
			throw new RuntimeException(e);
		}
	}

	public PackageAccess getPackageAccess() {
		return packageAccess;
	}

	@org.apache.poi.util.NotImplemented
	public boolean validatePackage(OPCPackage pkg) throws InvalidFormatException {
		throw new InvalidOperationException("Not implemented yet !!!");
	}

	public void save(File targetFile) throws IOException {
		if (targetFile == null) {
			throw new IllegalArgumentException("targetFile");
		}
		this.throwExceptionIfReadOnly();
		if ((targetFile.exists()) && (targetFile.getAbsolutePath().equals(this.originalPackagePath))) {
			throw new InvalidOperationException(("You can't call save(File) to save to the currently open " + "file. To save to the current file, please just call close()"));
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetFile);
			this.save(fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	public void save(OutputStream outputStream) throws IOException {
		throwExceptionIfReadOnly();
		this.saveImpl(outputStream);
	}

	protected abstract PackagePart createPartImpl(PackagePartName partName, String contentType, boolean loadRelationships);

	protected abstract void removePartImpl(PackagePartName partName);

	protected abstract void flushImpl();

	protected abstract void closeImpl() throws IOException;

	protected abstract void revertImpl();

	protected abstract void saveImpl(OutputStream outputStream) throws IOException;

	protected abstract PackagePartCollection getPartsImpl() throws InvalidFormatException;

	public boolean replaceContentType(String oldContentType, String newContentType) {
		boolean success = false;
		ArrayList<PackagePart> list = getPartsByContentType(oldContentType);
		for (PackagePart packagePart : list) {
			if (packagePart.getContentType().equals(oldContentType)) {
				PackagePartName partName = packagePart.getPartName();
				contentTypeManager.addContentType(partName, newContentType);
				try {
					packagePart.setContentType(newContentType);
				} catch (InvalidFormatException e) {
					throw new OpenXML4JRuntimeException(("invalid content type - " + newContentType), e);
				}
				success = true;
				this.isDirty = true;
			}
		}
		return success;
	}

	public void registerPartAndContentType(PackagePart part) {
		addPackagePart(part);
		this.contentTypeManager.addContentType(part.getPartName(), part.getContentType());
		this.isDirty = true;
	}

	public void unregisterPartAndContentType(PackagePartName partName) {
		removePart(partName);
		this.contentTypeManager.removeContentType(partName);
		this.isDirty = true;
	}

	public int getUnusedPartIndex(final String nameTemplate) throws InvalidFormatException {
		return partList.getUnusedPartIndex(nameTemplate);
	}
}

