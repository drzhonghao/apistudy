import org.apache.cassandra.index.sasi.analyzer.AbstractAnalyzer;
import org.apache.cassandra.index.sasi.analyzer.*;


import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.cassandra.db.marshal.AbstractType;

/**
 * Default noOp tokenizer. The iterator will iterate only once
 * returning the unmodified input
 */
public class NoOpAnalyzer extends AbstractAnalyzer
{
    private ByteBuffer input;
    private boolean hasNext = false;

    public void init(Map<String, String> options, AbstractType validator)
    {}

    public boolean hasNext()
    {
        if (hasNext)
        {
            this.next = input;
            this.hasNext = false;
            return true;
        }
        return false;
    }

    public void reset(ByteBuffer input)
    {
        this.next = null;
        this.input = input;
        this.hasNext = true;
    }
}
