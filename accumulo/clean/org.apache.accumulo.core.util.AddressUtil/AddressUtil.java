import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.*;


public class AddressUtil extends org.apache.accumulo.fate.util.AddressUtil {

  static public HostAndPort parseAddress(String address, boolean ignoreMissingPort)
      throws NumberFormatException {
    address = address.replace('+', ':');
    HostAndPort hap = HostAndPort.fromString(address);
    if (!ignoreMissingPort && !hap.hasPort())
      throw new IllegalArgumentException(
          "Address was expected to contain port. address=" + address);

    return hap;
  }

  public static HostAndPort parseAddress(String address, int defaultPort) {
    return parseAddress(address, true).withDefaultPort(defaultPort);
  }
}
