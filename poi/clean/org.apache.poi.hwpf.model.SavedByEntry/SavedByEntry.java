import org.apache.poi.hwpf.model.*;


import org.apache.poi.util.Internal;


/**
 * A single entry in the {@link SavedByTable}.
 *
 * @author Daniel Noll
 */
@Internal
public final class SavedByEntry
{
  private String userName;
  private String saveLocation;

  public SavedByEntry(String userName, String saveLocation)
  {
    this.userName = userName;
    this.saveLocation = saveLocation;
  }

  public String getUserName()
  {
    return userName;
  }

  public String getSaveLocation()
  {
    return saveLocation;
  }

  /**
   * Compares this object with another, for equality.
   *
   * @param other the object to compare to this one.
   * @return <code>true</code> iff the other object is equal to this one.
   */
  public boolean equals(Object other)
  {
    if (other == this) return true;
    if (!(other instanceof SavedByEntry)) return false;
    SavedByEntry that = (SavedByEntry) other;
    return that.userName.equals(userName) &&
           that.saveLocation.equals(saveLocation);
  }

  /**
   * Generates a hash code for consistency with {@link #equals(Object)}.
   *
   * @return the hash code.
   */
  public int hashCode()
  {
    int hash = 29;
    hash = hash * 13 + userName.hashCode();
    hash = hash * 13 + saveLocation.hashCode();
    return hash;
  }

  /**
   * Returns a string for display.
   *
   * @return the string.
   */
  public String toString()
  {
    return "SavedByEntry[userName=" + getUserName() +
                       ",saveLocation=" + getSaveLocation() + "]";
  }
}
