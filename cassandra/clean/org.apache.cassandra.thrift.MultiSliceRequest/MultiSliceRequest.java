import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnSlice;
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
 * Used to perform multiple slices on a single row key in one rpc operation
 * @param key. The row key to be multi sliced
 * @param column_parent. The column family (super columns are unsupported)
 * @param column_slices. 0 to many ColumnSlice objects each will be used to select columns
 * @param reversed. Direction of slice
 * @param count. Maximum number of columns
 * @param consistency_level. Level to perform the operation at
 */
public class MultiSliceRequest implements org.apache.thrift.TBase<MultiSliceRequest, MultiSliceRequest._Fields>, java.io.Serializable, Cloneable, Comparable<MultiSliceRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("MultiSliceRequest");

  private static final org.apache.thrift.protocol.TField KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("key", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField COLUMN_PARENT_FIELD_DESC = new org.apache.thrift.protocol.TField("column_parent", org.apache.thrift.protocol.TType.STRUCT, (short)2);
  private static final org.apache.thrift.protocol.TField COLUMN_SLICES_FIELD_DESC = new org.apache.thrift.protocol.TField("column_slices", org.apache.thrift.protocol.TType.LIST, (short)3);
  private static final org.apache.thrift.protocol.TField REVERSED_FIELD_DESC = new org.apache.thrift.protocol.TField("reversed", org.apache.thrift.protocol.TType.BOOL, (short)4);
  private static final org.apache.thrift.protocol.TField COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("count", org.apache.thrift.protocol.TType.I32, (short)5);
  private static final org.apache.thrift.protocol.TField CONSISTENCY_LEVEL_FIELD_DESC = new org.apache.thrift.protocol.TField("consistency_level", org.apache.thrift.protocol.TType.I32, (short)6);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new MultiSliceRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new MultiSliceRequestTupleSchemeFactory());
  }

  public ByteBuffer key; // optional
  public ColumnParent column_parent; // optional
  public List<ColumnSlice> column_slices; // optional
  public boolean reversed; // optional
  public int count; // optional
  /**
   * 
   * @see ConsistencyLevel
   */
  public ConsistencyLevel consistency_level; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    KEY((short)1, "key"),
    COLUMN_PARENT((short)2, "column_parent"),
    COLUMN_SLICES((short)3, "column_slices"),
    REVERSED((short)4, "reversed"),
    COUNT((short)5, "count"),
    /**
     * 
     * @see ConsistencyLevel
     */
    CONSISTENCY_LEVEL((short)6, "consistency_level");

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
        case 2: // COLUMN_PARENT
          return COLUMN_PARENT;
        case 3: // COLUMN_SLICES
          return COLUMN_SLICES;
        case 4: // REVERSED
          return REVERSED;
        case 5: // COUNT
          return COUNT;
        case 6: // CONSISTENCY_LEVEL
          return CONSISTENCY_LEVEL;
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
  private static final int __REVERSED_ISSET_ID = 0;
  private static final int __COUNT_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.KEY,_Fields.COLUMN_PARENT,_Fields.COLUMN_SLICES,_Fields.REVERSED,_Fields.COUNT,_Fields.CONSISTENCY_LEVEL};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.KEY, new org.apache.thrift.meta_data.FieldMetaData("key", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.COLUMN_PARENT, new org.apache.thrift.meta_data.FieldMetaData("column_parent", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ColumnParent.class)));
    tmpMap.put(_Fields.COLUMN_SLICES, new org.apache.thrift.meta_data.FieldMetaData("column_slices", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ColumnSlice.class))));
    tmpMap.put(_Fields.REVERSED, new org.apache.thrift.meta_data.FieldMetaData("reversed", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.COUNT, new org.apache.thrift.meta_data.FieldMetaData("count", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.CONSISTENCY_LEVEL, new org.apache.thrift.meta_data.FieldMetaData("consistency_level", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, ConsistencyLevel.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(MultiSliceRequest.class, metaDataMap);
  }

  public MultiSliceRequest() {
    this.reversed = false;

    this.count = 1000;

    this.consistency_level = org.apache.cassandra.thrift.ConsistencyLevel.ONE;

  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public MultiSliceRequest(MultiSliceRequest other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetKey()) {
      this.key = org.apache.thrift.TBaseHelper.copyBinary(other.key);
;
    }
    if (other.isSetColumn_parent()) {
      this.column_parent = new ColumnParent(other.column_parent);
    }
    if (other.isSetColumn_slices()) {
      List<ColumnSlice> __this__column_slices = new ArrayList<ColumnSlice>(other.column_slices.size());
      for (ColumnSlice other_element : other.column_slices) {
        __this__column_slices.add(new ColumnSlice(other_element));
      }
      this.column_slices = __this__column_slices;
    }
    this.reversed = other.reversed;
    this.count = other.count;
    if (other.isSetConsistency_level()) {
      this.consistency_level = other.consistency_level;
    }
  }

  public MultiSliceRequest deepCopy() {
    return new MultiSliceRequest(this);
  }

  @Override
  public void clear() {
    this.key = null;
    this.column_parent = null;
    this.column_slices = null;
    this.reversed = false;

    this.count = 1000;

    this.consistency_level = org.apache.cassandra.thrift.ConsistencyLevel.ONE;

  }

  public byte[] getKey() {
    setKey(org.apache.thrift.TBaseHelper.rightSize(key));
    return key == null ? null : key.array();
  }

  public ByteBuffer bufferForKey() {
    return key;
  }

  public MultiSliceRequest setKey(byte[] key) {
    setKey(key == null ? (ByteBuffer)null : ByteBuffer.wrap(key));
    return this;
  }

  public MultiSliceRequest setKey(ByteBuffer key) {
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

  public ColumnParent getColumn_parent() {
    return this.column_parent;
  }

  public MultiSliceRequest setColumn_parent(ColumnParent column_parent) {
    this.column_parent = column_parent;
    return this;
  }

  public void unsetColumn_parent() {
    this.column_parent = null;
  }

  /** Returns true if field column_parent is set (has been assigned a value) and false otherwise */
  public boolean isSetColumn_parent() {
    return this.column_parent != null;
  }

  public void setColumn_parentIsSet(boolean value) {
    if (!value) {
      this.column_parent = null;
    }
  }

  public int getColumn_slicesSize() {
    return (this.column_slices == null) ? 0 : this.column_slices.size();
  }

  public java.util.Iterator<ColumnSlice> getColumn_slicesIterator() {
    return (this.column_slices == null) ? null : this.column_slices.iterator();
  }

  public void addToColumn_slices(ColumnSlice elem) {
    if (this.column_slices == null) {
      this.column_slices = new ArrayList<ColumnSlice>();
    }
    this.column_slices.add(elem);
  }

  public List<ColumnSlice> getColumn_slices() {
    return this.column_slices;
  }

  public MultiSliceRequest setColumn_slices(List<ColumnSlice> column_slices) {
    this.column_slices = column_slices;
    return this;
  }

  public void unsetColumn_slices() {
    this.column_slices = null;
  }

  /** Returns true if field column_slices is set (has been assigned a value) and false otherwise */
  public boolean isSetColumn_slices() {
    return this.column_slices != null;
  }

  public void setColumn_slicesIsSet(boolean value) {
    if (!value) {
      this.column_slices = null;
    }
  }

  public boolean isReversed() {
    return this.reversed;
  }

  public MultiSliceRequest setReversed(boolean reversed) {
    this.reversed = reversed;
    setReversedIsSet(true);
    return this;
  }

  public void unsetReversed() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __REVERSED_ISSET_ID);
  }

  /** Returns true if field reversed is set (has been assigned a value) and false otherwise */
  public boolean isSetReversed() {
    return EncodingUtils.testBit(__isset_bitfield, __REVERSED_ISSET_ID);
  }

  public void setReversedIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __REVERSED_ISSET_ID, value);
  }

  public int getCount() {
    return this.count;
  }

  public MultiSliceRequest setCount(int count) {
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

  /**
   * 
   * @see ConsistencyLevel
   */
  public ConsistencyLevel getConsistency_level() {
    return this.consistency_level;
  }

  /**
   * 
   * @see ConsistencyLevel
   */
  public MultiSliceRequest setConsistency_level(ConsistencyLevel consistency_level) {
    this.consistency_level = consistency_level;
    return this;
  }

  public void unsetConsistency_level() {
    this.consistency_level = null;
  }

  /** Returns true if field consistency_level is set (has been assigned a value) and false otherwise */
  public boolean isSetConsistency_level() {
    return this.consistency_level != null;
  }

  public void setConsistency_levelIsSet(boolean value) {
    if (!value) {
      this.consistency_level = null;
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

    case COLUMN_PARENT:
      if (value == null) {
        unsetColumn_parent();
      } else {
        setColumn_parent((ColumnParent)value);
      }
      break;

    case COLUMN_SLICES:
      if (value == null) {
        unsetColumn_slices();
      } else {
        setColumn_slices((List<ColumnSlice>)value);
      }
      break;

    case REVERSED:
      if (value == null) {
        unsetReversed();
      } else {
        setReversed((Boolean)value);
      }
      break;

    case COUNT:
      if (value == null) {
        unsetCount();
      } else {
        setCount((Integer)value);
      }
      break;

    case CONSISTENCY_LEVEL:
      if (value == null) {
        unsetConsistency_level();
      } else {
        setConsistency_level((ConsistencyLevel)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case KEY:
      return getKey();

    case COLUMN_PARENT:
      return getColumn_parent();

    case COLUMN_SLICES:
      return getColumn_slices();

    case REVERSED:
      return Boolean.valueOf(isReversed());

    case COUNT:
      return Integer.valueOf(getCount());

    case CONSISTENCY_LEVEL:
      return getConsistency_level();

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
    case COLUMN_PARENT:
      return isSetColumn_parent();
    case COLUMN_SLICES:
      return isSetColumn_slices();
    case REVERSED:
      return isSetReversed();
    case COUNT:
      return isSetCount();
    case CONSISTENCY_LEVEL:
      return isSetConsistency_level();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof MultiSliceRequest)
      return this.equals((MultiSliceRequest)that);
    return false;
  }

  public boolean equals(MultiSliceRequest that) {
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

    boolean this_present_column_parent = true && this.isSetColumn_parent();
    boolean that_present_column_parent = true && that.isSetColumn_parent();
    if (this_present_column_parent || that_present_column_parent) {
      if (!(this_present_column_parent && that_present_column_parent))
        return false;
      if (!this.column_parent.equals(that.column_parent))
        return false;
    }

    boolean this_present_column_slices = true && this.isSetColumn_slices();
    boolean that_present_column_slices = true && that.isSetColumn_slices();
    if (this_present_column_slices || that_present_column_slices) {
      if (!(this_present_column_slices && that_present_column_slices))
        return false;
      if (!this.column_slices.equals(that.column_slices))
        return false;
    }

    boolean this_present_reversed = true && this.isSetReversed();
    boolean that_present_reversed = true && that.isSetReversed();
    if (this_present_reversed || that_present_reversed) {
      if (!(this_present_reversed && that_present_reversed))
        return false;
      if (this.reversed != that.reversed)
        return false;
    }

    boolean this_present_count = true && this.isSetCount();
    boolean that_present_count = true && that.isSetCount();
    if (this_present_count || that_present_count) {
      if (!(this_present_count && that_present_count))
        return false;
      if (this.count != that.count)
        return false;
    }

    boolean this_present_consistency_level = true && this.isSetConsistency_level();
    boolean that_present_consistency_level = true && that.isSetConsistency_level();
    if (this_present_consistency_level || that_present_consistency_level) {
      if (!(this_present_consistency_level && that_present_consistency_level))
        return false;
      if (!this.consistency_level.equals(that.consistency_level))
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

    boolean present_column_parent = true && (isSetColumn_parent());
    builder.append(present_column_parent);
    if (present_column_parent)
      builder.append(column_parent);

    boolean present_column_slices = true && (isSetColumn_slices());
    builder.append(present_column_slices);
    if (present_column_slices)
      builder.append(column_slices);

    boolean present_reversed = true && (isSetReversed());
    builder.append(present_reversed);
    if (present_reversed)
      builder.append(reversed);

    boolean present_count = true && (isSetCount());
    builder.append(present_count);
    if (present_count)
      builder.append(count);

    boolean present_consistency_level = true && (isSetConsistency_level());
    builder.append(present_consistency_level);
    if (present_consistency_level)
      builder.append(consistency_level.getValue());

    return builder.toHashCode();
  }

  @Override
  public int compareTo(MultiSliceRequest other) {
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
    lastComparison = Boolean.valueOf(isSetColumn_parent()).compareTo(other.isSetColumn_parent());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_parent()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.column_parent, other.column_parent);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetColumn_slices()).compareTo(other.isSetColumn_slices());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_slices()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.column_slices, other.column_slices);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetReversed()).compareTo(other.isSetReversed());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetReversed()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.reversed, other.reversed);
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
    lastComparison = Boolean.valueOf(isSetConsistency_level()).compareTo(other.isSetConsistency_level());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetConsistency_level()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.consistency_level, other.consistency_level);
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
    StringBuilder sb = new StringBuilder("MultiSliceRequest(");
    boolean first = true;

    if (isSetKey()) {
      sb.append("key:");
      if (this.key == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.key, sb);
      }
      first = false;
    }
    if (isSetColumn_parent()) {
      if (!first) sb.append(", ");
      sb.append("column_parent:");
      if (this.column_parent == null) {
        sb.append("null");
      } else {
        sb.append(this.column_parent);
      }
      first = false;
    }
    if (isSetColumn_slices()) {
      if (!first) sb.append(", ");
      sb.append("column_slices:");
      if (this.column_slices == null) {
        sb.append("null");
      } else {
        sb.append(this.column_slices);
      }
      first = false;
    }
    if (isSetReversed()) {
      if (!first) sb.append(", ");
      sb.append("reversed:");
      sb.append(this.reversed);
      first = false;
    }
    if (isSetCount()) {
      if (!first) sb.append(", ");
      sb.append("count:");
      sb.append(this.count);
      first = false;
    }
    if (isSetConsistency_level()) {
      if (!first) sb.append(", ");
      sb.append("consistency_level:");
      if (this.consistency_level == null) {
        sb.append("null");
      } else {
        sb.append(this.consistency_level);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (column_parent != null) {
      column_parent.validate();
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

  private static class MultiSliceRequestStandardSchemeFactory implements SchemeFactory {
    public MultiSliceRequestStandardScheme getScheme() {
      return new MultiSliceRequestStandardScheme();
    }
  }

  private static class MultiSliceRequestStandardScheme extends StandardScheme<MultiSliceRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, MultiSliceRequest struct) throws org.apache.thrift.TException {
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
          case 2: // COLUMN_PARENT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.column_parent = new ColumnParent();
              struct.column_parent.read(iprot);
              struct.setColumn_parentIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // COLUMN_SLICES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list216 = iprot.readListBegin();
                struct.column_slices = new ArrayList<ColumnSlice>(_list216.size);
                for (int _i217 = 0; _i217 < _list216.size; ++_i217)
                {
                  ColumnSlice _elem218;
                  _elem218 = new ColumnSlice();
                  _elem218.read(iprot);
                  struct.column_slices.add(_elem218);
                }
                iprot.readListEnd();
              }
              struct.setColumn_slicesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // REVERSED
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.reversed = iprot.readBool();
              struct.setReversedIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.count = iprot.readI32();
              struct.setCountIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // CONSISTENCY_LEVEL
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.consistency_level = ConsistencyLevel.findByValue(iprot.readI32());
              struct.setConsistency_levelIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, MultiSliceRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.key != null) {
        if (struct.isSetKey()) {
          oprot.writeFieldBegin(KEY_FIELD_DESC);
          oprot.writeBinary(struct.key);
          oprot.writeFieldEnd();
        }
      }
      if (struct.column_parent != null) {
        if (struct.isSetColumn_parent()) {
          oprot.writeFieldBegin(COLUMN_PARENT_FIELD_DESC);
          struct.column_parent.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      if (struct.column_slices != null) {
        if (struct.isSetColumn_slices()) {
          oprot.writeFieldBegin(COLUMN_SLICES_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.column_slices.size()));
            for (ColumnSlice _iter219 : struct.column_slices)
            {
              _iter219.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetReversed()) {
        oprot.writeFieldBegin(REVERSED_FIELD_DESC);
        oprot.writeBool(struct.reversed);
        oprot.writeFieldEnd();
      }
      if (struct.isSetCount()) {
        oprot.writeFieldBegin(COUNT_FIELD_DESC);
        oprot.writeI32(struct.count);
        oprot.writeFieldEnd();
      }
      if (struct.consistency_level != null) {
        if (struct.isSetConsistency_level()) {
          oprot.writeFieldBegin(CONSISTENCY_LEVEL_FIELD_DESC);
          oprot.writeI32(struct.consistency_level.getValue());
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class MultiSliceRequestTupleSchemeFactory implements SchemeFactory {
    public MultiSliceRequestTupleScheme getScheme() {
      return new MultiSliceRequestTupleScheme();
    }
  }

  private static class MultiSliceRequestTupleScheme extends TupleScheme<MultiSliceRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, MultiSliceRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetKey()) {
        optionals.set(0);
      }
      if (struct.isSetColumn_parent()) {
        optionals.set(1);
      }
      if (struct.isSetColumn_slices()) {
        optionals.set(2);
      }
      if (struct.isSetReversed()) {
        optionals.set(3);
      }
      if (struct.isSetCount()) {
        optionals.set(4);
      }
      if (struct.isSetConsistency_level()) {
        optionals.set(5);
      }
      oprot.writeBitSet(optionals, 6);
      if (struct.isSetKey()) {
        oprot.writeBinary(struct.key);
      }
      if (struct.isSetColumn_parent()) {
        struct.column_parent.write(oprot);
      }
      if (struct.isSetColumn_slices()) {
        {
          oprot.writeI32(struct.column_slices.size());
          for (ColumnSlice _iter220 : struct.column_slices)
          {
            _iter220.write(oprot);
          }
        }
      }
      if (struct.isSetReversed()) {
        oprot.writeBool(struct.reversed);
      }
      if (struct.isSetCount()) {
        oprot.writeI32(struct.count);
      }
      if (struct.isSetConsistency_level()) {
        oprot.writeI32(struct.consistency_level.getValue());
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, MultiSliceRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(6);
      if (incoming.get(0)) {
        struct.key = iprot.readBinary();
        struct.setKeyIsSet(true);
      }
      if (incoming.get(1)) {
        struct.column_parent = new ColumnParent();
        struct.column_parent.read(iprot);
        struct.setColumn_parentIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TList _list221 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.column_slices = new ArrayList<ColumnSlice>(_list221.size);
          for (int _i222 = 0; _i222 < _list221.size; ++_i222)
          {
            ColumnSlice _elem223;
            _elem223 = new ColumnSlice();
            _elem223.read(iprot);
            struct.column_slices.add(_elem223);
          }
        }
        struct.setColumn_slicesIsSet(true);
      }
      if (incoming.get(3)) {
        struct.reversed = iprot.readBool();
        struct.setReversedIsSet(true);
      }
      if (incoming.get(4)) {
        struct.count = iprot.readI32();
        struct.setCountIsSet(true);
      }
      if (incoming.get(5)) {
        struct.consistency_level = ConsistencyLevel.findByValue(iprot.readI32());
        struct.setConsistency_levelIsSet(true);
      }
    }
  }

}

