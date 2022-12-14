import org.apache.accumulo.core.data.thrift.*;


import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class MapFileInfo implements org.apache.thrift.TBase<MapFileInfo, MapFileInfo._Fields>, java.io.Serializable, Cloneable, Comparable<MapFileInfo> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("MapFileInfo");

  private static final org.apache.thrift.protocol.TField ESTIMATED_SIZE_FIELD_DESC = new org.apache.thrift.protocol.TField("estimatedSize", org.apache.thrift.protocol.TType.I64, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new MapFileInfoStandardSchemeFactory());
    schemes.put(TupleScheme.class, new MapFileInfoTupleSchemeFactory());
  }

  public long estimatedSize; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    ESTIMATED_SIZE((short)1, "estimatedSize");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // ESTIMATED_SIZE
          return ESTIMATED_SIZE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __ESTIMATEDSIZE_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.ESTIMATED_SIZE, new org.apache.thrift.meta_data.FieldMetaData("estimatedSize", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(MapFileInfo.class, metaDataMap);
  }

  public MapFileInfo() {
  }

  public MapFileInfo(
    long estimatedSize)
  {
    this();
    this.estimatedSize = estimatedSize;
    setEstimatedSizeIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public MapFileInfo(MapFileInfo other) {
    __isset_bitfield = other.__isset_bitfield;
    this.estimatedSize = other.estimatedSize;
  }

  public MapFileInfo deepCopy() {
    return new MapFileInfo(this);
  }

  @Override
  public void clear() {
    setEstimatedSizeIsSet(false);
    this.estimatedSize = 0;
  }

  public long getEstimatedSize() {
    return this.estimatedSize;
  }

  public MapFileInfo setEstimatedSize(long estimatedSize) {
    this.estimatedSize = estimatedSize;
    setEstimatedSizeIsSet(true);
    return this;
  }

  public void unsetEstimatedSize() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ESTIMATEDSIZE_ISSET_ID);
  }

  /** Returns true if field estimatedSize is set (has been assigned a value) and false otherwise */
  public boolean isSetEstimatedSize() {
    return EncodingUtils.testBit(__isset_bitfield, __ESTIMATEDSIZE_ISSET_ID);
  }

  public void setEstimatedSizeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ESTIMATEDSIZE_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case ESTIMATED_SIZE:
      if (value == null) {
        unsetEstimatedSize();
      } else {
        setEstimatedSize((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case ESTIMATED_SIZE:
      return getEstimatedSize();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case ESTIMATED_SIZE:
      return isSetEstimatedSize();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof MapFileInfo)
      return this.equals((MapFileInfo)that);
    return false;
  }

  public boolean equals(MapFileInfo that) {
    if (that == null)
      return false;

    boolean this_present_estimatedSize = true;
    boolean that_present_estimatedSize = true;
    if (this_present_estimatedSize || that_present_estimatedSize) {
      if (!(this_present_estimatedSize && that_present_estimatedSize))
        return false;
      if (this.estimatedSize != that.estimatedSize)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_estimatedSize = true;
    list.add(present_estimatedSize);
    if (present_estimatedSize)
      list.add(estimatedSize);

    return list.hashCode();
  }

  @Override
  public int compareTo(MapFileInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetEstimatedSize()).compareTo(other.isSetEstimatedSize());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetEstimatedSize()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.estimatedSize, other.estimatedSize);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MapFileInfo(");
    boolean first = true;

    sb.append("estimatedSize:");
    sb.append(this.estimatedSize);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class MapFileInfoStandardSchemeFactory implements SchemeFactory {
    public MapFileInfoStandardScheme getScheme() {
      return new MapFileInfoStandardScheme();
    }
  }

  private static class MapFileInfoStandardScheme extends StandardScheme<MapFileInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, MapFileInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // ESTIMATED_SIZE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.estimatedSize = iprot.readI64();
              struct.setEstimatedSizeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, MapFileInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(ESTIMATED_SIZE_FIELD_DESC);
      oprot.writeI64(struct.estimatedSize);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class MapFileInfoTupleSchemeFactory implements SchemeFactory {
    public MapFileInfoTupleScheme getScheme() {
      return new MapFileInfoTupleScheme();
    }
  }

  private static class MapFileInfoTupleScheme extends TupleScheme<MapFileInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, MapFileInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetEstimatedSize()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetEstimatedSize()) {
        oprot.writeI64(struct.estimatedSize);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, MapFileInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.estimatedSize = iprot.readI64();
        struct.setEstimatedSizeIsSet(true);
      }
    }
  }

}

