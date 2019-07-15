import org.apache.cassandra.schema.*;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public final class TriggerMetadata
{
    public static final String CLASS = "class";

    public final String name;

    // For now, the only supported option is 'class'.
    // Proper trigger parametrization will be added later.
    public final String classOption;

    public TriggerMetadata(String name, String classOption)
    {
        this.name = name;
        this.classOption = classOption;
    }

    public static TriggerMetadata create(String name, String classOption)
    {
        return new TriggerMetadata(name, classOption);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof TriggerMetadata))
            return false;

        TriggerMetadata td = (TriggerMetadata) o;

        return name.equals(td.name) && classOption.equals(td.classOption);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, classOption);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("class", classOption)
                          .toString();
    }
}
