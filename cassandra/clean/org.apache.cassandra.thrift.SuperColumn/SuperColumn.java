import org.apache.cassandra.thrift.Column;
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
 * A named list of columns.
 * @param name. see Column.name.
 * @param columns. A collection of standard Columns.  The columns within a super column are defined in an adhoc manner.
 *                 Columns within a super column do not have to have matching structures (similarly named child columns).
 */
public class SuperColumn implements org.apache.thrift.TBase<SuperColumn, SuperColumn._Fields>, java.io.Serializable, Cloneable, Comparable<SuperColumn> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("SuperColumn");

  private static final org.apache.thrift.protocol.TField NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("name", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField COLUMNS_FIELD_DESC = new org.apache.thrift.protocol.TField("columns", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new SuperColumnStandardSchemeFactory());
    schemes.put(TupleScheme.class, new SuperColumnTupleSchemeFactory());
  }

  public ByteBuffer name; // required
  public List<Column> columns; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    NAME((short)1, "name"),
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
        case 1: // NAME
          return NAME;
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
    tmpMap.put(_Fields.NAME, new org.apache.thrift.meta_data.FieldMetaData("name", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.COLUMNS, new org.apache.thrift.meta_data.FieldMetaData("columns", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Column.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(SuperColumn.class, metaDataMap);
  }

  public SuperColumn() {
  }

  public SuperColumn(
    ByteBuffer name,
    List<Column> columns)
  {
    this();
    this.name = name;
    this.columns = columns;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public SuperColumn(SuperColumn other) {
    if (other.isSetName()) {
      this.name = org.apache.thrift.TBaseHelper.copyBinary(other.name);
;
    }
    if (other.isSetColumns()) {
      List<Column> __this__columns = new ArrayList<Column>(other.columns.size());
      for (Column other_element : other.columns) {
        __this__columns.add(new Column(other_element));
      }
      this.columns = __this__columns;
    }
  }

  public SuperColumn deepCopy() {
    return new SuperColumn(this);
  }

  @Override
  public void clear() {
    this.name = null;
    this.columns = null;
  }

  public byte[] getName() {
    setName(org.apache.thrift.TBaseHelper.rightSize(name));
    return name == null ? null : name.array();
  }

  public ByteBuffer bufferForName() {
    return name;
  }

  public SuperColumn setName(byte[] name) {
    setName(name == null ? (ByteBuffer)null : ByteBuffer.wrap(name));
    return this;
  }

  public SuperColumn setName(ByteBuffer name) {
    this.name = name;
    return this;
  }

  public void unsetName() {
    this.name = null;
  }

  /** Returns true if field name is set (has been assigned a value) and false otherwise */
  public boolean isSetName() {
    return this.name != null;
  }

  public void setNameIsSet(boolean value) {
    if (!value) {
      this.name = null;
    }
  }

  public int getColumnsSize() {
    return (this.columns == null) ? 0 : this.columns.size();
  }

  public java.util.Iterator<Column> getColumnsIterator() {
    return (this.columns == null) ? null : this.columns.iterator();
  }

  public void addToColumns(Column elem) {
    if (this.columns == null) {
      this.columns = new ArrayList<Column>();
    }
    this.columns.add(elem);
  }

  public List<Column> getColumns() {
    return this.columns;
  }

  public SuperColumn setColumns(List<Column> columns) {
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
    case NAME:
      if (value == null) {
        unsetName();
      } else {
        setName((ByteBuffer)value);
      }
      break;

    case COLUMNS:
      if (value == null) {
        unsetColumns();
      } else {
        setColumns((List<Column>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case NAME:
      return getName();

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
    case NAME:
      return isSetName();
    case COLUMNS:
      return isSetColumns();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof SuperColumn)
      return this.equals((SuperColumn)that);
    return false;
  }

  public boolean equals(SuperColumn that) {
    if (that == null)
      return false;

    boolean this_present_name = true && this.isSetName();
    boolean that_present_name = true && that.isSetName();
    if (this_present_name || that_present_name) {
      if (!(this_present_name && that_present_name))
        return false;
      if (!this.name.equals(that.name))
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

    boolean present_name = true && (isSetName());
    builder.append(present_name);
    if (present_name)
      builder.append(name);

    boolean present_columns = true && (isSetColumns());
    builder.append(present_columns);
    if (present_columns)
      builder.append(columns);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(SuperColumn other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetName()).compareTo(other.isSetName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.name, other.name);
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
    StringBuilder sb = new StringBuilder("SuperColumn(");
    boolean first = true;

    sb.append("name:");
    if (this.name == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.name, sb);
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
    if (name == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'name' was not present! Struct: " + toString());
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

  private static class SuperColumnStandardSchemeFactory implements SchemeFactory {
    public SuperColumnStandardScheme getScheme() {
      return new SuperColumnStandardScheme();
    }
  }

  private static class SuperColumnStandardScheme extends StandardScheme<SuperColumn> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, SuperColumn struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.name = iprot.readBinary();
              struct.setNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // COLUMNS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.columns = new ArrayList<Column>(_list0.size);
                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                {
                  Column _elem2;
                  _elem2 = new Column();
                  _elem2.read(iprot);
                  struct.columns.add(_elem2);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, SuperColumn struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.name != null) {
        oprot.writeFieldBegin(NAME_FIELD_DESC);
        oprot.writeBinary(struct.name);
        oprot.writeFieldEnd();
      }
      if (struct.columns != null) {
        oprot.writeFieldBegin(COLUMNS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.columns.size()));
          for (Column _iter3 : struct.columns)
          {
            _iter3.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class SuperColumnTupleSchemeFactory implements SchemeFactory {
    public SuperColumnTupleScheme getScheme() {
      return new SuperColumnTupleScheme();
    }
  }

  private static class SuperColumnTupleScheme extends TupleScheme<SuperColumn> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, SuperColumn struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeBinary(struct.name);
      {
        oprot.writeI32(struct.columns.size());
        for (Column _iter4 : struct.columns)
        {
          _iter4.write(oprot);
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, SuperColumn struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.name = iprot.readBinary();
      struct.setNameIsSet(true);
      {
        org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
        struct.columns = new ArrayList<Column>(_list5.size);
        for (int _i6 = 0; _i6 < _list5.size; ++_i6)
        {
          Column _elem7;
          _elem7 = new Column();
          _elem7.read(iprot);
          struct.columns.add(_elem7);
        }
      }
      struct.setColumnsIsSet(true);
    }
  }

}

