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
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;

public class ColumnDef implements org.apache.thrift.TBase<ColumnDef, ColumnDef._Fields>, java.io.Serializable, Cloneable, Comparable<ColumnDef> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ColumnDef");

  private static final org.apache.thrift.protocol.TField NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("name", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField VALIDATION_CLASS_FIELD_DESC = new org.apache.thrift.protocol.TField("validation_class", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField INDEX_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("index_type", org.apache.thrift.protocol.TType.I32, (short)3);
  private static final org.apache.thrift.protocol.TField INDEX_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("index_name", org.apache.thrift.protocol.TType.STRING, (short)4);
  private static final org.apache.thrift.protocol.TField INDEX_OPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("index_options", org.apache.thrift.protocol.TType.MAP, (short)5);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ColumnDefStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ColumnDefTupleSchemeFactory());
  }

  public ByteBuffer name; // required
  public String validation_class; // required
  /**
   * 
   * @see IndexType
   */
  public IndexType index_type; // optional
  public String index_name; // optional
  public Map<String,String> index_options; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    NAME((short)1, "name"),
    VALIDATION_CLASS((short)2, "validation_class"),
    /**
     * 
     * @see IndexType
     */
    INDEX_TYPE((short)3, "index_type"),
    INDEX_NAME((short)4, "index_name"),
    INDEX_OPTIONS((short)5, "index_options");

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
        case 2: // VALIDATION_CLASS
          return VALIDATION_CLASS;
        case 3: // INDEX_TYPE
          return INDEX_TYPE;
        case 4: // INDEX_NAME
          return INDEX_NAME;
        case 5: // INDEX_OPTIONS
          return INDEX_OPTIONS;
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
  private _Fields optionals[] = {_Fields.INDEX_TYPE,_Fields.INDEX_NAME,_Fields.INDEX_OPTIONS};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.NAME, new org.apache.thrift.meta_data.FieldMetaData("name", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.VALIDATION_CLASS, new org.apache.thrift.meta_data.FieldMetaData("validation_class", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.INDEX_TYPE, new org.apache.thrift.meta_data.FieldMetaData("index_type", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, IndexType.class)));
    tmpMap.put(_Fields.INDEX_NAME, new org.apache.thrift.meta_data.FieldMetaData("index_name", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.INDEX_OPTIONS, new org.apache.thrift.meta_data.FieldMetaData("index_options", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ColumnDef.class, metaDataMap);
  }

  public ColumnDef() {
  }

  public ColumnDef(
    ByteBuffer name,
    String validation_class)
  {
    this();
    this.name = name;
    this.validation_class = validation_class;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ColumnDef(ColumnDef other) {
    if (other.isSetName()) {
      this.name = org.apache.thrift.TBaseHelper.copyBinary(other.name);
;
    }
    if (other.isSetValidation_class()) {
      this.validation_class = other.validation_class;
    }
    if (other.isSetIndex_type()) {
      this.index_type = other.index_type;
    }
    if (other.isSetIndex_name()) {
      this.index_name = other.index_name;
    }
    if (other.isSetIndex_options()) {
      Map<String,String> __this__index_options = new HashMap<String,String>(other.index_options);
      this.index_options = __this__index_options;
    }
  }

  public ColumnDef deepCopy() {
    return new ColumnDef(this);
  }

  @Override
  public void clear() {
    this.name = null;
    this.validation_class = null;
    this.index_type = null;
    this.index_name = null;
    this.index_options = null;
  }

  public byte[] getName() {
    setName(org.apache.thrift.TBaseHelper.rightSize(name));
    return name == null ? null : name.array();
  }

  public ByteBuffer bufferForName() {
    return name;
  }

  public ColumnDef setName(byte[] name) {
    setName(name == null ? (ByteBuffer)null : ByteBuffer.wrap(name));
    return this;
  }

  public ColumnDef setName(ByteBuffer name) {
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

  public String getValidation_class() {
    return this.validation_class;
  }

  public ColumnDef setValidation_class(String validation_class) {
    this.validation_class = validation_class;
    return this;
  }

  public void unsetValidation_class() {
    this.validation_class = null;
  }

  /** Returns true if field validation_class is set (has been assigned a value) and false otherwise */
  public boolean isSetValidation_class() {
    return this.validation_class != null;
  }

  public void setValidation_classIsSet(boolean value) {
    if (!value) {
      this.validation_class = null;
    }
  }

  /**
   * 
   * @see IndexType
   */
  public IndexType getIndex_type() {
    return this.index_type;
  }

  /**
   * 
   * @see IndexType
   */
  public ColumnDef setIndex_type(IndexType index_type) {
    this.index_type = index_type;
    return this;
  }

  public void unsetIndex_type() {
    this.index_type = null;
  }

  /** Returns true if field index_type is set (has been assigned a value) and false otherwise */
  public boolean isSetIndex_type() {
    return this.index_type != null;
  }

  public void setIndex_typeIsSet(boolean value) {
    if (!value) {
      this.index_type = null;
    }
  }

  public String getIndex_name() {
    return this.index_name;
  }

  public ColumnDef setIndex_name(String index_name) {
    this.index_name = index_name;
    return this;
  }

  public void unsetIndex_name() {
    this.index_name = null;
  }

  /** Returns true if field index_name is set (has been assigned a value) and false otherwise */
  public boolean isSetIndex_name() {
    return this.index_name != null;
  }

  public void setIndex_nameIsSet(boolean value) {
    if (!value) {
      this.index_name = null;
    }
  }

  public int getIndex_optionsSize() {
    return (this.index_options == null) ? 0 : this.index_options.size();
  }

  public void putToIndex_options(String key, String val) {
    if (this.index_options == null) {
      this.index_options = new HashMap<String,String>();
    }
    this.index_options.put(key, val);
  }

  public Map<String,String> getIndex_options() {
    return this.index_options;
  }

  public ColumnDef setIndex_options(Map<String,String> index_options) {
    this.index_options = index_options;
    return this;
  }

  public void unsetIndex_options() {
    this.index_options = null;
  }

  /** Returns true if field index_options is set (has been assigned a value) and false otherwise */
  public boolean isSetIndex_options() {
    return this.index_options != null;
  }

  public void setIndex_optionsIsSet(boolean value) {
    if (!value) {
      this.index_options = null;
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

    case VALIDATION_CLASS:
      if (value == null) {
        unsetValidation_class();
      } else {
        setValidation_class((String)value);
      }
      break;

    case INDEX_TYPE:
      if (value == null) {
        unsetIndex_type();
      } else {
        setIndex_type((IndexType)value);
      }
      break;

    case INDEX_NAME:
      if (value == null) {
        unsetIndex_name();
      } else {
        setIndex_name((String)value);
      }
      break;

    case INDEX_OPTIONS:
      if (value == null) {
        unsetIndex_options();
      } else {
        setIndex_options((Map<String,String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case NAME:
      return getName();

    case VALIDATION_CLASS:
      return getValidation_class();

    case INDEX_TYPE:
      return getIndex_type();

    case INDEX_NAME:
      return getIndex_name();

    case INDEX_OPTIONS:
      return getIndex_options();

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
    case VALIDATION_CLASS:
      return isSetValidation_class();
    case INDEX_TYPE:
      return isSetIndex_type();
    case INDEX_NAME:
      return isSetIndex_name();
    case INDEX_OPTIONS:
      return isSetIndex_options();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ColumnDef)
      return this.equals((ColumnDef)that);
    return false;
  }

  public boolean equals(ColumnDef that) {
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

    boolean this_present_validation_class = true && this.isSetValidation_class();
    boolean that_present_validation_class = true && that.isSetValidation_class();
    if (this_present_validation_class || that_present_validation_class) {
      if (!(this_present_validation_class && that_present_validation_class))
        return false;
      if (!this.validation_class.equals(that.validation_class))
        return false;
    }

    boolean this_present_index_type = true && this.isSetIndex_type();
    boolean that_present_index_type = true && that.isSetIndex_type();
    if (this_present_index_type || that_present_index_type) {
      if (!(this_present_index_type && that_present_index_type))
        return false;
      if (!this.index_type.equals(that.index_type))
        return false;
    }

    boolean this_present_index_name = true && this.isSetIndex_name();
    boolean that_present_index_name = true && that.isSetIndex_name();
    if (this_present_index_name || that_present_index_name) {
      if (!(this_present_index_name && that_present_index_name))
        return false;
      if (!this.index_name.equals(that.index_name))
        return false;
    }

    boolean this_present_index_options = true && this.isSetIndex_options();
    boolean that_present_index_options = true && that.isSetIndex_options();
    if (this_present_index_options || that_present_index_options) {
      if (!(this_present_index_options && that_present_index_options))
        return false;
      if (!this.index_options.equals(that.index_options))
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

    boolean present_validation_class = true && (isSetValidation_class());
    builder.append(present_validation_class);
    if (present_validation_class)
      builder.append(validation_class);

    boolean present_index_type = true && (isSetIndex_type());
    builder.append(present_index_type);
    if (present_index_type)
      builder.append(index_type.getValue());

    boolean present_index_name = true && (isSetIndex_name());
    builder.append(present_index_name);
    if (present_index_name)
      builder.append(index_name);

    boolean present_index_options = true && (isSetIndex_options());
    builder.append(present_index_options);
    if (present_index_options)
      builder.append(index_options);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(ColumnDef other) {
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
    lastComparison = Boolean.valueOf(isSetValidation_class()).compareTo(other.isSetValidation_class());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetValidation_class()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.validation_class, other.validation_class);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIndex_type()).compareTo(other.isSetIndex_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndex_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.index_type, other.index_type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIndex_name()).compareTo(other.isSetIndex_name());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndex_name()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.index_name, other.index_name);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIndex_options()).compareTo(other.isSetIndex_options());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndex_options()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.index_options, other.index_options);
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
    StringBuilder sb = new StringBuilder("ColumnDef(");
    boolean first = true;

    sb.append("name:");
    if (this.name == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.name, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("validation_class:");
    if (this.validation_class == null) {
      sb.append("null");
    } else {
      sb.append(this.validation_class);
    }
    first = false;
    if (isSetIndex_type()) {
      if (!first) sb.append(", ");
      sb.append("index_type:");
      if (this.index_type == null) {
        sb.append("null");
      } else {
        sb.append(this.index_type);
      }
      first = false;
    }
    if (isSetIndex_name()) {
      if (!first) sb.append(", ");
      sb.append("index_name:");
      if (this.index_name == null) {
        sb.append("null");
      } else {
        sb.append(this.index_name);
      }
      first = false;
    }
    if (isSetIndex_options()) {
      if (!first) sb.append(", ");
      sb.append("index_options:");
      if (this.index_options == null) {
        sb.append("null");
      } else {
        sb.append(this.index_options);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (name == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'name' was not present! Struct: " + toString());
    }
    if (validation_class == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'validation_class' was not present! Struct: " + toString());
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

  private static class ColumnDefStandardSchemeFactory implements SchemeFactory {
    public ColumnDefStandardScheme getScheme() {
      return new ColumnDefStandardScheme();
    }
  }

  private static class ColumnDefStandardScheme extends StandardScheme<ColumnDef> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ColumnDef struct) throws org.apache.thrift.TException {
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
          case 2: // VALIDATION_CLASS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.validation_class = iprot.readString();
              struct.setValidation_classIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // INDEX_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.index_type = IndexType.findByValue(iprot.readI32());
              struct.setIndex_typeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // INDEX_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.index_name = iprot.readString();
              struct.setIndex_nameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // INDEX_OPTIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map90 = iprot.readMapBegin();
                struct.index_options = new HashMap<String,String>(2*_map90.size);
                for (int _i91 = 0; _i91 < _map90.size; ++_i91)
                {
                  String _key92;
                  String _val93;
                  _key92 = iprot.readString();
                  _val93 = iprot.readString();
                  struct.index_options.put(_key92, _val93);
                }
                iprot.readMapEnd();
              }
              struct.setIndex_optionsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ColumnDef struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.name != null) {
        oprot.writeFieldBegin(NAME_FIELD_DESC);
        oprot.writeBinary(struct.name);
        oprot.writeFieldEnd();
      }
      if (struct.validation_class != null) {
        oprot.writeFieldBegin(VALIDATION_CLASS_FIELD_DESC);
        oprot.writeString(struct.validation_class);
        oprot.writeFieldEnd();
      }
      if (struct.index_type != null) {
        if (struct.isSetIndex_type()) {
          oprot.writeFieldBegin(INDEX_TYPE_FIELD_DESC);
          oprot.writeI32(struct.index_type.getValue());
          oprot.writeFieldEnd();
        }
      }
      if (struct.index_name != null) {
        if (struct.isSetIndex_name()) {
          oprot.writeFieldBegin(INDEX_NAME_FIELD_DESC);
          oprot.writeString(struct.index_name);
          oprot.writeFieldEnd();
        }
      }
      if (struct.index_options != null) {
        if (struct.isSetIndex_options()) {
          oprot.writeFieldBegin(INDEX_OPTIONS_FIELD_DESC);
          {
            oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.index_options.size()));
            for (Map.Entry<String, String> _iter94 : struct.index_options.entrySet())
            {
              oprot.writeString(_iter94.getKey());
              oprot.writeString(_iter94.getValue());
            }
            oprot.writeMapEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ColumnDefTupleSchemeFactory implements SchemeFactory {
    public ColumnDefTupleScheme getScheme() {
      return new ColumnDefTupleScheme();
    }
  }

  private static class ColumnDefTupleScheme extends TupleScheme<ColumnDef> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ColumnDef struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeBinary(struct.name);
      oprot.writeString(struct.validation_class);
      BitSet optionals = new BitSet();
      if (struct.isSetIndex_type()) {
        optionals.set(0);
      }
      if (struct.isSetIndex_name()) {
        optionals.set(1);
      }
      if (struct.isSetIndex_options()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetIndex_type()) {
        oprot.writeI32(struct.index_type.getValue());
      }
      if (struct.isSetIndex_name()) {
        oprot.writeString(struct.index_name);
      }
      if (struct.isSetIndex_options()) {
        {
          oprot.writeI32(struct.index_options.size());
          for (Map.Entry<String, String> _iter95 : struct.index_options.entrySet())
          {
            oprot.writeString(_iter95.getKey());
            oprot.writeString(_iter95.getValue());
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ColumnDef struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.name = iprot.readBinary();
      struct.setNameIsSet(true);
      struct.validation_class = iprot.readString();
      struct.setValidation_classIsSet(true);
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.index_type = IndexType.findByValue(iprot.readI32());
        struct.setIndex_typeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.index_name = iprot.readString();
        struct.setIndex_nameIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TMap _map96 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.index_options = new HashMap<String,String>(2*_map96.size);
          for (int _i97 = 0; _i97 < _map96.size; ++_i97)
          {
            String _key98;
            String _val99;
            _key98 = iprot.readString();
            _val99 = iprot.readString();
            struct.index_options.put(_key98, _val99);
          }
        }
        struct.setIndex_optionsIsSet(true);
      }
    }
  }

}

