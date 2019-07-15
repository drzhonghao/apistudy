import org.apache.poi.hwpf.model.*;


import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

/**
 * Section Descriptor (SED)
 * 
 * See page 186 for details.
 */
@Internal
public final class SectionDescriptor
{

    /**
     * "Used internally by Word"
     */
    private short fn;

    /**
     * "File offset in main stream to beginning of SEPX stored for section. If
     * sed.fcSepx==0xFFFFFFFF, the section properties for the section are equal
     * to the standard SEP (see SEP definition)."
     */
    private int fcSepx;
    
    /**
     * "Used internally by Word"
     */
    private short fnMpr;

    /**
     * "Points to offset in FC space of main stream where the Macintosh Print
     * Record for a document created on a Macintosh will be stored"
     */
    private int fcMpr;

  public SectionDescriptor()
  {
  }

  public SectionDescriptor(byte[] buf, int offset)
  {
    fn = LittleEndian.getShort(buf, offset);
    offset += LittleEndian.SHORT_SIZE;
    fcSepx = LittleEndian.getInt(buf, offset);
    offset += LittleEndian.INT_SIZE;
    fnMpr = LittleEndian.getShort(buf, offset);
    offset += LittleEndian.SHORT_SIZE;
    fcMpr = LittleEndian.getInt(buf, offset);
  }

  public int getFc()
  {
    return fcSepx;
  }

  public void setFc(int fc)
  {
    this.fcSepx = fc;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof SectionDescriptor)) return false;
    SectionDescriptor sed = (SectionDescriptor)o;
    return sed.fn == fn && sed.fnMpr == fnMpr;
  }

  @Override
  public int hashCode() {
      assert false : "hashCode not designed";
      return 42; // any arbitrary constant will do
  }
  
  public byte[] toByteArray()
  {
    int offset = 0;
    byte[] buf = new byte[12];

    LittleEndian.putShort(buf, offset, fn);
    offset += LittleEndian.SHORT_SIZE;
    LittleEndian.putInt(buf, offset, fcSepx);
    offset += LittleEndian.INT_SIZE;
    LittleEndian.putShort(buf, offset, fnMpr);
    offset += LittleEndian.SHORT_SIZE;
    LittleEndian.putInt(buf, offset, fcMpr);

    return buf;
  }

    @Override
    public String toString()
    {
        return "[SED] (fn: " + fn + "; fcSepx: " + fcSepx + "; fnMpr: " + fnMpr
                + "; fcMpr: " + fcMpr + ")";
    }
}
