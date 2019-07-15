import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.*;


/**
 * A simple abstract implementation of SharedCloseable
 */
public abstract class SharedCloseableImpl implements SharedCloseable
{
    final Ref<?> ref;

    public SharedCloseableImpl(RefCounted.Tidy tidy)
    {
        ref = new Ref<Object>(null, tidy);
    }

    protected SharedCloseableImpl(SharedCloseableImpl copy)
    {
        this.ref = copy.ref.ref();
    }

    public boolean isCleanedUp()
    {
        return ref.globalCount() == 0;
    }

    public void close()
    {
        ref.ensureReleased();
    }

    public Throwable close(Throwable accumulate)
    {
        return ref.ensureReleased(accumulate);
    }

    public void addTo(Ref.IdentityCollection identities)
    {
        identities.add(ref);
    }
}
