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

public class CASResult implements org.apache.thrift.TBase<CASResult, CASResult._Fields>, java.io.Serializable, Cloneable, Comparable<CASResult> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CASResult");

  private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.BOOL, (short)1);
  private static final org.apache.thrift.protocol.TField CURRENT_VALUES_FIELD_DESC = new org.apache.thrift.protocol.TField("current_values", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CASResultStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CASResultTupleSchemeFactory());
  }

  public boolean success; // required
  public List<Column> current_values; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    SUCCESS((short)1, "success"),
    CURRENT_VALUES((short)2, "current_values");

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
        case 1: // SUCCESS
          return SUCCESS;
        case 2: // CURRENT_VALUES
          return CURRENT_VALUES;
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
  private static final int __SUCCESS_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.CURRENT_VALUES};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.CURRENT_VALUES, new org.apache.thrift.meta_data.FieldMetaData("current_values", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Column.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CASResult.class, metaDataMap);
  }

  public CASResult() {
  }

  public CASResult(
    boolean success)
  {
    this();
    this.success = success;
    setSuccessIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CASResult(CASResult other) {
    __isset_bitfield = other.__isset_bitfield;
    this.success = other.success;
    if (other.isSetCurrent_values()) {
      List<Column> __this__current_values = new ArrayList<Column>(other.current_values.size());
      for (Column other_element : other.current_values) {
        __this__current_values.add(new Column(other_element));
      }
      this.current_values = __this__current_values;
    }
  }

  public CASResult deepCopy() {
    return new CASResult(this);
  }

  @Override
  public void clear() {
    setSuccessIsSet(false);
    this.success = false;
    this.current_values = null;
  }

  public boolean isSuccess() {
    return this.success;
  }

  public CASResult setSuccess(boolean success) {
    this.success = success;
    setSuccessIsSet(true);
    return this;
  }

  public void unsetSuccess() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __SUCCESS_ISSET_ID);
  }

  /** Returns true if field success is set (has been assigned a value) and false otherwise */
  public boolean isSetSuccess() {
    return EncodingUtils.testBit(__isset_bitfield, __SUCCESS_ISSET_ID);
  }

  public void setSuccessIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __SUCCESS_ISSET_ID, value);
  }

  public int getCurrent_valuesSize() {
    return (this.current_values == null) ? 0 : this.current_values.size();
  }

  public java.util.Iterator<Column> getCurrent_valuesIterator() {
    return (this.current_values == null) ? null : this.current_values.iterator();
  }

  public void addToCurrent_values(Column elem) {
    if (this.current_values == null) {
      this.current_values = new ArrayList<Column>();
    }
    this.current_values.add(elem);
  }

  public List<Column> getCurrent_values() {
    return this.current_values;
  }

  public CASResult setCurrent_values(List<Column> current_values) {
    this.current_values = current_values;
    return this;
  }

  public void unsetCurrent_values() {
    this.current_values = null;
  }

  /** Returns true if field current_values is set (has been assigned a value) and false otherwise */
  public boolean isSetCurrent_values() {
    return this.current_values != null;
  }

  public void setCurrent_valuesIsSet(boolean value) {
    if (!value) {
      this.current_values = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case SUCCESS:
      if (value == null) {
        unsetSuccess();
      } else {
        setSuccess((Boolean)value);
      }
      break;

    case CURRENT_VALUES:
      if (value == null) {
        unsetCurrent_values();
      } else {
        setCurrent_values((List<Column>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case SUCCESS:
      return Boolean.valueOf(isSuccess());

    case CURRENT_VALUES:
      return getCurrent_values();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case SUCCESS:
      return isSetSuccess();
    case CURRENT_VALUES:
      return isSetCurrent_values();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CASResult)
      return this.equals((CASResult)that);
    return false;
  }

  public boolean equals(CASResult that) {
    if (that == null)
      return false;

    boolean this_present_success = true;
    boolean that_present_success = true;
    if (this_present_success || that_present_success) {
      if (!(this_present_success && that_present_success))
        return false;
      if (this.success != that.success)
        return false;
    }

    boolean this_present_current_values = true && this.isSetCurrent_values();
    boolean that_present_current_values = true && that.isSetCurrent_values();
    if (this_present_current_values || that_present_current_values) {
      if (!(this_present_current_values && that_present_current_values))
        return false;
      if (!this.current_values.equals(that.current_values))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_success = true;
    builder.append(present_success);
    if (present_success)
      builder.append(success);

    boolean present_current_values = true && (isSetCurrent_values());
    builder.append(present_current_values);
    if (present_current_values)
      builder.append(current_values);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(CASResult other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetSuccess()).compareTo(other.isSetSuccess());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuccess()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, other.success);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCurrent_values()).compareTo(other.isSetCurrent_values());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCurrent_values()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.current_values, other.current_values);
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
    StringBuilder sb = new StringBuilder("CASResult(");
    boolean first = true;

    sb.append("success:");
    sb.append(this.success);
    first = false;
    if (isSetCurrent_values()) {
      if (!first) sb.append(", ");
      sb.append("current_values:");
      if (this.current_values == null) {
        sb.append("null");
      } else {
        sb.append(this.current_values);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // alas, we cannot check 'success' because it's a primitive and you chose the non-beans generator.
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

  private static class CASResultStandardSchemeFactory implements SchemeFactory {
    public CASResultStandardScheme getScheme() {
      return new CASResultStandardScheme();
    }
  }

  private static class CASResultStandardScheme extends StandardScheme<CASResult> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CASResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // SUCCESS
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.success = iprot.readBool();
              struct.setSuccessIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // CURRENT_VALUES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list48 = iprot.readListBegin();
                struct.current_values = new ArrayList<Column>(_list48.size);
                for (int _i49 = 0; _i49 < _list48.size; ++_i49)
                {
                  Column _elem50;
                  _elem50 = new Column();
                  _elem50.read(iprot);
                  struct.current_values.add(_elem50);
                }
                iprot.readListEnd();
              }
              struct.setCurrent_valuesIsSet(true);
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
      if (!struct.isSetSuccess()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'success' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, CASResult struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
      oprot.writeBool(struct.success);
      oprot.writeFieldEnd();
      if (struct.current_values != null) {
        if (struct.isSetCurrent_values()) {
          oprot.writeFieldBegin(CURRENT_VALUES_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.current_values.size()));
            for (Column _iter51 : struct.current_values)
            {
              _iter51.write(oprot);
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

  private static class CASResultTupleSchemeFactory implements SchemeFactory {
    public CASResultTupleScheme getScheme() {
      return new CASResultTupleScheme();
    }
  }

  private static class CASResultTupleScheme extends TupleScheme<CASResult> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CASResult struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeBool(struct.success);
      BitSet optionals = new BitSet();
      if (struct.isSetCurrent_values()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetCurrent_values()) {
        {
          oprot.writeI32(struct.current_values.size());
          for (Column _iter52 : struct.current_values)
          {
            _iter52.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CASResult struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.success = iprot.readBool();
      struct.setSuccessIsSet(true);
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list53 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.current_values = new ArrayList<Column>(_list53.size);
          for (int _i54 = 0; _i54 < _list53.size; ++_i54)
          {
            Column _elem55;
            _elem55 = new Column();
            _elem55.read(iprot);
            struct.current_values.add(_elem55);
          }
        }
        struct.setCurrent_valuesIsSet(true);
      }
    }
  }

}

