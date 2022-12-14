import org.apache.cassandra.thrift.SlicePredicate;
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
 * Note that the timestamp is only optional in case of counter deletion.
 */
public class Deletion implements org.apache.thrift.TBase<Deletion, Deletion._Fields>, java.io.Serializable, Cloneable, Comparable<Deletion> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Deletion");

  private static final org.apache.thrift.protocol.TField TIMESTAMP_FIELD_DESC = new org.apache.thrift.protocol.TField("timestamp", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField SUPER_COLUMN_FIELD_DESC = new org.apache.thrift.protocol.TField("super_column", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField PREDICATE_FIELD_DESC = new org.apache.thrift.protocol.TField("predicate", org.apache.thrift.protocol.TType.STRUCT, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new DeletionStandardSchemeFactory());
    schemes.put(TupleScheme.class, new DeletionTupleSchemeFactory());
  }

  public long timestamp; // optional
  public ByteBuffer super_column; // optional
  public SlicePredicate predicate; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    TIMESTAMP((short)1, "timestamp"),
    SUPER_COLUMN((short)2, "super_column"),
    PREDICATE((short)3, "predicate");

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
        case 1: // TIMESTAMP
          return TIMESTAMP;
        case 2: // SUPER_COLUMN
          return SUPER_COLUMN;
        case 3: // PREDICATE
          return PREDICATE;
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
  private static final int __TIMESTAMP_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.TIMESTAMP,_Fields.SUPER_COLUMN,_Fields.PREDICATE};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.TIMESTAMP, new org.apache.thrift.meta_data.FieldMetaData("timestamp", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.SUPER_COLUMN, new org.apache.thrift.meta_data.FieldMetaData("super_column", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.PREDICATE, new org.apache.thrift.meta_data.FieldMetaData("predicate", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, SlicePredicate.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Deletion.class, metaDataMap);
  }

  public Deletion() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Deletion(Deletion other) {
    __isset_bitfield = other.__isset_bitfield;
    this.timestamp = other.timestamp;
    if (other.isSetSuper_column()) {
      this.super_column = org.apache.thrift.TBaseHelper.copyBinary(other.super_column);
;
    }
    if (other.isSetPredicate()) {
      this.predicate = new SlicePredicate(other.predicate);
    }
  }

  public Deletion deepCopy() {
    return new Deletion(this);
  }

  @Override
  public void clear() {
    setTimestampIsSet(false);
    this.timestamp = 0;
    this.super_column = null;
    this.predicate = null;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public Deletion setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    setTimestampIsSet(true);
    return this;
  }

  public void unsetTimestamp() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __TIMESTAMP_ISSET_ID);
  }

  /** Returns true if field timestamp is set (has been assigned a value) and false otherwise */
  public boolean isSetTimestamp() {
    return EncodingUtils.testBit(__isset_bitfield, __TIMESTAMP_ISSET_ID);
  }

  public void setTimestampIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __TIMESTAMP_ISSET_ID, value);
  }

  public byte[] getSuper_column() {
    setSuper_column(org.apache.thrift.TBaseHelper.rightSize(super_column));
    return super_column == null ? null : super_column.array();
  }

  public ByteBuffer bufferForSuper_column() {
    return super_column;
  }

  public Deletion setSuper_column(byte[] super_column) {
    setSuper_column(super_column == null ? (ByteBuffer)null : ByteBuffer.wrap(super_column));
    return this;
  }

  public Deletion setSuper_column(ByteBuffer super_column) {
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

  public SlicePredicate getPredicate() {
    return this.predicate;
  }

  public Deletion setPredicate(SlicePredicate predicate) {
    this.predicate = predicate;
    return this;
  }

  public void unsetPredicate() {
    this.predicate = null;
  }

  /** Returns true if field predicate is set (has been assigned a value) and false otherwise */
  public boolean isSetPredicate() {
    return this.predicate != null;
  }

  public void setPredicateIsSet(boolean value) {
    if (!value) {
      this.predicate = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case TIMESTAMP:
      if (value == null) {
        unsetTimestamp();
      } else {
        setTimestamp((Long)value);
      }
      break;

    case SUPER_COLUMN:
      if (value == null) {
        unsetSuper_column();
      } else {
        setSuper_column((ByteBuffer)value);
      }
      break;

    case PREDICATE:
      if (value == null) {
        unsetPredicate();
      } else {
        setPredicate((SlicePredicate)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case TIMESTAMP:
      return Long.valueOf(getTimestamp());

    case SUPER_COLUMN:
      return getSuper_column();

    case PREDICATE:
      return getPredicate();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case TIMESTAMP:
      return isSetTimestamp();
    case SUPER_COLUMN:
      return isSetSuper_column();
    case PREDICATE:
      return isSetPredicate();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Deletion)
      return this.equals((Deletion)that);
    return false;
  }

  public boolean equals(Deletion that) {
    if (that == null)
      return false;

    boolean this_present_timestamp = true && this.isSetTimestamp();
    boolean that_present_timestamp = true && that.isSetTimestamp();
    if (this_present_timestamp || that_present_timestamp) {
      if (!(this_present_timestamp && that_present_timestamp))
        return false;
      if (this.timestamp != that.timestamp)
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

    boolean this_present_predicate = true && this.isSetPredicate();
    boolean that_present_predicate = true && that.isSetPredicate();
    if (this_present_predicate || that_present_predicate) {
      if (!(this_present_predicate && that_present_predicate))
        return false;
      if (!this.predicate.equals(that.predicate))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_timestamp = true && (isSetTimestamp());
    builder.append(present_timestamp);
    if (present_timestamp)
      builder.append(timestamp);

    boolean present_super_column = true && (isSetSuper_column());
    builder.append(present_super_column);
    if (present_super_column)
      builder.append(super_column);

    boolean present_predicate = true && (isSetPredicate());
    builder.append(present_predicate);
    if (present_predicate)
      builder.append(predicate);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(Deletion other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetTimestamp()).compareTo(other.isSetTimestamp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTimestamp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.timestamp, other.timestamp);
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
    lastComparison = Boolean.valueOf(isSetPredicate()).compareTo(other.isSetPredicate());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPredicate()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.predicate, other.predicate);
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
    StringBuilder sb = new StringBuilder("Deletion(");
    boolean first = true;

    if (isSetTimestamp()) {
      sb.append("timestamp:");
      sb.append(this.timestamp);
      first = false;
    }
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
    if (isSetPredicate()) {
      if (!first) sb.append(", ");
      sb.append("predicate:");
      if (this.predicate == null) {
        sb.append("null");
      } else {
        sb.append(this.predicate);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (predicate != null) {
      predicate.validate();
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

  private static class DeletionStandardSchemeFactory implements SchemeFactory {
    public DeletionStandardScheme getScheme() {
      return new DeletionStandardScheme();
    }
  }

  private static class DeletionStandardScheme extends StandardScheme<Deletion> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Deletion struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // TIMESTAMP
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.timestamp = iprot.readI64();
              struct.setTimestampIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // SUPER_COLUMN
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.super_column = iprot.readBinary();
              struct.setSuper_columnIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // PREDICATE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.predicate = new SlicePredicate();
              struct.predicate.read(iprot);
              struct.setPredicateIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, Deletion struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.isSetTimestamp()) {
        oprot.writeFieldBegin(TIMESTAMP_FIELD_DESC);
        oprot.writeI64(struct.timestamp);
        oprot.writeFieldEnd();
      }
      if (struct.super_column != null) {
        if (struct.isSetSuper_column()) {
          oprot.writeFieldBegin(SUPER_COLUMN_FIELD_DESC);
          oprot.writeBinary(struct.super_column);
          oprot.writeFieldEnd();
        }
      }
      if (struct.predicate != null) {
        if (struct.isSetPredicate()) {
          oprot.writeFieldBegin(PREDICATE_FIELD_DESC);
          struct.predicate.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class DeletionTupleSchemeFactory implements SchemeFactory {
    public DeletionTupleScheme getScheme() {
      return new DeletionTupleScheme();
    }
  }

  private static class DeletionTupleScheme extends TupleScheme<Deletion> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Deletion struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetTimestamp()) {
        optionals.set(0);
      }
      if (struct.isSetSuper_column()) {
        optionals.set(1);
      }
      if (struct.isSetPredicate()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetTimestamp()) {
        oprot.writeI64(struct.timestamp);
      }
      if (struct.isSetSuper_column()) {
        oprot.writeBinary(struct.super_column);
      }
      if (struct.isSetPredicate()) {
        struct.predicate.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Deletion struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.timestamp = iprot.readI64();
        struct.setTimestampIsSet(true);
      }
      if (incoming.get(1)) {
        struct.super_column = iprot.readBinary();
        struct.setSuper_columnIsSet(true);
      }
      if (incoming.get(2)) {
        struct.predicate = new SlicePredicate();
        struct.predicate.read(iprot);
        struct.setPredicateIsSet(true);
      }
    }
  }

}

