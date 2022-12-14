import org.apache.cassandra.thrift.*;

/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import org.apache.commons.lang3.builder.HashCodeBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CqlPreparedResult implements org.apache.thrift.TBase<CqlPreparedResult, CqlPreparedResult._Fields>, java.io.Serializable, Cloneable, Comparable<CqlPreparedResult> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CqlPreparedResult");

  private static final org.apache.thrift.protocol.TField ITEM_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("itemId", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("count", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField VARIABLE_TYPES_FIELD_DESC = new org.apache.thrift.protocol.TField("variable_types", org.apache.thrift.protocol.TType.LIST, (short)3);
  private static final org.apache.thrift.protocol.TField VARIABLE_NAMES_FIELD_DESC = new org.apache.thrift.protocol.TField("variable_names", org.apache.thrift.protocol.TType.LIST, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CqlPreparedResultStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CqlPreparedResultTupleSchemeFactory());
  }

  public int itemId; // required
  public int count; // required
  public List<String> variable_types; // optional
  public List<String> variable_names; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    ITEM_ID((short)1, "itemId"),
    COUNT((short)2, "count"),
    VARIABLE_TYPES((short)3, "variable_types"),
    VARIABLE_NAMES((short)4, "variable_names");

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
        case 1: // ITEM_ID
          return ITEM_ID;
        case 2: // COUNT
          return COUNT;
        case 3: // VARIABLE_TYPES
          return VARIABLE_TYPES;
        case 4: // VARIABLE_NAMES
          return VARIABLE_NAMES;
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
  private static final int __ITEMID_ISSET_ID = 0;
  private static final int __COUNT_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.VARIABLE_TYPES,_Fields.VARIABLE_NAMES};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.ITEM_ID, new org.apache.thrift.meta_data.FieldMetaData("itemId", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.COUNT, new org.apache.thrift.meta_data.FieldMetaData("count", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.VARIABLE_TYPES, new org.apache.thrift.meta_data.FieldMetaData("variable_types", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.VARIABLE_NAMES, new org.apache.thrift.meta_data.FieldMetaData("variable_names", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CqlPreparedResult.class, metaDataMap);
  }

  public CqlPreparedResult() {
  }

  public CqlPreparedResult(
    int itemId,
    int count)
  {
    this();
    this.itemId = itemId;
    setItemIdIsSet(true);
    this.count = count;
    setCountIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CqlPreparedResult(CqlPreparedResult other) {
    __isset_bitfield = other.__isset_bitfield;
    this.itemId = other.itemId;
    this.count = other.count;
    if (other.isSetVariable_types()) {
      List<String> __this__variable_types = new ArrayList<String>(other.variable_types);
      this.variable_types = __this__variable_types;
    }
    if (other.isSetVariable_names()) {
      List<String> __this__variable_names = new ArrayList<String>(other.variable_names);
      this.variable_names = __this__variable_names;
    }
  }

  public CqlPreparedResult deepCopy() {
    return new CqlPreparedResult(this);
  }

  @Override
  public void clear() {
    setItemIdIsSet(false);
    this.itemId = 0;
    setCountIsSet(false);
    this.count = 0;
    this.variable_types = null;
    this.variable_names = null;
  }

  public int getItemId() {
    return this.itemId;
  }

  public CqlPreparedResult setItemId(int itemId) {
    this.itemId = itemId;
    setItemIdIsSet(true);
    return this;
  }

  public void unsetItemId() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ITEMID_ISSET_ID);
  }

  /** Returns true if field itemId is set (has been assigned a value) and false otherwise */
  public boolean isSetItemId() {
    return EncodingUtils.testBit(__isset_bitfield, __ITEMID_ISSET_ID);
  }

  public void setItemIdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ITEMID_ISSET_ID, value);
  }

  public int getCount() {
    return this.count;
  }

  public CqlPreparedResult setCount(int count) {
    this.count = count;
    setCountIsSet(true);
    return this;
  }

  public void unsetCount() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __COUNT_ISSET_ID);
  }

  /** Returns true if field count is set (has been assigned a value) and false otherwise */
  public boolean isSetCount() {
    return EncodingUtils.testBit(__isset_bitfield, __COUNT_ISSET_ID);
  }

  public void setCountIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __COUNT_ISSET_ID, value);
  }

  public int getVariable_typesSize() {
    return (this.variable_types == null) ? 0 : this.variable_types.size();
  }

  public java.util.Iterator<String> getVariable_typesIterator() {
    return (this.variable_types == null) ? null : this.variable_types.iterator();
  }

  public void addToVariable_types(String elem) {
    if (this.variable_types == null) {
      this.variable_types = new ArrayList<String>();
    }
    this.variable_types.add(elem);
  }

  public List<String> getVariable_types() {
    return this.variable_types;
  }

  public CqlPreparedResult setVariable_types(List<String> variable_types) {
    this.variable_types = variable_types;
    return this;
  }

  public void unsetVariable_types() {
    this.variable_types = null;
  }

  /** Returns true if field variable_types is set (has been assigned a value) and false otherwise */
  public boolean isSetVariable_types() {
    return this.variable_types != null;
  }

  public void setVariable_typesIsSet(boolean value) {
    if (!value) {
      this.variable_types = null;
    }
  }

  public int getVariable_namesSize() {
    return (this.variable_names == null) ? 0 : this.variable_names.size();
  }

  public java.util.Iterator<String> getVariable_namesIterator() {
    return (this.variable_names == null) ? null : this.variable_names.iterator();
  }

  public void addToVariable_names(String elem) {
    if (this.variable_names == null) {
      this.variable_names = new ArrayList<String>();
    }
    this.variable_names.add(elem);
  }

  public List<String> getVariable_names() {
    return this.variable_names;
  }

  public CqlPreparedResult setVariable_names(List<String> variable_names) {
    this.variable_names = variable_names;
    return this;
  }

  public void unsetVariable_names() {
    this.variable_names = null;
  }

  /** Returns true if field variable_names is set (has been assigned a value) and false otherwise */
  public boolean isSetVariable_names() {
    return this.variable_names != null;
  }

  public void setVariable_namesIsSet(boolean value) {
    if (!value) {
      this.variable_names = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case ITEM_ID:
      if (value == null) {
        unsetItemId();
      } else {
        setItemId((Integer)value);
      }
      break;

    case COUNT:
      if (value == null) {
        unsetCount();
      } else {
        setCount((Integer)value);
      }
      break;

    case VARIABLE_TYPES:
      if (value == null) {
        unsetVariable_types();
      } else {
        setVariable_types((List<String>)value);
      }
      break;

    case VARIABLE_NAMES:
      if (value == null) {
        unsetVariable_names();
      } else {
        setVariable_names((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case ITEM_ID:
      return Integer.valueOf(getItemId());

    case COUNT:
      return Integer.valueOf(getCount());

    case VARIABLE_TYPES:
      return getVariable_types();

    case VARIABLE_NAMES:
      return getVariable_names();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case ITEM_ID:
      return isSetItemId();
    case COUNT:
      return isSetCount();
    case VARIABLE_TYPES:
      return isSetVariable_types();
    case VARIABLE_NAMES:
      return isSetVariable_names();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CqlPreparedResult)
      return this.equals((CqlPreparedResult)that);
    return false;
  }

  public boolean equals(CqlPreparedResult that) {
    if (that == null)
      return false;

    boolean this_present_itemId = true;
    boolean that_present_itemId = true;
    if (this_present_itemId || that_present_itemId) {
      if (!(this_present_itemId && that_present_itemId))
        return false;
      if (this.itemId != that.itemId)
        return false;
    }

    boolean this_present_count = true;
    boolean that_present_count = true;
    if (this_present_count || that_present_count) {
      if (!(this_present_count && that_present_count))
        return false;
      if (this.count != that.count)
        return false;
    }

    boolean this_present_variable_types = true && this.isSetVariable_types();
    boolean that_present_variable_types = true && that.isSetVariable_types();
    if (this_present_variable_types || that_present_variable_types) {
      if (!(this_present_variable_types && that_present_variable_types))
        return false;
      if (!this.variable_types.equals(that.variable_types))
        return false;
    }

    boolean this_present_variable_names = true && this.isSetVariable_names();
    boolean that_present_variable_names = true && that.isSetVariable_names();
    if (this_present_variable_names || that_present_variable_names) {
      if (!(this_present_variable_names && that_present_variable_names))
        return false;
      if (!this.variable_names.equals(that.variable_names))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_itemId = true;
    builder.append(present_itemId);
    if (present_itemId)
      builder.append(itemId);

    boolean present_count = true;
    builder.append(present_count);
    if (present_count)
      builder.append(count);

    boolean present_variable_types = true && (isSetVariable_types());
    builder.append(present_variable_types);
    if (present_variable_types)
      builder.append(variable_types);

    boolean present_variable_names = true && (isSetVariable_names());
    builder.append(present_variable_names);
    if (present_variable_names)
      builder.append(variable_names);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(CqlPreparedResult other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetItemId()).compareTo(other.isSetItemId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetItemId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.itemId, other.itemId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCount()).compareTo(other.isSetCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.count, other.count);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetVariable_types()).compareTo(other.isSetVariable_types());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVariable_types()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.variable_types, other.variable_types);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetVariable_names()).compareTo(other.isSetVariable_names());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVariable_names()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.variable_names, other.variable_names);
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
    StringBuilder sb = new StringBuilder("CqlPreparedResult(");
    boolean first = true;

    sb.append("itemId:");
    sb.append(this.itemId);
    first = false;
    if (!first) sb.append(", ");
    sb.append("count:");
    sb.append(this.count);
    first = false;
    if (isSetVariable_types()) {
      if (!first) sb.append(", ");
      sb.append("variable_types:");
      if (this.variable_types == null) {
        sb.append("null");
      } else {
        sb.append(this.variable_types);
      }
      first = false;
    }
    if (isSetVariable_names()) {
      if (!first) sb.append(", ");
      sb.append("variable_names:");
      if (this.variable_names == null) {
        sb.append("null");
      } else {
        sb.append(this.variable_names);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // alas, we cannot check 'itemId' because it's a primitive and you chose the non-beans generator.
    // alas, we cannot check 'count' because it's a primitive and you chose the non-beans generator.
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

  private static class CqlPreparedResultStandardSchemeFactory implements SchemeFactory {
    public CqlPreparedResultStandardScheme getScheme() {
      return new CqlPreparedResultStandardScheme();
    }
  }

  private static class CqlPreparedResultStandardScheme extends StandardScheme<CqlPreparedResult> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CqlPreparedResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // ITEM_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.itemId = iprot.readI32();
              struct.setItemIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.count = iprot.readI32();
              struct.setCountIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // VARIABLE_TYPES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list200 = iprot.readListBegin();
                struct.variable_types = new ArrayList<String>(_list200.size);
                for (int _i201 = 0; _i201 < _list200.size; ++_i201)
                {
                  String _elem202;
                  _elem202 = iprot.readString();
                  struct.variable_types.add(_elem202);
                }
                iprot.readListEnd();
              }
              struct.setVariable_typesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // VARIABLE_NAMES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list203 = iprot.readListBegin();
                struct.variable_names = new ArrayList<String>(_list203.size);
                for (int _i204 = 0; _i204 < _list203.size; ++_i204)
                {
                  String _elem205;
                  _elem205 = iprot.readString();
                  struct.variable_names.add(_elem205);
                }
                iprot.readListEnd();
              }
              struct.setVariable_namesIsSet(true);
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
      if (!struct.isSetItemId()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'itemId' was not found in serialized data! Struct: " + toString());
      }
      if (!struct.isSetCount()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'count' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, CqlPreparedResult struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(ITEM_ID_FIELD_DESC);
      oprot.writeI32(struct.itemId);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(COUNT_FIELD_DESC);
      oprot.writeI32(struct.count);
      oprot.writeFieldEnd();
      if (struct.variable_types != null) {
        if (struct.isSetVariable_types()) {
          oprot.writeFieldBegin(VARIABLE_TYPES_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.variable_types.size()));
            for (String _iter206 : struct.variable_types)
            {
              oprot.writeString(_iter206);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.variable_names != null) {
        if (struct.isSetVariable_names()) {
          oprot.writeFieldBegin(VARIABLE_NAMES_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.variable_names.size()));
            for (String _iter207 : struct.variable_names)
            {
              oprot.writeString(_iter207);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CqlPreparedResultTupleSchemeFactory implements SchemeFactory {
    public CqlPreparedResultTupleScheme getScheme() {
      return new CqlPreparedResultTupleScheme();
    }
  }

  private static class CqlPreparedResultTupleScheme extends TupleScheme<CqlPreparedResult> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CqlPreparedResult struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeI32(struct.itemId);
      oprot.writeI32(struct.count);
      BitSet optionals = new BitSet();
      if (struct.isSetVariable_types()) {
        optionals.set(0);
      }
      if (struct.isSetVariable_names()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetVariable_types()) {
        {
          oprot.writeI32(struct.variable_types.size());
          for (String _iter208 : struct.variable_types)
          {
            oprot.writeString(_iter208);
          }
        }
      }
      if (struct.isSetVariable_names()) {
        {
          oprot.writeI32(struct.variable_names.size());
          for (String _iter209 : struct.variable_names)
          {
            oprot.writeString(_iter209);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CqlPreparedResult struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.itemId = iprot.readI32();
      struct.setItemIdIsSet(true);
      struct.count = iprot.readI32();
      struct.setCountIsSet(true);
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list210 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.variable_types = new ArrayList<String>(_list210.size);
          for (int _i211 = 0; _i211 < _list210.size; ++_i211)
          {
            String _elem212;
            _elem212 = iprot.readString();
            struct.variable_types.add(_elem212);
          }
        }
        struct.setVariable_typesIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list213 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.variable_names = new ArrayList<String>(_list213.size);
          for (int _i214 = 0; _i214 < _list213.size; ++_i214)
          {
            String _elem215;
            _elem215 = iprot.readString();
            struct.variable_names.add(_elem215);
          }
        }
        struct.setVariable_namesIsSet(true);
      }
    }
  }

}

