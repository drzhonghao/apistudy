

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAuthors;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTComment;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCommentList;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTComments;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CommentsDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CommentsDocument.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CommentsDocument.Factory.parse;


@Internal
public class CommentsTable extends POIXMLDocumentPart implements Comments {
	public static final String DEFAULT_AUTHOR = "";

	public static final int DEFAULT_AUTHOR_ID = 0;

	private CTComments comments;

	private Map<CellAddress, CTComment> commentRefs;

	public CommentsTable() {
		super();
		comments = CTComments.Factory.newInstance();
		comments.addNewCommentList();
		comments.addNewAuthors().addAuthor(CommentsTable.DEFAULT_AUTHOR);
	}

	public CommentsTable(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	public void readFrom(InputStream is) throws IOException {
		try {
			CommentsDocument doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			comments = doc.getComments();
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	public void writeTo(OutputStream out) throws IOException {
		CommentsDocument doc = newInstance();
		doc.setComments(comments);
		doc.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		writeTo(out);
		out.close();
	}

	public void referenceUpdated(CellAddress oldReference, CTComment comment) {
		if ((commentRefs) != null) {
			commentRefs.remove(oldReference);
			commentRefs.put(new CellAddress(comment.getRef()), comment);
		}
	}

	@Override
	public int getNumberOfComments() {
		return comments.getCommentList().sizeOfCommentArray();
	}

	@Override
	public int getNumberOfAuthors() {
		return comments.getAuthors().sizeOfAuthorArray();
	}

	@Override
	public String getAuthor(long authorId) {
		return comments.getAuthors().getAuthorArray(((int) (authorId)));
	}

	@Override
	public int findAuthor(String author) {
		String[] authorArray = comments.getAuthors().getAuthorArray();
		for (int i = 0; i < (authorArray.length); i++) {
			if (authorArray[i].equals(author)) {
				return i;
			}
		}
		return addNewAuthor(author);
	}

	@Override
	public XSSFComment findCellComment(CellAddress cellAddress) {
		CTComment ct = getCTComment(cellAddress);
		return null;
	}

	@Internal
	public CTComment getCTComment(CellAddress cellRef) {
		prepareCTCommentCache();
		return commentRefs.get(cellRef);
	}

	@Override
	public Iterator<CellAddress> getCellAddresses() {
		prepareCTCommentCache();
		return commentRefs.keySet().iterator();
	}

	@org.apache.poi.util.Removal(version = "4.2")
	@Deprecated
	public Map<CellAddress, XSSFComment> getCellComments() {
		prepareCTCommentCache();
		final TreeMap<CellAddress, XSSFComment> map = new TreeMap<>();
		for (final Map.Entry<CellAddress, CTComment> e : commentRefs.entrySet()) {
		}
		return map;
	}

	private void prepareCTCommentCache() {
		if ((commentRefs) == null) {
			commentRefs = new HashMap<>();
			for (CTComment comment : comments.getCommentList().getCommentArray()) {
				commentRefs.put(new CellAddress(comment.getRef()), comment);
			}
		}
	}

	@Internal
	public CTComment newComment(CellAddress ref) {
		CTComment ct = comments.getCommentList().addNewComment();
		ct.setRef(ref.formatAsString());
		ct.setAuthorId(CommentsTable.DEFAULT_AUTHOR_ID);
		if ((commentRefs) != null) {
			commentRefs.put(ref, ct);
		}
		return ct;
	}

	@Override
	public boolean removeComment(CellAddress cellRef) {
		final String stringRef = cellRef.formatAsString();
		CTCommentList lst = comments.getCommentList();
		if (lst != null) {
			CTComment[] commentArray = lst.getCommentArray();
			for (int i = 0; i < (commentArray.length); i++) {
				CTComment comment = commentArray[i];
				if (stringRef.equals(comment.getRef())) {
					lst.removeComment(i);
					if ((commentRefs) != null) {
						commentRefs.remove(cellRef);
					}
					return true;
				}
			}
		}
		return false;
	}

	private int addNewAuthor(String author) {
		int index = comments.getAuthors().sizeOfAuthorArray();
		comments.getAuthors().insertAuthor(index, author);
		return index;
	}

	@Internal
	public CTComments getCTComments() {
		return comments;
	}
}

