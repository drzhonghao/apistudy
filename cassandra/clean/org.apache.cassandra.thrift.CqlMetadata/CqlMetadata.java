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

public class CqlMetadata implements org.apache.thrift.TBase<CqlMetadata, CqlMetadata._Fields>, java.io.Serializable, Cloneable, Comparable<CqlMetadata> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CqlMetadata");

  private static final org.apache.thrift.protocol.TField NAME_TYPES_FIELD_DESC = new org.apache.thrift.protocol.TField("name_types", org.apache.thrift.protocol.TType.MAP, (short)1);
  private static final org.apache.thrift.protocol.TField VALUE_TYPES_FIELD_DESC = new org.apache.thrift.protocol.TField("value_types", org.apache.thrift.protocol.TType.MAP, (short)2);
  private static final org.apache.thrift.protocol.TField DEFAULT_NAME_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("default_name_type", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField DEFAULT_VALUE_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("default_value_type", org.apache.thrift.protocol.TType.STRING, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CqlMetadataStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CqlMetadataTupleSchemeFactory());
  }

  public Map<ByteBuffer,String> name_types; // required
  public Map<ByteBuffer,String> value_types; // required
  public String default_name_type; // required
  public String default_value_type; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    NAME_TYPES((short)1, "name_types"),
    VALUE_TYPES((short)2, "value_types"),
    DEFAULT_NAME_TYPE((short)3, "default_name_type"),
    DEFAULT_VALUE_TYPE((short)4, "default_value_type");

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
        case 1: // NAME_TYPES
          return NAME_TYPES;
        case 2: // VALUE_TYPES
          return VALUE_TYPES;
        case 3: // DEFAULT_NAME_TYPE
          return DEFAULT_NAME_TYPE;
        case 4: // DEFAULT_VALUE_TYPE
          return DEFAULT_VALUE_TYPE;
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
    tmpMap.put(_Fields.NAME_TYPES, new org.apache.thrift.meta_data.FieldMetaData("name_types", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING            , true), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.VALUE_TYPES, new org.apache.thrift.meta_data.FieldMetaData("value_types", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING            , true), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.DEFAULT_NAME_TYPE, new org.apache.thrift.meta_data.FieldMetaData("default_name_type", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.DEFAULT_VALUE_TYPE, new org.apache.thrift.meta_data.FieldMetaData("default_value_type", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CqlMetadata.class, metaDataMap);
  }

  public CqlMetadata() {
  }

  public CqlMetadata(
    Map<ByteBuffer,String> name_types,
    Map<ByteBuffer,String> value_types,
    String default_name_type,
    String default_value_type)
  {
    this();
    this.name_types = name_types;
    this.value_types = value_types;
    this.default_name_type = default_name_type;
    this.default_value_type = default_value_type;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CqlMetadata(CqlMetadata other) {
    if (other.isSetName_types()) {
      Map<ByteBuffer,String> __this__name_types = new HashMap<ByteBuffer,String>(other.name_types);
      this.name_types = __this__name_types;
    }
    if (other.isSetValue_types()) {
      Map<ByteBuffer,String> __this__value_types = new HashMap<ByteBuffer,String>(other.value_types);
      this.value_types = __this__value_types;
    }
    if (other.isSetDefault_name_type()) {
      this.default_name_type = other.default_name_type;
    }
    if (other.isSetDefault_value_type()) {
      this.default_value_type = other.default_value_type;
    }
  }

  public CqlMetadata deepCopy() {
    return new CqlMetadata(this);
  }

  @Override
  public void clear() {
    this.name_types = null;
    this.value_types = null;
    this.default_name_type = null;
    this.default_value_type = null;
  }

  public int getName_typesSize() {
    return (this.name_types == null) ? 0 : this.name_types.size();
  }

  public void putToName_types(ByteBuffer key, String val) {
    if (this.name_types == null) {
      this.name_types = new HashMap<ByteBuffer,String>();
    }
    this.name_types.put(key, val);
  }

  public Map<ByteBuffer,String> getName_types() {
    return this.name_types;
  }

  public CqlMetadata setName_types(Map<ByteBuffer,String> name_types) {
    this.name_types = name_types;
    return this;
  }

  public void unsetName_types() {
    this.name_types = null;
  }

  /** Returns true if field name_types is set (has been assigned a value) and false otherwise */
  public boolean isSetName_types() {
    return this.name_types != null;
  }

  public void setName_typesIsSet(boolean value) {
    if (!value) {
      this.name_types = null;
    }
  }

  public int getValue_typesSize() {
    return (this.value_types == null) ? 0 : this.value_types.size();
  }

  public void putToValue_types(ByteBuffer key, String val) {
    if (this.value_types == null) {
      this.value_types = new HashMap<ByteBuffer,String>();
    }
    this.value_types.put(key, val);
  }

  public Map<ByteBuffer,String> getValue_types() {
    return this.value_types;
  }

  public CqlMetadata setValue_types(Map<ByteBuffer,String> value_types) {
    this.value_types = value_types;
    return this;
  }

  public void unsetValue_types() {
    this.value_types = null;
  }

  /** Returns true if field value_types is set (has been assigned a value) and false otherwise */
  public boolean isSetValue_types() {
    return this.value_types != null;
  }

  public void setValue_typesIsSet(boolean value) {
    if (!value) {
      this.value_types = null;
    }
  }

  public String getDefault_name_type() {
    return this.default_name_type;
  }

  public CqlMetadata setDefault_name_type(String default_name_type) {
    this.default_name_type = default_name_type;
    return this;
  }

  public void unsetDefault_name_type() {
    this.default_name_type = null;
  }

  /** Returns true if field default_name_type is set (has been assigned a value) and false otherwise */
  public boolean isSetDefault_name_type() {
    return this.default_name_type != null;
  }

  public void setDefault_name_typeIsSet(boolean value) {
    if (!value) {
      this.default_name_type = null;
    }
  }

  public String getDefault_value_type() {
    return this.default_value_type;
  }

  public CqlMetadata setDefault_value_type(String default_value_type) {
    this.default_value_type = default_value_type;
    return this;
  }

  public void unsetDefault_value_type() {
    this.default_value_type = null;
  }

  /** Returns true if field default_value_type is set (has been assigned a value) and false otherwise */
  public boolean isSetDefault_value_type() {
    return this.default_value_type != null;
  }

  public void setDefault_value_typeIsSet(boolean value) {
    if (!value) {
      this.default_value_type = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case NAME_TYPES:
      if (value == null) {
        unsetName_types();
      } else {
        setName_types((Map<ByteBuffer,String>)value);
      }
      break;

    case VALUE_TYPES:
      if (value == null) {
        unsetValue_types();
      } else {
        setValue_types((Map<ByteBuffer,String>)value);
      }
      break;

    case DEFAULT_NAME_TYPE:
      if (value == null) {
        unsetDefault_name_type();
      } else {
        setDefault_name_type((String)value);
      }
      break;

    case DEFAULT_VALUE_TYPE:
      if (value == null) {
        unsetDefault_value_type();
      } else {
        setDefault_value_type((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case NAME_TYPES:
      return getName_types();

    case VALUE_TYPES:
      return getValue_types();

    case DEFAULT_NAME_TYPE:
      return getDefault_name_type();

    case DEFAULT_VALUE_TYPE:
      return getDefault_value_type();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case NAME_TYPES:
      return isSetName_types();
    case VALUE_TYPES:
      return isSetValue_types();
    case DEFAULT_NAME_TYPE:
      return isSetDefault_name_type();
    case DEFAULT_VALUE_TYPE:
      return isSetDefault_value_type();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CqlMetadata)
      return this.equals((CqlMetadata)that);
    return false;
  }

  public boolean equals(CqlMetadata that) {
    if (that == null)
      return false;

    boolean this_present_name_types = true && this.isSetName_types();
    boolean that_present_name_types = true && that.isSetName_types();
    if (this_present_name_types || that_present_name_types) {
      if (!(this_present_name_types && that_present_name_types))
        return false;
      if (!this.name_types.equals(that.name_types))
        return false;
    }

    boolean this_present_value_types = true && this.isSetValue_types();
    boolean that_present_value_types = true && that.isSetValue_types();
    if (this_present_value_types || that_present_value_types) {
      if (!(this_present_value_types && that_present_value_types))
        return false;
      if (!this.value_types.equals(that.value_types))
        return false;
    }

    boolean this_present_default_name_type = true && this.isSetDefault_name_type();
    boolean that_present_default_name_type = true && that.isSetDefault_name_type();
    if (this_present_default_name_type || that_present_default_name_type) {
      if (!(this_present_default_name_type && that_present_default_name_type))
        return false;
      if (!this.default_name_type.equals(that.default_name_type))
        return false;
    }

    boolean this_present_default_value_type = true && this.isSetDefault_value_type();
    boolean that_present_default_value_type = true && that.isSetDefault_value_type();
    if (this_present_default_value_type || that_present_default_value_type) {
      if (!(this_present_default_value_type && that_present_default_value_type))
        return false;
      if (!this.default_value_type.equals(that.default_value_type))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_name_types = true && (isSetName_types());
    builder.append(present_name_types);
    if (present_name_types)
      builder.append(name_types);

    boolean present_value_types = true && (isSetValue_types());
    builder.append(present_value_types);
    if (present_value_types)
      builder.append(value_types);

    boolean present_default_name_type = true && (isSetDefault_name_type());
    builder.append(present_default_name_type);
    if (present_default_name_type)
      builder.append(default_name_type);

    boolean present_default_value_type = true && (isSetDefault_value_type());
    builder.append(present_default_value_type);
    if (present_default_value_type)
      builder.append(default_value_type);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(CqlMetadata other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetName_types()).compareTo(other.isSetName_types());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetName_types()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.name_types, other.name_types);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetValue_types()).compareTo(other.isSetValue_types());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetValue_types()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.value_types, other.value_types);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDefault_name_type()).compareTo(other.isSetDefault_name_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDefault_name_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.default_name_type, other.default_name_type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDefault_value_type()).compareTo(other.isSetDefault_value_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDefault_value_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.default_value_type, other.default_value_type);
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
    StringBuilder sb = new StringBuilder("CqlMetadata(");
    boolean first = true;

    sb.append("name_types:");
    if (this.name_types == null) {
      sb.append("null");
    } else {
      sb.append(this.name_types);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("value_types:");
    if (this.value_types == null) {
      sb.append("null");
    } else {
      sb.append(this.value_types);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("default_name_type:");
    if (this.default_name_type == null) {
      sb.append("null");
    } else {
      sb.append(this.default_name_type);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("default_value_type:");
    if (this.default_value_type == null) {
      sb.append("null");
    } else {
      sb.append(this.default_value_type);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (name_types == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'name_types' was not present! Struct: " + toString());
    }
    if (value_types == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'value_types' was not present! Struct: " + toString());
    }
    if (default_name_type == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'default_name_type' was not present! Struct: " + toString());
    }
    if (default_value_type == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'default_value_type' was not present! Struct: " + toString());
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

  private static class CqlMetadataStandardSchemeFactory implements SchemeFactory {
    public CqlMetadataStandardScheme getScheme() {
      return new CqlMetadataStandardScheme();
    }
  }

  private static class CqlMetadataStandardScheme extends StandardScheme<CqlMetadata> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CqlMetadata struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // NAME_TYPES
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map172 = iprot.readMapBegin();
                struct.name_types = new HashMap<ByteBuffer,String>(2*_map172.size);
                for (int _i173 = 0; _i173 < _map172.size; ++_i173)
                {
                  ByteBuffer _key174;
                  String _val175;
                  _key174 = iprot.readBinary();
                  _val175 = iprot.readString();
                  struct.name_types.put(_key174, _val175);
                }
                iprot.readMapEnd();
              }
              struct.setName_typesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // VALUE_TYPES
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map176 = iprot.readMapBegin();
                struct.value_types = new HashMap<ByteBuffer,String>(2*_map176.size);
                for (int _i177 = 0; _i177 < _map176.size; ++_i177)
                {
                  ByteBuffer _key178;
                  String _val179;
                  _key178 = iprot.readBinary();
                  _val179 = iprot.readString();
                  struct.value_types.put(_key178, _val179);
                }
                iprot.readMapEnd();
              }
              struct.setValue_typesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // DEFAULT_NAME_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.default_name_type = iprot.readString();
              struct.setDefault_name_typeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // DEFAULT_VALUE_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.default_value_type = iprot.readString();
              struct.setDefault_value_typeIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, CqlMetadata struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.name_types != null) {
        oprot.writeFieldBegin(NAME_TYPES_FIELD_DESC);
        {
          oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.name_types.size()));
          for (Map.Entry<ByteBuffer, String> _iter180 : struct.name_types.entrySet())
          {
            oprot.writeBinary(_iter180.getKey());
            oprot.writeString(_iter180.getValue());
          }
          oprot.writeMapEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.value_types != null) {
        oprot.writeFieldBegin(VALUE_TYPES_FIELD_DESC);
        {
          oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.value_types.size()));
          for (Map.Entry<ByteBuffer, String> _iter181 : struct.value_types.entrySet())
          {
            oprot.writeBinary(_iter181.getKey());
            oprot.writeString(_iter181.getValue());
          }
          oprot.writeMapEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.default_name_type != null) {
        oprot.writeFieldBegin(DEFAULT_NAME_TYPE_FIELD_DESC);
        oprot.writeString(struct.default_name_type);
        oprot.writeFieldEnd();
      }
      if (struct.default_value_type != null) {
        oprot.writeFieldBegin(DEFAULT_VALUE_TYPE_FIELD_DESC);
        oprot.writeString(struct.default_value_type);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CqlMetadataTupleSchemeFactory implements SchemeFactory {
    public CqlMetadataTupleScheme getScheme() {
      return new CqlMetadataTupleScheme();
    }
  }

  private static class CqlMetadataTupleScheme extends TupleScheme<CqlMetadata> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CqlMetadata struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.name_types.size());
        for (Map.Entry<ByteBuffer, String> _iter182 : struct.name_types.entrySet())
        {
          oprot.writeBinary(_iter182.getKey());
          oprot.writeString(_iter182.getValue());
        }
      }
      {
        oprot.writeI32(struct.value_types.size());
        for (Map.Entry<ByteBuffer, String> _iter183 : struct.value_types.entrySet())
        {
          oprot.writeBinary(_iter183.getKey());
          oprot.writeString(_iter183.getValue());
        }
      }
      oprot.writeString(struct.default_name_type);
      oprot.writeString(struct.default_value_type);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CqlMetadata struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TMap _map184 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.name_types = new HashMap<ByteBuffer,String>(2*_map184.size);
        for (int _i185 = 0; _i185 < _map184.size; ++_i185)
        {
          ByteBuffer _key186;
          String _val187;
          _key186 = iprot.readBinary();
          _val187 = iprot.readString();
          struct.name_types.put(_key186, _val187);
        }
      }
      struct.setName_typesIsSet(true);
      {
        org.apache.thrift.protocol.TMap _map188 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.value_types = new HashMap<ByteBuffer,String>(2*_map188.size);
        for (int _i189 = 0; _i189 < _map188.size; ++_i189)
        {
          ByteBuffer _key190;
          String _val191;
          _key190 = iprot.readBinary();
          _val191 = iprot.readString();
          struct.value_types.put(_key190, _val191);
        }
      }
      struct.setValue_typesIsSet(true);
      struct.default_name_type = iprot.readString();
      struct.setDefault_name_typeIsSet(true);
      struct.default_value_type = iprot.readString();
      struct.setDefault_value_typeIsSet(true);
    }
  }

}

