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
 * ColumnParent is used when selecting groups of columns from the same ColumnFamily. In directory structure terms, imagine
 * ColumnParent as ColumnPath + '/../'.
 * 
 * See also <a href="cassandra.html#Struct_ColumnPath">ColumnPath</a>
 */
public class ColumnParent implements org.apache.thrift.TBase<ColumnParent, ColumnParent._Fields>, java.io.Serializable, Cloneable, Comparable<ColumnParent> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ColumnParent");

  private static final org.apache.thrift.protocol.TField COLUMN_FAMILY_FIELD_DESC = new org.apache.thrift.protocol.TField("column_family", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField SUPER_COLUMN_FIELD_DESC = new org.apache.thrift.protocol.TField("super_column", org.apache.thrift.protocol.TType.STRING, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ColumnParentStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ColumnParentTupleSchemeFactory());
  }

  public String column_family; // required
  public ByteBuffer super_column; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    COLUMN_FAMILY((short)3, "column_family"),
    SUPER_COLUMN((short)4, "super_column");

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
        case 3: // COLUMN_FAMILY
          return COLUMN_FAMILY;
        case 4: // SUPER_COLUMN
          return SUPER_COLUMN;
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
  private _Fields optionals[] = {_Fields.SUPER_COLUMN};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.COLUMN_FAMILY, new org.apache.thrift.meta_data.FieldMetaData("column_family", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.SUPER_COLUMN, new org.apache.thrift.meta_data.FieldMetaData("super_column", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ColumnParent.class, metaDataMap);
  }

  public ColumnParent() {
  }

  public ColumnParent(
    String column_family)
  {
    this();
    this.column_family = column_family;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ColumnParent(ColumnParent other) {
    if (other.isSetColumn_family()) {
      this.column_family = other.column_family;
    }
    if (other.isSetSuper_column()) {
      this.super_column = org.apache.thrift.TBaseHelper.copyBinary(other.super_column);
;
    }
  }

  public ColumnParent deepCopy() {
    return new ColumnParent(this);
  }

  @Override
  public void clear() {
    this.column_family = null;
    this.super_column = null;
  }

  public String getColumn_family() {
    return this.column_family;
  }

  public ColumnParent setColumn_family(String column_family) {
    this.column_family = column_family;
    return this;
  }

  public void unsetColumn_family() {
    this.column_family = null;
  }

  /** Returns true if field column_family is set (has been assigned a value) and false otherwise */
  public boolean isSetColumn_family() {
    return this.column_family != null;
  }

  public void setColumn_familyIsSet(boolean value) {
    if (!value) {
      this.column_family = null;
    }
  }

  public byte[] getSuper_column() {
    setSuper_column(org.apache.thrift.TBaseHelper.rightSize(super_column));
    return super_column == null ? null : super_column.array();
  }

  public ByteBuffer bufferForSuper_column() {
    return super_column;
  }

  public ColumnParent setSuper_column(byte[] super_column) {
    setSuper_column(super_column == null ? (ByteBuffer)null : ByteBuffer.wrap(super_column));
    return this;
  }

  public ColumnParent setSuper_column(ByteBuffer super_column) {
    this.super_column = super_column;
    return this;
  }

  public void unsetSuper_column() {
    this.super_column = null;
  }

  /** Returns true if field super_column is set (has been assigned a value) and false otherwise */
  public boolean isSetSuper_column() {
    return this.super_column != null;
  }

  public void setSuper_columnIsSet(boolean value) {
    if (!value) {
      this.super_column = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case COLUMN_FAMILY:
      if (value == null) {
        unsetColumn_family();
      } else {
        setColumn_family((String)value);
      }
      break;

    case SUPER_COLUMN:
      if (value == null) {
        unsetSuper_column();
      } else {
        setSuper_column((ByteBuffer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case COLUMN_FAMILY:
      return getColumn_family();

    case SUPER_COLUMN:
      return getSuper_column();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case COLUMN_FAMILY:
      return isSetColumn_family();
    case SUPER_COLUMN:
      return isSetSuper_column();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ColumnParent)
      return this.equals((ColumnParent)that);
    return false;
  }

  public boolean equals(ColumnParent that) {
    if (that == null)
      return false;

    boolean this_present_column_family = true && this.isSetColumn_family();
    boolean that_present_column_family = true && that.isSetColumn_family();
    if (this_present_column_family || that_present_column_family) {
      if (!(this_present_column_family && that_present_column_family))
        return false;
      if (!this.column_family.equals(that.column_family))
        return false;
    }

    boolean this_present_super_column = true && this.isSetSuper_column();
    boolean that_present_super_column = true && that.isSetSuper_column();
    if (this_present_super_column || that_present_super_column) {
      if (!(this_present_super_column && that_present_super_column))
        return false;
      if (!this.super_column.equals(that.super_column))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_column_family = true && (isSetColumn_family());
    builder.append(present_column_family);
    if (present_column_family)
      builder.append(column_family);

    boolean present_super_column = true && (isSetSuper_column());
    builder.append(present_super_column);
    if (present_super_column)
      builder.append(super_column);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(ColumnParent other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetColumn_family()).compareTo(other.isSetColumn_family());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_family()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.column_family, other.column_family);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSuper_column()).compareTo(other.isSetSuper_column());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuper_column()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.super_column, other.super_column);
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
    StringBuilder sb = new StringBuilder("ColumnParent(");
    boolean first = true;

    sb.append("column_family:");
    if (this.column_family == null) {
      sb.append("null");
    } else {
      sb.append(this.column_family);
    }
    first = false;
    if (isSetSuper_column()) {
      if (!first) sb.append(", ");
      sb.append("super_column:");
      if (this.super_column == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.super_column, sb);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (column_family == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'column_family' was not present! Struct: " + toString());
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

  private static class ColumnParentStandardSchemeFactory implements SchemeFactory {
    public ColumnParentStandardScheme getScheme() {
      return new ColumnParentStandardScheme();
    }
  }

  private static class ColumnParentStandardScheme extends StandardScheme<ColumnParent> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ColumnParent struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 3: // COLUMN_FAMILY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.column_family = iprot.readString();
              struct.setColumn_familyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // SUPER_COLUMN
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.super_column = iprot.readBinary();
              struct.setSuper_columnIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ColumnParent struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.column_family != null) {
        oprot.writeFieldBegin(COLUMN_FAMILY_FIELD_DESC);
        oprot.writeString(struct.column_family);
        oprot.writeFieldEnd();
      }
      if (struct.super_column != null) {
        if (struct.isSetSuper_column()) {
          oprot.writeFieldBegin(SUPER_COLUMN_FIELD_DESC);
          oprot.writeBinary(struct.super_column);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ColumnParentTupleSchemeFactory implements SchemeFactory {
    public ColumnParentTupleScheme getScheme() {
      return new ColumnParentTupleScheme();
    }
  }

  private static class ColumnParentTupleScheme extends TupleScheme<ColumnParent> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ColumnParent struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeString(struct.column_family);
      BitSet optionals = new BitSet();
      if (struct.isSetSuper_column()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetSuper_column()) {
        oprot.writeBinary(struct.super_column);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ColumnParent struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.column_family = iprot.readString();
      struct.setColumn_familyIsSet(true);
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.super_column = iprot.readBinary();
        struct.setSuper_columnIsSet(true);
      }
    }
  }

}

