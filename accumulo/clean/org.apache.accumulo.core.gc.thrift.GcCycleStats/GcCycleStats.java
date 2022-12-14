import org.apache.accumulo.core.gc.thrift.*;


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
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class GcCycleStats implements org.apache.thrift.TBase<GcCycleStats, GcCycleStats._Fields>, java.io.Serializable, Cloneable, Comparable<GcCycleStats> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("GcCycleStats");

  private static final org.apache.thrift.protocol.TField STARTED_FIELD_DESC = new org.apache.thrift.protocol.TField("started", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField FINISHED_FIELD_DESC = new org.apache.thrift.protocol.TField("finished", org.apache.thrift.protocol.TType.I64, (short)2);
  private static final org.apache.thrift.protocol.TField CANDIDATES_FIELD_DESC = new org.apache.thrift.protocol.TField("candidates", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField IN_USE_FIELD_DESC = new org.apache.thrift.protocol.TField("inUse", org.apache.thrift.protocol.TType.I64, (short)4);
  private static final org.apache.thrift.protocol.TField DELETED_FIELD_DESC = new org.apache.thrift.protocol.TField("deleted", org.apache.thrift.protocol.TType.I64, (short)5);
  private static final org.apache.thrift.protocol.TField ERRORS_FIELD_DESC = new org.apache.thrift.protocol.TField("errors", org.apache.thrift.protocol.TType.I64, (short)6);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new GcCycleStatsStandardSchemeFactory());
    schemes.put(TupleScheme.class, new GcCycleStatsTupleSchemeFactory());
  }

  public long started; // required
  public long finished; // required
  public long candidates; // required
  public long inUse; // required
  public long deleted; // required
  public long errors; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    STARTED((short)1, "started"),
    FINISHED((short)2, "finished"),
    CANDIDATES((short)3, "candidates"),
    IN_USE((short)4, "inUse"),
    DELETED((short)5, "deleted"),
    ERRORS((short)6, "errors");

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
        case 1: // STARTED
          return STARTED;
        case 2: // FINISHED
          return FINISHED;
        case 3: // CANDIDATES
          return CANDIDATES;
        case 4: // IN_USE
          return IN_USE;
        case 5: // DELETED
          return DELETED;
        case 6: // ERRORS
          return ERRORS;
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
  private static final int __STARTED_ISSET_ID = 0;
  private static final int __FINISHED_ISSET_ID = 1;
  private static final int __CANDIDATES_ISSET_ID = 2;
  private static final int __INUSE_ISSET_ID = 3;
  private static final int __DELETED_ISSET_ID = 4;
  private static final int __ERRORS_ISSET_ID = 5;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.STARTED, new org.apache.thrift.meta_data.FieldMetaData("started", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.FINISHED, new org.apache.thrift.meta_data.FieldMetaData("finished", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.CANDIDATES, new org.apache.thrift.meta_data.FieldMetaData("candidates", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.IN_USE, new org.apache.thrift.meta_data.FieldMetaData("inUse", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.DELETED, new org.apache.thrift.meta_data.FieldMetaData("deleted", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.ERRORS, new org.apache.thrift.meta_data.FieldMetaData("errors", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(GcCycleStats.class, metaDataMap);
  }

  public GcCycleStats() {
  }

  public GcCycleStats(
    long started,
    long finished,
    long candidates,
    long inUse,
    long deleted,
    long errors)
  {
    this();
    this.started = started;
    setStartedIsSet(true);
    this.finished = finished;
    setFinishedIsSet(true);
    this.candidates = candidates;
    setCandidatesIsSet(true);
    this.inUse = inUse;
    setInUseIsSet(true);
    this.deleted = deleted;
    setDeletedIsSet(true);
    this.errors = errors;
    setErrorsIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public GcCycleStats(GcCycleStats other) {
    __isset_bitfield = other.__isset_bitfield;
    this.started = other.started;
    this.finished = other.finished;
    this.candidates = other.candidates;
    this.inUse = other.inUse;
    this.deleted = other.deleted;
    this.errors = other.errors;
  }

  public GcCycleStats deepCopy() {
    return new GcCycleStats(this);
  }

  @Override
  public void clear() {
    setStartedIsSet(false);
    this.started = 0;
    setFinishedIsSet(false);
    this.finished = 0;
    setCandidatesIsSet(false);
    this.candidates = 0;
    setInUseIsSet(false);
    this.inUse = 0;
    setDeletedIsSet(false);
    this.deleted = 0;
    setErrorsIsSet(false);
    this.errors = 0;
  }

  public long getStarted() {
    return this.started;
  }

  public GcCycleStats setStarted(long started) {
    this.started = started;
    setStartedIsSet(true);
    return this;
  }

  public void unsetStarted() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __STARTED_ISSET_ID);
  }

  /** Returns true if field started is set (has been assigned a value) and false otherwise */
  public boolean isSetStarted() {
    return EncodingUtils.testBit(__isset_bitfield, __STARTED_ISSET_ID);
  }

  public void setStartedIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __STARTED_ISSET_ID, value);
  }

  public long getFinished() {
    return this.finished;
  }

  public GcCycleStats setFinished(long finished) {
    this.finished = finished;
    setFinishedIsSet(true);
    return this;
  }

  public void unsetFinished() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __FINISHED_ISSET_ID);
  }

  /** Returns true if field finished is set (has been assigned a value) and false otherwise */
  public boolean isSetFinished() {
    return EncodingUtils.testBit(__isset_bitfield, __FINISHED_ISSET_ID);
  }

  public void setFinishedIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __FINISHED_ISSET_ID, value);
  }

  public long getCandidates() {
    return this.candidates;
  }

  public GcCycleStats setCandidates(long candidates) {
    this.candidates = candidates;
    setCandidatesIsSet(true);
    return this;
  }

  public void unsetCandidates() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __CANDIDATES_ISSET_ID);
  }

  /** Returns true if field candidates is set (has been assigned a value) and false otherwise */
  public boolean isSetCandidates() {
    return EncodingUtils.testBit(__isset_bitfield, __CANDIDATES_ISSET_ID);
  }

  public void setCandidatesIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __CANDIDATES_ISSET_ID, value);
  }

  public long getInUse() {
    return this.inUse;
  }

  public GcCycleStats setInUse(long inUse) {
    this.inUse = inUse;
    setInUseIsSet(true);
    return this;
  }

  public void unsetInUse() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __INUSE_ISSET_ID);
  }

  /** Returns true if field inUse is set (has been assigned a value) and false otherwise */
  public boolean isSetInUse() {
    return EncodingUtils.testBit(__isset_bitfield, __INUSE_ISSET_ID);
  }

  public void setInUseIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __INUSE_ISSET_ID, value);
  }

  public long getDeleted() {
    return this.deleted;
  }

  public GcCycleStats setDeleted(long deleted) {
    this.deleted = deleted;
    setDeletedIsSet(true);
    return this;
  }

  public void unsetDeleted() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __DELETED_ISSET_ID);
  }

  /** Returns true if field deleted is set (has been assigned a value) and false otherwise */
  public boolean isSetDeleted() {
    return EncodingUtils.testBit(__isset_bitfield, __DELETED_ISSET_ID);
  }

  public void setDeletedIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __DELETED_ISSET_ID, value);
  }

  public long getErrors() {
    return this.errors;
  }

  public GcCycleStats setErrors(long errors) {
    this.errors = errors;
    setErrorsIsSet(true);
    return this;
  }

  public void unsetErrors() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ERRORS_ISSET_ID);
  }

  /** Returns true if field errors is set (has been assigned a value) and false otherwise */
  public boolean isSetErrors() {
    return EncodingUtils.testBit(__isset_bitfield, __ERRORS_ISSET_ID);
  }

  public void setErrorsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ERRORS_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case STARTED:
      if (value == null) {
        unsetStarted();
      } else {
        setStarted((Long)value);
      }
      break;

    case FINISHED:
      if (value == null) {
        unsetFinished();
      } else {
        setFinished((Long)value);
      }
      break;

    case CANDIDATES:
      if (value == null) {
        unsetCandidates();
      } else {
        setCandidates((Long)value);
      }
      break;

    case IN_USE:
      if (value == null) {
        unsetInUse();
      } else {
        setInUse((Long)value);
      }
      break;

    case DELETED:
      if (value == null) {
        unsetDeleted();
      } else {
        setDeleted((Long)value);
      }
      break;

    case ERRORS:
      if (value == null) {
        unsetErrors();
      } else {
        setErrors((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case STARTED:
      return getStarted();

    case FINISHED:
      return getFinished();

    case CANDIDATES:
      return getCandidates();

    case IN_USE:
      return getInUse();

    case DELETED:
      return getDeleted();

    case ERRORS:
      return getErrors();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case STARTED:
      return isSetStarted();
    case FINISHED:
      return isSetFinished();
    case CANDIDATES:
      return isSetCandidates();
    case IN_USE:
      return isSetInUse();
    case DELETED:
      return isSetDeleted();
    case ERRORS:
      return isSetErrors();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof GcCycleStats)
      return this.equals((GcCycleStats)that);
    return false;
  }

  public boolean equals(GcCycleStats that) {
    if (that == null)
      return false;

    boolean this_present_started = true;
    boolean that_present_started = true;
    if (this_present_started || that_present_started) {
      if (!(this_present_started && that_present_started))
        return false;
      if (this.started != that.started)
        return false;
    }

    boolean this_present_finished = true;
    boolean that_present_finished = true;
    if (this_present_finished || that_present_finished) {
      if (!(this_present_finished && that_present_finished))
        return false;
      if (this.finished != that.finished)
        return false;
    }

    boolean this_present_candidates = true;
    boolean that_present_candidates = true;
    if (this_present_candidates || that_present_candidates) {
      if (!(this_present_candidates && that_present_candidates))
        return false;
      if (this.candidates != that.candidates)
        return false;
    }

    boolean this_present_inUse = true;
    boolean that_present_inUse = true;
    if (this_present_inUse || that_present_inUse) {
      if (!(this_present_inUse && that_present_inUse))
        return false;
      if (this.inUse != that.inUse)
        return false;
    }

    boolean this_present_deleted = true;
    boolean that_present_deleted = true;
    if (this_present_deleted || that_present_deleted) {
      if (!(this_present_deleted && that_present_deleted))
        return false;
      if (this.deleted != that.deleted)
        return false;
    }

    boolean this_present_errors = true;
    boolean that_present_errors = true;
    if (this_present_errors || that_present_errors) {
      if (!(this_present_errors && that_present_errors))
        return false;
      if (this.errors != that.errors)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_started = true;
    list.add(present_started);
    if (present_started)
      list.add(started);

    boolean present_finished = true;
    list.add(present_finished);
    if (present_finished)
      list.add(finished);

    boolean present_candidates = true;
    list.add(present_candidates);
    if (present_candidates)
      list.add(candidates);

    boolean present_inUse = true;
    list.add(present_inUse);
    if (present_inUse)
      list.add(inUse);

    boolean present_deleted = true;
    list.add(present_deleted);
    if (present_deleted)
      list.add(deleted);

    boolean present_errors = true;
    list.add(present_errors);
    if (present_errors)
      list.add(errors);

    return list.hashCode();
  }

  @Override
  public int compareTo(GcCycleStats other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetStarted()).compareTo(other.isSetStarted());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStarted()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.started, other.started);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFinished()).compareTo(other.isSetFinished());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFinished()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.finished, other.finished);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCandidates()).compareTo(other.isSetCandidates());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCandidates()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.candidates, other.candidates);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetInUse()).compareTo(other.isSetInUse());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetInUse()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.inUse, other.inUse);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDeleted()).compareTo(other.isSetDeleted());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDeleted()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.deleted, other.deleted);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetErrors()).compareTo(other.isSetErrors());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetErrors()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.errors, other.errors);
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
    StringBuilder sb = new StringBuilder("GcCycleStats(");
    boolean first = true;

    sb.append("started:");
    sb.append(this.started);
    first = false;
    if (!first) sb.append(", ");
    sb.append("finished:");
    sb.append(this.finished);
    first = false;
    if (!first) sb.append(", ");
    sb.append("candidates:");
    sb.append(this.candidates);
    first = false;
    if (!first) sb.append(", ");
    sb.append("inUse:");
    sb.append(this.inUse);
    first = false;
    if (!first) sb.append(", ");
    sb.append("deleted:");
    sb.append(this.deleted);
    first = false;
    if (!first) sb.append(", ");
    sb.append("errors:");
    sb.append(this.errors);
    first = false;
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

  private static class GcCycleStatsStandardSchemeFactory implements SchemeFactory {
    public GcCycleStatsStandardScheme getScheme() {
      return new GcCycleStatsStandardScheme();
    }
  }

  private static class GcCycleStatsStandardScheme extends StandardScheme<GcCycleStats> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, GcCycleStats struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // STARTED
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.started = iprot.readI64();
              struct.setStartedIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // FINISHED
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.finished = iprot.readI64();
              struct.setFinishedIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // CANDIDATES
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.candidates = iprot.readI64();
              struct.setCandidatesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // IN_USE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.inUse = iprot.readI64();
              struct.setInUseIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // DELETED
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.deleted = iprot.readI64();
              struct.setDeletedIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // ERRORS
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.errors = iprot.readI64();
              struct.setErrorsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, GcCycleStats struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(STARTED_FIELD_DESC);
      oprot.writeI64(struct.started);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(FINISHED_FIELD_DESC);
      oprot.writeI64(struct.finished);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(CANDIDATES_FIELD_DESC);
      oprot.writeI64(struct.candidates);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(IN_USE_FIELD_DESC);
      oprot.writeI64(struct.inUse);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(DELETED_FIELD_DESC);
      oprot.writeI64(struct.deleted);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(ERRORS_FIELD_DESC);
      oprot.writeI64(struct.errors);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class GcCycleStatsTupleSchemeFactory implements SchemeFactory {
    public GcCycleStatsTupleScheme getScheme() {
      return new GcCycleStatsTupleScheme();
    }
  }

  private static class GcCycleStatsTupleScheme extends TupleScheme<GcCycleStats> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, GcCycleStats struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetStarted()) {
        optionals.set(0);
      }
      if (struct.isSetFinished()) {
        optionals.set(1);
      }
      if (struct.isSetCandidates()) {
        optionals.set(2);
      }
      if (struct.isSetInUse()) {
        optionals.set(3);
      }
      if (struct.isSetDeleted()) {
        optionals.set(4);
      }
      if (struct.isSetErrors()) {
        optionals.set(5);
      }
      oprot.writeBitSet(optionals, 6);
      if (struct.isSetStarted()) {
        oprot.writeI64(struct.started);
      }
      if (struct.isSetFinished()) {
        oprot.writeI64(struct.finished);
      }
      if (struct.isSetCandidates()) {
        oprot.writeI64(struct.candidates);
      }
      if (struct.isSetInUse()) {
        oprot.writeI64(struct.inUse);
      }
      if (struct.isSetDeleted()) {
        oprot.writeI64(struct.deleted);
      }
      if (struct.isSetErrors()) {
        oprot.writeI64(struct.errors);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, GcCycleStats struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(6);
      if (incoming.get(0)) {
        struct.started = iprot.readI64();
        struct.setStartedIsSet(true);
      }
      if (incoming.get(1)) {
        struct.finished = iprot.readI64();
        struct.setFinishedIsSet(true);
      }
      if (incoming.get(2)) {
        struct.candidates = iprot.readI64();
        struct.setCandidatesIsSet(true);
      }
      if (incoming.get(3)) {
        struct.inUse = iprot.readI64();
        struct.setInUseIsSet(true);
      }
      if (incoming.get(4)) {
        struct.deleted = iprot.readI64();
        struct.setDeletedIsSet(true);
      }
      if (incoming.get(5)) {
        struct.errors = iprot.readI64();
        struct.setErrorsIsSet(true);
      }
    }
  }

}

