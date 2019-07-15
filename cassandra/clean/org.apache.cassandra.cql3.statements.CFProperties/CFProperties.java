import org.apache.cassandra.cql3.statements.TableAttributes;
import org.apache.cassandra.cql3.statements.*;


import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.ReversedType;

public class CFProperties
{
    public final TableAttributes properties = new TableAttributes();
    final Map<ColumnIdentifier, Boolean> definedOrdering = new LinkedHashMap<>(); // Insertion ordering is important
    boolean useCompactStorage = false;

    public void validate()
    {
        properties.validate();
    }

    public void setOrdering(ColumnIdentifier alias, boolean reversed)
    {
        definedOrdering.put(alias, reversed);
    }

    public void setCompactStorage()
    {
        useCompactStorage = true;
    }

    public AbstractType getReversableType(ColumnIdentifier targetIdentifier, AbstractType<?> type)
    {
        if (!definedOrdering.containsKey(targetIdentifier))
        {
            return type;
        }
        return definedOrdering.get(targetIdentifier) ? ReversedType.getInstance(type) : type;
    }
}
