import org.apache.cassandra.io.util.ChecksumWriter;
import org.apache.cassandra.io.util.*;


import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ChecksummedSequentialWriter extends SequentialWriter
{
    private static final SequentialWriterOption CRC_WRITER_OPTION = SequentialWriterOption.newBuilder()
                                                                                          .bufferSize(8 * 1024)
                                                                                          .build();

    private final SequentialWriter crcWriter;
    private final ChecksumWriter crcMetadata;
    private final Optional<File> digestFile;

    public ChecksummedSequentialWriter(File file, File crcPath, File digestFile, SequentialWriterOption option)
    {
        super(file, option);
        crcWriter = new SequentialWriter(crcPath, CRC_WRITER_OPTION);
        crcMetadata = new ChecksumWriter(crcWriter);
        crcMetadata.writeChunkSize(buffer.capacity());
        this.digestFile = Optional.ofNullable(digestFile);
    }

    @Override
    protected void flushData()
    {
        super.flushData();
        ByteBuffer toAppend = buffer.duplicate();
        toAppend.position(0);
        toAppend.limit(buffer.position());
        crcMetadata.appendDirect(toAppend, false);
    }

    protected class TransactionalProxy extends SequentialWriter.TransactionalProxy
    {
        @Override
        protected Throwable doCommit(Throwable accumulate)
        {
            return super.doCommit(crcWriter.commit(accumulate));
        }

        @Override
        protected Throwable doAbort(Throwable accumulate)
        {
            return super.doAbort(crcWriter.abort(accumulate));
        }

        @Override
        protected void doPrepare()
        {
            syncInternal();
            digestFile.ifPresent(crcMetadata::writeFullChecksum);
            crcWriter.prepareToCommit();
        }
    }

    @Override
    protected SequentialWriter.TransactionalProxy txnProxy()
    {
        return new TransactionalProxy();
    }
}
