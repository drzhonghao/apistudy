import org.apache.cassandra.thrift.IndexExpression;
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
 * @deprecated use a KeyRange with row_filter in get_range_slices instead
 */
public class IndexClause implements org.apache.thrift.TBase<IndexClause, IndexClause._Fields>, java.io.Serializable, Cloneable, Comparable<IndexClause> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("IndexClause");

  private static final org.apache.thrift.protocol.TField EXPRESSIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("expressions", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField START_KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("start_key", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("count", org.apache.thrift.protocol.TType.I32, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new IndexClauseStandardSchemeFactory());
    schemes.put(TupleScheme.class, new IndexClauseTupleSchemeFactory());
  }

  public List<IndexExpression> expressions; // required
  public ByteBuffer start_key; // required
  public int count; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    EXPRESSIONS((short)1, "expressions"),
    START_KEY((short)2, "start_key"),
    COUNT((short)3, "count");

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
        case 1: // EXPRESSIONS
          return EXPRESSIONS;
        case 2: // START_KEY
          return START_KEY;
        case 3: // COUNT
          return COUNT;
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
  private static final int __COUNT_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.EXPRESSIONS, new org.apache.thrift.meta_data.FieldMetaData("expressions", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, IndexExpression.class))));
    tmpMap.put(_Fields.START_KEY, new org.apache.thrift.meta_data.FieldMetaData("start_key", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.COUNT, new org.apache.thrift.meta_data.FieldMetaData("count", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(IndexClause.class, metaDataMap);
  }

  public IndexClause() {
    this.count = 100;

  }

  public IndexClause(
    List<IndexExpression> expressions,
    ByteBuffer start_key,
    int count)
  {
    this();
    this.expressions = expressions;
    this.start_key = start_key;
    this.count = count;
    setCountIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public IndexClause(IndexClause other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetExpressions()) {
      List<IndexExpression> __this__expressions = new ArrayList<IndexExpression>(other.expressions.size());
      for (IndexExpression other_element : other.expressions) {
        __this__expressions.add(new IndexExpression(other_element));
      }
      this.expressions = __this__expressions;
    }
    if (other.isSetStart_key()) {
      this.start_key = org.apache.thrift.TBaseHelper.copyBinary(other.start_key);
;
    }
    this.count = other.count;
  }

  public IndexClause deepCopy() {
    return new IndexClause(this);
  }

  @Override
  public void clear() {
    this.expressions = null;
    this.start_key = null;
    this.count = 100;

  }

  public int getExpressionsSize() {
    return (this.expressions == null) ? 0 : this.expressions.size();
  }

  public java.util.Iterator<IndexExpression> getExpressionsIterator() {
    return (this.expressions == null) ? null : this.expressions.iterator();
  }

  public void addToExpressions(IndexExpression elem) {
    if (this.expressions == null) {
      this.expressions = new ArrayList<IndexExpression>();
    }
    this.expressions.add(elem);
  }

  public List<IndexExpression> getExpressions() {
    return this.expressions;
  }

  public IndexClause setExpressions(List<IndexExpression> expressions) {
    this.expressions = expressions;
    return this;
  }

  public void unsetExpressions() {
    this.expressions = null;
  }

  /** Returns true if field expressions is set (has been assigned a value) and false otherwise */
  public boolean isSetExpressions() {
    return this.expressions != null;
  }

  public void setExpressionsIsSet(boolean value) {
    if (!value) {
      this.expressions = null;
    }
  }

  public byte[] getStart_key() {
    setStart_key(org.apache.thrift.TBaseHelper.rightSize(start_key));
    return start_key == null ? null : start_key.array();
  }

  public ByteBuffer bufferForStart_key() {
    return start_key;
  }

  public IndexClause setStart_key(byte[] start_key) {
    setStart_key(start_key == null ? (ByteBuffer)null : ByteBuffer.wrap(start_key));
    return this;
  }

  public IndexClause setStart_key(ByteBuffer start_key) {
    this.start_key = start_key;
    return this;
  }

  public void unsetStart_key() {
    this.start_key = null;
  }

  /** Returns true if field start_key is set (has been assigned a value) and false otherwise */
  public boolean isSetStart_key() {
    return this.start_key != null;
  }

  public void setStart_keyIsSet(boolean value) {
    if (!value) {
      this.start_key = null;
    }
  }

  public int getCount() {
    return this.count;
  }

  public IndexClause setCount(int count) {
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

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case EXPRESSIONS:
      if (value == null) {
        unsetExpressions();
      } else {
        setExpressions((List<IndexExpression>)value);
      }
      break;

    case START_KEY:
      if (value == null) {
        unsetStart_key();
      } else {
        setStart_key((ByteBuffer)value);
      }
      break;

    case COUNT:
      if (value == null) {
        unsetCount();
      } else {
        setCount((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case EXPRESSIONS:
      return getExpressions();

    case START_KEY:
      return getStart_key();

    case COUNT:
      return Integer.valueOf(getCount());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case EXPRESSIONS:
      return isSetExpressions();
    case START_KEY:
      return isSetStart_key();
    case COUNT:
      return isSetCount();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof IndexClause)
      return this.equals((IndexClause)that);
    return false;
  }

  public boolean equals(IndexClause that) {
    if (that == null)
      return false;

    boolean this_present_expressions = true && this.isSetExpressions();
    boolean that_present_expressions = true && that.isSetExpressions();
    if (this_present_expressions || that_present_expressions) {
      if (!(this_present_expressions && that_present_expressions))
        return false;
      if (!this.expressions.equals(that.expressions))
        return false;
    }

    boolean this_present_start_key = true && this.isSetStart_key();
    boolean that_present_start_key = true && that.isSetStart_key();
    if (this_present_start_key || that_present_start_key) {
      if (!(this_present_start_key && that_present_start_key))
        return false;
      if (!this.start_key.equals(that.start_key))
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

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_expressions = true && (isSetExpressions());
    builder.append(present_expressions);
    if (present_expressions)
      builder.append(expressions);

    boolean present_start_key = true && (isSetStart_key());
    builder.append(present_start_key);
    if (present_start_key)
      builder.append(start_key);

    boolean present_count = true;
    builder.append(present_count);
    if (present_count)
      builder.append(count);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(IndexClause other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetExpressions()).compareTo(other.isSetExpressions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExpressions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.expressions, other.expressions);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetStart_key()).compareTo(other.isSetStart_key());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStart_key()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.start_key, other.start_key);
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
    StringBuilder sb = new StringBuilder("IndexClause(");
    boolean first = true;

    sb.append("expressions:");
    if (this.expressions == null) {
      sb.append("null");
    } else {
      sb.append(this.expressions);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("start_key:");
    if (this.start_key == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.start_key, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("count:");
    sb.append(this.count);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (expressions == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'expressions' was not present! Struct: " + toString());
    }
    if (start_key == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'start_key' was not present! Struct: " + toString());
    }
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

  private static class IndexClauseStandardSchemeFactory implements SchemeFactory {
    public IndexClauseStandardScheme getScheme() {
      return new IndexClauseStandardScheme();
    }
  }

  private static class IndexClauseStandardScheme extends StandardScheme<IndexClause> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, IndexClause struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // EXPRESSIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list24 = iprot.readListBegin();
                struct.expressions = new ArrayList<IndexExpression>(_list24.size);
                for (int _i25 = 0; _i25 < _list24.size; ++_i25)
                {
                  IndexExpression _elem26;
                  _elem26 = new IndexExpression();
                  _elem26.read(iprot);
                  struct.expressions.add(_elem26);
                }
                iprot.readListEnd();
              }
              struct.setExpressionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // START_KEY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.start_key = iprot.readBinary();
              struct.setStart_keyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.count = iprot.readI32();
              struct.setCountIsSet(true);
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
      if (!struct.isSetCount()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'count' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, IndexClause struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.expressions != null) {
        oprot.writeFieldBegin(EXPRESSIONS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.expressions.size()));
          for (IndexExpression _iter27 : struct.expressions)
          {
            _iter27.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.start_key != null) {
        oprot.writeFieldBegin(START_KEY_FIELD_DESC);
        oprot.writeBinary(struct.start_key);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(COUNT_FIELD_DESC);
      oprot.writeI32(struct.count);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class IndexClauseTupleSchemeFactory implements SchemeFactory {
    public IndexClauseTupleScheme getScheme() {
      return new IndexClauseTupleScheme();
    }
  }

  private static class IndexClauseTupleScheme extends TupleScheme<IndexClause> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, IndexClause struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.expressions.size());
        for (IndexExpression _iter28 : struct.expressions)
        {
          _iter28.write(oprot);
        }
      }
      oprot.writeBinary(struct.start_key);
      oprot.writeI32(struct.count);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, IndexClause struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TList _list29 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
        struct.expressions = new ArrayList<IndexExpression>(_list29.size);
        for (int _i30 = 0; _i30 < _list29.size; ++_i30)
        {
          IndexExpression _elem31;
          _elem31 = new IndexExpression();
          _elem31.read(iprot);
          struct.expressions.add(_elem31);
        }
      }
      struct.setExpressionsIsSet(true);
      struct.start_key = iprot.readBinary();
      struct.setStart_keyIsSet(true);
      struct.count = iprot.readI32();
      struct.setCountIsSet(true);
    }
  }

}

