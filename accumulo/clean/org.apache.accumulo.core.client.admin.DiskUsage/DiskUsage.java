import org.apache.accumulo.core.client.admin.*;


import java.util.SortedSet;

public class DiskUsage {

  protected final SortedSet<String> tables;
  protected Long usage;

  public DiskUsage(SortedSet<String> tables, Long usage) {
    this.tables = tables;
    this.usage = usage;
  }

  public SortedSet<String> getTables() {
    return tables;
  }

  public Long getUsage() {
    return usage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DiskUsage))
      return false;

    DiskUsage diskUsage = (DiskUsage) o;

    if (tables != null ? !tables.equals(diskUsage.tables) : diskUsage.tables != null)
      return false;
    if (usage != null ? !usage.equals(diskUsage.usage) : diskUsage.usage != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = tables != null ? tables.hashCode() : 0;
    result = 31 * result + (usage != null ? usage.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DiskUsage{" + "tables=" + tables + ", usage=" + usage + '}';
  }
}
