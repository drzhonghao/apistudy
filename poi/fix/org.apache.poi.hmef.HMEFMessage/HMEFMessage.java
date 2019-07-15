

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.attribute.MAPIAttribute;
import org.apache.poi.hmef.attribute.MAPIStringAttribute;
import org.apache.poi.hmef.attribute.TNEFAttribute;
import org.apache.poi.hmef.attribute.TNEFMAPIAttribute;
import org.apache.poi.hmef.attribute.TNEFProperty;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.util.LittleEndian;


public final class HMEFMessage {
	public static final int HEADER_SIGNATURE = 574529400;

	@SuppressWarnings("unused")
	private int fileId;

	private final List<TNEFAttribute> messageAttributes = new ArrayList<>();

	private final List<MAPIAttribute> mapiAttributes = new ArrayList<>();

	private final List<Attachment> attachments = new ArrayList<>();

	public HMEFMessage(InputStream inp) throws IOException {
		try {
			int sig = LittleEndian.readInt(inp);
			if (sig != (HMEFMessage.HEADER_SIGNATURE)) {
				throw new IllegalArgumentException((((("TNEF signature not detected in file, " + "expected ") + (HMEFMessage.HEADER_SIGNATURE)) + " but got ") + sig));
			}
			fileId = LittleEndian.readUShort(inp);
			process(inp);
		} finally {
			inp.close();
		}
	}

	private void process(InputStream inp) throws IOException {
		int level;
		do {
			level = inp.read();
			switch (level) {
				case TNEFProperty.LEVEL_MESSAGE :
					processMessage(inp);
					break;
				case TNEFProperty.LEVEL_ATTACHMENT :
					processAttachment(inp);
					break;
				case '\r' :
				case '\n' :
				case TNEFProperty.LEVEL_END_OF_FILE :
					break;
				default :
					throw new IllegalStateException(("Unhandled level " + level));
			}
		} while (level != (TNEFProperty.LEVEL_END_OF_FILE) );
	}

	void processMessage(InputStream inp) throws IOException {
		TNEFAttribute attr = TNEFAttribute.create(inp);
		messageAttributes.add(attr);
		if (attr instanceof TNEFMAPIAttribute) {
			TNEFMAPIAttribute tnefMAPI = ((TNEFMAPIAttribute) (attr));
			mapiAttributes.addAll(tnefMAPI.getMAPIAttributes());
		}
	}

	void processAttachment(InputStream inp) throws IOException {
		TNEFAttribute attr = TNEFAttribute.create(inp);
		if ((attachments.isEmpty()) || ((attr.getProperty()) == (TNEFProperty.ID_ATTACHRENDERDATA))) {
			attachments.add(new Attachment());
		}
		Attachment attach = attachments.get(((attachments.size()) - 1));
	}

	public List<TNEFAttribute> getMessageAttributes() {
		return Collections.unmodifiableList(messageAttributes);
	}

	public List<MAPIAttribute> getMessageMAPIAttributes() {
		return Collections.unmodifiableList(mapiAttributes);
	}

	public List<Attachment> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	public TNEFAttribute getMessageAttribute(TNEFProperty id) {
		for (TNEFAttribute attr : messageAttributes) {
			if ((attr.getProperty()) == id) {
				return attr;
			}
		}
		return null;
	}

	public MAPIAttribute getMessageMAPIAttribute(MAPIProperty id) {
		for (MAPIAttribute attr : mapiAttributes) {
			if ((attr.getProperty().id) == (id.id)) {
				return attr;
			}
		}
		return null;
	}

	private String getString(MAPIProperty id) {
		return MAPIStringAttribute.getAsString(getMessageMAPIAttribute(id));
	}

	public String getSubject() {
		return getString(MAPIProperty.CONVERSATION_TOPIC);
	}

	public String getBody() {
		return getString(MAPIProperty.RTF_COMPRESSED);
	}
}

