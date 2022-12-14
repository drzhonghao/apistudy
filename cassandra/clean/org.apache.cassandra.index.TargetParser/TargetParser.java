import org.apache.cassandra.index.*;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.utils.Pair;

public class TargetParser
{
    private static final Pattern TARGET_REGEX = Pattern.compile("^(keys|entries|values|full)\\((.+)\\)$");
    private static final Pattern TWO_QUOTES = Pattern.compile("\"\"");
    private static final String QUOTE = "\"";

    public static Pair<ColumnDefinition, IndexTarget.Type> parse(CFMetaData cfm, IndexMetadata indexDef)
    {
        String target = indexDef.options.get("target");
        assert target != null : String.format("No target definition found for index %s", indexDef.name);
        Pair<ColumnDefinition, IndexTarget.Type> result = parse(cfm, target);
        if (result == null)
            throw new ConfigurationException(String.format("Unable to parse targets for index %s (%s)", indexDef.name, target));
        return result;
    }

    public static Pair<ColumnDefinition, IndexTarget.Type> parse(CFMetaData cfm, String target)
    {
        // if the regex matches then the target is in the form "keys(foo)", "entries(bar)" etc
        // if not, then it must be a simple column name and implictly its type is VALUES
        Matcher matcher = TARGET_REGEX.matcher(target);
        String columnName;
        IndexTarget.Type targetType;
        if (matcher.matches())
        {
            targetType = IndexTarget.Type.fromString(matcher.group(1));
            columnName = matcher.group(2);
        }
        else
        {
            columnName = target;
            targetType = IndexTarget.Type.VALUES;
        }

        // in the case of a quoted column name the name in the target string
        // will be enclosed in quotes, which we need to unwrap. It may also
        // include quote characters internally, escaped like so:
        //      abc"def -> abc""def.
        // Because the target string is stored in a CQL compatible form, we
        // need to un-escape any such quotes to get the actual column name
        if (columnName.startsWith(QUOTE))
        {
            columnName = StringUtils.substring(StringUtils.substring(columnName, 1), 0, -1);
            columnName = TWO_QUOTES.matcher(columnName).replaceAll(QUOTE);
        }

        // if it's not a CQL table, we can't assume that the column name is utf8, so
        // in that case we have to do a linear scan of the cfm's columns to get the matching one.
        // After dropping compact storage (see CASSANDRA-10857), we can't distinguish between the
        // former compact/thrift table, so we have to fall back to linear scan in both cases.
        ColumnDefinition cd = cfm.getColumnDefinition(new ColumnIdentifier(columnName, true));
        if (cd != null)
            return Pair.create(cd, targetType);

        for (ColumnDefinition column : cfm.allColumns())
            if (column.name.toString().equals(columnName))
                return Pair.create(column, targetType);

        return null;
    }
}
