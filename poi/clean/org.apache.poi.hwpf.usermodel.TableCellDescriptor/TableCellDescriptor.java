import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.*;


import org.apache.poi.hwpf.model.types.TCAbstractType;
import org.apache.poi.util.LittleEndian;

public final class TableCellDescriptor extends TCAbstractType implements
        Cloneable
{
  public static final int SIZE = 20;

  public TableCellDescriptor()
  {
  }

  protected void fillFields(byte[] data, int offset)
  {
    field_1_rgf = LittleEndian.getShort(data, 0x0 + offset);
    field_2_wWidth = LittleEndian.getShort(data, 0x2 + offset);
    setBrcTop(new BorderCode(data, 0x4 + offset));
    setBrcLeft(new BorderCode(data, 0x8 + offset));
    setBrcBottom(new BorderCode(data, 0xc + offset));
    setBrcRight(new BorderCode(data, 0x10 + offset));
  }

  public void serialize(byte[] data, int offset)
  {
      LittleEndian.putShort(data, 0x0 + offset, field_1_rgf);
      LittleEndian.putShort(data, 0x2 + offset, field_2_wWidth);
      getBrcTop().serialize(data, 0x4 + offset);
      getBrcLeft().serialize(data, 0x8 + offset);
      getBrcBottom().serialize(data, 0xc + offset);
      getBrcRight().serialize(data, 0x10 + offset);
  }

  public Object clone()
    throws CloneNotSupportedException
  {
    TableCellDescriptor tc = (TableCellDescriptor)super.clone();
    tc.setShd( getShd().clone() );
    tc.setBrcTop((BorderCode)getBrcTop().clone());
    tc.setBrcLeft((BorderCode)getBrcLeft().clone());
    tc.setBrcBottom((BorderCode)getBrcBottom().clone());
    tc.setBrcRight((BorderCode)getBrcRight().clone());
    return tc;
  }

  public static TableCellDescriptor convertBytesToTC(byte[] buf, int offset)
  {
    TableCellDescriptor tc = new TableCellDescriptor();
    tc.fillFields(buf, offset);
    return tc;
  }

}
