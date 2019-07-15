

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.ChunkGroupWithProperties;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.PropertiesChunk;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.StringChunk;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class RecipientChunks implements ChunkGroupWithProperties {
	private static final POILogger LOG = POILogFactory.getLogger(RecipientChunks.class);

	public static final String PREFIX = "__recip_version1.0_#";

	public static final MAPIProperty RECIPIENT_NAME = MAPIProperty.DISPLAY_NAME;

	public static final MAPIProperty DELIVERY_TYPE = MAPIProperty.ADDRTYPE;

	public static final MAPIProperty RECIPIENT_EMAIL_ADDRESS = MAPIProperty.EMAIL_ADDRESS;

	public static final MAPIProperty RECIPIENT_SEARCH = MAPIProperty.SEARCH_KEY;

	public static final MAPIProperty RECIPIENT_SMTP_ADDRESS = MAPIProperty.SMTP_ADDRESS;

	public static final MAPIProperty RECIPIENT_DISPLAY_NAME = MAPIProperty.RECIPIENT_DISPLAY_NAME;

	public int recipientNumber;

	public ByteChunk recipientSearchChunk;

	public StringChunk recipientNameChunk;

	public StringChunk recipientEmailChunk;

	public StringChunk recipientSMTPChunk;

	public StringChunk deliveryTypeChunk;

	public StringChunk recipientDisplayNameChunk;

	private PropertiesChunk recipientProperties;

	public RecipientChunks(String name) {
		recipientNumber = -1;
		int splitAt = name.lastIndexOf('#');
		if (splitAt > (-1)) {
			String number = name.substring((splitAt + 1));
			try {
				recipientNumber = Integer.parseInt(number, 16);
			} catch (NumberFormatException e) {
				RecipientChunks.LOG.log(POILogger.ERROR, ("Invalid recipient number in name " + name));
			}
		}
	}

	public String getRecipientName() {
		if ((recipientNameChunk) != null) {
			return recipientNameChunk.getValue();
		}
		if ((recipientDisplayNameChunk) != null) {
			return recipientDisplayNameChunk.getValue();
		}
		return null;
	}

	public String getRecipientEmailAddress() {
		if ((recipientSMTPChunk) != null) {
			return recipientSMTPChunk.getValue();
		}
		if ((recipientEmailChunk) != null) {
			String email = recipientEmailChunk.getValue();
			int cne = email.indexOf("/CN=");
			if (cne < 0) {
				return email;
			}else {
				return email.substring((cne + 4));
			}
		}
		if ((recipientNameChunk) != null) {
			String name = recipientNameChunk.getValue();
			if (name.contains("@")) {
				if ((name.startsWith("'")) && (name.endsWith("'"))) {
					return name.substring(1, ((name.length()) - 1));
				}
				return name;
			}
		}
		if ((recipientSearchChunk) != null) {
			String search = recipientSearchChunk.getAs7bitString();
			int idx = search.indexOf("SMTP:");
			if (idx >= 0) {
				return search.substring((idx + 5));
			}
		}
		return null;
	}

	private List<Chunk> allChunks = new ArrayList<>();

	@Override
	public Map<MAPIProperty, List<PropertyValue>> getProperties() {
		if ((recipientProperties) != null) {
			return recipientProperties.getProperties();
		}else {
			return Collections.emptyMap();
		}
	}

	public Chunk[] getAll() {
		return allChunks.toArray(new Chunk[allChunks.size()]);
	}

	@Override
	public Chunk[] getChunks() {
		return getAll();
	}

	@Override
	public void record(Chunk chunk) {
		if ((chunk.getChunkId()) == (RecipientChunks.RECIPIENT_SEARCH.id)) {
			recipientSearchChunk = ((ByteChunk) (chunk));
		}else
			if ((chunk.getChunkId()) == (RecipientChunks.RECIPIENT_NAME.id)) {
				recipientDisplayNameChunk = ((StringChunk) (chunk));
			}else
				if ((chunk.getChunkId()) == (RecipientChunks.RECIPIENT_DISPLAY_NAME.id)) {
					recipientNameChunk = ((StringChunk) (chunk));
				}else
					if ((chunk.getChunkId()) == (RecipientChunks.RECIPIENT_EMAIL_ADDRESS.id)) {
						recipientEmailChunk = ((StringChunk) (chunk));
					}else
						if ((chunk.getChunkId()) == (RecipientChunks.RECIPIENT_SMTP_ADDRESS.id)) {
							recipientSMTPChunk = ((StringChunk) (chunk));
						}else
							if ((chunk.getChunkId()) == (RecipientChunks.DELIVERY_TYPE.id)) {
								deliveryTypeChunk = ((StringChunk) (chunk));
							}else
								if (chunk instanceof PropertiesChunk) {
									recipientProperties = ((PropertiesChunk) (chunk));
								}






		allChunks.add(chunk);
	}

	@Override
	public void chunksComplete() {
		if ((recipientProperties) != null) {
		}else {
			RecipientChunks.LOG.log(POILogger.WARN, "Recipeints Chunk didn't contain a list of properties!");
		}
	}

	public static class RecipientChunksSorter implements Serializable , Comparator<RecipientChunks> {
		@Override
		public int compare(RecipientChunks a, RecipientChunks b) {
			return Integer.compare(a.recipientNumber, b.recipientNumber);
		}
	}
}

