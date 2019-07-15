import org.apache.cassandra.thrift.EndpointDetails;
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
 * A TokenRange describes part of the Cassandra ring, it is a mapping from a range to
 * endpoints responsible for that range.
 * @param start_token The first token in the range
 * @param end_token The last token in the range
 * @param endpoints The endpoints responsible for the range (listed by their configured listen_address)
 * @param rpc_endpoints The endpoints responsible for the range (listed by their configured rpc_address)
 */
public class TokenRange implements org.apache.thrift.TBase<TokenRange, TokenRange._Fields>, java.io.Serializable, Cloneable, Comparable<TokenRange> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TokenRange");

  private static final org.apache.thrift.protocol.TField START_TOKEN_FIELD_DESC = new org.apache.thrift.protocol.TField("start_token", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField END_TOKEN_FIELD_DESC = new org.apache.thrift.protocol.TField("end_token", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField ENDPOINTS_FIELD_DESC = new org.apache.thrift.protocol.TField("endpoints", org.apache.thrift.protocol.TType.LIST, (short)3);
  private static final org.apache.thrift.protocol.TField RPC_ENDPOINTS_FIELD_DESC = new org.apache.thrift.protocol.TField("rpc_endpoints", org.apache.thrift.protocol.TType.LIST, (short)4);
  private static final org.apache.thrift.protocol.TField ENDPOINT_DETAILS_FIELD_DESC = new org.apache.thrift.protocol.TField("endpoint_details", org.apache.thrift.protocol.TType.LIST, (short)5);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TokenRangeStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TokenRangeTupleSchemeFactory());
  }

  public String start_token; // required
  public String end_token; // required
  public List<String> endpoints; // required
  public List<String> rpc_endpoints; // optional
  public List<EndpointDetails> endpoint_details; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    START_TOKEN((short)1, "start_token"),
    END_TOKEN((short)2, "end_token"),
    ENDPOINTS((short)3, "endpoints"),
    RPC_ENDPOINTS((short)4, "rpc_endpoints"),
    ENDPOINT_DETAILS((short)5, "endpoint_details");

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
        case 1: // START_TOKEN
          return START_TOKEN;
        case 2: // END_TOKEN
          return END_TOKEN;
        case 3: // ENDPOINTS
          return ENDPOINTS;
        case 4: // RPC_ENDPOINTS
          return RPC_ENDPOINTS;
        case 5: // ENDPOINT_DETAILS
          return ENDPOINT_DETAILS;
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
  private _Fields optionals[] = {_Fields.RPC_ENDPOINTS,_Fields.ENDPOINT_DETAILS};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.START_TOKEN, new org.apache.thrift.meta_data.FieldMetaData("start_token", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.END_TOKEN, new org.apache.thrift.meta_data.FieldMetaData("end_token", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.ENDPOINTS, new org.apache.thrift.meta_data.FieldMetaData("endpoints", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.RPC_ENDPOINTS, new org.apache.thrift.meta_data.FieldMetaData("rpc_endpoints", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.ENDPOINT_DETAILS, new org.apache.thrift.meta_data.FieldMetaData("endpoint_details", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, EndpointDetails.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TokenRange.class, metaDataMap);
  }

  public TokenRange() {
  }

  public TokenRange(
    String start_token,
    String end_token,
    List<String> endpoints)
  {
    this();
    this.start_token = start_token;
    this.end_token = end_token;
    this.endpoints = endpoints;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TokenRange(TokenRange other) {
    if (other.isSetStart_token()) {
      this.start_token = other.start_token;
    }
    if (other.isSetEnd_token()) {
      this.end_token = other.end_token;
    }
    if (other.isSetEndpoints()) {
      List<String> __this__endpoints = new ArrayList<String>(other.endpoints);
      this.endpoints = __this__endpoints;
    }
    if (other.isSetRpc_endpoints()) {
      List<String> __this__rpc_endpoints = new ArrayList<String>(other.rpc_endpoints);
      this.rpc_endpoints = __this__rpc_endpoints;
    }
    if (other.isSetEndpoint_details()) {
      List<EndpointDetails> __this__endpoint_details = new ArrayList<EndpointDetails>(other.endpoint_details.size());
      for (EndpointDetails other_element : other.endpoint_details) {
        __this__endpoint_details.add(new EndpointDetails(other_element));
      }
      this.endpoint_details = __this__endpoint_details;
    }
  }

  public TokenRange deepCopy() {
    return new TokenRange(this);
  }

  @Override
  public void clear() {
    this.start_token = null;
    this.end_token = null;
    this.endpoints = null;
    this.rpc_endpoints = null;
    this.endpoint_details = null;
  }

  public String getStart_token() {
    return this.start_token;
  }

  public TokenRange setStart_token(String start_token) {
    this.start_token = start_token;
    return this;
  }

  public void unsetStart_token() {
    this.start_token = null;
  }

  /** Returns true if field start_token is set (has been assigned a value) and false otherwise */
  public boolean isSetStart_token() {
    return this.start_token != null;
  }

  public void setStart_tokenIsSet(boolean value) {
    if (!value) {
      this.start_token = null;
    }
  }

  public String getEnd_token() {
    return this.end_token;
  }

  public TokenRange setEnd_token(String end_token) {
    this.end_token = end_token;
    return this;
  }

  public void unsetEnd_token() {
    this.end_token = null;
  }

  /** Returns true if field end_token is set (has been assigned a value) and false otherwise */
  public boolean isSetEnd_token() {
    return this.end_token != null;
  }

  public void setEnd_tokenIsSet(boolean value) {
    if (!value) {
      this.end_token = null;
    }
  }

  public int getEndpointsSize() {
    return (this.endpoints == null) ? 0 : this.endpoints.size();
  }

  public java.util.Iterator<String> getEndpointsIterator() {
    return (this.endpoints == null) ? null : this.endpoints.iterator();
  }

  public void addToEndpoints(String elem) {
    if (this.endpoints == null) {
      this.endpoints = new ArrayList<String>();
    }
    this.endpoints.add(elem);
  }

  public List<String> getEndpoints() {
    return this.endpoints;
  }

  public TokenRange setEndpoints(List<String> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public void unsetEndpoints() {
    this.endpoints = null;
  }

  /** Returns true if field endpoints is set (has been assigned a value) and false otherwise */
  public boolean isSetEndpoints() {
    return this.endpoints != null;
  }

  public void setEndpointsIsSet(boolean value) {
    if (!value) {
      this.endpoints = null;
    }
  }

  public int getRpc_endpointsSize() {
    return (this.rpc_endpoints == null) ? 0 : this.rpc_endpoints.size();
  }

  public java.util.Iterator<String> getRpc_endpointsIterator() {
    return (this.rpc_endpoints == null) ? null : this.rpc_endpoints.iterator();
  }

  public void addToRpc_endpoints(String elem) {
    if (this.rpc_endpoints == null) {
      this.rpc_endpoints = new ArrayList<String>();
    }
    this.rpc_endpoints.add(elem);
  }

  public List<String> getRpc_endpoints() {
    return this.rpc_endpoints;
  }

  public TokenRange setRpc_endpoints(List<String> rpc_endpoints) {
    this.rpc_endpoints = rpc_endpoints;
    return this;
  }

  public void unsetRpc_endpoints() {
    this.rpc_endpoints = null;
  }

  /** Returns true if field rpc_endpoints is set (has been assigned a value) and false otherwise */
  public boolean isSetRpc_endpoints() {
    return this.rpc_endpoints != null;
  }

  public void setRpc_endpointsIsSet(boolean value) {
    if (!value) {
      this.rpc_endpoints = null;
    }
  }

  public int getEndpoint_detailsSize() {
    return (this.endpoint_details == null) ? 0 : this.endpoint_details.size();
  }

  public java.util.Iterator<EndpointDetails> getEndpoint_detailsIterator() {
    return (this.endpoint_details == null) ? null : this.endpoint_details.iterator();
  }

  public void addToEndpoint_details(EndpointDetails elem) {
    if (this.endpoint_details == null) {
      this.endpoint_details = new ArrayList<EndpointDetails>();
    }
    this.endpoint_details.add(elem);
  }

  public List<EndpointDetails> getEndpoint_details() {
    return this.endpoint_details;
  }

  public TokenRange setEndpoint_details(List<EndpointDetails> endpoint_details) {
    this.endpoint_details = endpoint_details;
    return this;
  }

  public void unsetEndpoint_details() {
    this.endpoint_details = null;
  }

  /** Returns true if field endpoint_details is set (has been assigned a value) and false otherwise */
  public boolean isSetEndpoint_details() {
    return this.endpoint_details != null;
  }

  public void setEndpoint_detailsIsSet(boolean value) {
    if (!value) {
      this.endpoint_details = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case START_TOKEN:
      if (value == null) {
        unsetStart_token();
      } else {
        setStart_token((String)value);
      }
      break;

    case END_TOKEN:
      if (value == null) {
        unsetEnd_token();
      } else {
        setEnd_token((String)value);
      }
      break;

    case ENDPOINTS:
      if (value == null) {
        unsetEndpoints();
      } else {
        setEndpoints((List<String>)value);
      }
      break;

    case RPC_ENDPOINTS:
      if (value == null) {
        unsetRpc_endpoints();
      } else {
        setRpc_endpoints((List<String>)value);
      }
      break;

    case ENDPOINT_DETAILS:
      if (value == null) {
        unsetEndpoint_details();
      } else {
        setEndpoint_details((List<EndpointDetails>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case START_TOKEN:
      return getStart_token();

    case END_TOKEN:
      return getEnd_token();

    case ENDPOINTS:
      return getEndpoints();

    case RPC_ENDPOINTS:
      return getRpc_endpoints();

    case ENDPOINT_DETAILS:
      return getEndpoint_details();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case START_TOKEN:
      return isSetStart_token();
    case END_TOKEN:
      return isSetEnd_token();
    case ENDPOINTS:
      return isSetEndpoints();
    case RPC_ENDPOINTS:
      return isSetRpc_endpoints();
    case ENDPOINT_DETAILS:
      return isSetEndpoint_details();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TokenRange)
      return this.equals((TokenRange)that);
    return false;
  }

  public boolean equals(TokenRange that) {
    if (that == null)
      return false;

    boolean this_present_start_token = true && this.isSetStart_token();
    boolean that_present_start_token = true && that.isSetStart_token();
    if (this_present_start_token || that_present_start_token) {
      if (!(this_present_start_token && that_present_start_token))
        return false;
      if (!this.start_token.equals(that.start_token))
        return false;
    }

    boolean this_present_end_token = true && this.isSetEnd_token();
    boolean that_present_end_token = true && that.isSetEnd_token();
    if (this_present_end_token || that_present_end_token) {
      if (!(this_present_end_token && that_present_end_token))
        return false;
      if (!this.end_token.equals(that.end_token))
        return false;
    }

    boolean this_present_endpoints = true && this.isSetEndpoints();
    boolean that_present_endpoints = true && that.isSetEndpoints();
    if (this_present_endpoints || that_present_endpoints) {
      if (!(this_present_endpoints && that_present_endpoints))
        return false;
      if (!this.endpoints.equals(that.endpoints))
        return false;
    }

    boolean this_present_rpc_endpoints = true && this.isSetRpc_endpoints();
    boolean that_present_rpc_endpoints = true && that.isSetRpc_endpoints();
    if (this_present_rpc_endpoints || that_present_rpc_endpoints) {
      if (!(this_present_rpc_endpoints && that_present_rpc_endpoints))
        return false;
      if (!this.rpc_endpoints.equals(that.rpc_endpoints))
        return false;
    }

    boolean this_present_endpoint_details = true && this.isSetEndpoint_details();
    boolean that_present_endpoint_details = true && that.isSetEndpoint_details();
    if (this_present_endpoint_details || that_present_endpoint_details) {
      if (!(this_present_endpoint_details && that_present_endpoint_details))
        return false;
      if (!this.endpoint_details.equals(that.endpoint_details))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_start_token = true && (isSetStart_token());
    builder.append(present_start_token);
    if (present_start_token)
      builder.append(start_token);

    boolean present_end_token = true && (isSetEnd_token());
    builder.append(present_end_token);
    if (present_end_token)
      builder.append(end_token);

    boolean present_endpoints = true && (isSetEndpoints());
    builder.append(present_endpoints);
    if (present_endpoints)
      builder.append(endpoints);

    boolean present_rpc_endpoints = true && (isSetRpc_endpoints());
    builder.append(present_rpc_endpoints);
    if (present_rpc_endpoints)
      builder.append(rpc_endpoints);

    boolean present_endpoint_details = true && (isSetEndpoint_details());
    builder.append(present_endpoint_details);
    if (present_endpoint_details)
      builder.append(endpoint_details);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(TokenRange other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetStart_token()).compareTo(other.isSetStart_token());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStart_token()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.start_token, other.start_token);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetEnd_token()).compareTo(other.isSetEnd_token());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetEnd_token()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.end_token, other.end_token);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetEndpoints()).compareTo(other.isSetEndpoints());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetEndpoints()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.endpoints, other.endpoints);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRpc_endpoints()).compareTo(other.isSetRpc_endpoints());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRpc_endpoints()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.rpc_endpoints, other.rpc_endpoints);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetEndpoint_details()).compareTo(other.isSetEndpoint_details());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetEndpoint_details()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.endpoint_details, other.endpoint_details);
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
    StringBuilder sb = new StringBuilder("TokenRange(");
    boolean first = true;

    sb.append("start_token:");
    if (this.start_token == null) {
      sb.append("null");
    } else {
      sb.append(this.start_token);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("end_token:");
    if (this.end_token == null) {
      sb.append("null");
    } else {
      sb.append(this.end_token);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("endpoints:");
    if (this.endpoints == null) {
      sb.append("null");
    } else {
      sb.append(this.endpoints);
    }
    first = false;
    if (isSetRpc_endpoints()) {
      if (!first) sb.append(", ");
      sb.append("rpc_endpoints:");
      if (this.rpc_endpoints == null) {
        sb.append("null");
      } else {
        sb.append(this.rpc_endpoints);
      }
      first = false;
    }
    if (isSetEndpoint_details()) {
      if (!first) sb.append(", ");
      sb.append("endpoint_details:");
      if (this.endpoint_details == null) {
        sb.append("null");
      } else {
        sb.append(this.endpoint_details);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (start_token == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'start_token' was not present! Struct: " + toString());
    }
    if (end_token == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'end_token' was not present! Struct: " + toString());
    }
    if (endpoints == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'endpoints' was not present! Struct: " + toString());
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

  private static class TokenRangeStandardSchemeFactory implements SchemeFactory {
    public TokenRangeStandardScheme getScheme() {
      return new TokenRangeStandardScheme();
    }
  }

  private static class TokenRangeStandardScheme extends StandardScheme<TokenRange> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TokenRange struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // START_TOKEN
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.start_token = iprot.readString();
              struct.setStart_tokenIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // END_TOKEN
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.end_token = iprot.readString();
              struct.setEnd_tokenIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // ENDPOINTS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list56 = iprot.readListBegin();
                struct.endpoints = new ArrayList<String>(_list56.size);
                for (int _i57 = 0; _i57 < _list56.size; ++_i57)
                {
                  String _elem58;
                  _elem58 = iprot.readString();
                  struct.endpoints.add(_elem58);
                }
                iprot.readListEnd();
              }
              struct.setEndpointsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // RPC_ENDPOINTS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list59 = iprot.readListBegin();
                struct.rpc_endpoints = new ArrayList<String>(_list59.size);
                for (int _i60 = 0; _i60 < _list59.size; ++_i60)
                {
                  String _elem61;
                  _elem61 = iprot.readString();
                  struct.rpc_endpoints.add(_elem61);
                }
                iprot.readListEnd();
              }
              struct.setRpc_endpointsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // ENDPOINT_DETAILS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list62 = iprot.readListBegin();
                struct.endpoint_details = new ArrayList<EndpointDetails>(_list62.size);
                for (int _i63 = 0; _i63 < _list62.size; ++_i63)
                {
                  EndpointDetails _elem64;
                  _elem64 = new EndpointDetails();
                  _elem64.read(iprot);
                  struct.endpoint_details.add(_elem64);
                }
                iprot.readListEnd();
              }
              struct.setEndpoint_detailsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TokenRange struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.start_token != null) {
        oprot.writeFieldBegin(START_TOKEN_FIELD_DESC);
        oprot.writeString(struct.start_token);
        oprot.writeFieldEnd();
      }
      if (struct.end_token != null) {
        oprot.writeFieldBegin(END_TOKEN_FIELD_DESC);
        oprot.writeString(struct.end_token);
        oprot.writeFieldEnd();
      }
      if (struct.endpoints != null) {
        oprot.writeFieldBegin(ENDPOINTS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.endpoints.size()));
          for (String _iter65 : struct.endpoints)
          {
            oprot.writeString(_iter65);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.rpc_endpoints != null) {
        if (struct.isSetRpc_endpoints()) {
          oprot.writeFieldBegin(RPC_ENDPOINTS_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.rpc_endpoints.size()));
            for (String _iter66 : struct.rpc_endpoints)
            {
              oprot.writeString(_iter66);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.endpoint_details != null) {
        if (struct.isSetEndpoint_details()) {
          oprot.writeFieldBegin(ENDPOINT_DETAILS_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.endpoint_details.size()));
            for (EndpointDetails _iter67 : struct.endpoint_details)
            {
              _iter67.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TokenRangeTupleSchemeFactory implements SchemeFactory {
    public TokenRangeTupleScheme getScheme() {
      return new TokenRangeTupleScheme();
    }
  }

  private static class TokenRangeTupleScheme extends TupleScheme<TokenRange> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TokenRange struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeString(struct.start_token);
      oprot.writeString(struct.end_token);
      {
        oprot.writeI32(struct.endpoints.size());
        for (String _iter68 : struct.endpoints)
        {
          oprot.writeString(_iter68);
        }
      }
      BitSet optionals = new BitSet();
      if (struct.isSetRpc_endpoints()) {
        optionals.set(0);
      }
      if (struct.isSetEndpoint_details()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetRpc_endpoints()) {
        {
          oprot.writeI32(struct.rpc_endpoints.size());
          for (String _iter69 : struct.rpc_endpoints)
          {
            oprot.writeString(_iter69);
          }
        }
      }
      if (struct.isSetEndpoint_details()) {
        {
          oprot.writeI32(struct.endpoint_details.size());
          for (EndpointDetails _iter70 : struct.endpoint_details)
          {
            _iter70.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TokenRange struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.start_token = iprot.readString();
      struct.setStart_tokenIsSet(true);
      struct.end_token = iprot.readString();
      struct.setEnd_tokenIsSet(true);
      {
        org.apache.thrift.protocol.TList _list71 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.endpoints = new ArrayList<String>(_list71.size);
        for (int _i72 = 0; _i72 < _list71.size; ++_i72)
        {
          String _elem73;
          _elem73 = iprot.readString();
          struct.endpoints.add(_elem73);
        }
      }
      struct.setEndpointsIsSet(true);
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list74 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.rpc_endpoints = new ArrayList<String>(_list74.size);
          for (int _i75 = 0; _i75 < _list74.size; ++_i75)
          {
            String _elem76;
            _elem76 = iprot.readString();
            struct.rpc_endpoints.add(_elem76);
          }
        }
        struct.setRpc_endpointsIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list77 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.endpoint_details = new ArrayList<EndpointDetails>(_list77.size);
          for (int _i78 = 0; _i78 < _list77.size; ++_i78)
          {
            EndpointDetails _elem79;
            _elem79 = new EndpointDetails();
            _elem79.read(iprot);
            struct.endpoint_details.add(_elem79);
          }
        }
        struct.setEndpoint_detailsIsSet(true);
      }
    }
  }

}

