import org.apache.cassandra.cql3.selection.*;


import java.util.List;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.cql3.ColumnIdentifier;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class RawSelector
{
    public final Selectable.Raw selectable;
    public final ColumnIdentifier alias;

    public RawSelector(Selectable.Raw selectable, ColumnIdentifier alias)
    {
        this.selectable = selectable;
        this.alias = alias;
    }

    /**
     * Converts the specified list of <code>RawSelector</code>s into a list of <code>Selectable</code>s.
     *
     * @param raws the <code>RawSelector</code>s to converts.
     * @return a list of <code>Selectable</code>s
     */
    public static List<Selectable> toSelectables(List<RawSelector> raws, final CFMetaData cfm)
    {
        return Lists.transform(raws, new Function<RawSelector, Selectable>()
        {
            public Selectable apply(RawSelector raw)
            {
                return raw.selectable.prepare(cfm);
            }
        });
    }

    public boolean processesSelection()
    {
        return selectable.processesSelection();
    }
}
