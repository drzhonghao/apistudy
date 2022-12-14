import org.apache.accumulo.core.data.thrift.TCondition;
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
public class TConditionalMutation implements org.apache.thrift.TBase<TConditionalMutation, TConditionalMutation._Fields>, java.io.Serializable, Cloneable, Comparable<TConditionalMutation> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TConditionalMutation");

  private static final org.apache.thrift.protocol.TField CONDITIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("conditions", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField MUTATION_FIELD_DESC = new org.apache.thrift.protocol.TField("mutation", org.apache.thrift.protocol.TType.STRUCT, (short)2);
  private static final org.apache.thrift.protocol.TField ID_FIELD_DESC = new org.apache.thrift.protocol.TField("id", org.apache.thrift.protocol.TType.I64, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TConditionalMutationStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TConditionalMutationTupleSchemeFactory());
  }

  public List<TCondition> conditions; // required
  public TMutation mutation; // required
  public long id; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    CONDITIONS((short)1, "conditions"),
    MUTATION((short)2, "mutation"),
    ID((short)3, "id");

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
        case 1: // CONDITIONS
          return CONDITIONS;
        case 2: // MUTATION
          return MUTATION;
        case 3: // ID
          return ID;
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
  private static final int __ID_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.CONDITIONS, new org.apache.thrift.meta_data.FieldMetaData("conditions", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TCondition.class))));
    tmpMap.put(_Fields.MUTATION, new org.apache.thrift.meta_data.FieldMetaData("mutation", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TMutation.class)));
    tmpMap.put(_Fields.ID, new org.apache.thrift.meta_data.FieldMetaData("id", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TConditionalMutation.class, metaDataMap);
  }

  public TConditionalMutation() {
  }

  public TConditionalMutation(
    List<TCondition> conditions,
    TMutation mutation,
    long id)
  {
    this();
    this.conditions = conditions;
    this.mutation = mutation;
    this.id = id;
    setIdIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TConditionalMutation(TConditionalMutation other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetConditions()) {
      List<TCondition> __this__conditions = new ArrayList<TCondition>(other.conditions.size());
      for (TCondition other_element : other.conditions) {
        __this__conditions.add(new TCondition(other_element));
      }
      this.conditions = __this__conditions;
    }
    if (other.isSetMutation()) {
      this.mutation = new TMutation(other.mutation);
    }
    this.id = other.id;
  }

  public TConditionalMutation deepCopy() {
    return new TConditionalMutation(this);
  }

  @Override
  public void clear() {
    this.conditions = null;
    this.mutation = null;
    setIdIsSet(false);
    this.id = 0;
  }

  public int getConditionsSize() {
    return (this.conditions == null) ? 0 : this.conditions.size();
  }

  public java.util.Iterator<TCondition> getConditionsIterator() {
    return (this.conditions == null) ? null : this.conditions.iterator();
  }

  public void addToConditions(TCondition elem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<TCondition>();
    }
    this.conditions.add(elem);
  }

  public List<TCondition> getConditions() {
    return this.conditions;
  }

  public TConditionalMutation setConditions(List<TCondition> conditions) {
    this.conditions = conditions;
    return this;
  }

  public void unsetConditions() {
    this.conditions = null;
  }

  /** Returns true if field conditions is set (has been assigned a value) and false otherwise */
  public boolean isSetConditions() {
    return this.conditions != null;
  }

  public void setConditionsIsSet(boolean value) {
    if (!value) {
      this.conditions = null;
    }
  }

  public TMutation getMutation() {
    return this.mutation;
  }

  public TConditionalMutation setMutation(TMutation mutation) {
    this.mutation = mutation;
    return this;
  }

  public void unsetMutation() {
    this.mutation = null;
  }

  /** Returns true if field mutation is set (has been assigned a value) and false otherwise */
  public boolean isSetMutation() {
    return this.mutation != null;
  }

  public void setMutationIsSet(boolean value) {
    if (!value) {
      this.mutation = null;
    }
  }

  public long getId() {
    return this.id;
  }

  public TConditionalMutation setId(long id) {
    this.id = id;
    setIdIsSet(true);
    return this;
  }

  public void unsetId() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ID_ISSET_ID);
  }

  /** Returns true if field id is set (has been assigned a value) and false otherwise */
  public boolean isSetId() {
    return EncodingUtils.testBit(__isset_bitfield, __ID_ISSET_ID);
  }

  public void setIdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ID_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case CONDITIONS:
      if (value == null) {
        unsetConditions();
      } else {
        setConditions((List<TCondition>)value);
      }
      break;

    case MUTATION:
      if (value == null) {
        unsetMutation();
      } else {
        setMutation((TMutation)value);
      }
      break;

    case ID:
      if (value == null) {
        unsetId();
      } else {
        setId((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case CONDITIONS:
      return getConditions();

    case MUTATION:
      return getMutation();

    case ID:
      return getId();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case CONDITIONS:
      return isSetConditions();
    case MUTATION:
      return isSetMutation();
    case ID:
      return isSetId();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TConditionalMutation)
      return this.equals((TConditionalMutation)that);
    return false;
  }

  public boolean equals(TConditionalMutation that) {
    if (that == null)
      return false;

    boolean this_present_conditions = true && this.isSetConditions();
    boolean that_present_conditions = true && that.isSetConditions();
    if (this_present_conditions || that_present_conditions) {
      if (!(this_present_conditions && that_present_conditions))
        return false;
      if (!this.conditions.equals(that.conditions))
        return false;
    }

    boolean this_present_mutation = true && this.isSetMutation();
    boolean that_present_mutation = true && that.isSetMutation();
    if (this_present_mutation || that_present_mutation) {
      if (!(this_present_mutation && that_present_mutation))
        return false;
      if (!this.mutation.equals(that.mutation))
        return false;
    }

    boolean this_present_id = true;
    boolean that_present_id = true;
    if (this_present_id || that_present_id) {
      if (!(this_present_id && that_present_id))
        return false;
      if (this.id != that.id)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_conditions = true && (isSetConditions());
    list.add(present_conditions);
    if (present_conditions)
      list.add(conditions);

    boolean present_mutation = true && (isSetMutation());
    list.add(present_mutation);
    if (present_mutation)
      list.add(mutation);

    boolean present_id = true;
    list.add(present_id);
    if (present_id)
      list.add(id);

    return list.hashCode();
  }

  @Override
  public int compareTo(TConditionalMutation other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetConditions()).compareTo(other.isSetConditions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetConditions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.conditions, other.conditions);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMutation()).compareTo(other.isSetMutation());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMutation()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.mutation, other.mutation);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetId()).compareTo(other.isSetId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.id, other.id);
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
    StringBuilder sb = new StringBuilder("TConditionalMutation(");
    boolean first = true;

    sb.append("conditions:");
    if (this.conditions == null) {
      sb.append("null");
    } else {
      sb.append(this.conditions);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("mutation:");
    if (this.mutation == null) {
      sb.append("null");
    } else {
      sb.append(this.mutation);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("id:");
    sb.append(this.id);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (mutation != null) {
      mutation.validate();
    }
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

  private static class TConditionalMutationStandardSchemeFactory implements SchemeFactory {
    public TConditionalMutationStandardScheme getScheme() {
      return new TConditionalMutationStandardScheme();
    }
  }

  private static class TConditionalMutationStandardScheme extends StandardScheme<TConditionalMutation> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TConditionalMutation struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // CONDITIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list86 = iprot.readListBegin();
                struct.conditions = new ArrayList<TCondition>(_list86.size);
                TCondition _elem87;
                for (int _i88 = 0; _i88 < _list86.size; ++_i88)
                {
                  _elem87 = new TCondition();
                  _elem87.read(iprot);
                  struct.conditions.add(_elem87);
                }
                iprot.readListEnd();
              }
              struct.setConditionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // MUTATION
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.mutation = new TMutation();
              struct.mutation.read(iprot);
              struct.setMutationIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.id = iprot.readI64();
              struct.setIdIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TConditionalMutation struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.conditions != null) {
        oprot.writeFieldBegin(CONDITIONS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.conditions.size()));
          for (TCondition _iter89 : struct.conditions)
          {
            _iter89.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.mutation != null) {
        oprot.writeFieldBegin(MUTATION_FIELD_DESC);
        struct.mutation.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(ID_FIELD_DESC);
      oprot.writeI64(struct.id);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TConditionalMutationTupleSchemeFactory implements SchemeFactory {
    public TConditionalMutationTupleScheme getScheme() {
      return new TConditionalMutationTupleScheme();
    }
  }

  private static class TConditionalMutationTupleScheme extends TupleScheme<TConditionalMutation> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TConditionalMutation struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetConditions()) {
        optionals.set(0);
      }
      if (struct.isSetMutation()) {
        optionals.set(1);
      }
      if (struct.isSetId()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetConditions()) {
        {
          oprot.writeI32(struct.conditions.size());
          for (TCondition _iter90 : struct.conditions)
          {
            _iter90.write(oprot);
          }
        }
      }
      if (struct.isSetMutation()) {
        struct.mutation.write(oprot);
      }
      if (struct.isSetId()) {
        oprot.writeI64(struct.id);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TConditionalMutation struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list91 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.conditions = new ArrayList<TCondition>(_list91.size);
          TCondition _elem92;
          for (int _i93 = 0; _i93 < _list91.size; ++_i93)
          {
            _elem92 = new TCondition();
            _elem92.read(iprot);
            struct.conditions.add(_elem92);
          }
        }
        struct.setConditionsIsSet(true);
      }
      if (incoming.get(1)) {
        struct.mutation = new TMutation();
        struct.mutation.read(iprot);
        struct.setMutationIsSet(true);
      }
      if (incoming.get(2)) {
        struct.id = iprot.readI64();
        struct.setIdIsSet(true);
      }
    }
  }

}

