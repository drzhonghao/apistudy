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
 * The ColumnSlice is used to select a set of columns from inside a row.
 * If start or finish are unspecified they will default to the start-of
 * end-of value.
 * @param start. The start of the ColumnSlice inclusive
 * @param finish. The end of the ColumnSlice inclusive
 */
public class ColumnSlice implements org.apache.thrift.TBase<ColumnSlice, ColumnSlice._Fields>, java.io.Serializable, Cloneable, Comparable<ColumnSlice> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ColumnSlice");

  private static final org.apache.thrift.protocol.TField START_FIELD_DESC = new org.apache.thrift.protocol.TField("start", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField FINISH_FIELD_DESC = new org.apache.thrift.protocol.TField("finish", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ColumnSliceStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ColumnSliceTupleSchemeFactory());
  }

  public ByteBuffer start; // optional
  public ByteBuffer finish; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    START((short)1, "start"),
    FINISH((short)2, "finish");

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
        case 1: // START
          return START;
        case 2: // FINISH
          return FINISH;
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
  private _Fields optionals[] = {_Fields.START,_Fields.FINISH};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.START, new org.apache.thrift.meta_data.FieldMetaData("start", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.FINISH, new org.apache.thrift.meta_data.FieldMetaData("finish", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ColumnSlice.class, metaDataMap);
  }

  public ColumnSlice() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ColumnSlice(ColumnSlice other) {
    if (other.isSetStart()) {
      this.start = org.apache.thrift.TBaseHelper.copyBinary(other.start);
;
    }
    if (other.isSetFinish()) {
      this.finish = org.apache.thrift.TBaseHelper.copyBinary(other.finish);
;
    }
  }

  public ColumnSlice deepCopy() {
    return new ColumnSlice(this);
  }

  @Override
  public void clear() {
    this.start = null;
    this.finish = null;
  }

  public byte[] getStart() {
    setStart(org.apache.thrift.TBaseHelper.rightSize(start));
    return start == null ? null : start.array();
  }

  public ByteBuffer bufferForStart() {
    return start;
  }

  public ColumnSlice setStart(byte[] start) {
    setStart(start == null ? (ByteBuffer)null : ByteBuffer.wrap(start));
    return this;
  }

  public ColumnSlice setStart(ByteBuffer start) {
    this.start = start;
    return this;
  }

  public void unsetStart() {
    this.start = null;
  }

  /** Returns true if field start is set (has been assigned a value) and false otherwise */
  public boolean isSetStart() {
    return this.start != null;
  }

  public void setStartIsSet(boolean value) {
    if (!value) {
      this.start = null;
    }
  }

  public byte[] getFinish() {
    setFinish(org.apache.thrift.TBaseHelper.rightSize(finish));
    return finish == null ? null : finish.array();
  }

  public ByteBuffer bufferForFinish() {
    return finish;
  }

  public ColumnSlice setFinish(byte[] finish) {
    setFinish(finish == null ? (ByteBuffer)null : ByteBuffer.wrap(finish));
    return this;
  }

  public ColumnSlice setFinish(ByteBuffer finish) {
    this.finish = finish;
    return this;
  }

  public void unsetFinish() {
    this.finish = null;
  }

  /** Returns true if field finish is set (has been assigned a value) and false otherwise */
  public boolean isSetFinish() {
    return this.finish != null;
  }

  public void setFinishIsSet(boolean value) {
    if (!value) {
      this.finish = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case START:
      if (value == null) {
        unsetStart();
      } else {
        setStart((ByteBuffer)value);
      }
      break;

    case FINISH:
      if (value == null) {
        unsetFinish();
      } else {
        setFinish((ByteBuffer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case START:
      return getStart();

    case FINISH:
      return getFinish();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case START:
      return isSetStart();
    case FINISH:
      return isSetFinish();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ColumnSlice)
      return this.equals((ColumnSlice)that);
    return false;
  }

  public boolean equals(ColumnSlice that) {
    if (that == null)
      return false;

    boolean this_present_start = true && this.isSetStart();
    boolean that_present_start = true && that.isSetStart();
    if (this_present_start || that_present_start) {
      if (!(this_present_start && that_present_start))
        return false;
      if (!this.start.equals(that.start))
        return false;
    }

    boolean this_present_finish = true && this.isSetFinish();
    boolean that_present_finish = true && that.isSetFinish();
    if (this_present_finish || that_present_finish) {
      if (!(this_present_finish && that_present_finish))
        return false;
      if (!this.finish.equals(that.finish))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_start = true && (isSetStart());
    builder.append(present_start);
    if (present_start)
      builder.append(start);

    boolean present_finish = true && (isSetFinish());
    builder.append(present_finish);
    if (present_finish)
      builder.append(finish);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(ColumnSlice other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetStart()).compareTo(other.isSetStart());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStart()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.start, other.start);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFinish()).compareTo(other.isSetFinish());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFinish()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.finish, other.finish);
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
    StringBuilder sb = new StringBuilder("ColumnSlice(");
    boolean first = true;

    if (isSetStart()) {
      sb.append("start:");
      if (this.start == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.start, sb);
      }
      first = false;
    }
    if (isSetFinish()) {
      if (!first) sb.append(", ");
      sb.append("finish:");
      if (this.finish == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.finish, sb);
      }
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ColumnSliceStandardSchemeFactory implements SchemeFactory {
    public ColumnSliceStandardScheme getScheme() {
      return new ColumnSliceStandardScheme();
    }
  }

  private static class ColumnSliceStandardScheme extends StandardScheme<ColumnSlice> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ColumnSlice struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // START
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.start = iprot.readBinary();
              struct.setStartIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // FINISH
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.finish = iprot.readBinary();
              struct.setFinishIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ColumnSlice struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.start != null) {
        if (struct.isSetStart()) {
          oprot.writeFieldBegin(START_FIELD_DESC);
          oprot.writeBinary(struct.start);
          oprot.writeFieldEnd();
        }
      }
      if (struct.finish != null) {
        if (struct.isSetFinish()) {
          oprot.writeFieldBegin(FINISH_FIELD_DESC);
          oprot.writeBinary(struct.finish);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ColumnSliceTupleSchemeFactory implements SchemeFactory {
    public ColumnSliceTupleScheme getScheme() {
      return new ColumnSliceTupleScheme();
    }
  }

  private static class ColumnSliceTupleScheme extends TupleScheme<ColumnSlice> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ColumnSlice struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetStart()) {
        optionals.set(0);
      }
      if (struct.isSetFinish()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetStart()) {
        oprot.writeBinary(struct.start);
      }
      if (struct.isSetFinish()) {
        oprot.writeBinary(struct.finish);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ColumnSlice struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.start = iprot.readBinary();
        struct.setStartIsSet(true);
      }
      if (incoming.get(1)) {
        struct.finish = iprot.readBinary();
        struct.setFinishIsSet(true);
      }
    }
  }

}

