import org.apache.cassandra.thrift.ColumnOrSuperColumn;
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

/**
 * A KeySlice is key followed by the data it maps to. A collection of KeySlice is returned by the get_range_slice operation.
 * 
 * @param key. a row key
 * @param columns. List of data represented by the key. Typically, the list is pared down to only the columns specified by
 *                 a SlicePredicate.
 */
public class KeySlice implements org.apache.thrift.TBase<KeySlice, KeySlice._Fields>, java.io.Serializable, Cloneable, Comparable<KeySlice> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("KeySlice");

  private static final org.apache.thrift.protocol.TField KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("key", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField COLUMNS_FIELD_DESC = new org.apache.thrift.protocol.TField("columns", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new KeySliceStandardSchemeFactory());
    schemes.put(TupleScheme.class, new KeySliceTupleSchemeFactory());
  }

  public ByteBuffer key; // required
  public List<ColumnOrSuperColumn> columns; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    KEY((short)1, "key"),
    COLUMNS((short)2, "columns");

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
        case 1: // KEY
          return KEY;
        case 2: // COLUMNS
          return COLUMNS;
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
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.KEY, new org.apache.thrift.meta_data.FieldMetaData("key", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.COLUMNS, new org.apache.thrift.meta_data.FieldMetaData("columns", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ColumnOrSuperColumn.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(KeySlice.class, metaDataMap);
  }

  public KeySlice() {
  }

  public KeySlice(
    ByteBuffer key,
    List<ColumnOrSuperColumn> columns)
  {
    this();
    this.key = key;
    this.columns = columns;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public KeySlice(KeySlice other) {
    if (other.isSetKey()) {
      this.key = org.apache.thrift.TBaseHelper.copyBinary(other.key);
;
    }
    if (other.isSetColumns()) {
      List<ColumnOrSuperColumn> __this__columns = new ArrayList<ColumnOrSuperColumn>(other.columns.size());
      for (ColumnOrSuperColumn other_element : other.columns) {
        __this__columns.add(new ColumnOrSuperColumn(other_element));
      }
      this.columns = __this__columns;
    }
  }

  public KeySlice deepCopy() {
    return new KeySlice(this);
  }

  @Override
  public void clear() {
    this.key = null;
    this.columns = null;
  }

  public byte[] getKey() {
    setKey(org.apache.thrift.TBaseHelper.rightSize(key));
    return key == null ? null : key.array();
  }

  public ByteBuffer bufferForKey() {
    return key;
  }

  public KeySlice setKey(byte[] key) {
    setKey(key == null ? (ByteBuffer)null : ByteBuffer.wrap(key));
    return this;
  }

  public KeySlice setKey(ByteBuffer key) {
    this.key = key;
    return this;
  }

  public void unsetKey() {
    this.key = null;
  }

  /** Returns true if field key is set (has been assigned a value) and false otherwise */
  public boolean isSetKey() {
    return this.key != null;
  }

  public void setKeyIsSet(boolean value) {
    if (!value) {
      this.key = null;
    }
  }

  public int getColumnsSize() {
    return (this.columns == null) ? 0 : this.columns.size();
  }

  public java.util.Iterator<ColumnOrSuperColumn> getColumnsIterator() {
    return (this.columns == null) ? null : this.columns.iterator();
  }

  public void addToColumns(ColumnOrSuperColumn elem) {
    if (this.columns == null) {
      this.columns = new ArrayList<ColumnOrSuperColumn>();
    }
    this.columns.add(elem);
  }

  public List<ColumnOrSuperColumn> getColumns() {
    return this.columns;
  }

  public KeySlice setColumns(List<ColumnOrSuperColumn> columns) {
    this.columns = columns;
    return this;
  }

  public void unsetColumns() {
    this.columns = null;
  }

  /** Returns true if field columns is set (has been assigned a value) and false otherwise */
  public boolean isSetColumns() {
    return this.columns != null;
  }

  public void setColumnsIsSet(boolean value) {
    if (!value) {
      this.columns = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case KEY:
      if (value == null) {
        unsetKey();
      } else {
        setKey((ByteBuffer)value);
      }
      break;

    case COLUMNS:
      if (value == null) {
        unsetColumns();
      } else {
        setColumns((List<ColumnOrSuperColumn>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case KEY:
      return getKey();

    case COLUMNS:
      return getColumns();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case KEY:
      return isSetKey();
    case COLUMNS:
      return isSetColumns();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof KeySlice)
      return this.equals((KeySlice)that);
    return false;
  }

  public boolean equals(KeySlice that) {
    if (that == null)
      return false;

    boolean this_present_key = true && this.isSetKey();
    boolean that_present_key = true && that.isSetKey();
    if (this_present_key || that_present_key) {
      if (!(this_present_key && that_present_key))
        return false;
      if (!this.key.equals(that.key))
        return false;
    }

    boolean this_present_columns = true && this.isSetColumns();
    boolean that_present_columns = true && that.isSetColumns();
    if (this_present_columns || that_present_columns) {
      if (!(this_present_columns && that_present_columns))
        return false;
      if (!this.columns.equals(that.columns))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_key = true && (isSetKey());
    builder.append(present_key);
    if (present_key)
      builder.append(key);

    boolean present_columns = true && (isSetColumns());
    builder.append(present_columns);
    if (present_columns)
      builder.append(columns);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(KeySlice other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetKey()).compareTo(other.isSetKey());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key, other.key);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetColumns()).compareTo(other.isSetColumns());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumns()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.columns, other.columns);
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
    StringBuilder sb = new StringBuilder("KeySlice(");
    boolean first = true;

    sb.append("key:");
    if (this.key == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.key, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("columns:");
    if (this.columns == null) {
      sb.append("null");
    } else {
      sb.append(this.columns);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (key == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'key' was not present! Struct: " + toString());
    }
    if (columns == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'columns' was not present! Struct: " + toString());
    }
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class KeySliceStandardSchemeFactory implements SchemeFactory {
    public KeySliceStandardScheme getScheme() {
      return new KeySliceStandardScheme();
    }
  }

  private static class KeySliceStandardScheme extends StandardScheme<KeySlice> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, KeySlice struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // KEY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.key = iprot.readBinary();
              struct.setKeyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // COLUMNS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list40 = iprot.readListBegin();
                struct.columns = new ArrayList<ColumnOrSuperColumn>(_list40.size);
                for (int _i41 = 0; _i41 < _list40.size; ++_i41)
                {
                  ColumnOrSuperColumn _elem42;
                  _elem42 = new ColumnOrSuperColumn();
                  _elem42.read(iprot);
                  struct.columns.add(_elem42);
                }
                iprot.readListEnd();
              }
              struct.setColumnsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, KeySlice struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.key != null) {
        oprot.writeFieldBegin(KEY_FIELD_DESC);
        oprot.writeBinary(struct.key);
        oprot.writeFieldEnd();
      }
      if (struct.columns != null) {
        oprot.writeFieldBegin(COLUMNS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.columns.size()));
          for (ColumnOrSuperColumn _iter43 : struct.columns)
          {
            _iter43.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class KeySliceTupleSchemeFactory implements SchemeFactory {
    public KeySliceTupleScheme getScheme() {
      return new KeySliceTupleScheme();
    }
  }

  private static class KeySliceTupleScheme extends TupleScheme<KeySlice> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, KeySlice struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeBinary(struct.key);
      {
        oprot.writeI32(struct.columns.size());
        for (ColumnOrSuperColumn _iter44 : struct.columns)
        {
          _iter44.write(oprot);
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, KeySlice struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.key = iprot.readBinary();
      struct.setKeyIsSet(true);
      {
        org.apache.thrift.protocol.TList _list45 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
        struct.columns = new ArrayList<ColumnOrSuperColumn>(_list45.size);
        for (int _i46 = 0; _i46 < _list45.size; ++_i46)
        {
          ColumnOrSuperColumn _elem47;
          _elem47 = new ColumnOrSuperColumn();
          _elem47.read(iprot);
          struct.columns.add(_elem47);
        }
      }
      struct.setColumnsIsSet(true);
    }
  }

}

