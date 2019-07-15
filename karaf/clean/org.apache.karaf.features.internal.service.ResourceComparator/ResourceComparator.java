import org.apache.karaf.features.internal.service.*;


import java.util.Comparator;

import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

public class ResourceComparator implements Comparator<Resource> {

    @Override
    public int compare(Resource o1, Resource o2) {
        String bsn1 = ResolverUtil.getSymbolicName(o1);
        String bsn2 = ResolverUtil.getSymbolicName(o2);
        int c = bsn1.compareTo(bsn2);
        if (c == 0) {
            Version v1 = ResolverUtil.getVersion(o1);
            Version v2 = ResolverUtil.getVersion(o2);
            c = v1.compareTo(v2);
            if (c == 0) {
                c = o1.hashCode() - o2.hashCode();
            }
        }
        return c;
    }

}
