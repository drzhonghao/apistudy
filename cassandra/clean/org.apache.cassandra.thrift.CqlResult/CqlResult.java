import org.apache.cassandra.thrift.CqlRow;
import org.apache.cassandra.thrift.CqlMetadata;
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

public class CqlResult implements org.apache.thrift.TBase<CqlResult, CqlResult._Fields>, java.io.Serializable, Cloneable, Comparable<CqlResult> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CqlResult");

  private static final org.apache.thrift.protocol.TField TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("type", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField ROWS_FIELD_DESC = new org.apache.thrift.protocol.TField("rows", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField NUM_FIELD_DESC = new org.apache.thrift.protocol.TField("num", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField SCHEMA_FIELD_DESC = new org.apache.thrift.protocol.TField("schema", org.apache.thrift.protocol.TType.STRUCT, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CqlResultStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CqlResultTupleSchemeFactory());
  }

  /**
   * 
   * @see CqlResultType
   */
  public CqlResultType type; // required
  public List<CqlRow> rows; // optional
  public int num; // optional
  public CqlMetadata schema; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    /**
     * 
     * @see CqlResultType
     */
    TYPE((short)1, "type"),
    ROWS((short)2, "rows"),
    NUM((short)3, "num"),
    SCHEMA((short)4, "schema");

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
        case 1: // TYPE
          return TYPE;
        case 2: // ROWS
          return ROWS;
        case 3: // NUM
          return NUM;
        case 4: // SCHEMA
          return SCHEMA;
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
  private static final int __NUM_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.ROWS,_Fields.NUM,_Fields.SCHEMA};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.TYPE, new org.apache.thrift.meta_data.FieldMetaData("type", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, CqlResultType.class)));
    tmpMap.put(_Fields.ROWS, new org.apache.thrift.meta_data.FieldMetaData("rows", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, CqlRow.class))));
    tmpMap.put(_Fields.NUM, new org.apache.thrift.meta_data.FieldMetaData("num", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.SCHEMA, new org.apache.thrift.meta_data.FieldMetaData("schema", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, CqlMetadata.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CqlResult.class, metaDataMap);
  }

  public CqlResult() {
  }

  public CqlResult(
    CqlResultType type)
  {
    this();
    this.type = type;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CqlResult(CqlResult other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetType()) {
      this.type = other.type;
    }
    if (other.isSetRows()) {
      List<CqlRow> __this__rows = new ArrayList<CqlRow>(other.rows.size());
      for (CqlRow other_element : other.rows) {
        __this__rows.add(new CqlRow(other_element));
      }
      this.rows = __this__rows;
    }
    this.num = other.num;
    if (other.isSetSchema()) {
      this.schema = new CqlMetadata(other.schema);
    }
  }

  public CqlResult deepCopy() {
    return new CqlResult(this);
  }

  @Override
  public void clear() {
    this.type = null;
    this.rows = null;
    setNumIsSet(false);
    this.num = 0;
    this.schema = null;
  }

  /**
   * 
   * @see CqlResultType
   */
  public CqlResultType getType() {
    return this.type;
  }

  /**
   * 
   * @see CqlResultType
   */
  public CqlResult setType(CqlResultType type) {
    this.type = type;
    return this;
  }

  public void unsetType() {
    this.type = null;
  }

  /** Returns true if field type is set (has been assigned a value) and false otherwise */
  public boolean isSetType() {
    return this.type != null;
  }

  public void setTypeIsSet(boolean value) {
    if (!value) {
      this.type = null;
    }
  }

  public int getRowsSize() {
    return (this.rows == null) ? 0 : this.rows.size();
  }

  public java.util.Iterator<CqlRow> getRowsIterator() {
    return (this.rows == null) ? null : this.rows.iterator();
  }

  public void addToRows(CqlRow elem) {
    if (this.rows == null) {
      this.rows = new ArrayList<CqlRow>();
    }
    this.rows.add(elem);
  }

  public List<CqlRow> getRows() {
    return this.rows;
  }

  public CqlResult setRows(List<CqlRow> rows) {
    this.rows = rows;
    return this;
  }

  public void unsetRows() {
    this.rows = null;
  }

  /** Returns true if field rows is set (has been assigned a value) and false otherwise */
  public boolean isSetRows() {
    return this.rows != null;
  }

  public void setRowsIsSet(boolean value) {
    if (!value) {
      this.rows = null;
    }
  }

  public int getNum() {
    return this.num;
  }

  public CqlResult setNum(int num) {
    this.num = num;
    setNumIsSet(true);
    return this;
  }

  public void unsetNum() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __NUM_ISSET_ID);
  }

  /** Returns true if field num is set (has been assigned a value) and false otherwise */
  public boolean isSetNum() {
    return EncodingUtils.testBit(__isset_bitfield, __NUM_ISSET_ID);
  }

  public void setNumIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __NUM_ISSET_ID, value);
  }

  public CqlMetadata getSchema() {
    return this.schema;
  }

  public CqlResult setSchema(CqlMetadata schema) {
    this.schema = schema;
    return this;
  }

  public void unsetSchema() {
    this.schema = null;
  }

  /** Returns true if field schema is set (has been assigned a value) and false otherwise */
  public boolean isSetSchema() {
    return this.schema != null;
  }

  public void setSchemaIsSet(boolean value) {
    if (!value) {
      this.schema = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case TYPE:
      if (value == null) {
        unsetType();
      } else {
        setType((CqlResultType)value);
      }
      break;

    case ROWS:
      if (value == null) {
        unsetRows();
      } else {
        setRows((List<CqlRow>)value);
      }
      break;

    case NUM:
      if (value == null) {
        unsetNum();
      } else {
        setNum((Integer)value);
      }
      break;

    case SCHEMA:
      if (value == null) {
        unsetSchema();
      } else {
        setSchema((CqlMetadata)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case TYPE:
      return getType();

    case ROWS:
      return getRows();

    case NUM:
      return Integer.valueOf(getNum());

    case SCHEMA:
      return getSchema();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case TYPE:
      return isSetType();
    case ROWS:
      return isSetRows();
    case NUM:
      return isSetNum();
    case SCHEMA:
      return isSetSchema();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CqlResult)
      return this.equals((CqlResult)that);
    return false;
  }

  public boolean equals(CqlResult that) {
    if (that == null)
      return false;

    boolean this_present_type = true && this.isSetType();
    boolean that_present_type = true && that.isSetType();
    if (this_present_type || that_present_type) {
      if (!(this_present_type && that_present_type))
        return false;
      if (!this.type.equals(that.type))
        return false;
    }

    boolean this_present_rows = true && this.isSetRows();
    boolean that_present_rows = true && that.isSetRows();
    if (this_present_rows || that_present_rows) {
      if (!(this_present_rows && that_present_rows))
        return false;
      if (!this.rows.equals(that.rows))
        return false;
    }

    boolean this_present_num = true && this.isSetNum();
    boolean that_present_num = true && that.isSetNum();
    if (this_present_num || that_present_num) {
      if (!(this_present_num && that_present_num))
        return false;
      if (this.num != that.num)
        return false;
    }

    boolean this_present_schema = true && this.isSetSchema();
    boolean that_present_schema = true && that.isSetSchema();
    if (this_present_schema || that_present_schema) {
      if (!(this_present_schema && that_present_schema))
        return false;
      if (!this.schema.equals(that.schema))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_type = true && (isSetType());
    builder.append(present_type);
    if (present_type)
      builder.append(type.getValue());

    boolean present_rows = true && (isSetRows());
    builder.append(present_rows);
    if (present_rows)
      builder.append(rows);

    boolean present_num = true && (isSetNum());
    builder.append(present_num);
    if (present_num)
      builder.append(num);

    boolean present_schema = true && (isSetSchema());
    builder.append(present_schema);
    if (present_schema)
      builder.append(schema);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(CqlResult other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetType()).compareTo(other.isSetType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.type, other.type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRows()).compareTo(other.isSetRows());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRows()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.rows, other.rows);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetNum()).compareTo(other.isSetNum());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetNum()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.num, other.num);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSchema()).compareTo(other.isSetSchema());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSchema()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.schema, other.schema);
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
    StringBuilder sb = new StringBuilder("CqlResult(");
    boolean first = true;

    sb.append("type:");
    if (this.type == null) {
      sb.append("null");
    } else {
      sb.append(this.type);
    }
    first = false;
    if (isSetRows()) {
      if (!first) sb.append(", ");
      sb.append("rows:");
      if (this.rows == null) {
        sb.append("null");
      } else {
        sb.append(this.rows);
      }
      first = false;
    }
    if (isSetNum()) {
      if (!first) sb.append(", ");
      sb.append("num:");
      sb.append(this.num);
      first = false;
    }
    if (isSetSchema()) {
      if (!first) sb.append(", ");
      sb.append("schema:");
      if (this.schema == null) {
        sb.append("null");
      } else {
        sb.append(this.schema);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (type == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'type' was not present! Struct: " + toString());
    }
    // check for sub-struct validity
    if (schema != null) {
      schema.validate();
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

  private static class CqlResultStandardSchemeFactory implements SchemeFactory {
    public CqlResultStandardScheme getScheme() {
      return new CqlResultStandardScheme();
    }
  }

  private static class CqlResultStandardScheme extends StandardScheme<CqlResult> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CqlResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.type = CqlResultType.findByValue(iprot.readI32());
              struct.setTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // ROWS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list192 = iprot.readListBegin();
                struct.rows = new ArrayList<CqlRow>(_list192.size);
                for (int _i193 = 0; _i193 < _list192.size; ++_i193)
                {
                  CqlRow _elem194;
                  _elem194 = new CqlRow();
                  _elem194.read(iprot);
                  struct.rows.add(_elem194);
                }
                iprot.readListEnd();
              }
              struct.setRowsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // NUM
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.num = iprot.readI32();
              struct.setNumIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // SCHEMA
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.schema = new CqlMetadata();
              struct.schema.read(iprot);
              struct.setSchemaIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, CqlResult struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.type != null) {
        oprot.writeFieldBegin(TYPE_FIELD_DESC);
        oprot.writeI32(struct.type.getValue());
        oprot.writeFieldEnd();
      }
      if (struct.rows != null) {
        if (struct.isSetRows()) {
          oprot.writeFieldBegin(ROWS_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.rows.size()));
            for (CqlRow _iter195 : struct.rows)
            {
              _iter195.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetNum()) {
        oprot.writeFieldBegin(NUM_FIELD_DESC);
        oprot.writeI32(struct.num);
        oprot.writeFieldEnd();
      }
      if (struct.schema != null) {
        if (struct.isSetSchema()) {
          oprot.writeFieldBegin(SCHEMA_FIELD_DESC);
          struct.schema.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CqlResultTupleSchemeFactory implements SchemeFactory {
    public CqlResultTupleScheme getScheme() {
      return new CqlResultTupleScheme();
    }
  }

  private static class CqlResultTupleScheme extends TupleScheme<CqlResult> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CqlResult struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeI32(struct.type.getValue());
      BitSet optionals = new BitSet();
      if (struct.isSetRows()) {
        optionals.set(0);
      }
      if (struct.isSetNum()) {
        optionals.set(1);
      }
      if (struct.isSetSchema()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetRows()) {
        {
          oprot.writeI32(struct.rows.size());
          for (CqlRow _iter196 : struct.rows)
          {
            _iter196.write(oprot);
          }
        }
      }
      if (struct.isSetNum()) {
        oprot.writeI32(struct.num);
      }
      if (struct.isSetSchema()) {
        struct.schema.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CqlResult struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.type = CqlResultType.findByValue(iprot.readI32());
      struct.setTypeIsSet(true);
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list197 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.rows = new ArrayList<CqlRow>(_list197.size);
          for (int _i198 = 0; _i198 < _list197.size; ++_i198)
          {
            CqlRow _elem199;
            _elem199 = new CqlRow();
            _elem199.read(iprot);
            struct.rows.add(_elem199);
          }
        }
        struct.setRowsIsSet(true);
      }
      if (incoming.get(1)) {
        struct.num = iprot.readI32();
        struct.setNumIsSet(true);
      }
      if (incoming.get(2)) {
        struct.schema = new CqlMetadata();
        struct.schema.read(iprot);
        struct.setSchemaIsSet(true);
      }
    }
  }

}

