import org.apache.karaf.features.internal.model.*;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Additional requirement for a feature.</p>
 * <p>Java class for bundle complex type.</p>
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * <pre>
 * &lt;complexType name="capability"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "requirement", propOrder = {"value"})
public class Requirement implements org.apache.karaf.features.Requirement {

    @XmlValue
    protected String value;


    public Requirement() {
    }

    public Requirement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Requirement bundle = (Requirement) o;
        return value != null ? value.equals(bundle.value) : bundle.value == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        return result;
    }
}
