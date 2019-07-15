import org.apache.cassandra.index.sasi.sa.Term;
import org.apache.cassandra.index.sasi.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.TermSize;
import org.apache.cassandra.index.sasi.utils.MappedBuffer;
import org.apache.cassandra.db.marshal.AbstractType;

import static org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.IS_PARTIAL_BIT;

public class Term
{
    protected final MappedBuffer content;
    protected final TermSize termSize;

    private final boolean hasMarkedPartials;

    public Term(MappedBuffer content, TermSize size, boolean hasMarkedPartials)
    {
        this.content = content;
        this.termSize = size;
        this.hasMarkedPartials = hasMarkedPartials;
    }

    public ByteBuffer getTerm()
    {
        long offset = termSize.isConstant() ? content.position() : content.position() + 2;
        int  length = termSize.isConstant() ? termSize.size : readLength(content.position());

        return content.getPageRegion(offset, length);
    }

    public boolean isPartial()
    {
        return !termSize.isConstant()
               && hasMarkedPartials
               && (content.getShort(content.position()) & (1 << IS_PARTIAL_BIT)) != 0;
    }

    public long getDataOffset()
    {
        long position = content.position();
        return position + (termSize.isConstant() ? termSize.size : 2 + readLength(position));
    }

    public int compareTo(AbstractType<?> comparator, ByteBuffer query)
    {
        return compareTo(comparator, query, true);
    }

    public int compareTo(AbstractType<?> comparator, ByteBuffer query, boolean checkFully)
    {
        long position = content.position();
        int padding = termSize.isConstant() ? 0 : 2;
        int len = termSize.isConstant() ? termSize.size : readLength(position);

        return content.comparePageTo(position + padding, checkFully ? len : Math.min(len, query.remaining()), comparator, query);
    }

    private short readLength(long position)
    {
        return (short) (content.getShort(position) & ~(1 << IS_PARTIAL_BIT));
    }
}
