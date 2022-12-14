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

public class EndpointDetails implements org.apache.thrift.TBase<EndpointDetails, EndpointDetails._Fields>, java.io.Serializable, Cloneable, Comparable<EndpointDetails> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("EndpointDetails");

  private static final org.apache.thrift.protocol.TField HOST_FIELD_DESC = new org.apache.thrift.protocol.TField("host", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField DATACENTER_FIELD_DESC = new org.apache.thrift.protocol.TField("datacenter", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField RACK_FIELD_DESC = new org.apache.thrift.protocol.TField("rack", org.apache.thrift.protocol.TType.STRING, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new EndpointDetailsStandardSchemeFactory());
    schemes.put(TupleScheme.class, new EndpointDetailsTupleSchemeFactory());
  }

  public String host; // required
  public String datacenter; // required
  public String rack; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    HOST((short)1, "host"),
    DATACENTER((short)2, "datacenter"),
    RACK((short)3, "rack");

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
        case 1: // HOST
          return HOST;
        case 2: // DATACENTER
          return DATACENTER;
        case 3: // RACK
          return RACK;
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
  private _Fields optionals[] = {_Fields.RACK};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.HOST, new org.apache.thrift.meta_data.FieldMetaData("host", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.DATACENTER, new org.apache.thrift.meta_data.FieldMetaData("datacenter", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.RACK, new org.apache.thrift.meta_data.FieldMetaData("rack", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(EndpointDetails.class, metaDataMap);
  }

  public EndpointDetails() {
  }

  public EndpointDetails(
    String host,
    String datacenter)
  {
    this();
    this.host = host;
    this.datacenter = datacenter;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public EndpointDetails(EndpointDetails other) {
    if (other.isSetHost()) {
      this.host = other.host;
    }
    if (other.isSetDatacenter()) {
      this.datacenter = other.datacenter;
    }
    if (other.isSetRack()) {
      this.rack = other.rack;
    }
  }

  public EndpointDetails deepCopy() {
    return new EndpointDetails(this);
  }

  @Override
  public void clear() {
    this.host = null;
    this.datacenter = null;
    this.rack = null;
  }

  public String getHost() {
    return this.host;
  }

  public EndpointDetails setHost(String host) {
    this.host = host;
    return this;
  }

  public void unsetHost() {
    this.host = null;
  }

  /** Returns true if field host is set (has been assigned a value) and false otherwise */
  public boolean isSetHost() {
    return this.host != null;
  }

  public void setHostIsSet(boolean value) {
    if (!value) {
      this.host = null;
    }
  }

  public String getDatacenter() {
    return this.datacenter;
  }

  public EndpointDetails setDatacenter(String datacenter) {
    this.datacenter = datacenter;
    return this;
  }

  public void unsetDatacenter() {
    this.datacenter = null;
  }

  /** Returns true if field datacenter is set (has been assigned a value) and false otherwise */
  public boolean isSetDatacenter() {
    return this.datacenter != null;
  }

  public void setDatacenterIsSet(boolean value) {
    if (!value) {
      this.datacenter = null;
    }
  }

  public String getRack() {
    return this.rack;
  }

  public EndpointDetails setRack(String rack) {
    this.rack = rack;
    return this;
  }

  public void unsetRack() {
    this.rack = null;
  }

  /** Returns true if field rack is set (has been assigned a value) and false otherwise */
  public boolean isSetRack() {
    return this.rack != null;
  }

  public void setRackIsSet(boolean value) {
    if (!value) {
      this.rack = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case HOST:
      if (value == null) {
        unsetHost();
      } else {
        setHost((String)value);
      }
      break;

    case DATACENTER:
      if (value == null) {
        unsetDatacenter();
      } else {
        setDatacenter((String)value);
      }
      break;

    case RACK:
      if (value == null) {
        unsetRack();
      } else {
        setRack((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case HOST:
      return getHost();

    case DATACENTER:
      return getDatacenter();

    case RACK:
      return getRack();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case HOST:
      return isSetHost();
    case DATACENTER:
      return isSetDatacenter();
    case RACK:
      return isSetRack();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof EndpointDetails)
      return this.equals((EndpointDetails)that);
    return false;
  }

  public boolean equals(EndpointDetails that) {
    if (that == null)
      return false;

    boolean this_present_host = true && this.isSetHost();
    boolean that_present_host = true && that.isSetHost();
    if (this_present_host || that_present_host) {
      if (!(this_present_host && that_present_host))
        return false;
      if (!this.host.equals(that.host))
        return false;
    }

    boolean this_present_datacenter = true && this.isSetDatacenter();
    boolean that_present_datacenter = true && that.isSetDatacenter();
    if (this_present_datacenter || that_present_datacenter) {
      if (!(this_present_datacenter && that_present_datacenter))
        return false;
      if (!this.datacenter.equals(that.datacenter))
        return false;
    }

    boolean this_present_rack = true && this.isSetRack();
    boolean that_present_rack = true && that.isSetRack();
    if (this_present_rack || that_present_rack) {
      if (!(this_present_rack && that_present_rack))
        return false;
      if (!this.rack.equals(that.rack))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_host = true && (isSetHost());
    builder.append(present_host);
    if (present_host)
      builder.append(host);

    boolean present_datacenter = true && (isSetDatacenter());
    builder.append(present_datacenter);
    if (present_datacenter)
      builder.append(datacenter);

    boolean present_rack = true && (isSetRack());
    builder.append(present_rack);
    if (present_rack)
      builder.append(rack);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(EndpointDetails other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetHost()).compareTo(other.isSetHost());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHost()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.host, other.host);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDatacenter()).compareTo(other.isSetDatacenter());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDatacenter()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.datacenter, other.datacenter);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRack()).compareTo(other.isSetRack());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRack()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.rack, other.rack);
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
    StringBuilder sb = new StringBuilder("EndpointDetails(");
    boolean first = true;

    sb.append("host:");
    if (this.host == null) {
      sb.append("null");
    } else {
      sb.append(this.host);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("datacenter:");
    if (this.datacenter == null) {
      sb.append("null");
    } else {
      sb.append(this.datacenter);
    }
    first = false;
    if (isSetRack()) {
      if (!first) sb.append(", ");
      sb.append("rack:");
      if (this.rack == null) {
        sb.append("null");
      } else {
        sb.append(this.rack);
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

  private static class EndpointDetailsStandardSchemeFactory implements SchemeFactory {
    public EndpointDetailsStandardScheme getScheme() {
      return new EndpointDetailsStandardScheme();
    }
  }

  private static class EndpointDetailsStandardScheme extends StandardScheme<EndpointDetails> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, EndpointDetails struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // HOST
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.host = iprot.readString();
              struct.setHostIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // DATACENTER
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.datacenter = iprot.readString();
              struct.setDatacenterIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // RACK
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.rack = iprot.readString();
              struct.setRackIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, EndpointDetails struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.host != null) {
        oprot.writeFieldBegin(HOST_FIELD_DESC);
        oprot.writeString(struct.host);
        oprot.writeFieldEnd();
      }
      if (struct.datacenter != null) {
        oprot.writeFieldBegin(DATACENTER_FIELD_DESC);
        oprot.writeString(struct.datacenter);
        oprot.writeFieldEnd();
      }
      if (struct.rack != null) {
        if (struct.isSetRack()) {
          oprot.writeFieldBegin(RACK_FIELD_DESC);
          oprot.writeString(struct.rack);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class EndpointDetailsTupleSchemeFactory implements SchemeFactory {
    public EndpointDetailsTupleScheme getScheme() {
      return new EndpointDetailsTupleScheme();
    }
  }

  private static class EndpointDetailsTupleScheme extends TupleScheme<EndpointDetails> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, EndpointDetails struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetHost()) {
        optionals.set(0);
      }
      if (struct.isSetDatacenter()) {
        optionals.set(1);
      }
      if (struct.isSetRack()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetHost()) {
        oprot.writeString(struct.host);
      }
      if (struct.isSetDatacenter()) {
        oprot.writeString(struct.datacenter);
      }
      if (struct.isSetRack()) {
        oprot.writeString(struct.rack);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, EndpointDetails struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.host = iprot.readString();
        struct.setHostIsSet(true);
      }
      if (incoming.get(1)) {
        struct.datacenter = iprot.readString();
        struct.setDatacenterIsSet(true);
      }
      if (incoming.get(2)) {
        struct.rack = iprot.readString();
        struct.setRackIsSet(true);
      }
    }
  }

}

