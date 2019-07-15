import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.AddressUtil;
import org.apache.accumulo.core.util.*;


import java.util.EnumMap;

public class ServerServices implements Comparable<ServerServices> {
  public static enum Service {
    TSERV_CLIENT, GC_CLIENT;
  }

  public static final String SERVICE_SEPARATOR = ";";
  public static final String SEPARATOR_CHAR = "=";

  private EnumMap<Service,String> services;
  private String stringForm = null;

  public ServerServices(String services) {
    this.services = new EnumMap<>(Service.class);

    String[] addresses = services.split(SERVICE_SEPARATOR);
    for (String address : addresses) {
      String[] sa = address.split(SEPARATOR_CHAR, 2);
      this.services.put(Service.valueOf(sa[0]), sa[1]);
    }
  }

  public ServerServices(String address, Service service) {
    this(service.name() + SEPARATOR_CHAR + address);
  }

  public String getAddressString(Service service) {
    return services.get(service);
  }

  public HostAndPort getAddress(Service service) {
    return AddressUtil.parseAddress(getAddressString(service), false);
  }

  // DON'T CHANGE THIS; WE'RE USING IT FOR SERIALIZATION!!!
  @Override
  public String toString() {
    if (stringForm == null) {
      StringBuilder sb = new StringBuilder();
      String prefix = "";
      for (Service service : new Service[] {Service.TSERV_CLIENT, Service.GC_CLIENT}) {
        if (services.containsKey(service)) {
          sb.append(prefix).append(service.name()).append(SEPARATOR_CHAR)
              .append(services.get(service));
          prefix = SERVICE_SEPARATOR;
        }
      }
      stringForm = sb.toString();
    }
    return stringForm;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ServerServices)
      return toString().equals(((ServerServices) o).toString());
    return false;
  }

  @Override
  public int compareTo(ServerServices other) {
    return toString().compareTo(other.toString());
  }
}
