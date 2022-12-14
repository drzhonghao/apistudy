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
 * RPC timeout was exceeded.  either a node failed mid-operation, or load was too high, or the requested op was too large.
 */
public class TimedOutException extends TException implements org.apache.thrift.TBase<TimedOutException, TimedOutException._Fields>, java.io.Serializable, Cloneable, Comparable<TimedOutException> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TimedOutException");

  private static final org.apache.thrift.protocol.TField ACKNOWLEDGED_BY_FIELD_DESC = new org.apache.thrift.protocol.TField("acknowledged_by", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField ACKNOWLEDGED_BY_BATCHLOG_FIELD_DESC = new org.apache.thrift.protocol.TField("acknowledged_by_batchlog", org.apache.thrift.protocol.TType.BOOL, (short)2);
  private static final org.apache.thrift.protocol.TField PAXOS_IN_PROGRESS_FIELD_DESC = new org.apache.thrift.protocol.TField("paxos_in_progress", org.apache.thrift.protocol.TType.BOOL, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TimedOutExceptionStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TimedOutExceptionTupleSchemeFactory());
  }

  /**
   * if a write operation was acknowledged by some replicas but not by enough to
   * satisfy the required ConsistencyLevel, the number of successful
   * replies will be given here. In case of atomic_batch_mutate method this field
   * will be set to -1 if the batch was written to the batchlog and to 0 if it wasn't.
   */
  public int acknowledged_by; // optional
  /**
   * in case of atomic_batch_mutate method this field tells if the batch
   * was written to the batchlog.
   */
  public boolean acknowledged_by_batchlog; // optional
  /**
   * for the CAS method, this field tells if we timed out during the paxos
   * protocol, as opposed to during the commit of our update
   */
  public boolean paxos_in_progress; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    /**
     * if a write operation was acknowledged by some replicas but not by enough to
     * satisfy the required ConsistencyLevel, the number of successful
     * replies will be given here. In case of atomic_batch_mutate method this field
     * will be set to -1 if the batch was written to the batchlog and to 0 if it wasn't.
     */
    ACKNOWLEDGED_BY((short)1, "acknowledged_by"),
    /**
     * in case of atomic_batch_mutate method this field tells if the batch
     * was written to the batchlog.
     */
    ACKNOWLEDGED_BY_BATCHLOG((short)2, "acknowledged_by_batchlog"),
    /**
     * for the CAS method, this field tells if we timed out during the paxos
     * protocol, as opposed to during the commit of our update
     */
    PAXOS_IN_PROGRESS((short)3, "paxos_in_progress");

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
        case 1: // ACKNOWLEDGED_BY
          return ACKNOWLEDGED_BY;
        case 2: // ACKNOWLEDGED_BY_BATCHLOG
          return ACKNOWLEDGED_BY_BATCHLOG;
        case 3: // PAXOS_IN_PROGRESS
          return PAXOS_IN_PROGRESS;
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
  private static final int __ACKNOWLEDGED_BY_ISSET_ID = 0;
  private static final int __ACKNOWLEDGED_BY_BATCHLOG_ISSET_ID = 1;
  private static final int __PAXOS_IN_PROGRESS_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.ACKNOWLEDGED_BY,_Fields.ACKNOWLEDGED_BY_BATCHLOG,_Fields.PAXOS_IN_PROGRESS};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.ACKNOWLEDGED_BY, new org.apache.thrift.meta_data.FieldMetaData("acknowledged_by", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.ACKNOWLEDGED_BY_BATCHLOG, new org.apache.thrift.meta_data.FieldMetaData("acknowledged_by_batchlog", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.PAXOS_IN_PROGRESS, new org.apache.thrift.meta_data.FieldMetaData("paxos_in_progress", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TimedOutException.class, metaDataMap);
  }

  public TimedOutException() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TimedOutException(TimedOutException other) {
    __isset_bitfield = other.__isset_bitfield;
    this.acknowledged_by = other.acknowledged_by;
    this.acknowledged_by_batchlog = other.acknowledged_by_batchlog;
    this.paxos_in_progress = other.paxos_in_progress;
  }

  public TimedOutException deepCopy() {
    return new TimedOutException(this);
  }

  @Override
  public void clear() {
    setAcknowledged_byIsSet(false);
    this.acknowledged_by = 0;
    setAcknowledged_by_batchlogIsSet(false);
    this.acknowledged_by_batchlog = false;
    setPaxos_in_progressIsSet(false);
    this.paxos_in_progress = false;
  }

  /**
   * if a write operation was acknowledged by some replicas but not by enough to
   * satisfy the required ConsistencyLevel, the number of successful
   * replies will be given here. In case of atomic_batch_mutate method this field
   * will be set to -1 if the batch was written to the batchlog and to 0 if it wasn't.
   */
  public int getAcknowledged_by() {
    return this.acknowledged_by;
  }

  /**
   * if a write operation was acknowledged by some replicas but not by enough to
   * satisfy the required ConsistencyLevel, the number of successful
   * replies will be given here. In case of atomic_batch_mutate method this field
   * will be set to -1 if the batch was written to the batchlog and to 0 if it wasn't.
   */
  public TimedOutException setAcknowledged_by(int acknowledged_by) {
    this.acknowledged_by = acknowledged_by;
    setAcknowledged_byIsSet(true);
    return this;
  }

  public void unsetAcknowledged_by() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ACKNOWLEDGED_BY_ISSET_ID);
  }

  /** Returns true if field acknowledged_by is set (has been assigned a value) and false otherwise */
  public boolean isSetAcknowledged_by() {
    return EncodingUtils.testBit(__isset_bitfield, __ACKNOWLEDGED_BY_ISSET_ID);
  }

  public void setAcknowledged_byIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ACKNOWLEDGED_BY_ISSET_ID, value);
  }

  /**
   * in case of atomic_batch_mutate method this field tells if the batch
   * was written to the batchlog.
   */
  public boolean isAcknowledged_by_batchlog() {
    return this.acknowledged_by_batchlog;
  }

  /**
   * in case of atomic_batch_mutate method this field tells if the batch
   * was written to the batchlog.
   */
  public TimedOutException setAcknowledged_by_batchlog(boolean acknowledged_by_batchlog) {
    this.acknowledged_by_batchlog = acknowledged_by_batchlog;
    setAcknowledged_by_batchlogIsSet(true);
    return this;
  }

  public void unsetAcknowledged_by_batchlog() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ACKNOWLEDGED_BY_BATCHLOG_ISSET_ID);
  }

  /** Returns true if field acknowledged_by_batchlog is set (has been assigned a value) and false otherwise */
  public boolean isSetAcknowledged_by_batchlog() {
    return EncodingUtils.testBit(__isset_bitfield, __ACKNOWLEDGED_BY_BATCHLOG_ISSET_ID);
  }

  public void setAcknowledged_by_batchlogIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ACKNOWLEDGED_BY_BATCHLOG_ISSET_ID, value);
  }

  /**
   * for the CAS method, this field tells if we timed out during the paxos
   * protocol, as opposed to during the commit of our update
   */
  public boolean isPaxos_in_progress() {
    return this.paxos_in_progress;
  }

  /**
   * for the CAS method, this field tells if we timed out during the paxos
   * protocol, as opposed to during the commit of our update
   */
  public TimedOutException setPaxos_in_progress(boolean paxos_in_progress) {
    this.paxos_in_progress = paxos_in_progress;
    setPaxos_in_progressIsSet(true);
    return this;
  }

  public void unsetPaxos_in_progress() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __PAXOS_IN_PROGRESS_ISSET_ID);
  }

  /** Returns true if field paxos_in_progress is set (has been assigned a value) and false otherwise */
  public boolean isSetPaxos_in_progress() {
    return EncodingUtils.testBit(__isset_bitfield, __PAXOS_IN_PROGRESS_ISSET_ID);
  }

  public void setPaxos_in_progressIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __PAXOS_IN_PROGRESS_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case ACKNOWLEDGED_BY:
      if (value == null) {
        unsetAcknowledged_by();
      } else {
        setAcknowledged_by((Integer)value);
      }
      break;

    case ACKNOWLEDGED_BY_BATCHLOG:
      if (value == null) {
        unsetAcknowledged_by_batchlog();
      } else {
        setAcknowledged_by_batchlog((Boolean)value);
      }
      break;

    case PAXOS_IN_PROGRESS:
      if (value == null) {
        unsetPaxos_in_progress();
      } else {
        setPaxos_in_progress((Boolean)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case ACKNOWLEDGED_BY:
      return Integer.valueOf(getAcknowledged_by());

    case ACKNOWLEDGED_BY_BATCHLOG:
      return Boolean.valueOf(isAcknowledged_by_batchlog());

    case PAXOS_IN_PROGRESS:
      return Boolean.valueOf(isPaxos_in_progress());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case ACKNOWLEDGED_BY:
      return isSetAcknowledged_by();
    case ACKNOWLEDGED_BY_BATCHLOG:
      return isSetAcknowledged_by_batchlog();
    case PAXOS_IN_PROGRESS:
      return isSetPaxos_in_progress();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TimedOutException)
      return this.equals((TimedOutException)that);
    return false;
  }

  public boolean equals(TimedOutException that) {
    if (that == null)
      return false;

    boolean this_present_acknowledged_by = true && this.isSetAcknowledged_by();
    boolean that_present_acknowledged_by = true && that.isSetAcknowledged_by();
    if (this_present_acknowledged_by || that_present_acknowledged_by) {
      if (!(this_present_acknowledged_by && that_present_acknowledged_by))
        return false;
      if (this.acknowledged_by != that.acknowledged_by)
        return false;
    }

    boolean this_present_acknowledged_by_batchlog = true && this.isSetAcknowledged_by_batchlog();
    boolean that_present_acknowledged_by_batchlog = true && that.isSetAcknowledged_by_batchlog();
    if (this_present_acknowledged_by_batchlog || that_present_acknowledged_by_batchlog) {
      if (!(this_present_acknowledged_by_batchlog && that_present_acknowledged_by_batchlog))
        return false;
      if (this.acknowledged_by_batchlog != that.acknowledged_by_batchlog)
        return false;
    }

    boolean this_present_paxos_in_progress = true && this.isSetPaxos_in_progress();
    boolean that_present_paxos_in_progress = true && that.isSetPaxos_in_progress();
    if (this_present_paxos_in_progress || that_present_paxos_in_progress) {
      if (!(this_present_paxos_in_progress && that_present_paxos_in_progress))
        return false;
      if (this.paxos_in_progress != that.paxos_in_progress)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_acknowledged_by = true && (isSetAcknowledged_by());
    builder.append(present_acknowledged_by);
    if (present_acknowledged_by)
      builder.append(acknowledged_by);

    boolean present_acknowledged_by_batchlog = true && (isSetAcknowledged_by_batchlog());
    builder.append(present_acknowledged_by_batchlog);
    if (present_acknowledged_by_batchlog)
      builder.append(acknowledged_by_batchlog);

    boolean present_paxos_in_progress = true && (isSetPaxos_in_progress());
    builder.append(present_paxos_in_progress);
    if (present_paxos_in_progress)
      builder.append(paxos_in_progress);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(TimedOutException other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetAcknowledged_by()).compareTo(other.isSetAcknowledged_by());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAcknowledged_by()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.acknowledged_by, other.acknowledged_by);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAcknowledged_by_batchlog()).compareTo(other.isSetAcknowledged_by_batchlog());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAcknowledged_by_batchlog()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.acknowledged_by_batchlog, other.acknowledged_by_batchlog);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetPaxos_in_progress()).compareTo(other.isSetPaxos_in_progress());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPaxos_in_progress()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.paxos_in_progress, other.paxos_in_progress);
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
    StringBuilder sb = new StringBuilder("TimedOutException(");
    boolean first = true;

    if (isSetAcknowledged_by()) {
      sb.append("acknowledged_by:");
      sb.append(this.acknowledged_by);
      first = false;
    }
    if (isSetAcknowledged_by_batchlog()) {
      if (!first) sb.append(", ");
      sb.append("acknowledged_by_batchlog:");
      sb.append(this.acknowledged_by_batchlog);
      first = false;
    }
    if (isSetPaxos_in_progress()) {
      if (!first) sb.append(", ");
      sb.append("paxos_in_progress:");
      sb.append(this.paxos_in_progress);
      first = false;
    }
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

  private static class TimedOutExceptionStandardSchemeFactory implements SchemeFactory {
    public TimedOutExceptionStandardScheme getScheme() {
      return new TimedOutExceptionStandardScheme();
    }
  }

  private static class TimedOutExceptionStandardScheme extends StandardScheme<TimedOutException> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TimedOutException struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // ACKNOWLEDGED_BY
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.acknowledged_by = iprot.readI32();
              struct.setAcknowledged_byIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // ACKNOWLEDGED_BY_BATCHLOG
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.acknowledged_by_batchlog = iprot.readBool();
              struct.setAcknowledged_by_batchlogIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // PAXOS_IN_PROGRESS
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.paxos_in_progress = iprot.readBool();
              struct.setPaxos_in_progressIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TimedOutException struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.isSetAcknowledged_by()) {
        oprot.writeFieldBegin(ACKNOWLEDGED_BY_FIELD_DESC);
        oprot.writeI32(struct.acknowledged_by);
        oprot.writeFieldEnd();
      }
      if (struct.isSetAcknowledged_by_batchlog()) {
        oprot.writeFieldBegin(ACKNOWLEDGED_BY_BATCHLOG_FIELD_DESC);
        oprot.writeBool(struct.acknowledged_by_batchlog);
        oprot.writeFieldEnd();
      }
      if (struct.isSetPaxos_in_progress()) {
        oprot.writeFieldBegin(PAXOS_IN_PROGRESS_FIELD_DESC);
        oprot.writeBool(struct.paxos_in_progress);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TimedOutExceptionTupleSchemeFactory implements SchemeFactory {
    public TimedOutExceptionTupleScheme getScheme() {
      return new TimedOutExceptionTupleScheme();
    }
  }

  private static class TimedOutExceptionTupleScheme extends TupleScheme<TimedOutException> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TimedOutException struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetAcknowledged_by()) {
        optionals.set(0);
      }
      if (struct.isSetAcknowledged_by_batchlog()) {
        optionals.set(1);
      }
      if (struct.isSetPaxos_in_progress()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetAcknowledged_by()) {
        oprot.writeI32(struct.acknowledged_by);
      }
      if (struct.isSetAcknowledged_by_batchlog()) {
        oprot.writeBool(struct.acknowledged_by_batchlog);
      }
      if (struct.isSetPaxos_in_progress()) {
        oprot.writeBool(struct.paxos_in_progress);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TimedOutException struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.acknowledged_by = iprot.readI32();
        struct.setAcknowledged_byIsSet(true);
      }
      if (incoming.get(1)) {
        struct.acknowledged_by_batchlog = iprot.readBool();
        struct.setAcknowledged_by_batchlogIsSet(true);
      }
      if (incoming.get(2)) {
        struct.paxos_in_progress = iprot.readBool();
        struct.setPaxos_in_progressIsSet(true);
      }
    }
  }

}

