import org.apache.cassandra.gms.*;


import java.io.*;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

/**
 * HeartBeat State associated with any given endpoint.
 */
class HeartBeatState
{
    public static final IVersionedSerializer<HeartBeatState> serializer = new HeartBeatStateSerializer();

    private volatile int generation;
    private volatile int version;

    HeartBeatState(int gen)
    {
        this(gen, 0);
    }

    HeartBeatState(int gen, int ver)
    {
        generation = gen;
        version = ver;
    }

    int getGeneration()
    {
        return generation;
    }

    void updateHeartBeat()
    {
        version = VersionGenerator.getNextVersion();
    }

    int getHeartBeatVersion()
    {
        return version;
    }

    void forceNewerGenerationUnsafe()
    {
        generation += 1;
    }

    void forceHighestPossibleVersionUnsafe()
    {
        version = Integer.MAX_VALUE;
    }

    public String toString()
    {
        return String.format("HeartBeat: generation = %d, version = %d", generation, version);
    }
}

class HeartBeatStateSerializer implements IVersionedSerializer<HeartBeatState>
{
    public void serialize(HeartBeatState hbState, DataOutputPlus out, int version) throws IOException
    {
        out.writeInt(hbState.getGeneration());
        out.writeInt(hbState.getHeartBeatVersion());
    }

    public HeartBeatState deserialize(DataInputPlus in, int version) throws IOException
    {
        return new HeartBeatState(in.readInt(), in.readInt());
    }

    public long serializedSize(HeartBeatState state, int version)
    {
        return TypeSizes.sizeof(state.getGeneration()) + TypeSizes.sizeof(state.getHeartBeatVersion());
    }
}
