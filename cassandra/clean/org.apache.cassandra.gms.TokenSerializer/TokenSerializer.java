import org.apache.cassandra.gms.*;


import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;


public class TokenSerializer
{
    private static final Logger logger = LoggerFactory.getLogger(TokenSerializer.class);

    public static void serialize(IPartitioner partitioner, Collection<Token> tokens, DataOutput out) throws IOException
    {
        for (Token token : tokens)
        {
            ByteBuffer tokenBuffer = partitioner.getTokenFactory().toByteArray(token);
            assert tokenBuffer.arrayOffset() == 0;
            ByteBufferUtil.writeWithLength(tokenBuffer.array(), out);
        }
        out.writeInt(0);
    }

    public static Collection<Token> deserialize(IPartitioner partitioner, DataInput in) throws IOException
    {
        Collection<Token> tokens = new ArrayList<Token>();
        while (true)
        {
            int size = in.readInt();
            if (size < 1)
                break;
            logger.trace("Reading token of {}", FBUtilities.prettyPrintMemory(size));
            byte[] bintoken = new byte[size];
            in.readFully(bintoken);
            tokens.add(partitioner.getTokenFactory().fromByteArray(ByteBuffer.wrap(bintoken)));
        }
        return tokens;
    }
}
