import org.apache.poi.hwpf.model.*;


import java.util.Arrays;

import org.apache.poi.util.Internal;

@Internal
public final class UPX
{
  private byte[] _upx;

  public UPX(byte[] upx)
  {
    _upx = upx;
  }

  public byte[] getUPX()
  {
    return _upx;
  }
  public int size()
  {
    return _upx.length;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof UPX)) return false;
    UPX upx = (UPX)o;
    return Arrays.equals(_upx, upx._upx);
  }

  @Override
  public int hashCode() {
      assert false : "hashCode not designed";
      return 42; // any arbitrary constant will do
  }

    @Override
    public String toString()
    {
        return "[UPX] " + Arrays.toString( _upx );
    }
}
