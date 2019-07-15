import org.apache.cassandra.cql3.statements.*;


import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.exceptions.SyntaxException;

public class PropertyDefinitions
{
    private static final Pattern PATTERN_POSITIVE = Pattern.compile("(1|true|yes)");
    
    protected static final Logger logger = LoggerFactory.getLogger(PropertyDefinitions.class);

    protected final Map<String, Object> properties = new HashMap<String, Object>();

    public void addProperty(String name, String value) throws SyntaxException
    {
        if (properties.put(name, value) != null)
            throw new SyntaxException(String.format("Multiple definition for property '%s'", name));
    }

    public void addProperty(String name, Map<String, String> value) throws SyntaxException
    {
        if (properties.put(name, value) != null)
            throw new SyntaxException(String.format("Multiple definition for property '%s'", name));
    }

    public void validate(Set<String> keywords, Set<String> obsolete) throws SyntaxException
    {
        for (String name : properties.keySet())
        {
            if (keywords.contains(name))
                continue;

            if (obsolete.contains(name))
                logger.warn("Ignoring obsolete property {}", name);
            else
                throw new SyntaxException(String.format("Unknown property '%s'", name));
        }
    }

    protected String getSimple(String name) throws SyntaxException
    {
        Object val = properties.get(name);
        if (val == null)
            return null;
        if (!(val instanceof String))
            throw new SyntaxException(String.format("Invalid value for property '%s'. It should be a string", name));
        return (String)val;
    }

    protected Map<String, String> getMap(String name) throws SyntaxException
    {
        Object val = properties.get(name);
        if (val == null)
            return null;
        if (!(val instanceof Map))
            throw new SyntaxException(String.format("Invalid value for property '%s'. It should be a map.", name));
        return (Map<String, String>)val;
    }

    public Boolean hasProperty(String name)
    {
        return properties.containsKey(name);
    }

    public String getString(String key, String defaultValue) throws SyntaxException
    {
        String value = getSimple(key);
        return value != null ? value : defaultValue;
    }

    // Return a property value, typed as a Boolean
    public Boolean getBoolean(String key, Boolean defaultValue) throws SyntaxException
    {
        String value = getSimple(key);
        return (value == null) ? defaultValue : PATTERN_POSITIVE.matcher(value.toLowerCase()).matches();
    }

    // Return a property value, typed as a double
    public double getDouble(String key, double defaultValue) throws SyntaxException
    {
        String value = getSimple(key);
        if (value == null)
        {
            return defaultValue;
        }
        else
        {
            try
            {
                return Double.parseDouble(value);
            }
            catch (NumberFormatException e)
            {
                throw new SyntaxException(String.format("Invalid double value %s for '%s'", value, key));
            }
        }
    }

    // Return a property value, typed as an Integer
    public Integer getInt(String key, Integer defaultValue) throws SyntaxException
    {
        String value = getSimple(key);
        return toInt(key, value, defaultValue);
    }

    public static Integer toInt(String key, String value, Integer defaultValue) throws SyntaxException
    {
        if (value == null)
        {
            return defaultValue;
        }
        else
        {
            try
            {
                return Integer.valueOf(value);
            }
            catch (NumberFormatException e)
            {
                throw new SyntaxException(String.format("Invalid integer value %s for '%s'", value, key));
            }
        }
    }
}
