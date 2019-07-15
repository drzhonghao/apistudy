import org.apache.accumulo.core.client.impl.*;


import org.apache.accumulo.core.client.Durability;
import org.apache.accumulo.core.tabletserver.thrift.TDurability;

public class DurabilityImpl {

  public static TDurability toThrift(Durability durability) {
    switch (durability) {
      case DEFAULT:
        return TDurability.DEFAULT;
      case SYNC:
        return TDurability.SYNC;
      case FLUSH:
        return TDurability.FLUSH;
      case LOG:
        return TDurability.LOG;
      default:
        return TDurability.NONE;
    }
  }

  public static Durability fromString(String value) {
    return Durability.valueOf(value.toUpperCase());
  }

  public static Durability fromThrift(TDurability tdurabilty) {
    if (tdurabilty == null) {
      return Durability.DEFAULT;
    }
    switch (tdurabilty) {
      case DEFAULT:
        return Durability.DEFAULT;
      case SYNC:
        return Durability.SYNC;
      case FLUSH:
        return Durability.FLUSH;
      case LOG:
        return Durability.LOG;
      default:
        return Durability.NONE;
    }
  }

  public static Durability resolveDurabilty(Durability durability, Durability tabletDurability) {
    if (durability == Durability.DEFAULT) {
      return tabletDurability;
    }
    return durability;
  }

}
