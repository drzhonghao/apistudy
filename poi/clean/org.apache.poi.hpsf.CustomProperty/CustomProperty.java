import org.apache.poi.hpsf.Property;
import org.apache.poi.hpsf.*;


/**
 * This class represents custom properties in the document summary
 * information stream. The difference to normal properties is that custom
 * properties have an optional name. If the name is not {@code null} it
 * will be maintained in the section's dictionary.
 */
public class CustomProperty extends Property
{

    private String name;

    /**
     * Creates an empty {@link CustomProperty}. The set methods must be
     * called to make it usable.
     */
    public CustomProperty() {
        this.name = null;
    }

    /**
     * Creates a {@link CustomProperty} without a name by copying the
     * underlying {@link Property}' attributes.
     * 
     * @param property the property to copy
     */
    public CustomProperty(final Property property) {
        this(property, null);
    }

    /**
     * Creates a {@link CustomProperty} with a name.
     * 
     * @param property This property's attributes are copied to the new custom
     *        property.
     * @param name The new custom property's name.
     */
    public CustomProperty(final Property property, final String name) {
        super(property);
        this.name = name;
    }

    /**
     * Gets the property's name.
     *
     * @return the property's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the property's name.
     *
     * @param name The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }


    /**
     * Compares two custom properties for equality. The method returns
     * {@code true} if all attributes of the two custom properties are
     * equal.
     * 
     * @param o The custom property to compare with.
     * @return {@code true} if both custom properties are equal, else
     *         {@code false}.
     * 
     * @see java.util.AbstractSet#equals(java.lang.Object)
     */
    public boolean equalsContents(final Object o) {
        final CustomProperty c = (CustomProperty) o;
        final String name1 = c.getName();
        final String name2 = this.getName();
        boolean equalNames = true;
        if (name1 == null) {
            equalNames = name2 == null;
        } else {
            equalNames = name1.equals(name2);
        }
        return equalNames && c.getID() == this.getID()
                && c.getType() == this.getType()
                && c.getValue().equals(this.getValue());
    }

    /**
     * @see java.util.AbstractSet#hashCode()
     */
    @Override
    public int hashCode() {
        return (int) this.getID();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof CustomProperty) ? equalsContents(o) : false;
    }
}
