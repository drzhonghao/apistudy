import org.apache.poi.hsmf.datatypes.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.util.LittleEndian;

/**
 * A {@link PropertiesChunk} for a Message or Embedded-Message. This has a 32
 * byte header
 */
public class MessagePropertiesChunk extends PropertiesChunk {
    private boolean isEmbedded;
    private long nextRecipientId;
    private long nextAttachmentId;
    private long recipientCount;
    private long attachmentCount;

    public MessagePropertiesChunk(ChunkGroup parentGroup) {
        super(parentGroup);
    }

    public MessagePropertiesChunk(ChunkGroup parentGroup, boolean isEmbedded) {
        super(parentGroup);
        this.isEmbedded = isEmbedded;
    }

    public long getNextRecipientId() {
        return nextRecipientId;
    }

    public long getNextAttachmentId() {
        return nextAttachmentId;
    }

    public long getRecipientCount() {
        return recipientCount;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }
    
    public void setNextRecipientId(long nextRecipientId) {
      this.nextRecipientId = nextRecipientId;
    }
    
    public void setNextAttachmentId(long nextAttachmentId) {
      this.nextAttachmentId = nextAttachmentId;
    }

    public void setRecipientCount(long recipientCount) {
      this.recipientCount = recipientCount;
    }

    public void setAttachmentCount(long attachmentCount) {
      this.attachmentCount = attachmentCount;
    }

    @Override
    protected void readProperties(InputStream stream) throws IOException {
        // 8 bytes of reserved zeros
        LittleEndian.readLong(stream);

        // Nexts and counts
        nextRecipientId = LittleEndian.readUInt(stream);
        nextAttachmentId = LittleEndian.readUInt(stream);
        recipientCount = LittleEndian.readUInt(stream);
        attachmentCount = LittleEndian.readUInt(stream);

        if (!isEmbedded) {
          // 8 bytes of reserved zeros (top level properties stream only)
          LittleEndian.readLong(stream);
        }

        // Now properties
        super.readProperties(stream);
    }

    @Override
    public void readValue(InputStream value) throws IOException {
        readProperties(value);
    }

    @Override
    protected List<PropertyValue> writeProperties(OutputStream stream) throws IOException
    {
        // 8 bytes of reserved zeros
        LittleEndian.putLong(0, stream);

        // Nexts and counts
        LittleEndian.putUInt(nextRecipientId, stream);
        LittleEndian.putUInt(nextAttachmentId, stream);
        LittleEndian.putUInt(recipientCount, stream);
        LittleEndian.putUInt(attachmentCount, stream);

        if (!isEmbedded) {
            // 8 bytes of reserved zeros (top level properties stream only)
            LittleEndian.putLong(0, stream);
        }

        // Now properties.
        return super.writeProperties(stream);
    }

    @Override
    public void writeValue(OutputStream stream) throws IOException {
        // write properties without variable length properties
        writeProperties(stream);
    }
}
