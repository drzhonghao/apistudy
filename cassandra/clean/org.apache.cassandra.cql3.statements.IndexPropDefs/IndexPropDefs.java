import org.apache.cassandra.cql3.statements.PropertyDefinitions;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.cql3.statements.*;


import java.util.*;

import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.SyntaxException;

public class IndexPropDefs extends PropertyDefinitions
{
    public static final String KW_OPTIONS = "options";

    public static final Set<String> keywords = new HashSet<>();
    public static final Set<String> obsoleteKeywords = new HashSet<>();

    public boolean isCustom;
    public String customClass;

    static
    {
        keywords.add(KW_OPTIONS);
    }

    public void validate() throws RequestValidationException
    {
        validate(keywords, obsoleteKeywords);

        if (isCustom && customClass == null)
            throw new InvalidRequestException("CUSTOM index requires specifiying the index class");

        if (!isCustom && customClass != null)
            throw new InvalidRequestException("Cannot specify index class for a non-CUSTOM index");

        if (!isCustom && !properties.isEmpty())
            throw new InvalidRequestException("Cannot specify options for a non-CUSTOM index");

        if (getRawOptions().containsKey(IndexTarget.CUSTOM_INDEX_OPTION_NAME))
            throw new InvalidRequestException(String.format("Cannot specify %s as a CUSTOM option",
                                                            IndexTarget.CUSTOM_INDEX_OPTION_NAME));

        if (getRawOptions().containsKey(IndexTarget.TARGET_OPTION_NAME))
            throw new InvalidRequestException(String.format("Cannot specify %s as a CUSTOM option",
                                                            IndexTarget.TARGET_OPTION_NAME));

    }

    public Map<String, String> getRawOptions() throws SyntaxException
    {
        Map<String, String> options = getMap(KW_OPTIONS);
        return options == null ? Collections.<String, String>emptyMap() : options;
    }

    public Map<String, String> getOptions() throws SyntaxException
    {
        Map<String, String> options = new HashMap<>(getRawOptions());
        options.put(IndexTarget.CUSTOM_INDEX_OPTION_NAME, customClass);
        return options;
    }
}
