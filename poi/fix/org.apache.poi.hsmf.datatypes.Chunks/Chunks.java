

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.ChunkGroupWithProperties;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.MessagePropertiesChunk;
import org.apache.poi.hsmf.datatypes.MessageSubmissionChunk;
import org.apache.poi.hsmf.datatypes.PropertiesChunk;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.StringChunk;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class Chunks implements ChunkGroupWithProperties {
	private static final POILogger LOG = POILogFactory.getLogger(Chunks.class);

	private Map<MAPIProperty, List<Chunk>> allChunks = new HashMap<>();

	private StringChunk messageClass;

	private StringChunk textBodyChunk;

	private StringChunk htmlBodyChunkString;

	private ByteChunk htmlBodyChunkBinary;

	private ByteChunk rtfBodyChunk;

	private StringChunk subjectChunk;

	private StringChunk displayToChunk;

	private StringChunk displayFromChunk;

	private StringChunk displayCCChunk;

	private StringChunk displayBCCChunk;

	private StringChunk conversationTopic;

	private StringChunk sentByServerType;

	private StringChunk messageHeaders;

	private MessageSubmissionChunk submissionChunk;

	private StringChunk emailFromChunk;

	private StringChunk messageId;

	private MessagePropertiesChunk messageProperties;

	@Override
	public Map<MAPIProperty, List<PropertyValue>> getProperties() {
		if ((messageProperties) != null) {
			return messageProperties.getProperties();
		}else {
			return Collections.emptyMap();
		}
	}

	public Map<MAPIProperty, PropertyValue> getRawProperties() {
		if ((messageProperties) != null) {
			return messageProperties.getRawProperties();
		}else {
			return Collections.emptyMap();
		}
	}

	public Map<MAPIProperty, List<Chunk>> getAll() {
		return allChunks;
	}

	@Override
	public Chunk[] getChunks() {
		ArrayList<Chunk> chunks = new ArrayList<>(allChunks.size());
		for (List<Chunk> c : allChunks.values()) {
			chunks.addAll(c);
		}
		return chunks.toArray(new Chunk[chunks.size()]);
	}

	public StringChunk getMessageClass() {
		return messageClass;
	}

	public StringChunk getTextBodyChunk() {
		return textBodyChunk;
	}

	public StringChunk getHtmlBodyChunkString() {
		return htmlBodyChunkString;
	}

	public ByteChunk getHtmlBodyChunkBinary() {
		return htmlBodyChunkBinary;
	}

	public ByteChunk getRtfBodyChunk() {
		return rtfBodyChunk;
	}

	public StringChunk getSubjectChunk() {
		return subjectChunk;
	}

	public StringChunk getDisplayToChunk() {
		return displayToChunk;
	}

	public StringChunk getDisplayFromChunk() {
		return displayFromChunk;
	}

	public StringChunk getDisplayCCChunk() {
		return displayCCChunk;
	}

	public StringChunk getDisplayBCCChunk() {
		return displayBCCChunk;
	}

	public StringChunk getConversationTopic() {
		return conversationTopic;
	}

	public StringChunk getSentByServerType() {
		return sentByServerType;
	}

	public StringChunk getMessageHeaders() {
		return messageHeaders;
	}

	public MessageSubmissionChunk getSubmissionChunk() {
		return submissionChunk;
	}

	public StringChunk getEmailFromChunk() {
		return emailFromChunk;
	}

	public StringChunk getMessageId() {
		return messageId;
	}

	public MessagePropertiesChunk getMessageProperties() {
		return messageProperties;
	}

	@Override
	public void record(Chunk chunk) {
		MAPIProperty prop = MAPIProperty.get(chunk.getChunkId());
		if (prop == (MAPIProperty.MESSAGE_CLASS)) {
			messageClass = ((StringChunk) (chunk));
		}else
			if (prop == (MAPIProperty.INTERNET_MESSAGE_ID)) {
				messageId = ((StringChunk) (chunk));
			}else
				if (prop == (MAPIProperty.MESSAGE_SUBMISSION_ID)) {
					submissionChunk = ((MessageSubmissionChunk) (chunk));
				}else
					if (prop == (MAPIProperty.RECEIVED_BY_ADDRTYPE)) {
						sentByServerType = ((StringChunk) (chunk));
					}else
						if (prop == (MAPIProperty.TRANSPORT_MESSAGE_HEADERS)) {
							messageHeaders = ((StringChunk) (chunk));
						}else
							if (prop == (MAPIProperty.CONVERSATION_TOPIC)) {
								conversationTopic = ((StringChunk) (chunk));
							}else
								if (prop == (MAPIProperty.SUBJECT)) {
									subjectChunk = ((StringChunk) (chunk));
								}else
									if (prop == (MAPIProperty.DISPLAY_TO)) {
										displayToChunk = ((StringChunk) (chunk));
									}else
										if (prop == (MAPIProperty.DISPLAY_CC)) {
											displayCCChunk = ((StringChunk) (chunk));
										}else
											if (prop == (MAPIProperty.DISPLAY_BCC)) {
												displayBCCChunk = ((StringChunk) (chunk));
											}else
												if (prop == (MAPIProperty.SENDER_EMAIL_ADDRESS)) {
													emailFromChunk = ((StringChunk) (chunk));
												}else
													if (prop == (MAPIProperty.SENDER_NAME)) {
														displayFromChunk = ((StringChunk) (chunk));
													}else
														if (prop == (MAPIProperty.BODY)) {
															textBodyChunk = ((StringChunk) (chunk));
														}else
															if (prop == (MAPIProperty.BODY_HTML)) {
																if (chunk instanceof StringChunk) {
																	htmlBodyChunkString = ((StringChunk) (chunk));
																}
																if (chunk instanceof ByteChunk) {
																	htmlBodyChunkBinary = ((ByteChunk) (chunk));
																}
															}else
																if (prop == (MAPIProperty.RTF_COMPRESSED)) {
																	rtfBodyChunk = ((ByteChunk) (chunk));
																}else
																	if (chunk instanceof MessagePropertiesChunk) {
																		messageProperties = ((MessagePropertiesChunk) (chunk));
																	}















		if ((allChunks.get(prop)) == null) {
			allChunks.put(prop, new ArrayList<>());
		}
		allChunks.get(prop).add(chunk);
	}

	@Override
	public void chunksComplete() {
		if ((messageProperties) != null) {
		}else {
			Chunks.LOG.log(POILogger.WARN, "Message didn't contain a root list of properties!");
		}
	}
}

