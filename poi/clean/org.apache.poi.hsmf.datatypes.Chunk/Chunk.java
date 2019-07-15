import org.apache.poi.hsmf.datatypes.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.poi.hsmf.datatypes.Types.MAPIType;

public abstract class Chunk {
    public static final String DEFAULT_NAME_PREFIX = "__substg1.0_";

    private final int chunkId;
    private final MAPIType type;
    private final String namePrefix;

    protected Chunk(String namePrefix, int chunkId, MAPIType type) {
        this.namePrefix = namePrefix;
        this.chunkId = chunkId;
        this.type = type;
    }

    protected Chunk(int chunkId, MAPIType type) {
        this(DEFAULT_NAME_PREFIX, chunkId, type);
    }

    /**
     * Gets the id of this chunk
     */
    public int getChunkId() {
        return this.chunkId;
    }

    /**
     * Gets the numeric type of this chunk.
     */
    public MAPIType getType() {
        return this.type;
    }

    /**
     * Creates a string to use to identify this chunk in the POI file system
     * object.
     */
    public String getEntryName() {
        String type = this.type.asFileEnding();

        String chunkId = Integer.toHexString(this.chunkId);
        while (chunkId.length() < 4) {
            chunkId = "0" + chunkId;
        }

        return this.namePrefix
            + chunkId.toUpperCase(Locale.ROOT)
            + type.toUpperCase(Locale.ROOT);
    }

    /**
     * Writes the value of this chunk back out again.
     */
    public abstract void writeValue(OutputStream out) throws IOException;

    /**
     * Reads the value of this chunk using an InputStream
     */
    public abstract void readValue(InputStream value) throws IOException;
}
