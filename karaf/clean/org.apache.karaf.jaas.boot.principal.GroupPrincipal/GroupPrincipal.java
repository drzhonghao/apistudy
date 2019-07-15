import org.apache.karaf.jaas.boot.principal.*;


import java.io.Serializable;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;

public class GroupPrincipal implements Group, Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private Hashtable<String,Principal> members = new Hashtable<>();

    public GroupPrincipal(String name) {
        assert name != null;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupPrincipal that = (GroupPrincipal) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "GroupPrincipal[" + name + "]";
    }

    public boolean addMember(Principal user) {
        members.put(user.getName(), user);
        return true;
    }

    public boolean removeMember(Principal user) {
        members.remove(user.getName());
        return true;
    }

    public boolean isMember(Principal member) {
        return members.contains(member.getName());
    }

    public Enumeration<? extends Principal> members() {
        return members.elements();
    }
}
