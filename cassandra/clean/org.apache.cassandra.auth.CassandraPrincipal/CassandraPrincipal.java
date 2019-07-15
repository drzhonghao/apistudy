import org.apache.cassandra.auth.*;


import java.io.Serializable;
import java.security.Principal;

/**
 * <p> This class implements the <code>Principal</code> interface
 * and represents a user.
 *
 * <p> Principals such as this <code>CassPrincipal</code>
 * may be associated with a particular <code>Subject</code>
 * to augment that <code>Subject</code> with an additional
 * identity.  Refer to the <code>Subject</code> class for more information
 * on how to achieve this.  Authorization decisions can then be based upon
 * the Principals associated with a <code>Subject</code>.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class CassandraPrincipal implements Principal, Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String name;

    /**
     * Create a CassPrincipal with a username.
     *
     * <p>
     *
     * @param name the username for this user.
     *
     * @exception NullPointerException if the <code>name</code>
     *                  is <code>null</code>.
     */
    public CassandraPrincipal(String name)
    {
        if (name == null)
            throw new NullPointerException("illegal null input");

        this.name = name;
    }

    /**
     * Return the username for this <code>CassPrincipal</code>.
     *
     * <p>
     *
     * @return the username for this <code>CassPrincipal</code>
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Return a string representation of this <code>CassPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this <code>CassPrincipal</code>.
     */
    @Override
    public String toString()
    {
        return ("CassandraPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>CassPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>CassPrincipal</code> and the two CassPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *          <code>CassPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *          <code>CassPrincipal</code>.
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof CassandraPrincipal))
            return false;
        CassandraPrincipal that = (CassandraPrincipal) o;

        if (this.getName().equals(that.getName()))
            return true;
        return false;
    }

    /**
     * Return a hash code for this <code>CassPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>CassPrincipal</code>.
     */
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
