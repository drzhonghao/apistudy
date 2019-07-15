import org.apache.cassandra.thrift.ColumnDef;
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

public class CfDef implements org.apache.thrift.TBase<CfDef, CfDef._Fields>, java.io.Serializable, Cloneable, Comparable<CfDef> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CfDef");

  private static final org.apache.thrift.protocol.TField KEYSPACE_FIELD_DESC = new org.apache.thrift.protocol.TField("keyspace", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("name", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField COLUMN_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("column_type", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField COMPARATOR_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("comparator_type", org.apache.thrift.protocol.TType.STRING, (short)5);
  private static final org.apache.thrift.protocol.TField SUBCOMPARATOR_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("subcomparator_type", org.apache.thrift.protocol.TType.STRING, (short)6);
  private static final org.apache.thrift.protocol.TField COMMENT_FIELD_DESC = new org.apache.thrift.protocol.TField("comment", org.apache.thrift.protocol.TType.STRING, (short)8);
  private static final org.apache.thrift.protocol.TField READ_REPAIR_CHANCE_FIELD_DESC = new org.apache.thrift.protocol.TField("read_repair_chance", org.apache.thrift.protocol.TType.DOUBLE, (short)12);
  private static final org.apache.thrift.protocol.TField COLUMN_METADATA_FIELD_DESC = new org.apache.thrift.protocol.TField("column_metadata", org.apache.thrift.protocol.TType.LIST, (short)13);
  private static final org.apache.thrift.protocol.TField GC_GRACE_SECONDS_FIELD_DESC = new org.apache.thrift.protocol.TField("gc_grace_seconds", org.apache.thrift.protocol.TType.I32, (short)14);
  private static final org.apache.thrift.protocol.TField DEFAULT_VALIDATION_CLASS_FIELD_DESC = new org.apache.thrift.protocol.TField("default_validation_class", org.apache.thrift.protocol.TType.STRING, (short)15);
  private static final org.apache.thrift.protocol.TField ID_FIELD_DESC = new org.apache.thrift.protocol.TField("id", org.apache.thrift.protocol.TType.I32, (short)16);
  private static final org.apache.thrift.protocol.TField MIN_COMPACTION_THRESHOLD_FIELD_DESC = new org.apache.thrift.protocol.TField("min_compaction_threshold", org.apache.thrift.protocol.TType.I32, (short)17);
  private static final org.apache.thrift.protocol.TField MAX_COMPACTION_THRESHOLD_FIELD_DESC = new org.apache.thrift.protocol.TField("max_compaction_threshold", org.apache.thrift.protocol.TType.I32, (short)18);
  private static final org.apache.thrift.protocol.TField KEY_VALIDATION_CLASS_FIELD_DESC = new org.apache.thrift.protocol.TField("key_validation_class", org.apache.thrift.protocol.TType.STRING, (short)26);
  private static final org.apache.thrift.protocol.TField KEY_ALIAS_FIELD_DESC = new org.apache.thrift.protocol.TField("key_alias", org.apache.thrift.protocol.TType.STRING, (short)28);
  private static final org.apache.thrift.protocol.TField COMPACTION_STRATEGY_FIELD_DESC = new org.apache.thrift.protocol.TField("compaction_strategy", org.apache.thrift.protocol.TType.STRING, (short)29);
  private static final org.apache.thrift.protocol.TField COMPACTION_STRATEGY_OPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("compaction_strategy_options", org.apache.thrift.protocol.TType.MAP, (short)30);
  private static final org.apache.thrift.protocol.TField COMPRESSION_OPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("compression_options", org.apache.thrift.protocol.TType.MAP, (short)32);
  private static final org.apache.thrift.protocol.TField BLOOM_FILTER_FP_CHANCE_FIELD_DESC = new org.apache.thrift.protocol.TField("bloom_filter_fp_chance", org.apache.thrift.protocol.TType.DOUBLE, (short)33);
  private static final org.apache.thrift.protocol.TField CACHING_FIELD_DESC = new org.apache.thrift.protocol.TField("caching", org.apache.thrift.protocol.TType.STRING, (short)34);
  private static final org.apache.thrift.protocol.TField DCLOCAL_READ_REPAIR_CHANCE_FIELD_DESC = new org.apache.thrift.protocol.TField("dclocal_read_repair_chance", org.apache.thrift.protocol.TType.DOUBLE, (short)37);
  private static final org.apache.thrift.protocol.TField MEMTABLE_FLUSH_PERIOD_IN_MS_FIELD_DESC = new org.apache.thrift.protocol.TField("memtable_flush_period_in_ms", org.apache.thrift.protocol.TType.I32, (short)39);
  private static final org.apache.thrift.protocol.TField DEFAULT_TIME_TO_LIVE_FIELD_DESC = new org.apache.thrift.protocol.TField("default_time_to_live", org.apache.thrift.protocol.TType.I32, (short)40);
  private static final org.apache.thrift.protocol.TField SPECULATIVE_RETRY_FIELD_DESC = new org.apache.thrift.protocol.TField("speculative_retry", org.apache.thrift.protocol.TType.STRING, (short)42);
  private static final org.apache.thrift.protocol.TField TRIGGERS_FIELD_DESC = new org.apache.thrift.protocol.TField("triggers", org.apache.thrift.protocol.TType.LIST, (short)43);
  private static final org.apache.thrift.protocol.TField CELLS_PER_ROW_TO_CACHE_FIELD_DESC = new org.apache.thrift.protocol.TField("cells_per_row_to_cache", org.apache.thrift.protocol.TType.STRING, (short)44);
  private static final org.apache.thrift.protocol.TField MIN_INDEX_INTERVAL_FIELD_DESC = new org.apache.thrift.protocol.TField("min_index_interval", org.apache.thrift.protocol.TType.I32, (short)45);
  private static final org.apache.thrift.protocol.TField MAX_INDEX_INTERVAL_FIELD_DESC = new org.apache.thrift.protocol.TField("max_index_interval", org.apache.thrift.protocol.TType.I32, (short)46);
  private static final org.apache.thrift.protocol.TField ROW_CACHE_SIZE_FIELD_DESC = new org.apache.thrift.protocol.TField("row_cache_size", org.apache.thrift.protocol.TType.DOUBLE, (short)9);
  private static final org.apache.thrift.protocol.TField KEY_CACHE_SIZE_FIELD_DESC = new org.apache.thrift.protocol.TField("key_cache_size", org.apache.thrift.protocol.TType.DOUBLE, (short)11);
  private static final org.apache.thrift.protocol.TField ROW_CACHE_SAVE_PERIOD_IN_SECONDS_FIELD_DESC = new org.apache.thrift.protocol.TField("row_cache_save_period_in_seconds", org.apache.thrift.protocol.TType.I32, (short)19);
  private static final org.apache.thrift.protocol.TField KEY_CACHE_SAVE_PERIOD_IN_SECONDS_FIELD_DESC = new org.apache.thrift.protocol.TField("key_cache_save_period_in_seconds", org.apache.thrift.protocol.TType.I32, (short)20);
  private static final org.apache.thrift.protocol.TField MEMTABLE_FLUSH_AFTER_MINS_FIELD_DESC = new org.apache.thrift.protocol.TField("memtable_flush_after_mins", org.apache.thrift.protocol.TType.I32, (short)21);
  private static final org.apache.thrift.protocol.TField MEMTABLE_THROUGHPUT_IN_MB_FIELD_DESC = new org.apache.thrift.protocol.TField("memtable_throughput_in_mb", org.apache.thrift.protocol.TType.I32, (short)22);
  private static final org.apache.thrift.protocol.TField MEMTABLE_OPERATIONS_IN_MILLIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("memtable_operations_in_millions", org.apache.thrift.protocol.TType.DOUBLE, (short)23);
  private static final org.apache.thrift.protocol.TField REPLICATE_ON_WRITE_FIELD_DESC = new org.apache.thrift.protocol.TField("replicate_on_write", org.apache.thrift.protocol.TType.BOOL, (short)24);
  private static final org.apache.thrift.protocol.TField MERGE_SHARDS_CHANCE_FIELD_DESC = new org.apache.thrift.protocol.TField("merge_shards_chance", org.apache.thrift.protocol.TType.DOUBLE, (short)25);
  private static final org.apache.thrift.protocol.TField ROW_CACHE_PROVIDER_FIELD_DESC = new org.apache.thrift.protocol.TField("row_cache_provider", org.apache.thrift.protocol.TType.STRING, (short)27);
  private static final org.apache.thrift.protocol.TField ROW_CACHE_KEYS_TO_SAVE_FIELD_DESC = new org.apache.thrift.protocol.TField("row_cache_keys_to_save", org.apache.thrift.protocol.TType.I32, (short)31);
  private static final org.apache.thrift.protocol.TField POPULATE_IO_CACHE_ON_FLUSH_FIELD_DESC = new org.apache.thrift.protocol.TField("populate_io_cache_on_flush", org.apache.thrift.protocol.TType.BOOL, (short)38);
  private static final org.apache.thrift.protocol.TField INDEX_INTERVAL_FIELD_DESC = new org.apache.thrift.protocol.TField("index_interval", org.apache.thrift.protocol.TType.I32, (short)41);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CfDefStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CfDefTupleSchemeFactory());
  }

  public String keyspace; // required
  public String name; // required
  public String column_type; // optional
  public String comparator_type; // optional
  public String subcomparator_type; // optional
  public String comment; // optional
  public double read_repair_chance; // optional
  public List<ColumnDef> column_metadata; // optional
  public int gc_grace_seconds; // optional
  public String default_validation_class; // optional
  public int id; // optional
  public int min_compaction_threshold; // optional
  public int max_compaction_threshold; // optional
  public String key_validation_class; // optional
  public ByteBuffer key_alias; // optional
  public String compaction_strategy; // optional
  public Map<String,String> compaction_strategy_options; // optional
  public Map<String,String> compression_options; // optional
  public double bloom_filter_fp_chance; // optional
  public String caching; // optional
  public double dclocal_read_repair_chance; // optional
  public int memtable_flush_period_in_ms; // optional
  public int default_time_to_live; // optional
  public String speculative_retry; // optional
  public List<TriggerDef> triggers; // optional
  public String cells_per_row_to_cache; // optional
  public int min_index_interval; // optional
  public int max_index_interval; // optional
  /**
   * @deprecated
   */
  public double row_cache_size; // optional
  /**
   * @deprecated
   */
  public double key_cache_size; // optional
  /**
   * @deprecated
   */
  public int row_cache_save_period_in_seconds; // optional
  /**
   * @deprecated
   */
  public int key_cache_save_period_in_seconds; // optional
  /**
   * @deprecated
   */
  public int memtable_flush_after_mins; // optional
  /**
   * @deprecated
   */
  public int memtable_throughput_in_mb; // optional
  /**
   * @deprecated
   */
  public double memtable_operations_in_millions; // optional
  /**
   * @deprecated
   */
  public boolean replicate_on_write; // optional
  /**
   * @deprecated
   */
  public double merge_shards_chance; // optional
  /**
   * @deprecated
   */
  public String row_cache_provider; // optional
  /**
   * @deprecated
   */
  public int row_cache_keys_to_save; // optional
  /**
   * @deprecated
   */
  public boolean populate_io_cache_on_flush; // optional
  /**
   * @deprecated
   */
  public int index_interval; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    KEYSPACE((short)1, "keyspace"),
    NAME((short)2, "name"),
    COLUMN_TYPE((short)3, "column_type"),
    COMPARATOR_TYPE((short)5, "comparator_type"),
    SUBCOMPARATOR_TYPE((short)6, "subcomparator_type"),
    COMMENT((short)8, "comment"),
    READ_REPAIR_CHANCE((short)12, "read_repair_chance"),
    COLUMN_METADATA((short)13, "column_metadata"),
    GC_GRACE_SECONDS((short)14, "gc_grace_seconds"),
    DEFAULT_VALIDATION_CLASS((short)15, "default_validation_class"),
    ID((short)16, "id"),
    MIN_COMPACTION_THRESHOLD((short)17, "min_compaction_threshold"),
    MAX_COMPACTION_THRESHOLD((short)18, "max_compaction_threshold"),
    KEY_VALIDATION_CLASS((short)26, "key_validation_class"),
    KEY_ALIAS((short)28, "key_alias"),
    COMPACTION_STRATEGY((short)29, "compaction_strategy"),
    COMPACTION_STRATEGY_OPTIONS((short)30, "compaction_strategy_options"),
    COMPRESSION_OPTIONS((short)32, "compression_options"),
    BLOOM_FILTER_FP_CHANCE((short)33, "bloom_filter_fp_chance"),
    CACHING((short)34, "caching"),
    DCLOCAL_READ_REPAIR_CHANCE((short)37, "dclocal_read_repair_chance"),
    MEMTABLE_FLUSH_PERIOD_IN_MS((short)39, "memtable_flush_period_in_ms"),
    DEFAULT_TIME_TO_LIVE((short)40, "default_time_to_live"),
    SPECULATIVE_RETRY((short)42, "speculative_retry"),
    TRIGGERS((short)43, "triggers"),
    CELLS_PER_ROW_TO_CACHE((short)44, "cells_per_row_to_cache"),
    MIN_INDEX_INTERVAL((short)45, "min_index_interval"),
    MAX_INDEX_INTERVAL((short)46, "max_index_interval"),
    /**
     * @deprecated
     */
    ROW_CACHE_SIZE((short)9, "row_cache_size"),
    /**
     * @deprecated
     */
    KEY_CACHE_SIZE((short)11, "key_cache_size"),
    /**
     * @deprecated
     */
    ROW_CACHE_SAVE_PERIOD_IN_SECONDS((short)19, "row_cache_save_period_in_seconds"),
    /**
     * @deprecated
     */
    KEY_CACHE_SAVE_PERIOD_IN_SECONDS((short)20, "key_cache_save_period_in_seconds"),
    /**
     * @deprecated
     */
    MEMTABLE_FLUSH_AFTER_MINS((short)21, "memtable_flush_after_mins"),
    /**
     * @deprecated
     */
    MEMTABLE_THROUGHPUT_IN_MB((short)22, "memtable_throughput_in_mb"),
    /**
     * @deprecated
     */
    MEMTABLE_OPERATIONS_IN_MILLIONS((short)23, "memtable_operations_in_millions"),
    /**
     * @deprecated
     */
    REPLICATE_ON_WRITE((short)24, "replicate_on_write"),
    /**
     * @deprecated
     */
    MERGE_SHARDS_CHANCE((short)25, "merge_shards_chance"),
    /**
     * @deprecated
     */
    ROW_CACHE_PROVIDER((short)27, "row_cache_provider"),
    /**
     * @deprecated
     */
    ROW_CACHE_KEYS_TO_SAVE((short)31, "row_cache_keys_to_save"),
    /**
     * @deprecated
     */
    POPULATE_IO_CACHE_ON_FLUSH((short)38, "populate_io_cache_on_flush"),
    /**
     * @deprecated
     */
    INDEX_INTERVAL((short)41, "index_interval");

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
        case 1: // KEYSPACE
          return KEYSPACE;
        case 2: // NAME
          return NAME;
        case 3: // COLUMN_TYPE
          return COLUMN_TYPE;
        case 5: // COMPARATOR_TYPE
          return COMPARATOR_TYPE;
        case 6: // SUBCOMPARATOR_TYPE
          return SUBCOMPARATOR_TYPE;
        case 8: // COMMENT
          return COMMENT;
        case 12: // READ_REPAIR_CHANCE
          return READ_REPAIR_CHANCE;
        case 13: // COLUMN_METADATA
          return COLUMN_METADATA;
        case 14: // GC_GRACE_SECONDS
          return GC_GRACE_SECONDS;
        case 15: // DEFAULT_VALIDATION_CLASS
          return DEFAULT_VALIDATION_CLASS;
        case 16: // ID
          return ID;
        case 17: // MIN_COMPACTION_THRESHOLD
          return MIN_COMPACTION_THRESHOLD;
        case 18: // MAX_COMPACTION_THRESHOLD
          return MAX_COMPACTION_THRESHOLD;
        case 26: // KEY_VALIDATION_CLASS
          return KEY_VALIDATION_CLASS;
        case 28: // KEY_ALIAS
          return KEY_ALIAS;
        case 29: // COMPACTION_STRATEGY
          return COMPACTION_STRATEGY;
        case 30: // COMPACTION_STRATEGY_OPTIONS
          return COMPACTION_STRATEGY_OPTIONS;
        case 32: // COMPRESSION_OPTIONS
          return COMPRESSION_OPTIONS;
        case 33: // BLOOM_FILTER_FP_CHANCE
          return BLOOM_FILTER_FP_CHANCE;
        case 34: // CACHING
          return CACHING;
        case 37: // DCLOCAL_READ_REPAIR_CHANCE
          return DCLOCAL_READ_REPAIR_CHANCE;
        case 39: // MEMTABLE_FLUSH_PERIOD_IN_MS
          return MEMTABLE_FLUSH_PERIOD_IN_MS;
        case 40: // DEFAULT_TIME_TO_LIVE
          return DEFAULT_TIME_TO_LIVE;
        case 42: // SPECULATIVE_RETRY
          return SPECULATIVE_RETRY;
        case 43: // TRIGGERS
          return TRIGGERS;
        case 44: // CELLS_PER_ROW_TO_CACHE
          return CELLS_PER_ROW_TO_CACHE;
        case 45: // MIN_INDEX_INTERVAL
          return MIN_INDEX_INTERVAL;
        case 46: // MAX_INDEX_INTERVAL
          return MAX_INDEX_INTERVAL;
        case 9: // ROW_CACHE_SIZE
          return ROW_CACHE_SIZE;
        case 11: // KEY_CACHE_SIZE
          return KEY_CACHE_SIZE;
        case 19: // ROW_CACHE_SAVE_PERIOD_IN_SECONDS
          return ROW_CACHE_SAVE_PERIOD_IN_SECONDS;
        case 20: // KEY_CACHE_SAVE_PERIOD_IN_SECONDS
          return KEY_CACHE_SAVE_PERIOD_IN_SECONDS;
        case 21: // MEMTABLE_FLUSH_AFTER_MINS
          return MEMTABLE_FLUSH_AFTER_MINS;
        case 22: // MEMTABLE_THROUGHPUT_IN_MB
          return MEMTABLE_THROUGHPUT_IN_MB;
        case 23: // MEMTABLE_OPERATIONS_IN_MILLIONS
          return MEMTABLE_OPERATIONS_IN_MILLIONS;
        case 24: // REPLICATE_ON_WRITE
          return REPLICATE_ON_WRITE;
        case 25: // MERGE_SHARDS_CHANCE
          return MERGE_SHARDS_CHANCE;
        case 27: // ROW_CACHE_PROVIDER
          return ROW_CACHE_PROVIDER;
        case 31: // ROW_CACHE_KEYS_TO_SAVE
          return ROW_CACHE_KEYS_TO_SAVE;
        case 38: // POPULATE_IO_CACHE_ON_FLUSH
          return POPULATE_IO_CACHE_ON_FLUSH;
        case 41: // INDEX_INTERVAL
          return INDEX_INTERVAL;
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
  private static final int __READ_REPAIR_CHANCE_ISSET_ID = 0;
  private static final int __GC_GRACE_SECONDS_ISSET_ID = 1;
  private static final int __ID_ISSET_ID = 2;
  private static final int __MIN_COMPACTION_THRESHOLD_ISSET_ID = 3;
  private static final int __MAX_COMPACTION_THRESHOLD_ISSET_ID = 4;
  private static final int __BLOOM_FILTER_FP_CHANCE_ISSET_ID = 5;
  private static final int __DCLOCAL_READ_REPAIR_CHANCE_ISSET_ID = 6;
  private static final int __MEMTABLE_FLUSH_PERIOD_IN_MS_ISSET_ID = 7;
  private static final int __DEFAULT_TIME_TO_LIVE_ISSET_ID = 8;
  private static final int __MIN_INDEX_INTERVAL_ISSET_ID = 9;
  private static final int __MAX_INDEX_INTERVAL_ISSET_ID = 10;
  private static final int __ROW_CACHE_SIZE_ISSET_ID = 11;
  private static final int __KEY_CACHE_SIZE_ISSET_ID = 12;
  private static final int __ROW_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID = 13;
  private static final int __KEY_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID = 14;
  private static final int __MEMTABLE_FLUSH_AFTER_MINS_ISSET_ID = 15;
  private static final int __MEMTABLE_THROUGHPUT_IN_MB_ISSET_ID = 16;
  private static final int __MEMTABLE_OPERATIONS_IN_MILLIONS_ISSET_ID = 17;
  private static final int __REPLICATE_ON_WRITE_ISSET_ID = 18;
  private static final int __MERGE_SHARDS_CHANCE_ISSET_ID = 19;
  private static final int __ROW_CACHE_KEYS_TO_SAVE_ISSET_ID = 20;
  private static final int __POPULATE_IO_CACHE_ON_FLUSH_ISSET_ID = 21;
  private static final int __INDEX_INTERVAL_ISSET_ID = 22;
  private int __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.COLUMN_TYPE,_Fields.COMPARATOR_TYPE,_Fields.SUBCOMPARATOR_TYPE,_Fields.COMMENT,_Fields.READ_REPAIR_CHANCE,_Fields.COLUMN_METADATA,_Fields.GC_GRACE_SECONDS,_Fields.DEFAULT_VALIDATION_CLASS,_Fields.ID,_Fields.MIN_COMPACTION_THRESHOLD,_Fields.MAX_COMPACTION_THRESHOLD,_Fields.KEY_VALIDATION_CLASS,_Fields.KEY_ALIAS,_Fields.COMPACTION_STRATEGY,_Fields.COMPACTION_STRATEGY_OPTIONS,_Fields.COMPRESSION_OPTIONS,_Fields.BLOOM_FILTER_FP_CHANCE,_Fields.CACHING,_Fields.DCLOCAL_READ_REPAIR_CHANCE,_Fields.MEMTABLE_FLUSH_PERIOD_IN_MS,_Fields.DEFAULT_TIME_TO_LIVE,_Fields.SPECULATIVE_RETRY,_Fields.TRIGGERS,_Fields.CELLS_PER_ROW_TO_CACHE,_Fields.MIN_INDEX_INTERVAL,_Fields.MAX_INDEX_INTERVAL,_Fields.ROW_CACHE_SIZE,_Fields.KEY_CACHE_SIZE,_Fields.ROW_CACHE_SAVE_PERIOD_IN_SECONDS,_Fields.KEY_CACHE_SAVE_PERIOD_IN_SECONDS,_Fields.MEMTABLE_FLUSH_AFTER_MINS,_Fields.MEMTABLE_THROUGHPUT_IN_MB,_Fields.MEMTABLE_OPERATIONS_IN_MILLIONS,_Fields.REPLICATE_ON_WRITE,_Fields.MERGE_SHARDS_CHANCE,_Fields.ROW_CACHE_PROVIDER,_Fields.ROW_CACHE_KEYS_TO_SAVE,_Fields.POPULATE_IO_CACHE_ON_FLUSH,_Fields.INDEX_INTERVAL};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.KEYSPACE, new org.apache.thrift.meta_data.FieldMetaData("keyspace", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.NAME, new org.apache.thrift.meta_data.FieldMetaData("name", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.COLUMN_TYPE, new org.apache.thrift.meta_data.FieldMetaData("column_type", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.COMPARATOR_TYPE, new org.apache.thrift.meta_data.FieldMetaData("comparator_type", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.SUBCOMPARATOR_TYPE, new org.apache.thrift.meta_data.FieldMetaData("subcomparator_type", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.COMMENT, new org.apache.thrift.meta_data.FieldMetaData("comment", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.READ_REPAIR_CHANCE, new org.apache.thrift.meta_data.FieldMetaData("read_repair_chance", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.COLUMN_METADATA, new org.apache.thrift.meta_data.FieldMetaData("column_metadata", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ColumnDef.class))));
    tmpMap.put(_Fields.GC_GRACE_SECONDS, new org.apache.thrift.meta_data.FieldMetaData("gc_grace_seconds", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.DEFAULT_VALIDATION_CLASS, new org.apache.thrift.meta_data.FieldMetaData("default_validation_class", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.ID, new org.apache.thrift.meta_data.FieldMetaData("id", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MIN_COMPACTION_THRESHOLD, new org.apache.thrift.meta_data.FieldMetaData("min_compaction_threshold", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MAX_COMPACTION_THRESHOLD, new org.apache.thrift.meta_data.FieldMetaData("max_compaction_threshold", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.KEY_VALIDATION_CLASS, new org.apache.thrift.meta_data.FieldMetaData("key_validation_class", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.KEY_ALIAS, new org.apache.thrift.meta_data.FieldMetaData("key_alias", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.COMPACTION_STRATEGY, new org.apache.thrift.meta_data.FieldMetaData("compaction_strategy", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.COMPACTION_STRATEGY_OPTIONS, new org.apache.thrift.meta_data.FieldMetaData("compaction_strategy_options", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.COMPRESSION_OPTIONS, new org.apache.thrift.meta_data.FieldMetaData("compression_options", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.BLOOM_FILTER_FP_CHANCE, new org.apache.thrift.meta_data.FieldMetaData("bloom_filter_fp_chance", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.CACHING, new org.apache.thrift.meta_data.FieldMetaData("caching", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.DCLOCAL_READ_REPAIR_CHANCE, new org.apache.thrift.meta_data.FieldMetaData("dclocal_read_repair_chance", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.MEMTABLE_FLUSH_PERIOD_IN_MS, new org.apache.thrift.meta_data.FieldMetaData("memtable_flush_period_in_ms", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.DEFAULT_TIME_TO_LIVE, new org.apache.thrift.meta_data.FieldMetaData("default_time_to_live", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.SPECULATIVE_RETRY, new org.apache.thrift.meta_data.FieldMetaData("speculative_retry", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.TRIGGERS, new org.apache.thrift.meta_data.FieldMetaData("triggers", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TriggerDef.class))));
    tmpMap.put(_Fields.CELLS_PER_ROW_TO_CACHE, new org.apache.thrift.meta_data.FieldMetaData("cells_per_row_to_cache", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.MIN_INDEX_INTERVAL, new org.apache.thrift.meta_data.FieldMetaData("min_index_interval", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MAX_INDEX_INTERVAL, new org.apache.thrift.meta_data.FieldMetaData("max_index_interval", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.ROW_CACHE_SIZE, new org.apache.thrift.meta_data.FieldMetaData("row_cache_size", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.KEY_CACHE_SIZE, new org.apache.thrift.meta_data.FieldMetaData("key_cache_size", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.ROW_CACHE_SAVE_PERIOD_IN_SECONDS, new org.apache.thrift.meta_data.FieldMetaData("row_cache_save_period_in_seconds", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.KEY_CACHE_SAVE_PERIOD_IN_SECONDS, new org.apache.thrift.meta_data.FieldMetaData("key_cache_save_period_in_seconds", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MEMTABLE_FLUSH_AFTER_MINS, new org.apache.thrift.meta_data.FieldMetaData("memtable_flush_after_mins", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MEMTABLE_THROUGHPUT_IN_MB, new org.apache.thrift.meta_data.FieldMetaData("memtable_throughput_in_mb", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.MEMTABLE_OPERATIONS_IN_MILLIONS, new org.apache.thrift.meta_data.FieldMetaData("memtable_operations_in_millions", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.REPLICATE_ON_WRITE, new org.apache.thrift.meta_data.FieldMetaData("replicate_on_write", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.MERGE_SHARDS_CHANCE, new org.apache.thrift.meta_data.FieldMetaData("merge_shards_chance", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.ROW_CACHE_PROVIDER, new org.apache.thrift.meta_data.FieldMetaData("row_cache_provider", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.ROW_CACHE_KEYS_TO_SAVE, new org.apache.thrift.meta_data.FieldMetaData("row_cache_keys_to_save", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.POPULATE_IO_CACHE_ON_FLUSH, new org.apache.thrift.meta_data.FieldMetaData("populate_io_cache_on_flush", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.INDEX_INTERVAL, new org.apache.thrift.meta_data.FieldMetaData("index_interval", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CfDef.class, metaDataMap);
  }

  public CfDef() {
    this.column_type = "Standard";

    this.comparator_type = "BytesType";

    this.caching = "keys_only";

    this.dclocal_read_repair_chance = 0;

    this.speculative_retry = "NONE";

    this.cells_per_row_to_cache = "100";

  }

  public CfDef(
    String keyspace,
    String name)
  {
    this();
    this.keyspace = keyspace;
    this.name = name;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CfDef(CfDef other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetKeyspace()) {
      this.keyspace = other.keyspace;
    }
    if (other.isSetName()) {
      this.name = other.name;
    }
    if (other.isSetColumn_type()) {
      this.column_type = other.column_type;
    }
    if (other.isSetComparator_type()) {
      this.comparator_type = other.comparator_type;
    }
    if (other.isSetSubcomparator_type()) {
      this.subcomparator_type = other.subcomparator_type;
    }
    if (other.isSetComment()) {
      this.comment = other.comment;
    }
    this.read_repair_chance = other.read_repair_chance;
    if (other.isSetColumn_metadata()) {
      List<ColumnDef> __this__column_metadata = new ArrayList<ColumnDef>(other.column_metadata.size());
      for (ColumnDef other_element : other.column_metadata) {
        __this__column_metadata.add(new ColumnDef(other_element));
      }
      this.column_metadata = __this__column_metadata;
    }
    this.gc_grace_seconds = other.gc_grace_seconds;
    if (other.isSetDefault_validation_class()) {
      this.default_validation_class = other.default_validation_class;
    }
    this.id = other.id;
    this.min_compaction_threshold = other.min_compaction_threshold;
    this.max_compaction_threshold = other.max_compaction_threshold;
    if (other.isSetKey_validation_class()) {
      this.key_validation_class = other.key_validation_class;
    }
    if (other.isSetKey_alias()) {
      this.key_alias = org.apache.thrift.TBaseHelper.copyBinary(other.key_alias);
;
    }
    if (other.isSetCompaction_strategy()) {
      this.compaction_strategy = other.compaction_strategy;
    }
    if (other.isSetCompaction_strategy_options()) {
      Map<String,String> __this__compaction_strategy_options = new HashMap<String,String>(other.compaction_strategy_options);
      this.compaction_strategy_options = __this__compaction_strategy_options;
    }
    if (other.isSetCompression_options()) {
      Map<String,String> __this__compression_options = new HashMap<String,String>(other.compression_options);
      this.compression_options = __this__compression_options;
    }
    this.bloom_filter_fp_chance = other.bloom_filter_fp_chance;
    if (other.isSetCaching()) {
      this.caching = other.caching;
    }
    this.dclocal_read_repair_chance = other.dclocal_read_repair_chance;
    this.memtable_flush_period_in_ms = other.memtable_flush_period_in_ms;
    this.default_time_to_live = other.default_time_to_live;
    if (other.isSetSpeculative_retry()) {
      this.speculative_retry = other.speculative_retry;
    }
    if (other.isSetTriggers()) {
      List<TriggerDef> __this__triggers = new ArrayList<TriggerDef>(other.triggers.size());
      for (TriggerDef other_element : other.triggers) {
        __this__triggers.add(new TriggerDef(other_element));
      }
      this.triggers = __this__triggers;
    }
    if (other.isSetCells_per_row_to_cache()) {
      this.cells_per_row_to_cache = other.cells_per_row_to_cache;
    }
    this.min_index_interval = other.min_index_interval;
    this.max_index_interval = other.max_index_interval;
    this.row_cache_size = other.row_cache_size;
    this.key_cache_size = other.key_cache_size;
    this.row_cache_save_period_in_seconds = other.row_cache_save_period_in_seconds;
    this.key_cache_save_period_in_seconds = other.key_cache_save_period_in_seconds;
    this.memtable_flush_after_mins = other.memtable_flush_after_mins;
    this.memtable_throughput_in_mb = other.memtable_throughput_in_mb;
    this.memtable_operations_in_millions = other.memtable_operations_in_millions;
    this.replicate_on_write = other.replicate_on_write;
    this.merge_shards_chance = other.merge_shards_chance;
    if (other.isSetRow_cache_provider()) {
      this.row_cache_provider = other.row_cache_provider;
    }
    this.row_cache_keys_to_save = other.row_cache_keys_to_save;
    this.populate_io_cache_on_flush = other.populate_io_cache_on_flush;
    this.index_interval = other.index_interval;
  }

  public CfDef deepCopy() {
    return new CfDef(this);
  }

  @Override
  public void clear() {
    this.keyspace = null;
    this.name = null;
    this.column_type = "Standard";

    this.comparator_type = "BytesType";

    this.subcomparator_type = null;
    this.comment = null;
    setRead_repair_chanceIsSet(false);
    this.read_repair_chance = 0.0;
    this.column_metadata = null;
    setGc_grace_secondsIsSet(false);
    this.gc_grace_seconds = 0;
    this.default_validation_class = null;
    setIdIsSet(false);
    this.id = 0;
    setMin_compaction_thresholdIsSet(false);
    this.min_compaction_threshold = 0;
    setMax_compaction_thresholdIsSet(false);
    this.max_compaction_threshold = 0;
    this.key_validation_class = null;
    this.key_alias = null;
    this.compaction_strategy = null;
    this.compaction_strategy_options = null;
    this.compression_options = null;
    setBloom_filter_fp_chanceIsSet(false);
    this.bloom_filter_fp_chance = 0.0;
    this.caching = "keys_only";

    this.dclocal_read_repair_chance = 0;

    setMemtable_flush_period_in_msIsSet(false);
    this.memtable_flush_period_in_ms = 0;
    setDefault_time_to_liveIsSet(false);
    this.default_time_to_live = 0;
    this.speculative_retry = "NONE";

    this.triggers = null;
    this.cells_per_row_to_cache = "100";

    setMin_index_intervalIsSet(false);
    this.min_index_interval = 0;
    setMax_index_intervalIsSet(false);
    this.max_index_interval = 0;
    setRow_cache_sizeIsSet(false);
    this.row_cache_size = 0.0;
    setKey_cache_sizeIsSet(false);
    this.key_cache_size = 0.0;
    setRow_cache_save_period_in_secondsIsSet(false);
    this.row_cache_save_period_in_seconds = 0;
    setKey_cache_save_period_in_secondsIsSet(false);
    this.key_cache_save_period_in_seconds = 0;
    setMemtable_flush_after_minsIsSet(false);
    this.memtable_flush_after_mins = 0;
    setMemtable_throughput_in_mbIsSet(false);
    this.memtable_throughput_in_mb = 0;
    setMemtable_operations_in_millionsIsSet(false);
    this.memtable_operations_in_millions = 0.0;
    setReplicate_on_writeIsSet(false);
    this.replicate_on_write = false;
    setMerge_shards_chanceIsSet(false);
    this.merge_shards_chance = 0.0;
    this.row_cache_provider = null;
    setRow_cache_keys_to_saveIsSet(false);
    this.row_cache_keys_to_save = 0;
    setPopulate_io_cache_on_flushIsSet(false);
    this.populate_io_cache_on_flush = false;
    setIndex_intervalIsSet(false);
    this.index_interval = 0;
  }

  public String getKeyspace() {
    return this.keyspace;
  }

  public CfDef setKeyspace(String keyspace) {
    this.keyspace = keyspace;
    return this;
  }

  public void unsetKeyspace() {
    this.keyspace = null;
  }

  /** Returns true if field keyspace is set (has been assigned a value) and false otherwise */
  public boolean isSetKeyspace() {
    return this.keyspace != null;
  }

  public void setKeyspaceIsSet(boolean value) {
    if (!value) {
      this.keyspace = null;
    }
  }

  public String getName() {
    return this.name;
  }

  public CfDef setName(String name) {
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

  public String getColumn_type() {
    return this.column_type;
  }

  public CfDef setColumn_type(String column_type) {
    this.column_type = column_type;
    return this;
  }

  public void unsetColumn_type() {
    this.column_type = null;
  }

  /** Returns true if field column_type is set (has been assigned a value) and false otherwise */
  public boolean isSetColumn_type() {
    return this.column_type != null;
  }

  public void setColumn_typeIsSet(boolean value) {
    if (!value) {
      this.column_type = null;
    }
  }

  public String getComparator_type() {
    return this.comparator_type;
  }

  public CfDef setComparator_type(String comparator_type) {
    this.comparator_type = comparator_type;
    return this;
  }

  public void unsetComparator_type() {
    this.comparator_type = null;
  }

  /** Returns true if field comparator_type is set (has been assigned a value) and false otherwise */
  public boolean isSetComparator_type() {
    return this.comparator_type != null;
  }

  public void setComparator_typeIsSet(boolean value) {
    if (!value) {
      this.comparator_type = null;
    }
  }

  public String getSubcomparator_type() {
    return this.subcomparator_type;
  }

  public CfDef setSubcomparator_type(String subcomparator_type) {
    this.subcomparator_type = subcomparator_type;
    return this;
  }

  public void unsetSubcomparator_type() {
    this.subcomparator_type = null;
  }

  /** Returns true if field subcomparator_type is set (has been assigned a value) and false otherwise */
  public boolean isSetSubcomparator_type() {
    return this.subcomparator_type != null;
  }

  public void setSubcomparator_typeIsSet(boolean value) {
    if (!value) {
      this.subcomparator_type = null;
    }
  }

  public String getComment() {
    return this.comment;
  }

  public CfDef setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public void unsetComment() {
    this.comment = null;
  }

  /** Returns true if field comment is set (has been assigned a value) and false otherwise */
  public boolean isSetComment() {
    return this.comment != null;
  }

  public void setCommentIsSet(boolean value) {
    if (!value) {
      this.comment = null;
    }
  }

  public double getRead_repair_chance() {
    return this.read_repair_chance;
  }

  public CfDef setRead_repair_chance(double read_repair_chance) {
    this.read_repair_chance = read_repair_chance;
    setRead_repair_chanceIsSet(true);
    return this;
  }

  public void unsetRead_repair_chance() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __READ_REPAIR_CHANCE_ISSET_ID);
  }

  /** Returns true if field read_repair_chance is set (has been assigned a value) and false otherwise */
  public boolean isSetRead_repair_chance() {
    return EncodingUtils.testBit(__isset_bitfield, __READ_REPAIR_CHANCE_ISSET_ID);
  }

  public void setRead_repair_chanceIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __READ_REPAIR_CHANCE_ISSET_ID, value);
  }

  public int getColumn_metadataSize() {
    return (this.column_metadata == null) ? 0 : this.column_metadata.size();
  }

  public java.util.Iterator<ColumnDef> getColumn_metadataIterator() {
    return (this.column_metadata == null) ? null : this.column_metadata.iterator();
  }

  public void addToColumn_metadata(ColumnDef elem) {
    if (this.column_metadata == null) {
      this.column_metadata = new ArrayList<ColumnDef>();
    }
    this.column_metadata.add(elem);
  }

  public List<ColumnDef> getColumn_metadata() {
    return this.column_metadata;
  }

  public CfDef setColumn_metadata(List<ColumnDef> column_metadata) {
    this.column_metadata = column_metadata;
    return this;
  }

  public void unsetColumn_metadata() {
    this.column_metadata = null;
  }

  /** Returns true if field column_metadata is set (has been assigned a value) and false otherwise */
  public boolean isSetColumn_metadata() {
    return this.column_metadata != null;
  }

  public void setColumn_metadataIsSet(boolean value) {
    if (!value) {
      this.column_metadata = null;
    }
  }

  public int getGc_grace_seconds() {
    return this.gc_grace_seconds;
  }

  public CfDef setGc_grace_seconds(int gc_grace_seconds) {
    this.gc_grace_seconds = gc_grace_seconds;
    setGc_grace_secondsIsSet(true);
    return this;
  }

  public void unsetGc_grace_seconds() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __GC_GRACE_SECONDS_ISSET_ID);
  }

  /** Returns true if field gc_grace_seconds is set (has been assigned a value) and false otherwise */
  public boolean isSetGc_grace_seconds() {
    return EncodingUtils.testBit(__isset_bitfield, __GC_GRACE_SECONDS_ISSET_ID);
  }

  public void setGc_grace_secondsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __GC_GRACE_SECONDS_ISSET_ID, value);
  }

  public String getDefault_validation_class() {
    return this.default_validation_class;
  }

  public CfDef setDefault_validation_class(String default_validation_class) {
    this.default_validation_class = default_validation_class;
    return this;
  }

  public void unsetDefault_validation_class() {
    this.default_validation_class = null;
  }

  /** Returns true if field default_validation_class is set (has been assigned a value) and false otherwise */
  public boolean isSetDefault_validation_class() {
    return this.default_validation_class != null;
  }

  public void setDefault_validation_classIsSet(boolean value) {
    if (!value) {
      this.default_validation_class = null;
    }
  }

  public int getId() {
    return this.id;
  }

  public CfDef setId(int id) {
    this.id = id;
    setIdIsSet(true);
    return this;
  }

  public void unsetId() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ID_ISSET_ID);
  }

  /** Returns true if field id is set (has been assigned a value) and false otherwise */
  public boolean isSetId() {
    return EncodingUtils.testBit(__isset_bitfield, __ID_ISSET_ID);
  }

  public void setIdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ID_ISSET_ID, value);
  }

  public int getMin_compaction_threshold() {
    return this.min_compaction_threshold;
  }

  public CfDef setMin_compaction_threshold(int min_compaction_threshold) {
    this.min_compaction_threshold = min_compaction_threshold;
    setMin_compaction_thresholdIsSet(true);
    return this;
  }

  public void unsetMin_compaction_threshold() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MIN_COMPACTION_THRESHOLD_ISSET_ID);
  }

  /** Returns true if field min_compaction_threshold is set (has been assigned a value) and false otherwise */
  public boolean isSetMin_compaction_threshold() {
    return EncodingUtils.testBit(__isset_bitfield, __MIN_COMPACTION_THRESHOLD_ISSET_ID);
  }

  public void setMin_compaction_thresholdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MIN_COMPACTION_THRESHOLD_ISSET_ID, value);
  }

  public int getMax_compaction_threshold() {
    return this.max_compaction_threshold;
  }

  public CfDef setMax_compaction_threshold(int max_compaction_threshold) {
    this.max_compaction_threshold = max_compaction_threshold;
    setMax_compaction_thresholdIsSet(true);
    return this;
  }

  public void unsetMax_compaction_threshold() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MAX_COMPACTION_THRESHOLD_ISSET_ID);
  }

  /** Returns true if field max_compaction_threshold is set (has been assigned a value) and false otherwise */
  public boolean isSetMax_compaction_threshold() {
    return EncodingUtils.testBit(__isset_bitfield, __MAX_COMPACTION_THRESHOLD_ISSET_ID);
  }

  public void setMax_compaction_thresholdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MAX_COMPACTION_THRESHOLD_ISSET_ID, value);
  }

  public String getKey_validation_class() {
    return this.key_validation_class;
  }

  public CfDef setKey_validation_class(String key_validation_class) {
    this.key_validation_class = key_validation_class;
    return this;
  }

  public void unsetKey_validation_class() {
    this.key_validation_class = null;
  }

  /** Returns true if field key_validation_class is set (has been assigned a value) and false otherwise */
  public boolean isSetKey_validation_class() {
    return this.key_validation_class != null;
  }

  public void setKey_validation_classIsSet(boolean value) {
    if (!value) {
      this.key_validation_class = null;
    }
  }

  public byte[] getKey_alias() {
    setKey_alias(org.apache.thrift.TBaseHelper.rightSize(key_alias));
    return key_alias == null ? null : key_alias.array();
  }

  public ByteBuffer bufferForKey_alias() {
    return key_alias;
  }

  public CfDef setKey_alias(byte[] key_alias) {
    setKey_alias(key_alias == null ? (ByteBuffer)null : ByteBuffer.wrap(key_alias));
    return this;
  }

  public CfDef setKey_alias(ByteBuffer key_alias) {
    this.key_alias = key_alias;
    return this;
  }

  public void unsetKey_alias() {
    this.key_alias = null;
  }

  /** Returns true if field key_alias is set (has been assigned a value) and false otherwise */
  public boolean isSetKey_alias() {
    return this.key_alias != null;
  }

  public void setKey_aliasIsSet(boolean value) {
    if (!value) {
      this.key_alias = null;
    }
  }

  public String getCompaction_strategy() {
    return this.compaction_strategy;
  }

  public CfDef setCompaction_strategy(String compaction_strategy) {
    this.compaction_strategy = compaction_strategy;
    return this;
  }

  public void unsetCompaction_strategy() {
    this.compaction_strategy = null;
  }

  /** Returns true if field compaction_strategy is set (has been assigned a value) and false otherwise */
  public boolean isSetCompaction_strategy() {
    return this.compaction_strategy != null;
  }

  public void setCompaction_strategyIsSet(boolean value) {
    if (!value) {
      this.compaction_strategy = null;
    }
  }

  public int getCompaction_strategy_optionsSize() {
    return (this.compaction_strategy_options == null) ? 0 : this.compaction_strategy_options.size();
  }

  public void putToCompaction_strategy_options(String key, String val) {
    if (this.compaction_strategy_options == null) {
      this.compaction_strategy_options = new HashMap<String,String>();
    }
    this.compaction_strategy_options.put(key, val);
  }

  public Map<String,String> getCompaction_strategy_options() {
    return this.compaction_strategy_options;
  }

  public CfDef setCompaction_strategy_options(Map<String,String> compaction_strategy_options) {
    this.compaction_strategy_options = compaction_strategy_options;
    return this;
  }

  public void unsetCompaction_strategy_options() {
    this.compaction_strategy_options = null;
  }

  /** Returns true if field compaction_strategy_options is set (has been assigned a value) and false otherwise */
  public boolean isSetCompaction_strategy_options() {
    return this.compaction_strategy_options != null;
  }

  public void setCompaction_strategy_optionsIsSet(boolean value) {
    if (!value) {
      this.compaction_strategy_options = null;
    }
  }

  public int getCompression_optionsSize() {
    return (this.compression_options == null) ? 0 : this.compression_options.size();
  }

  public void putToCompression_options(String key, String val) {
    if (this.compression_options == null) {
      this.compression_options = new HashMap<String,String>();
    }
    this.compression_options.put(key, val);
  }

  public Map<String,String> getCompression_options() {
    return this.compression_options;
  }

  public CfDef setCompression_options(Map<String,String> compression_options) {
    this.compression_options = compression_options;
    return this;
  }

  public void unsetCompression_options() {
    this.compression_options = null;
  }

  /** Returns true if field compression_options is set (has been assigned a value) and false otherwise */
  public boolean isSetCompression_options() {
    return this.compression_options != null;
  }

  public void setCompression_optionsIsSet(boolean value) {
    if (!value) {
      this.compression_options = null;
    }
  }

  public double getBloom_filter_fp_chance() {
    return this.bloom_filter_fp_chance;
  }

  public CfDef setBloom_filter_fp_chance(double bloom_filter_fp_chance) {
    this.bloom_filter_fp_chance = bloom_filter_fp_chance;
    setBloom_filter_fp_chanceIsSet(true);
    return this;
  }

  public void unsetBloom_filter_fp_chance() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __BLOOM_FILTER_FP_CHANCE_ISSET_ID);
  }

  /** Returns true if field bloom_filter_fp_chance is set (has been assigned a value) and false otherwise */
  public boolean isSetBloom_filter_fp_chance() {
    return EncodingUtils.testBit(__isset_bitfield, __BLOOM_FILTER_FP_CHANCE_ISSET_ID);
  }

  public void setBloom_filter_fp_chanceIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __BLOOM_FILTER_FP_CHANCE_ISSET_ID, value);
  }

  public String getCaching() {
    return this.caching;
  }

  public CfDef setCaching(String caching) {
    this.caching = caching;
    return this;
  }

  public void unsetCaching() {
    this.caching = null;
  }

  /** Returns true if field caching is set (has been assigned a value) and false otherwise */
  public boolean isSetCaching() {
    return this.caching != null;
  }

  public void setCachingIsSet(boolean value) {
    if (!value) {
      this.caching = null;
    }
  }

  public double getDclocal_read_repair_chance() {
    return this.dclocal_read_repair_chance;
  }

  public CfDef setDclocal_read_repair_chance(double dclocal_read_repair_chance) {
    this.dclocal_read_repair_chance = dclocal_read_repair_chance;
    setDclocal_read_repair_chanceIsSet(true);
    return this;
  }

  public void unsetDclocal_read_repair_chance() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __DCLOCAL_READ_REPAIR_CHANCE_ISSET_ID);
  }

  /** Returns true if field dclocal_read_repair_chance is set (has been assigned a value) and false otherwise */
  public boolean isSetDclocal_read_repair_chance() {
    return EncodingUtils.testBit(__isset_bitfield, __DCLOCAL_READ_REPAIR_CHANCE_ISSET_ID);
  }

  public void setDclocal_read_repair_chanceIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __DCLOCAL_READ_REPAIR_CHANCE_ISSET_ID, value);
  }

  public int getMemtable_flush_period_in_ms() {
    return this.memtable_flush_period_in_ms;
  }

  public CfDef setMemtable_flush_period_in_ms(int memtable_flush_period_in_ms) {
    this.memtable_flush_period_in_ms = memtable_flush_period_in_ms;
    setMemtable_flush_period_in_msIsSet(true);
    return this;
  }

  public void unsetMemtable_flush_period_in_ms() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MEMTABLE_FLUSH_PERIOD_IN_MS_ISSET_ID);
  }

  /** Returns true if field memtable_flush_period_in_ms is set (has been assigned a value) and false otherwise */
  public boolean isSetMemtable_flush_period_in_ms() {
    return EncodingUtils.testBit(__isset_bitfield, __MEMTABLE_FLUSH_PERIOD_IN_MS_ISSET_ID);
  }

  public void setMemtable_flush_period_in_msIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MEMTABLE_FLUSH_PERIOD_IN_MS_ISSET_ID, value);
  }

  public int getDefault_time_to_live() {
    return this.default_time_to_live;
  }

  public CfDef setDefault_time_to_live(int default_time_to_live) {
    this.default_time_to_live = default_time_to_live;
    setDefault_time_to_liveIsSet(true);
    return this;
  }

  public void unsetDefault_time_to_live() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __DEFAULT_TIME_TO_LIVE_ISSET_ID);
  }

  /** Returns true if field default_time_to_live is set (has been assigned a value) and false otherwise */
  public boolean isSetDefault_time_to_live() {
    return EncodingUtils.testBit(__isset_bitfield, __DEFAULT_TIME_TO_LIVE_ISSET_ID);
  }

  public void setDefault_time_to_liveIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __DEFAULT_TIME_TO_LIVE_ISSET_ID, value);
  }

  public String getSpeculative_retry() {
    return this.speculative_retry;
  }

  public CfDef setSpeculative_retry(String speculative_retry) {
    this.speculative_retry = speculative_retry;
    return this;
  }

  public void unsetSpeculative_retry() {
    this.speculative_retry = null;
  }

  /** Returns true if field speculative_retry is set (has been assigned a value) and false otherwise */
  public boolean isSetSpeculative_retry() {
    return this.speculative_retry != null;
  }

  public void setSpeculative_retryIsSet(boolean value) {
    if (!value) {
      this.speculative_retry = null;
    }
  }

  public int getTriggersSize() {
    return (this.triggers == null) ? 0 : this.triggers.size();
  }

  public java.util.Iterator<TriggerDef> getTriggersIterator() {
    return (this.triggers == null) ? null : this.triggers.iterator();
  }

  public void addToTriggers(TriggerDef elem) {
    if (this.triggers == null) {
      this.triggers = new ArrayList<TriggerDef>();
    }
    this.triggers.add(elem);
  }

  public List<TriggerDef> getTriggers() {
    return this.triggers;
  }

  public CfDef setTriggers(List<TriggerDef> triggers) {
    this.triggers = triggers;
    return this;
  }

  public void unsetTriggers() {
    this.triggers = null;
  }

  /** Returns true if field triggers is set (has been assigned a value) and false otherwise */
  public boolean isSetTriggers() {
    return this.triggers != null;
  }

  public void setTriggersIsSet(boolean value) {
    if (!value) {
      this.triggers = null;
    }
  }

  public String getCells_per_row_to_cache() {
    return this.cells_per_row_to_cache;
  }

  public CfDef setCells_per_row_to_cache(String cells_per_row_to_cache) {
    this.cells_per_row_to_cache = cells_per_row_to_cache;
    return this;
  }

  public void unsetCells_per_row_to_cache() {
    this.cells_per_row_to_cache = null;
  }

  /** Returns true if field cells_per_row_to_cache is set (has been assigned a value) and false otherwise */
  public boolean isSetCells_per_row_to_cache() {
    return this.cells_per_row_to_cache != null;
  }

  public void setCells_per_row_to_cacheIsSet(boolean value) {
    if (!value) {
      this.cells_per_row_to_cache = null;
    }
  }

  public int getMin_index_interval() {
    return this.min_index_interval;
  }

  public CfDef setMin_index_interval(int min_index_interval) {
    this.min_index_interval = min_index_interval;
    setMin_index_intervalIsSet(true);
    return this;
  }

  public void unsetMin_index_interval() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MIN_INDEX_INTERVAL_ISSET_ID);
  }

  /** Returns true if field min_index_interval is set (has been assigned a value) and false otherwise */
  public boolean isSetMin_index_interval() {
    return EncodingUtils.testBit(__isset_bitfield, __MIN_INDEX_INTERVAL_ISSET_ID);
  }

  public void setMin_index_intervalIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MIN_INDEX_INTERVAL_ISSET_ID, value);
  }

  public int getMax_index_interval() {
    return this.max_index_interval;
  }

  public CfDef setMax_index_interval(int max_index_interval) {
    this.max_index_interval = max_index_interval;
    setMax_index_intervalIsSet(true);
    return this;
  }

  public void unsetMax_index_interval() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MAX_INDEX_INTERVAL_ISSET_ID);
  }

  /** Returns true if field max_index_interval is set (has been assigned a value) and false otherwise */
  public boolean isSetMax_index_interval() {
    return EncodingUtils.testBit(__isset_bitfield, __MAX_INDEX_INTERVAL_ISSET_ID);
  }

  public void setMax_index_intervalIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MAX_INDEX_INTERVAL_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public double getRow_cache_size() {
    return this.row_cache_size;
  }

  /**
   * @deprecated
   */
  public CfDef setRow_cache_size(double row_cache_size) {
    this.row_cache_size = row_cache_size;
    setRow_cache_sizeIsSet(true);
    return this;
  }

  public void unsetRow_cache_size() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ROW_CACHE_SIZE_ISSET_ID);
  }

  /** Returns true if field row_cache_size is set (has been assigned a value) and false otherwise */
  public boolean isSetRow_cache_size() {
    return EncodingUtils.testBit(__isset_bitfield, __ROW_CACHE_SIZE_ISSET_ID);
  }

  public void setRow_cache_sizeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ROW_CACHE_SIZE_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public double getKey_cache_size() {
    return this.key_cache_size;
  }

  /**
   * @deprecated
   */
  public CfDef setKey_cache_size(double key_cache_size) {
    this.key_cache_size = key_cache_size;
    setKey_cache_sizeIsSet(true);
    return this;
  }

  public void unsetKey_cache_size() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __KEY_CACHE_SIZE_ISSET_ID);
  }

  /** Returns true if field key_cache_size is set (has been assigned a value) and false otherwise */
  public boolean isSetKey_cache_size() {
    return EncodingUtils.testBit(__isset_bitfield, __KEY_CACHE_SIZE_ISSET_ID);
  }

  public void setKey_cache_sizeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __KEY_CACHE_SIZE_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public int getRow_cache_save_period_in_seconds() {
    return this.row_cache_save_period_in_seconds;
  }

  /**
   * @deprecated
   */
  public CfDef setRow_cache_save_period_in_seconds(int row_cache_save_period_in_seconds) {
    this.row_cache_save_period_in_seconds = row_cache_save_period_in_seconds;
    setRow_cache_save_period_in_secondsIsSet(true);
    return this;
  }

  public void unsetRow_cache_save_period_in_seconds() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ROW_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID);
  }

  /** Returns true if field row_cache_save_period_in_seconds is set (has been assigned a value) and false otherwise */
  public boolean isSetRow_cache_save_period_in_seconds() {
    return EncodingUtils.testBit(__isset_bitfield, __ROW_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID);
  }

  public void setRow_cache_save_period_in_secondsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ROW_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public int getKey_cache_save_period_in_seconds() {
    return this.key_cache_save_period_in_seconds;
  }

  /**
   * @deprecated
   */
  public CfDef setKey_cache_save_period_in_seconds(int key_cache_save_period_in_seconds) {
    this.key_cache_save_period_in_seconds = key_cache_save_period_in_seconds;
    setKey_cache_save_period_in_secondsIsSet(true);
    return this;
  }

  public void unsetKey_cache_save_period_in_seconds() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __KEY_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID);
  }

  /** Returns true if field key_cache_save_period_in_seconds is set (has been assigned a value) and false otherwise */
  public boolean isSetKey_cache_save_period_in_seconds() {
    return EncodingUtils.testBit(__isset_bitfield, __KEY_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID);
  }

  public void setKey_cache_save_period_in_secondsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __KEY_CACHE_SAVE_PERIOD_IN_SECONDS_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public int getMemtable_flush_after_mins() {
    return this.memtable_flush_after_mins;
  }

  /**
   * @deprecated
   */
  public CfDef setMemtable_flush_after_mins(int memtable_flush_after_mins) {
    this.memtable_flush_after_mins = memtable_flush_after_mins;
    setMemtable_flush_after_minsIsSet(true);
    return this;
  }

  public void unsetMemtable_flush_after_mins() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MEMTABLE_FLUSH_AFTER_MINS_ISSET_ID);
  }

  /** Returns true if field memtable_flush_after_mins is set (has been assigned a value) and false otherwise */
  public boolean isSetMemtable_flush_after_mins() {
    return EncodingUtils.testBit(__isset_bitfield, __MEMTABLE_FLUSH_AFTER_MINS_ISSET_ID);
  }

  public void setMemtable_flush_after_minsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MEMTABLE_FLUSH_AFTER_MINS_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public int getMemtable_throughput_in_mb() {
    return this.memtable_throughput_in_mb;
  }

  /**
   * @deprecated
   */
  public CfDef setMemtable_throughput_in_mb(int memtable_throughput_in_mb) {
    this.memtable_throughput_in_mb = memtable_throughput_in_mb;
    setMemtable_throughput_in_mbIsSet(true);
    return this;
  }

  public void unsetMemtable_throughput_in_mb() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MEMTABLE_THROUGHPUT_IN_MB_ISSET_ID);
  }

  /** Returns true if field memtable_throughput_in_mb is set (has been assigned a value) and false otherwise */
  public boolean isSetMemtable_throughput_in_mb() {
    return EncodingUtils.testBit(__isset_bitfield, __MEMTABLE_THROUGHPUT_IN_MB_ISSET_ID);
  }

  public void setMemtable_throughput_in_mbIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MEMTABLE_THROUGHPUT_IN_MB_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public double getMemtable_operations_in_millions() {
    return this.memtable_operations_in_millions;
  }

  /**
   * @deprecated
   */
  public CfDef setMemtable_operations_in_millions(double memtable_operations_in_millions) {
    this.memtable_operations_in_millions = memtable_operations_in_millions;
    setMemtable_operations_in_millionsIsSet(true);
    return this;
  }

  public void unsetMemtable_operations_in_millions() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MEMTABLE_OPERATIONS_IN_MILLIONS_ISSET_ID);
  }

  /** Returns true if field memtable_operations_in_millions is set (has been assigned a value) and false otherwise */
  public boolean isSetMemtable_operations_in_millions() {
    return EncodingUtils.testBit(__isset_bitfield, __MEMTABLE_OPERATIONS_IN_MILLIONS_ISSET_ID);
  }

  public void setMemtable_operations_in_millionsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MEMTABLE_OPERATIONS_IN_MILLIONS_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public boolean isReplicate_on_write() {
    return this.replicate_on_write;
  }

  /**
   * @deprecated
   */
  public CfDef setReplicate_on_write(boolean replicate_on_write) {
    this.replicate_on_write = replicate_on_write;
    setReplicate_on_writeIsSet(true);
    return this;
  }

  public void unsetReplicate_on_write() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __REPLICATE_ON_WRITE_ISSET_ID);
  }

  /** Returns true if field replicate_on_write is set (has been assigned a value) and false otherwise */
  public boolean isSetReplicate_on_write() {
    return EncodingUtils.testBit(__isset_bitfield, __REPLICATE_ON_WRITE_ISSET_ID);
  }

  public void setReplicate_on_writeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __REPLICATE_ON_WRITE_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public double getMerge_shards_chance() {
    return this.merge_shards_chance;
  }

  /**
   * @deprecated
   */
  public CfDef setMerge_shards_chance(double merge_shards_chance) {
    this.merge_shards_chance = merge_shards_chance;
    setMerge_shards_chanceIsSet(true);
    return this;
  }

  public void unsetMerge_shards_chance() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MERGE_SHARDS_CHANCE_ISSET_ID);
  }

  /** Returns true if field merge_shards_chance is set (has been assigned a value) and false otherwise */
  public boolean isSetMerge_shards_chance() {
    return EncodingUtils.testBit(__isset_bitfield, __MERGE_SHARDS_CHANCE_ISSET_ID);
  }

  public void setMerge_shards_chanceIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MERGE_SHARDS_CHANCE_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public String getRow_cache_provider() {
    return this.row_cache_provider;
  }

  /**
   * @deprecated
   */
  public CfDef setRow_cache_provider(String row_cache_provider) {
    this.row_cache_provider = row_cache_provider;
    return this;
  }

  public void unsetRow_cache_provider() {
    this.row_cache_provider = null;
  }

  /** Returns true if field row_cache_provider is set (has been assigned a value) and false otherwise */
  public boolean isSetRow_cache_provider() {
    return this.row_cache_provider != null;
  }

  public void setRow_cache_providerIsSet(boolean value) {
    if (!value) {
      this.row_cache_provider = null;
    }
  }

  /**
   * @deprecated
   */
  public int getRow_cache_keys_to_save() {
    return this.row_cache_keys_to_save;
  }

  /**
   * @deprecated
   */
  public CfDef setRow_cache_keys_to_save(int row_cache_keys_to_save) {
    this.row_cache_keys_to_save = row_cache_keys_to_save;
    setRow_cache_keys_to_saveIsSet(true);
    return this;
  }

  public void unsetRow_cache_keys_to_save() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ROW_CACHE_KEYS_TO_SAVE_ISSET_ID);
  }

  /** Returns true if field row_cache_keys_to_save is set (has been assigned a value) and false otherwise */
  public boolean isSetRow_cache_keys_to_save() {
    return EncodingUtils.testBit(__isset_bitfield, __ROW_CACHE_KEYS_TO_SAVE_ISSET_ID);
  }

  public void setRow_cache_keys_to_saveIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ROW_CACHE_KEYS_TO_SAVE_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public boolean isPopulate_io_cache_on_flush() {
    return this.populate_io_cache_on_flush;
  }

  /**
   * @deprecated
   */
  public CfDef setPopulate_io_cache_on_flush(boolean populate_io_cache_on_flush) {
    this.populate_io_cache_on_flush = populate_io_cache_on_flush;
    setPopulate_io_cache_on_flushIsSet(true);
    return this;
  }

  public void unsetPopulate_io_cache_on_flush() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __POPULATE_IO_CACHE_ON_FLUSH_ISSET_ID);
  }

  /** Returns true if field populate_io_cache_on_flush is set (has been assigned a value) and false otherwise */
  public boolean isSetPopulate_io_cache_on_flush() {
    return EncodingUtils.testBit(__isset_bitfield, __POPULATE_IO_CACHE_ON_FLUSH_ISSET_ID);
  }

  public void setPopulate_io_cache_on_flushIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __POPULATE_IO_CACHE_ON_FLUSH_ISSET_ID, value);
  }

  /**
   * @deprecated
   */
  public int getIndex_interval() {
    return this.index_interval;
  }

  /**
   * @deprecated
   */
  public CfDef setIndex_interval(int index_interval) {
    this.index_interval = index_interval;
    setIndex_intervalIsSet(true);
    return this;
  }

  public void unsetIndex_interval() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __INDEX_INTERVAL_ISSET_ID);
  }

  /** Returns true if field index_interval is set (has been assigned a value) and false otherwise */
  public boolean isSetIndex_interval() {
    return EncodingUtils.testBit(__isset_bitfield, __INDEX_INTERVAL_ISSET_ID);
  }

  public void setIndex_intervalIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __INDEX_INTERVAL_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case KEYSPACE:
      if (value == null) {
        unsetKeyspace();
      } else {
        setKeyspace((String)value);
      }
      break;

    case NAME:
      if (value == null) {
        unsetName();
      } else {
        setName((String)value);
      }
      break;

    case COLUMN_TYPE:
      if (value == null) {
        unsetColumn_type();
      } else {
        setColumn_type((String)value);
      }
      break;

    case COMPARATOR_TYPE:
      if (value == null) {
        unsetComparator_type();
      } else {
        setComparator_type((String)value);
      }
      break;

    case SUBCOMPARATOR_TYPE:
      if (value == null) {
        unsetSubcomparator_type();
      } else {
        setSubcomparator_type((String)value);
      }
      break;

    case COMMENT:
      if (value == null) {
        unsetComment();
      } else {
        setComment((String)value);
      }
      break;

    case READ_REPAIR_CHANCE:
      if (value == null) {
        unsetRead_repair_chance();
      } else {
        setRead_repair_chance((Double)value);
      }
      break;

    case COLUMN_METADATA:
      if (value == null) {
        unsetColumn_metadata();
      } else {
        setColumn_metadata((List<ColumnDef>)value);
      }
      break;

    case GC_GRACE_SECONDS:
      if (value == null) {
        unsetGc_grace_seconds();
      } else {
        setGc_grace_seconds((Integer)value);
      }
      break;

    case DEFAULT_VALIDATION_CLASS:
      if (value == null) {
        unsetDefault_validation_class();
      } else {
        setDefault_validation_class((String)value);
      }
      break;

    case ID:
      if (value == null) {
        unsetId();
      } else {
        setId((Integer)value);
      }
      break;

    case MIN_COMPACTION_THRESHOLD:
      if (value == null) {
        unsetMin_compaction_threshold();
      } else {
        setMin_compaction_threshold((Integer)value);
      }
      break;

    case MAX_COMPACTION_THRESHOLD:
      if (value == null) {
        unsetMax_compaction_threshold();
      } else {
        setMax_compaction_threshold((Integer)value);
      }
      break;

    case KEY_VALIDATION_CLASS:
      if (value == null) {
        unsetKey_validation_class();
      } else {
        setKey_validation_class((String)value);
      }
      break;

    case KEY_ALIAS:
      if (value == null) {
        unsetKey_alias();
      } else {
        setKey_alias((ByteBuffer)value);
      }
      break;

    case COMPACTION_STRATEGY:
      if (value == null) {
        unsetCompaction_strategy();
      } else {
        setCompaction_strategy((String)value);
      }
      break;

    case COMPACTION_STRATEGY_OPTIONS:
      if (value == null) {
        unsetCompaction_strategy_options();
      } else {
        setCompaction_strategy_options((Map<String,String>)value);
      }
      break;

    case COMPRESSION_OPTIONS:
      if (value == null) {
        unsetCompression_options();
      } else {
        setCompression_options((Map<String,String>)value);
      }
      break;

    case BLOOM_FILTER_FP_CHANCE:
      if (value == null) {
        unsetBloom_filter_fp_chance();
      } else {
        setBloom_filter_fp_chance((Double)value);
      }
      break;

    case CACHING:
      if (value == null) {
        unsetCaching();
      } else {
        setCaching((String)value);
      }
      break;

    case DCLOCAL_READ_REPAIR_CHANCE:
      if (value == null) {
        unsetDclocal_read_repair_chance();
      } else {
        setDclocal_read_repair_chance((Double)value);
      }
      break;

    case MEMTABLE_FLUSH_PERIOD_IN_MS:
      if (value == null) {
        unsetMemtable_flush_period_in_ms();
      } else {
        setMemtable_flush_period_in_ms((Integer)value);
      }
      break;

    case DEFAULT_TIME_TO_LIVE:
      if (value == null) {
        unsetDefault_time_to_live();
      } else {
        setDefault_time_to_live((Integer)value);
      }
      break;

    case SPECULATIVE_RETRY:
      if (value == null) {
        unsetSpeculative_retry();
      } else {
        setSpeculative_retry((String)value);
      }
      break;

    case TRIGGERS:
      if (value == null) {
        unsetTriggers();
      } else {
        setTriggers((List<TriggerDef>)value);
      }
      break;

    case CELLS_PER_ROW_TO_CACHE:
      if (value == null) {
        unsetCells_per_row_to_cache();
      } else {
        setCells_per_row_to_cache((String)value);
      }
      break;

    case MIN_INDEX_INTERVAL:
      if (value == null) {
        unsetMin_index_interval();
      } else {
        setMin_index_interval((Integer)value);
      }
      break;

    case MAX_INDEX_INTERVAL:
      if (value == null) {
        unsetMax_index_interval();
      } else {
        setMax_index_interval((Integer)value);
      }
      break;

    case ROW_CACHE_SIZE:
      if (value == null) {
        unsetRow_cache_size();
      } else {
        setRow_cache_size((Double)value);
      }
      break;

    case KEY_CACHE_SIZE:
      if (value == null) {
        unsetKey_cache_size();
      } else {
        setKey_cache_size((Double)value);
      }
      break;

    case ROW_CACHE_SAVE_PERIOD_IN_SECONDS:
      if (value == null) {
        unsetRow_cache_save_period_in_seconds();
      } else {
        setRow_cache_save_period_in_seconds((Integer)value);
      }
      break;

    case KEY_CACHE_SAVE_PERIOD_IN_SECONDS:
      if (value == null) {
        unsetKey_cache_save_period_in_seconds();
      } else {
        setKey_cache_save_period_in_seconds((Integer)value);
      }
      break;

    case MEMTABLE_FLUSH_AFTER_MINS:
      if (value == null) {
        unsetMemtable_flush_after_mins();
      } else {
        setMemtable_flush_after_mins((Integer)value);
      }
      break;

    case MEMTABLE_THROUGHPUT_IN_MB:
      if (value == null) {
        unsetMemtable_throughput_in_mb();
      } else {
        setMemtable_throughput_in_mb((Integer)value);
      }
      break;

    case MEMTABLE_OPERATIONS_IN_MILLIONS:
      if (value == null) {
        unsetMemtable_operations_in_millions();
      } else {
        setMemtable_operations_in_millions((Double)value);
      }
      break;

    case REPLICATE_ON_WRITE:
      if (value == null) {
        unsetReplicate_on_write();
      } else {
        setReplicate_on_write((Boolean)value);
      }
      break;

    case MERGE_SHARDS_CHANCE:
      if (value == null) {
        unsetMerge_shards_chance();
      } else {
        setMerge_shards_chance((Double)value);
      }
      break;

    case ROW_CACHE_PROVIDER:
      if (value == null) {
        unsetRow_cache_provider();
      } else {
        setRow_cache_provider((String)value);
      }
      break;

    case ROW_CACHE_KEYS_TO_SAVE:
      if (value == null) {
        unsetRow_cache_keys_to_save();
      } else {
        setRow_cache_keys_to_save((Integer)value);
      }
      break;

    case POPULATE_IO_CACHE_ON_FLUSH:
      if (value == null) {
        unsetPopulate_io_cache_on_flush();
      } else {
        setPopulate_io_cache_on_flush((Boolean)value);
      }
      break;

    case INDEX_INTERVAL:
      if (value == null) {
        unsetIndex_interval();
      } else {
        setIndex_interval((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case KEYSPACE:
      return getKeyspace();

    case NAME:
      return getName();

    case COLUMN_TYPE:
      return getColumn_type();

    case COMPARATOR_TYPE:
      return getComparator_type();

    case SUBCOMPARATOR_TYPE:
      return getSubcomparator_type();

    case COMMENT:
      return getComment();

    case READ_REPAIR_CHANCE:
      return Double.valueOf(getRead_repair_chance());

    case COLUMN_METADATA:
      return getColumn_metadata();

    case GC_GRACE_SECONDS:
      return Integer.valueOf(getGc_grace_seconds());

    case DEFAULT_VALIDATION_CLASS:
      return getDefault_validation_class();

    case ID:
      return Integer.valueOf(getId());

    case MIN_COMPACTION_THRESHOLD:
      return Integer.valueOf(getMin_compaction_threshold());

    case MAX_COMPACTION_THRESHOLD:
      return Integer.valueOf(getMax_compaction_threshold());

    case KEY_VALIDATION_CLASS:
      return getKey_validation_class();

    case KEY_ALIAS:
      return getKey_alias();

    case COMPACTION_STRATEGY:
      return getCompaction_strategy();

    case COMPACTION_STRATEGY_OPTIONS:
      return getCompaction_strategy_options();

    case COMPRESSION_OPTIONS:
      return getCompression_options();

    case BLOOM_FILTER_FP_CHANCE:
      return Double.valueOf(getBloom_filter_fp_chance());

    case CACHING:
      return getCaching();

    case DCLOCAL_READ_REPAIR_CHANCE:
      return Double.valueOf(getDclocal_read_repair_chance());

    case MEMTABLE_FLUSH_PERIOD_IN_MS:
      return Integer.valueOf(getMemtable_flush_period_in_ms());

    case DEFAULT_TIME_TO_LIVE:
      return Integer.valueOf(getDefault_time_to_live());

    case SPECULATIVE_RETRY:
      return getSpeculative_retry();

    case TRIGGERS:
      return getTriggers();

    case CELLS_PER_ROW_TO_CACHE:
      return getCells_per_row_to_cache();

    case MIN_INDEX_INTERVAL:
      return Integer.valueOf(getMin_index_interval());

    case MAX_INDEX_INTERVAL:
      return Integer.valueOf(getMax_index_interval());

    case ROW_CACHE_SIZE:
      return Double.valueOf(getRow_cache_size());

    case KEY_CACHE_SIZE:
      return Double.valueOf(getKey_cache_size());

    case ROW_CACHE_SAVE_PERIOD_IN_SECONDS:
      return Integer.valueOf(getRow_cache_save_period_in_seconds());

    case KEY_CACHE_SAVE_PERIOD_IN_SECONDS:
      return Integer.valueOf(getKey_cache_save_period_in_seconds());

    case MEMTABLE_FLUSH_AFTER_MINS:
      return Integer.valueOf(getMemtable_flush_after_mins());

    case MEMTABLE_THROUGHPUT_IN_MB:
      return Integer.valueOf(getMemtable_throughput_in_mb());

    case MEMTABLE_OPERATIONS_IN_MILLIONS:
      return Double.valueOf(getMemtable_operations_in_millions());

    case REPLICATE_ON_WRITE:
      return Boolean.valueOf(isReplicate_on_write());

    case MERGE_SHARDS_CHANCE:
      return Double.valueOf(getMerge_shards_chance());

    case ROW_CACHE_PROVIDER:
      return getRow_cache_provider();

    case ROW_CACHE_KEYS_TO_SAVE:
      return Integer.valueOf(getRow_cache_keys_to_save());

    case POPULATE_IO_CACHE_ON_FLUSH:
      return Boolean.valueOf(isPopulate_io_cache_on_flush());

    case INDEX_INTERVAL:
      return Integer.valueOf(getIndex_interval());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case KEYSPACE:
      return isSetKeyspace();
    case NAME:
      return isSetName();
    case COLUMN_TYPE:
      return isSetColumn_type();
    case COMPARATOR_TYPE:
      return isSetComparator_type();
    case SUBCOMPARATOR_TYPE:
      return isSetSubcomparator_type();
    case COMMENT:
      return isSetComment();
    case READ_REPAIR_CHANCE:
      return isSetRead_repair_chance();
    case COLUMN_METADATA:
      return isSetColumn_metadata();
    case GC_GRACE_SECONDS:
      return isSetGc_grace_seconds();
    case DEFAULT_VALIDATION_CLASS:
      return isSetDefault_validation_class();
    case ID:
      return isSetId();
    case MIN_COMPACTION_THRESHOLD:
      return isSetMin_compaction_threshold();
    case MAX_COMPACTION_THRESHOLD:
      return isSetMax_compaction_threshold();
    case KEY_VALIDATION_CLASS:
      return isSetKey_validation_class();
    case KEY_ALIAS:
      return isSetKey_alias();
    case COMPACTION_STRATEGY:
      return isSetCompaction_strategy();
    case COMPACTION_STRATEGY_OPTIONS:
      return isSetCompaction_strategy_options();
    case COMPRESSION_OPTIONS:
      return isSetCompression_options();
    case BLOOM_FILTER_FP_CHANCE:
      return isSetBloom_filter_fp_chance();
    case CACHING:
      return isSetCaching();
    case DCLOCAL_READ_REPAIR_CHANCE:
      return isSetDclocal_read_repair_chance();
    case MEMTABLE_FLUSH_PERIOD_IN_MS:
      return isSetMemtable_flush_period_in_ms();
    case DEFAULT_TIME_TO_LIVE:
      return isSetDefault_time_to_live();
    case SPECULATIVE_RETRY:
      return isSetSpeculative_retry();
    case TRIGGERS:
      return isSetTriggers();
    case CELLS_PER_ROW_TO_CACHE:
      return isSetCells_per_row_to_cache();
    case MIN_INDEX_INTERVAL:
      return isSetMin_index_interval();
    case MAX_INDEX_INTERVAL:
      return isSetMax_index_interval();
    case ROW_CACHE_SIZE:
      return isSetRow_cache_size();
    case KEY_CACHE_SIZE:
      return isSetKey_cache_size();
    case ROW_CACHE_SAVE_PERIOD_IN_SECONDS:
      return isSetRow_cache_save_period_in_seconds();
    case KEY_CACHE_SAVE_PERIOD_IN_SECONDS:
      return isSetKey_cache_save_period_in_seconds();
    case MEMTABLE_FLUSH_AFTER_MINS:
      return isSetMemtable_flush_after_mins();
    case MEMTABLE_THROUGHPUT_IN_MB:
      return isSetMemtable_throughput_in_mb();
    case MEMTABLE_OPERATIONS_IN_MILLIONS:
      return isSetMemtable_operations_in_millions();
    case REPLICATE_ON_WRITE:
      return isSetReplicate_on_write();
    case MERGE_SHARDS_CHANCE:
      return isSetMerge_shards_chance();
    case ROW_CACHE_PROVIDER:
      return isSetRow_cache_provider();
    case ROW_CACHE_KEYS_TO_SAVE:
      return isSetRow_cache_keys_to_save();
    case POPULATE_IO_CACHE_ON_FLUSH:
      return isSetPopulate_io_cache_on_flush();
    case INDEX_INTERVAL:
      return isSetIndex_interval();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CfDef)
      return this.equals((CfDef)that);
    return false;
  }

  public boolean equals(CfDef that) {
    if (that == null)
      return false;

    boolean this_present_keyspace = true && this.isSetKeyspace();
    boolean that_present_keyspace = true && that.isSetKeyspace();
    if (this_present_keyspace || that_present_keyspace) {
      if (!(this_present_keyspace && that_present_keyspace))
        return false;
      if (!this.keyspace.equals(that.keyspace))
        return false;
    }

    boolean this_present_name = true && this.isSetName();
    boolean that_present_name = true && that.isSetName();
    if (this_present_name || that_present_name) {
      if (!(this_present_name && that_present_name))
        return false;
      if (!this.name.equals(that.name))
        return false;
    }

    boolean this_present_column_type = true && this.isSetColumn_type();
    boolean that_present_column_type = true && that.isSetColumn_type();
    if (this_present_column_type || that_present_column_type) {
      if (!(this_present_column_type && that_present_column_type))
        return false;
      if (!this.column_type.equals(that.column_type))
        return false;
    }

    boolean this_present_comparator_type = true && this.isSetComparator_type();
    boolean that_present_comparator_type = true && that.isSetComparator_type();
    if (this_present_comparator_type || that_present_comparator_type) {
      if (!(this_present_comparator_type && that_present_comparator_type))
        return false;
      if (!this.comparator_type.equals(that.comparator_type))
        return false;
    }

    boolean this_present_subcomparator_type = true && this.isSetSubcomparator_type();
    boolean that_present_subcomparator_type = true && that.isSetSubcomparator_type();
    if (this_present_subcomparator_type || that_present_subcomparator_type) {
      if (!(this_present_subcomparator_type && that_present_subcomparator_type))
        return false;
      if (!this.subcomparator_type.equals(that.subcomparator_type))
        return false;
    }

    boolean this_present_comment = true && this.isSetComment();
    boolean that_present_comment = true && that.isSetComment();
    if (this_present_comment || that_present_comment) {
      if (!(this_present_comment && that_present_comment))
        return false;
      if (!this.comment.equals(that.comment))
        return false;
    }

    boolean this_present_read_repair_chance = true && this.isSetRead_repair_chance();
    boolean that_present_read_repair_chance = true && that.isSetRead_repair_chance();
    if (this_present_read_repair_chance || that_present_read_repair_chance) {
      if (!(this_present_read_repair_chance && that_present_read_repair_chance))
        return false;
      if (this.read_repair_chance != that.read_repair_chance)
        return false;
    }

    boolean this_present_column_metadata = true && this.isSetColumn_metadata();
    boolean that_present_column_metadata = true && that.isSetColumn_metadata();
    if (this_present_column_metadata || that_present_column_metadata) {
      if (!(this_present_column_metadata && that_present_column_metadata))
        return false;
      if (!this.column_metadata.equals(that.column_metadata))
        return false;
    }

    boolean this_present_gc_grace_seconds = true && this.isSetGc_grace_seconds();
    boolean that_present_gc_grace_seconds = true && that.isSetGc_grace_seconds();
    if (this_present_gc_grace_seconds || that_present_gc_grace_seconds) {
      if (!(this_present_gc_grace_seconds && that_present_gc_grace_seconds))
        return false;
      if (this.gc_grace_seconds != that.gc_grace_seconds)
        return false;
    }

    boolean this_present_default_validation_class = true && this.isSetDefault_validation_class();
    boolean that_present_default_validation_class = true && that.isSetDefault_validation_class();
    if (this_present_default_validation_class || that_present_default_validation_class) {
      if (!(this_present_default_validation_class && that_present_default_validation_class))
        return false;
      if (!this.default_validation_class.equals(that.default_validation_class))
        return false;
    }

    boolean this_present_id = true && this.isSetId();
    boolean that_present_id = true && that.isSetId();
    if (this_present_id || that_present_id) {
      if (!(this_present_id && that_present_id))
        return false;
      if (this.id != that.id)
        return false;
    }

    boolean this_present_min_compaction_threshold = true && this.isSetMin_compaction_threshold();
    boolean that_present_min_compaction_threshold = true && that.isSetMin_compaction_threshold();
    if (this_present_min_compaction_threshold || that_present_min_compaction_threshold) {
      if (!(this_present_min_compaction_threshold && that_present_min_compaction_threshold))
        return false;
      if (this.min_compaction_threshold != that.min_compaction_threshold)
        return false;
    }

    boolean this_present_max_compaction_threshold = true && this.isSetMax_compaction_threshold();
    boolean that_present_max_compaction_threshold = true && that.isSetMax_compaction_threshold();
    if (this_present_max_compaction_threshold || that_present_max_compaction_threshold) {
      if (!(this_present_max_compaction_threshold && that_present_max_compaction_threshold))
        return false;
      if (this.max_compaction_threshold != that.max_compaction_threshold)
        return false;
    }

    boolean this_present_key_validation_class = true && this.isSetKey_validation_class();
    boolean that_present_key_validation_class = true && that.isSetKey_validation_class();
    if (this_present_key_validation_class || that_present_key_validation_class) {
      if (!(this_present_key_validation_class && that_present_key_validation_class))
        return false;
      if (!this.key_validation_class.equals(that.key_validation_class))
        return false;
    }

    boolean this_present_key_alias = true && this.isSetKey_alias();
    boolean that_present_key_alias = true && that.isSetKey_alias();
    if (this_present_key_alias || that_present_key_alias) {
      if (!(this_present_key_alias && that_present_key_alias))
        return false;
      if (!this.key_alias.equals(that.key_alias))
        return false;
    }

    boolean this_present_compaction_strategy = true && this.isSetCompaction_strategy();
    boolean that_present_compaction_strategy = true && that.isSetCompaction_strategy();
    if (this_present_compaction_strategy || that_present_compaction_strategy) {
      if (!(this_present_compaction_strategy && that_present_compaction_strategy))
        return false;
      if (!this.compaction_strategy.equals(that.compaction_strategy))
        return false;
    }

    boolean this_present_compaction_strategy_options = true && this.isSetCompaction_strategy_options();
    boolean that_present_compaction_strategy_options = true && that.isSetCompaction_strategy_options();
    if (this_present_compaction_strategy_options || that_present_compaction_strategy_options) {
      if (!(this_present_compaction_strategy_options && that_present_compaction_strategy_options))
        return false;
      if (!this.compaction_strategy_options.equals(that.compaction_strategy_options))
        return false;
    }

    boolean this_present_compression_options = true && this.isSetCompression_options();
    boolean that_present_compression_options = true && that.isSetCompression_options();
    if (this_present_compression_options || that_present_compression_options) {
      if (!(this_present_compression_options && that_present_compression_options))
        return false;
      if (!this.compression_options.equals(that.compression_options))
        return false;
    }

    boolean this_present_bloom_filter_fp_chance = true && this.isSetBloom_filter_fp_chance();
    boolean that_present_bloom_filter_fp_chance = true && that.isSetBloom_filter_fp_chance();
    if (this_present_bloom_filter_fp_chance || that_present_bloom_filter_fp_chance) {
      if (!(this_present_bloom_filter_fp_chance && that_present_bloom_filter_fp_chance))
        return false;
      if (this.bloom_filter_fp_chance != that.bloom_filter_fp_chance)
        return false;
    }

    boolean this_present_caching = true && this.isSetCaching();
    boolean that_present_caching = true && that.isSetCaching();
    if (this_present_caching || that_present_caching) {
      if (!(this_present_caching && that_present_caching))
        return false;
      if (!this.caching.equals(that.caching))
        return false;
    }

    boolean this_present_dclocal_read_repair_chance = true && this.isSetDclocal_read_repair_chance();
    boolean that_present_dclocal_read_repair_chance = true && that.isSetDclocal_read_repair_chance();
    if (this_present_dclocal_read_repair_chance || that_present_dclocal_read_repair_chance) {
      if (!(this_present_dclocal_read_repair_chance && that_present_dclocal_read_repair_chance))
        return false;
      if (this.dclocal_read_repair_chance != that.dclocal_read_repair_chance)
        return false;
    }

    boolean this_present_memtable_flush_period_in_ms = true && this.isSetMemtable_flush_period_in_ms();
    boolean that_present_memtable_flush_period_in_ms = true && that.isSetMemtable_flush_period_in_ms();
    if (this_present_memtable_flush_period_in_ms || that_present_memtable_flush_period_in_ms) {
      if (!(this_present_memtable_flush_period_in_ms && that_present_memtable_flush_period_in_ms))
        return false;
      if (this.memtable_flush_period_in_ms != that.memtable_flush_period_in_ms)
        return false;
    }

    boolean this_present_default_time_to_live = true && this.isSetDefault_time_to_live();
    boolean that_present_default_time_to_live = true && that.isSetDefault_time_to_live();
    if (this_present_default_time_to_live || that_present_default_time_to_live) {
      if (!(this_present_default_time_to_live && that_present_default_time_to_live))
        return false;
      if (this.default_time_to_live != that.default_time_to_live)
        return false;
    }

    boolean this_present_speculative_retry = true && this.isSetSpeculative_retry();
    boolean that_present_speculative_retry = true && that.isSetSpeculative_retry();
    if (this_present_speculative_retry || that_present_speculative_retry) {
      if (!(this_present_speculative_retry && that_present_speculative_retry))
        return false;
      if (!this.speculative_retry.equals(that.speculative_retry))
        return false;
    }

    boolean this_present_triggers = true && this.isSetTriggers();
    boolean that_present_triggers = true && that.isSetTriggers();
    if (this_present_triggers || that_present_triggers) {
      if (!(this_present_triggers && that_present_triggers))
        return false;
      if (!this.triggers.equals(that.triggers))
        return false;
    }

    boolean this_present_cells_per_row_to_cache = true && this.isSetCells_per_row_to_cache();
    boolean that_present_cells_per_row_to_cache = true && that.isSetCells_per_row_to_cache();
    if (this_present_cells_per_row_to_cache || that_present_cells_per_row_to_cache) {
      if (!(this_present_cells_per_row_to_cache && that_present_cells_per_row_to_cache))
        return false;
      if (!this.cells_per_row_to_cache.equals(that.cells_per_row_to_cache))
        return false;
    }

    boolean this_present_min_index_interval = true && this.isSetMin_index_interval();
    boolean that_present_min_index_interval = true && that.isSetMin_index_interval();
    if (this_present_min_index_interval || that_present_min_index_interval) {
      if (!(this_present_min_index_interval && that_present_min_index_interval))
        return false;
      if (this.min_index_interval != that.min_index_interval)
        return false;
    }

    boolean this_present_max_index_interval = true && this.isSetMax_index_interval();
    boolean that_present_max_index_interval = true && that.isSetMax_index_interval();
    if (this_present_max_index_interval || that_present_max_index_interval) {
      if (!(this_present_max_index_interval && that_present_max_index_interval))
        return false;
      if (this.max_index_interval != that.max_index_interval)
        return false;
    }

    boolean this_present_row_cache_size = true && this.isSetRow_cache_size();
    boolean that_present_row_cache_size = true && that.isSetRow_cache_size();
    if (this_present_row_cache_size || that_present_row_cache_size) {
      if (!(this_present_row_cache_size && that_present_row_cache_size))
        return false;
      if (this.row_cache_size != that.row_cache_size)
        return false;
    }

    boolean this_present_key_cache_size = true && this.isSetKey_cache_size();
    boolean that_present_key_cache_size = true && that.isSetKey_cache_size();
    if (this_present_key_cache_size || that_present_key_cache_size) {
      if (!(this_present_key_cache_size && that_present_key_cache_size))
        return false;
      if (this.key_cache_size != that.key_cache_size)
        return false;
    }

    boolean this_present_row_cache_save_period_in_seconds = true && this.isSetRow_cache_save_period_in_seconds();
    boolean that_present_row_cache_save_period_in_seconds = true && that.isSetRow_cache_save_period_in_seconds();
    if (this_present_row_cache_save_period_in_seconds || that_present_row_cache_save_period_in_seconds) {
      if (!(this_present_row_cache_save_period_in_seconds && that_present_row_cache_save_period_in_seconds))
        return false;
      if (this.row_cache_save_period_in_seconds != that.row_cache_save_period_in_seconds)
        return false;
    }

    boolean this_present_key_cache_save_period_in_seconds = true && this.isSetKey_cache_save_period_in_seconds();
    boolean that_present_key_cache_save_period_in_seconds = true && that.isSetKey_cache_save_period_in_seconds();
    if (this_present_key_cache_save_period_in_seconds || that_present_key_cache_save_period_in_seconds) {
      if (!(this_present_key_cache_save_period_in_seconds && that_present_key_cache_save_period_in_seconds))
        return false;
      if (this.key_cache_save_period_in_seconds != that.key_cache_save_period_in_seconds)
        return false;
    }

    boolean this_present_memtable_flush_after_mins = true && this.isSetMemtable_flush_after_mins();
    boolean that_present_memtable_flush_after_mins = true && that.isSetMemtable_flush_after_mins();
    if (this_present_memtable_flush_after_mins || that_present_memtable_flush_after_mins) {
      if (!(this_present_memtable_flush_after_mins && that_present_memtable_flush_after_mins))
        return false;
      if (this.memtable_flush_after_mins != that.memtable_flush_after_mins)
        return false;
    }

    boolean this_present_memtable_throughput_in_mb = true && this.isSetMemtable_throughput_in_mb();
    boolean that_present_memtable_throughput_in_mb = true && that.isSetMemtable_throughput_in_mb();
    if (this_present_memtable_throughput_in_mb || that_present_memtable_throughput_in_mb) {
      if (!(this_present_memtable_throughput_in_mb && that_present_memtable_throughput_in_mb))
        return false;
      if (this.memtable_throughput_in_mb != that.memtable_throughput_in_mb)
        return false;
    }

    boolean this_present_memtable_operations_in_millions = true && this.isSetMemtable_operations_in_millions();
    boolean that_present_memtable_operations_in_millions = true && that.isSetMemtable_operations_in_millions();
    if (this_present_memtable_operations_in_millions || that_present_memtable_operations_in_millions) {
      if (!(this_present_memtable_operations_in_millions && that_present_memtable_operations_in_millions))
        return false;
      if (this.memtable_operations_in_millions != that.memtable_operations_in_millions)
        return false;
    }

    boolean this_present_replicate_on_write = true && this.isSetReplicate_on_write();
    boolean that_present_replicate_on_write = true && that.isSetReplicate_on_write();
    if (this_present_replicate_on_write || that_present_replicate_on_write) {
      if (!(this_present_replicate_on_write && that_present_replicate_on_write))
        return false;
      if (this.replicate_on_write != that.replicate_on_write)
        return false;
    }

    boolean this_present_merge_shards_chance = true && this.isSetMerge_shards_chance();
    boolean that_present_merge_shards_chance = true && that.isSetMerge_shards_chance();
    if (this_present_merge_shards_chance || that_present_merge_shards_chance) {
      if (!(this_present_merge_shards_chance && that_present_merge_shards_chance))
        return false;
      if (this.merge_shards_chance != that.merge_shards_chance)
        return false;
    }

    boolean this_present_row_cache_provider = true && this.isSetRow_cache_provider();
    boolean that_present_row_cache_provider = true && that.isSetRow_cache_provider();
    if (this_present_row_cache_provider || that_present_row_cache_provider) {
      if (!(this_present_row_cache_provider && that_present_row_cache_provider))
        return false;
      if (!this.row_cache_provider.equals(that.row_cache_provider))
        return false;
    }

    boolean this_present_row_cache_keys_to_save = true && this.isSetRow_cache_keys_to_save();
    boolean that_present_row_cache_keys_to_save = true && that.isSetRow_cache_keys_to_save();
    if (this_present_row_cache_keys_to_save || that_present_row_cache_keys_to_save) {
      if (!(this_present_row_cache_keys_to_save && that_present_row_cache_keys_to_save))
        return false;
      if (this.row_cache_keys_to_save != that.row_cache_keys_to_save)
        return false;
    }

    boolean this_present_populate_io_cache_on_flush = true && this.isSetPopulate_io_cache_on_flush();
    boolean that_present_populate_io_cache_on_flush = true && that.isSetPopulate_io_cache_on_flush();
    if (this_present_populate_io_cache_on_flush || that_present_populate_io_cache_on_flush) {
      if (!(this_present_populate_io_cache_on_flush && that_present_populate_io_cache_on_flush))
        return false;
      if (this.populate_io_cache_on_flush != that.populate_io_cache_on_flush)
        return false;
    }

    boolean this_present_index_interval = true && this.isSetIndex_interval();
    boolean that_present_index_interval = true && that.isSetIndex_interval();
    if (this_present_index_interval || that_present_index_interval) {
      if (!(this_present_index_interval && that_present_index_interval))
        return false;
      if (this.index_interval != that.index_interval)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_keyspace = true && (isSetKeyspace());
    builder.append(present_keyspace);
    if (present_keyspace)
      builder.append(keyspace);

    boolean present_name = true && (isSetName());
    builder.append(present_name);
    if (present_name)
      builder.append(name);

    boolean present_column_type = true && (isSetColumn_type());
    builder.append(present_column_type);
    if (present_column_type)
      builder.append(column_type);

    boolean present_comparator_type = true && (isSetComparator_type());
    builder.append(present_comparator_type);
    if (present_comparator_type)
      builder.append(comparator_type);

    boolean present_subcomparator_type = true && (isSetSubcomparator_type());
    builder.append(present_subcomparator_type);
    if (present_subcomparator_type)
      builder.append(subcomparator_type);

    boolean present_comment = true && (isSetComment());
    builder.append(present_comment);
    if (present_comment)
      builder.append(comment);

    boolean present_read_repair_chance = true && (isSetRead_repair_chance());
    builder.append(present_read_repair_chance);
    if (present_read_repair_chance)
      builder.append(read_repair_chance);

    boolean present_column_metadata = true && (isSetColumn_metadata());
    builder.append(present_column_metadata);
    if (present_column_metadata)
      builder.append(column_metadata);

    boolean present_gc_grace_seconds = true && (isSetGc_grace_seconds());
    builder.append(present_gc_grace_seconds);
    if (present_gc_grace_seconds)
      builder.append(gc_grace_seconds);

    boolean present_default_validation_class = true && (isSetDefault_validation_class());
    builder.append(present_default_validation_class);
    if (present_default_validation_class)
      builder.append(default_validation_class);

    boolean present_id = true && (isSetId());
    builder.append(present_id);
    if (present_id)
      builder.append(id);

    boolean present_min_compaction_threshold = true && (isSetMin_compaction_threshold());
    builder.append(present_min_compaction_threshold);
    if (present_min_compaction_threshold)
      builder.append(min_compaction_threshold);

    boolean present_max_compaction_threshold = true && (isSetMax_compaction_threshold());
    builder.append(present_max_compaction_threshold);
    if (present_max_compaction_threshold)
      builder.append(max_compaction_threshold);

    boolean present_key_validation_class = true && (isSetKey_validation_class());
    builder.append(present_key_validation_class);
    if (present_key_validation_class)
      builder.append(key_validation_class);

    boolean present_key_alias = true && (isSetKey_alias());
    builder.append(present_key_alias);
    if (present_key_alias)
      builder.append(key_alias);

    boolean present_compaction_strategy = true && (isSetCompaction_strategy());
    builder.append(present_compaction_strategy);
    if (present_compaction_strategy)
      builder.append(compaction_strategy);

    boolean present_compaction_strategy_options = true && (isSetCompaction_strategy_options());
    builder.append(present_compaction_strategy_options);
    if (present_compaction_strategy_options)
      builder.append(compaction_strategy_options);

    boolean present_compression_options = true && (isSetCompression_options());
    builder.append(present_compression_options);
    if (present_compression_options)
      builder.append(compression_options);

    boolean present_bloom_filter_fp_chance = true && (isSetBloom_filter_fp_chance());
    builder.append(present_bloom_filter_fp_chance);
    if (present_bloom_filter_fp_chance)
      builder.append(bloom_filter_fp_chance);

    boolean present_caching = true && (isSetCaching());
    builder.append(present_caching);
    if (present_caching)
      builder.append(caching);

    boolean present_dclocal_read_repair_chance = true && (isSetDclocal_read_repair_chance());
    builder.append(present_dclocal_read_repair_chance);
    if (present_dclocal_read_repair_chance)
      builder.append(dclocal_read_repair_chance);

    boolean present_memtable_flush_period_in_ms = true && (isSetMemtable_flush_period_in_ms());
    builder.append(present_memtable_flush_period_in_ms);
    if (present_memtable_flush_period_in_ms)
      builder.append(memtable_flush_period_in_ms);

    boolean present_default_time_to_live = true && (isSetDefault_time_to_live());
    builder.append(present_default_time_to_live);
    if (present_default_time_to_live)
      builder.append(default_time_to_live);

    boolean present_speculative_retry = true && (isSetSpeculative_retry());
    builder.append(present_speculative_retry);
    if (present_speculative_retry)
      builder.append(speculative_retry);

    boolean present_triggers = true && (isSetTriggers());
    builder.append(present_triggers);
    if (present_triggers)
      builder.append(triggers);

    boolean present_cells_per_row_to_cache = true && (isSetCells_per_row_to_cache());
    builder.append(present_cells_per_row_to_cache);
    if (present_cells_per_row_to_cache)
      builder.append(cells_per_row_to_cache);

    boolean present_min_index_interval = true && (isSetMin_index_interval());
    builder.append(present_min_index_interval);
    if (present_min_index_interval)
      builder.append(min_index_interval);

    boolean present_max_index_interval = true && (isSetMax_index_interval());
    builder.append(present_max_index_interval);
    if (present_max_index_interval)
      builder.append(max_index_interval);

    boolean present_row_cache_size = true && (isSetRow_cache_size());
    builder.append(present_row_cache_size);
    if (present_row_cache_size)
      builder.append(row_cache_size);

    boolean present_key_cache_size = true && (isSetKey_cache_size());
    builder.append(present_key_cache_size);
    if (present_key_cache_size)
      builder.append(key_cache_size);

    boolean present_row_cache_save_period_in_seconds = true && (isSetRow_cache_save_period_in_seconds());
    builder.append(present_row_cache_save_period_in_seconds);
    if (present_row_cache_save_period_in_seconds)
      builder.append(row_cache_save_period_in_seconds);

    boolean present_key_cache_save_period_in_seconds = true && (isSetKey_cache_save_period_in_seconds());
    builder.append(present_key_cache_save_period_in_seconds);
    if (present_key_cache_save_period_in_seconds)
      builder.append(key_cache_save_period_in_seconds);

    boolean present_memtable_flush_after_mins = true && (isSetMemtable_flush_after_mins());
    builder.append(present_memtable_flush_after_mins);
    if (present_memtable_flush_after_mins)
      builder.append(memtable_flush_after_mins);

    boolean present_memtable_throughput_in_mb = true && (isSetMemtable_throughput_in_mb());
    builder.append(present_memtable_throughput_in_mb);
    if (present_memtable_throughput_in_mb)
      builder.append(memtable_throughput_in_mb);

    boolean present_memtable_operations_in_millions = true && (isSetMemtable_operations_in_millions());
    builder.append(present_memtable_operations_in_millions);
    if (present_memtable_operations_in_millions)
      builder.append(memtable_operations_in_millions);

    boolean present_replicate_on_write = true && (isSetReplicate_on_write());
    builder.append(present_replicate_on_write);
    if (present_replicate_on_write)
      builder.append(replicate_on_write);

    boolean present_merge_shards_chance = true && (isSetMerge_shards_chance());
    builder.append(present_merge_shards_chance);
    if (present_merge_shards_chance)
      builder.append(merge_shards_chance);

    boolean present_row_cache_provider = true && (isSetRow_cache_provider());
    builder.append(present_row_cache_provider);
    if (present_row_cache_provider)
      builder.append(row_cache_provider);

    boolean present_row_cache_keys_to_save = true && (isSetRow_cache_keys_to_save());
    builder.append(present_row_cache_keys_to_save);
    if (present_row_cache_keys_to_save)
      builder.append(row_cache_keys_to_save);

    boolean present_populate_io_cache_on_flush = true && (isSetPopulate_io_cache_on_flush());
    builder.append(present_populate_io_cache_on_flush);
    if (present_populate_io_cache_on_flush)
      builder.append(populate_io_cache_on_flush);

    boolean present_index_interval = true && (isSetIndex_interval());
    builder.append(present_index_interval);
    if (present_index_interval)
      builder.append(index_interval);

    return builder.toHashCode();
  }

  @Override
  public int compareTo(CfDef other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetKeyspace()).compareTo(other.isSetKeyspace());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKeyspace()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.keyspace, other.keyspace);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
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
    lastComparison = Boolean.valueOf(isSetColumn_type()).compareTo(other.isSetColumn_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.column_type, other.column_type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetComparator_type()).compareTo(other.isSetComparator_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetComparator_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.comparator_type, other.comparator_type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSubcomparator_type()).compareTo(other.isSetSubcomparator_type());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSubcomparator_type()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.subcomparator_type, other.subcomparator_type);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetComment()).compareTo(other.isSetComment());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetComment()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.comment, other.comment);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRead_repair_chance()).compareTo(other.isSetRead_repair_chance());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRead_repair_chance()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.read_repair_chance, other.read_repair_chance);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetColumn_metadata()).compareTo(other.isSetColumn_metadata());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_metadata()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.column_metadata, other.column_metadata);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetGc_grace_seconds()).compareTo(other.isSetGc_grace_seconds());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetGc_grace_seconds()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.gc_grace_seconds, other.gc_grace_seconds);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDefault_validation_class()).compareTo(other.isSetDefault_validation_class());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDefault_validation_class()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.default_validation_class, other.default_validation_class);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetId()).compareTo(other.isSetId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.id, other.id);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMin_compaction_threshold()).compareTo(other.isSetMin_compaction_threshold());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMin_compaction_threshold()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.min_compaction_threshold, other.min_compaction_threshold);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMax_compaction_threshold()).compareTo(other.isSetMax_compaction_threshold());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMax_compaction_threshold()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.max_compaction_threshold, other.max_compaction_threshold);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKey_validation_class()).compareTo(other.isSetKey_validation_class());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey_validation_class()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key_validation_class, other.key_validation_class);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKey_alias()).compareTo(other.isSetKey_alias());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey_alias()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key_alias, other.key_alias);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCompaction_strategy()).compareTo(other.isSetCompaction_strategy());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCompaction_strategy()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.compaction_strategy, other.compaction_strategy);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCompaction_strategy_options()).compareTo(other.isSetCompaction_strategy_options());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCompaction_strategy_options()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.compaction_strategy_options, other.compaction_strategy_options);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCompression_options()).compareTo(other.isSetCompression_options());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCompression_options()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.compression_options, other.compression_options);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetBloom_filter_fp_chance()).compareTo(other.isSetBloom_filter_fp_chance());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetBloom_filter_fp_chance()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.bloom_filter_fp_chance, other.bloom_filter_fp_chance);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCaching()).compareTo(other.isSetCaching());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCaching()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.caching, other.caching);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDclocal_read_repair_chance()).compareTo(other.isSetDclocal_read_repair_chance());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDclocal_read_repair_chance()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.dclocal_read_repair_chance, other.dclocal_read_repair_chance);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMemtable_flush_period_in_ms()).compareTo(other.isSetMemtable_flush_period_in_ms());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMemtable_flush_period_in_ms()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.memtable_flush_period_in_ms, other.memtable_flush_period_in_ms);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDefault_time_to_live()).compareTo(other.isSetDefault_time_to_live());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDefault_time_to_live()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.default_time_to_live, other.default_time_to_live);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSpeculative_retry()).compareTo(other.isSetSpeculative_retry());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSpeculative_retry()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.speculative_retry, other.speculative_retry);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetTriggers()).compareTo(other.isSetTriggers());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTriggers()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.triggers, other.triggers);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCells_per_row_to_cache()).compareTo(other.isSetCells_per_row_to_cache());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCells_per_row_to_cache()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.cells_per_row_to_cache, other.cells_per_row_to_cache);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMin_index_interval()).compareTo(other.isSetMin_index_interval());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMin_index_interval()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.min_index_interval, other.min_index_interval);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMax_index_interval()).compareTo(other.isSetMax_index_interval());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMax_index_interval()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.max_index_interval, other.max_index_interval);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRow_cache_size()).compareTo(other.isSetRow_cache_size());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRow_cache_size()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.row_cache_size, other.row_cache_size);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKey_cache_size()).compareTo(other.isSetKey_cache_size());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey_cache_size()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key_cache_size, other.key_cache_size);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRow_cache_save_period_in_seconds()).compareTo(other.isSetRow_cache_save_period_in_seconds());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRow_cache_save_period_in_seconds()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.row_cache_save_period_in_seconds, other.row_cache_save_period_in_seconds);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKey_cache_save_period_in_seconds()).compareTo(other.isSetKey_cache_save_period_in_seconds());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey_cache_save_period_in_seconds()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key_cache_save_period_in_seconds, other.key_cache_save_period_in_seconds);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMemtable_flush_after_mins()).compareTo(other.isSetMemtable_flush_after_mins());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMemtable_flush_after_mins()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.memtable_flush_after_mins, other.memtable_flush_after_mins);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMemtable_throughput_in_mb()).compareTo(other.isSetMemtable_throughput_in_mb());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMemtable_throughput_in_mb()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.memtable_throughput_in_mb, other.memtable_throughput_in_mb);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMemtable_operations_in_millions()).compareTo(other.isSetMemtable_operations_in_millions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMemtable_operations_in_millions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.memtable_operations_in_millions, other.memtable_operations_in_millions);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetReplicate_on_write()).compareTo(other.isSetReplicate_on_write());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetReplicate_on_write()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.replicate_on_write, other.replicate_on_write);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMerge_shards_chance()).compareTo(other.isSetMerge_shards_chance());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMerge_shards_chance()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.merge_shards_chance, other.merge_shards_chance);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRow_cache_provider()).compareTo(other.isSetRow_cache_provider());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRow_cache_provider()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.row_cache_provider, other.row_cache_provider);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRow_cache_keys_to_save()).compareTo(other.isSetRow_cache_keys_to_save());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRow_cache_keys_to_save()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.row_cache_keys_to_save, other.row_cache_keys_to_save);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetPopulate_io_cache_on_flush()).compareTo(other.isSetPopulate_io_cache_on_flush());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPopulate_io_cache_on_flush()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.populate_io_cache_on_flush, other.populate_io_cache_on_flush);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIndex_interval()).compareTo(other.isSetIndex_interval());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndex_interval()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.index_interval, other.index_interval);
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
    StringBuilder sb = new StringBuilder("CfDef(");
    boolean first = true;

    sb.append("keyspace:");
    if (this.keyspace == null) {
      sb.append("null");
    } else {
      sb.append(this.keyspace);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("name:");
    if (this.name == null) {
      sb.append("null");
    } else {
      sb.append(this.name);
    }
    first = false;
    if (isSetColumn_type()) {
      if (!first) sb.append(", ");
      sb.append("column_type:");
      if (this.column_type == null) {
        sb.append("null");
      } else {
        sb.append(this.column_type);
      }
      first = false;
    }
    if (isSetComparator_type()) {
      if (!first) sb.append(", ");
      sb.append("comparator_type:");
      if (this.comparator_type == null) {
        sb.append("null");
      } else {
        sb.append(this.comparator_type);
      }
      first = false;
    }
    if (isSetSubcomparator_type()) {
      if (!first) sb.append(", ");
      sb.append("subcomparator_type:");
      if (this.subcomparator_type == null) {
        sb.append("null");
      } else {
        sb.append(this.subcomparator_type);
      }
      first = false;
    }
    if (isSetComment()) {
      if (!first) sb.append(", ");
      sb.append("comment:");
      if (this.comment == null) {
        sb.append("null");
      } else {
        sb.append(this.comment);
      }
      first = false;
    }
    if (isSetRead_repair_chance()) {
      if (!first) sb.append(", ");
      sb.append("read_repair_chance:");
      sb.append(this.read_repair_chance);
      first = false;
    }
    if (isSetColumn_metadata()) {
      if (!first) sb.append(", ");
      sb.append("column_metadata:");
      if (this.column_metadata == null) {
        sb.append("null");
      } else {
        sb.append(this.column_metadata);
      }
      first = false;
    }
    if (isSetGc_grace_seconds()) {
      if (!first) sb.append(", ");
      sb.append("gc_grace_seconds:");
      sb.append(this.gc_grace_seconds);
      first = false;
    }
    if (isSetDefault_validation_class()) {
      if (!first) sb.append(", ");
      sb.append("default_validation_class:");
      if (this.default_validation_class == null) {
        sb.append("null");
      } else {
        sb.append(this.default_validation_class);
      }
      first = false;
    }
    if (isSetId()) {
      if (!first) sb.append(", ");
      sb.append("id:");
      sb.append(this.id);
      first = false;
    }
    if (isSetMin_compaction_threshold()) {
      if (!first) sb.append(", ");
      sb.append("min_compaction_threshold:");
      sb.append(this.min_compaction_threshold);
      first = false;
    }
    if (isSetMax_compaction_threshold()) {
      if (!first) sb.append(", ");
      sb.append("max_compaction_threshold:");
      sb.append(this.max_compaction_threshold);
      first = false;
    }
    if (isSetKey_validation_class()) {
      if (!first) sb.append(", ");
      sb.append("key_validation_class:");
      if (this.key_validation_class == null) {
        sb.append("null");
      } else {
        sb.append(this.key_validation_class);
      }
      first = false;
    }
    if (isSetKey_alias()) {
      if (!first) sb.append(", ");
      sb.append("key_alias:");
      if (this.key_alias == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.key_alias, sb);
      }
      first = false;
    }
    if (isSetCompaction_strategy()) {
      if (!first) sb.append(", ");
      sb.append("compaction_strategy:");
      if (this.compaction_strategy == null) {
        sb.append("null");
      } else {
        sb.append(this.compaction_strategy);
      }
      first = false;
    }
    if (isSetCompaction_strategy_options()) {
      if (!first) sb.append(", ");
      sb.append("compaction_strategy_options:");
      if (this.compaction_strategy_options == null) {
        sb.append("null");
      } else {
        sb.append(this.compaction_strategy_options);
      }
      first = false;
    }
    if (isSetCompression_options()) {
      if (!first) sb.append(", ");
      sb.append("compression_options:");
      if (this.compression_options == null) {
        sb.append("null");
      } else {
        sb.append(this.compression_options);
      }
      first = false;
    }
    if (isSetBloom_filter_fp_chance()) {
      if (!first) sb.append(", ");
      sb.append("bloom_filter_fp_chance:");
      sb.append(this.bloom_filter_fp_chance);
      first = false;
    }
    if (isSetCaching()) {
      if (!first) sb.append(", ");
      sb.append("caching:");
      if (this.caching == null) {
        sb.append("null");
      } else {
        sb.append(this.caching);
      }
      first = false;
    }
    if (isSetDclocal_read_repair_chance()) {
      if (!first) sb.append(", ");
      sb.append("dclocal_read_repair_chance:");
      sb.append(this.dclocal_read_repair_chance);
      first = false;
    }
    if (isSetMemtable_flush_period_in_ms()) {
      if (!first) sb.append(", ");
      sb.append("memtable_flush_period_in_ms:");
      sb.append(this.memtable_flush_period_in_ms);
      first = false;
    }
    if (isSetDefault_time_to_live()) {
      if (!first) sb.append(", ");
      sb.append("default_time_to_live:");
      sb.append(this.default_time_to_live);
      first = false;
    }
    if (isSetSpeculative_retry()) {
      if (!first) sb.append(", ");
      sb.append("speculative_retry:");
      if (this.speculative_retry == null) {
        sb.append("null");
      } else {
        sb.append(this.speculative_retry);
      }
      first = false;
    }
    if (isSetTriggers()) {
      if (!first) sb.append(", ");
      sb.append("triggers:");
      if (this.triggers == null) {
        sb.append("null");
      } else {
        sb.append(this.triggers);
      }
      first = false;
    }
    if (isSetCells_per_row_to_cache()) {
      if (!first) sb.append(", ");
      sb.append("cells_per_row_to_cache:");
      if (this.cells_per_row_to_cache == null) {
        sb.append("null");
      } else {
        sb.append(this.cells_per_row_to_cache);
      }
      first = false;
    }
    if (isSetMin_index_interval()) {
      if (!first) sb.append(", ");
      sb.append("min_index_interval:");
      sb.append(this.min_index_interval);
      first = false;
    }
    if (isSetMax_index_interval()) {
      if (!first) sb.append(", ");
      sb.append("max_index_interval:");
      sb.append(this.max_index_interval);
      first = false;
    }
    if (isSetRow_cache_size()) {
      if (!first) sb.append(", ");
      sb.append("row_cache_size:");
      sb.append(this.row_cache_size);
      first = false;
    }
    if (isSetKey_cache_size()) {
      if (!first) sb.append(", ");
      sb.append("key_cache_size:");
      sb.append(this.key_cache_size);
      first = false;
    }
    if (isSetRow_cache_save_period_in_seconds()) {
      if (!first) sb.append(", ");
      sb.append("row_cache_save_period_in_seconds:");
      sb.append(this.row_cache_save_period_in_seconds);
      first = false;
    }
    if (isSetKey_cache_save_period_in_seconds()) {
      if (!first) sb.append(", ");
      sb.append("key_cache_save_period_in_seconds:");
      sb.append(this.key_cache_save_period_in_seconds);
      first = false;
    }
    if (isSetMemtable_flush_after_mins()) {
      if (!first) sb.append(", ");
      sb.append("memtable_flush_after_mins:");
      sb.append(this.memtable_flush_after_mins);
      first = false;
    }
    if (isSetMemtable_throughput_in_mb()) {
      if (!first) sb.append(", ");
      sb.append("memtable_throughput_in_mb:");
      sb.append(this.memtable_throughput_in_mb);
      first = false;
    }
    if (isSetMemtable_operations_in_millions()) {
      if (!first) sb.append(", ");
      sb.append("memtable_operations_in_millions:");
      sb.append(this.memtable_operations_in_millions);
      first = false;
    }
    if (isSetReplicate_on_write()) {
      if (!first) sb.append(", ");
      sb.append("replicate_on_write:");
      sb.append(this.replicate_on_write);
      first = false;
    }
    if (isSetMerge_shards_chance()) {
      if (!first) sb.append(", ");
      sb.append("merge_shards_chance:");
      sb.append(this.merge_shards_chance);
      first = false;
    }
    if (isSetRow_cache_provider()) {
      if (!first) sb.append(", ");
      sb.append("row_cache_provider:");
      if (this.row_cache_provider == null) {
        sb.append("null");
      } else {
        sb.append(this.row_cache_provider);
      }
      first = false;
    }
    if (isSetRow_cache_keys_to_save()) {
      if (!first) sb.append(", ");
      sb.append("row_cache_keys_to_save:");
      sb.append(this.row_cache_keys_to_save);
      first = false;
    }
    if (isSetPopulate_io_cache_on_flush()) {
      if (!first) sb.append(", ");
      sb.append("populate_io_cache_on_flush:");
      sb.append(this.populate_io_cache_on_flush);
      first = false;
    }
    if (isSetIndex_interval()) {
      if (!first) sb.append(", ");
      sb.append("index_interval:");
      sb.append(this.index_interval);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (keyspace == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'keyspace' was not present! Struct: " + toString());
    }
    if (name == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'name' was not present! Struct: " + toString());
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class CfDefStandardSchemeFactory implements SchemeFactory {
    public CfDefStandardScheme getScheme() {
      return new CfDefStandardScheme();
    }
  }

  private static class CfDefStandardScheme extends StandardScheme<CfDef> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CfDef struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // KEYSPACE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.keyspace = iprot.readString();
              struct.setKeyspaceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.name = iprot.readString();
              struct.setNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // COLUMN_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.column_type = iprot.readString();
              struct.setColumn_typeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // COMPARATOR_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.comparator_type = iprot.readString();
              struct.setComparator_typeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // SUBCOMPARATOR_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.subcomparator_type = iprot.readString();
              struct.setSubcomparator_typeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 8: // COMMENT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.comment = iprot.readString();
              struct.setCommentIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 12: // READ_REPAIR_CHANCE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.read_repair_chance = iprot.readDouble();
              struct.setRead_repair_chanceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 13: // COLUMN_METADATA
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list110 = iprot.readListBegin();
                struct.column_metadata = new ArrayList<ColumnDef>(_list110.size);
                for (int _i111 = 0; _i111 < _list110.size; ++_i111)
                {
                  ColumnDef _elem112;
                  _elem112 = new ColumnDef();
                  _elem112.read(iprot);
                  struct.column_metadata.add(_elem112);
                }
                iprot.readListEnd();
              }
              struct.setColumn_metadataIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 14: // GC_GRACE_SECONDS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.gc_grace_seconds = iprot.readI32();
              struct.setGc_grace_secondsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 15: // DEFAULT_VALIDATION_CLASS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.default_validation_class = iprot.readString();
              struct.setDefault_validation_classIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 16: // ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.id = iprot.readI32();
              struct.setIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 17: // MIN_COMPACTION_THRESHOLD
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.min_compaction_threshold = iprot.readI32();
              struct.setMin_compaction_thresholdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 18: // MAX_COMPACTION_THRESHOLD
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.max_compaction_threshold = iprot.readI32();
              struct.setMax_compaction_thresholdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 26: // KEY_VALIDATION_CLASS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.key_validation_class = iprot.readString();
              struct.setKey_validation_classIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 28: // KEY_ALIAS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.key_alias = iprot.readBinary();
              struct.setKey_aliasIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 29: // COMPACTION_STRATEGY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.compaction_strategy = iprot.readString();
              struct.setCompaction_strategyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 30: // COMPACTION_STRATEGY_OPTIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map113 = iprot.readMapBegin();
                struct.compaction_strategy_options = new HashMap<String,String>(2*_map113.size);
                for (int _i114 = 0; _i114 < _map113.size; ++_i114)
                {
                  String _key115;
                  String _val116;
                  _key115 = iprot.readString();
                  _val116 = iprot.readString();
                  struct.compaction_strategy_options.put(_key115, _val116);
                }
                iprot.readMapEnd();
              }
              struct.setCompaction_strategy_optionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 32: // COMPRESSION_OPTIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map117 = iprot.readMapBegin();
                struct.compression_options = new HashMap<String,String>(2*_map117.size);
                for (int _i118 = 0; _i118 < _map117.size; ++_i118)
                {
                  String _key119;
                  String _val120;
                  _key119 = iprot.readString();
                  _val120 = iprot.readString();
                  struct.compression_options.put(_key119, _val120);
                }
                iprot.readMapEnd();
              }
              struct.setCompression_optionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 33: // BLOOM_FILTER_FP_CHANCE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.bloom_filter_fp_chance = iprot.readDouble();
              struct.setBloom_filter_fp_chanceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 34: // CACHING
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.caching = iprot.readString();
              struct.setCachingIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 37: // DCLOCAL_READ_REPAIR_CHANCE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.dclocal_read_repair_chance = iprot.readDouble();
              struct.setDclocal_read_repair_chanceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 39: // MEMTABLE_FLUSH_PERIOD_IN_MS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.memtable_flush_period_in_ms = iprot.readI32();
              struct.setMemtable_flush_period_in_msIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 40: // DEFAULT_TIME_TO_LIVE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.default_time_to_live = iprot.readI32();
              struct.setDefault_time_to_liveIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 42: // SPECULATIVE_RETRY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.speculative_retry = iprot.readString();
              struct.setSpeculative_retryIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 43: // TRIGGERS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list121 = iprot.readListBegin();
                struct.triggers = new ArrayList<TriggerDef>(_list121.size);
                for (int _i122 = 0; _i122 < _list121.size; ++_i122)
                {
                  TriggerDef _elem123;
                  _elem123 = new TriggerDef();
                  _elem123.read(iprot);
                  struct.triggers.add(_elem123);
                }
                iprot.readListEnd();
              }
              struct.setTriggersIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 44: // CELLS_PER_ROW_TO_CACHE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.cells_per_row_to_cache = iprot.readString();
              struct.setCells_per_row_to_cacheIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 45: // MIN_INDEX_INTERVAL
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.min_index_interval = iprot.readI32();
              struct.setMin_index_intervalIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 46: // MAX_INDEX_INTERVAL
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.max_index_interval = iprot.readI32();
              struct.setMax_index_intervalIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 9: // ROW_CACHE_SIZE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.row_cache_size = iprot.readDouble();
              struct.setRow_cache_sizeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 11: // KEY_CACHE_SIZE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.key_cache_size = iprot.readDouble();
              struct.setKey_cache_sizeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 19: // ROW_CACHE_SAVE_PERIOD_IN_SECONDS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.row_cache_save_period_in_seconds = iprot.readI32();
              struct.setRow_cache_save_period_in_secondsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 20: // KEY_CACHE_SAVE_PERIOD_IN_SECONDS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.key_cache_save_period_in_seconds = iprot.readI32();
              struct.setKey_cache_save_period_in_secondsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 21: // MEMTABLE_FLUSH_AFTER_MINS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.memtable_flush_after_mins = iprot.readI32();
              struct.setMemtable_flush_after_minsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 22: // MEMTABLE_THROUGHPUT_IN_MB
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.memtable_throughput_in_mb = iprot.readI32();
              struct.setMemtable_throughput_in_mbIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 23: // MEMTABLE_OPERATIONS_IN_MILLIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.memtable_operations_in_millions = iprot.readDouble();
              struct.setMemtable_operations_in_millionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 24: // REPLICATE_ON_WRITE
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.replicate_on_write = iprot.readBool();
              struct.setReplicate_on_writeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 25: // MERGE_SHARDS_CHANCE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.merge_shards_chance = iprot.readDouble();
              struct.setMerge_shards_chanceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 27: // ROW_CACHE_PROVIDER
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.row_cache_provider = iprot.readString();
              struct.setRow_cache_providerIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 31: // ROW_CACHE_KEYS_TO_SAVE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.row_cache_keys_to_save = iprot.readI32();
              struct.setRow_cache_keys_to_saveIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 38: // POPULATE_IO_CACHE_ON_FLUSH
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.populate_io_cache_on_flush = iprot.readBool();
              struct.setPopulate_io_cache_on_flushIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 41: // INDEX_INTERVAL
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.index_interval = iprot.readI32();
              struct.setIndex_intervalIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, CfDef struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.keyspace != null) {
        oprot.writeFieldBegin(KEYSPACE_FIELD_DESC);
        oprot.writeString(struct.keyspace);
        oprot.writeFieldEnd();
      }
      if (struct.name != null) {
        oprot.writeFieldBegin(NAME_FIELD_DESC);
        oprot.writeString(struct.name);
        oprot.writeFieldEnd();
      }
      if (struct.column_type != null) {
        if (struct.isSetColumn_type()) {
          oprot.writeFieldBegin(COLUMN_TYPE_FIELD_DESC);
          oprot.writeString(struct.column_type);
          oprot.writeFieldEnd();
        }
      }
      if (struct.comparator_type != null) {
        if (struct.isSetComparator_type()) {
          oprot.writeFieldBegin(COMPARATOR_TYPE_FIELD_DESC);
          oprot.writeString(struct.comparator_type);
          oprot.writeFieldEnd();
        }
      }
      if (struct.subcomparator_type != null) {
        if (struct.isSetSubcomparator_type()) {
          oprot.writeFieldBegin(SUBCOMPARATOR_TYPE_FIELD_DESC);
          oprot.writeString(struct.subcomparator_type);
          oprot.writeFieldEnd();
        }
      }
      if (struct.comment != null) {
        if (struct.isSetComment()) {
          oprot.writeFieldBegin(COMMENT_FIELD_DESC);
          oprot.writeString(struct.comment);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetRow_cache_size()) {
        oprot.writeFieldBegin(ROW_CACHE_SIZE_FIELD_DESC);
        oprot.writeDouble(struct.row_cache_size);
        oprot.writeFieldEnd();
      }
      if (struct.isSetKey_cache_size()) {
        oprot.writeFieldBegin(KEY_CACHE_SIZE_FIELD_DESC);
        oprot.writeDouble(struct.key_cache_size);
        oprot.writeFieldEnd();
      }
      if (struct.isSetRead_repair_chance()) {
        oprot.writeFieldBegin(READ_REPAIR_CHANCE_FIELD_DESC);
        oprot.writeDouble(struct.read_repair_chance);
        oprot.writeFieldEnd();
      }
      if (struct.column_metadata != null) {
        if (struct.isSetColumn_metadata()) {
          oprot.writeFieldBegin(COLUMN_METADATA_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.column_metadata.size()));
            for (ColumnDef _iter124 : struct.column_metadata)
            {
              _iter124.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetGc_grace_seconds()) {
        oprot.writeFieldBegin(GC_GRACE_SECONDS_FIELD_DESC);
        oprot.writeI32(struct.gc_grace_seconds);
        oprot.writeFieldEnd();
      }
      if (struct.default_validation_class != null) {
        if (struct.isSetDefault_validation_class()) {
          oprot.writeFieldBegin(DEFAULT_VALIDATION_CLASS_FIELD_DESC);
          oprot.writeString(struct.default_validation_class);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetId()) {
        oprot.writeFieldBegin(ID_FIELD_DESC);
        oprot.writeI32(struct.id);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMin_compaction_threshold()) {
        oprot.writeFieldBegin(MIN_COMPACTION_THRESHOLD_FIELD_DESC);
        oprot.writeI32(struct.min_compaction_threshold);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMax_compaction_threshold()) {
        oprot.writeFieldBegin(MAX_COMPACTION_THRESHOLD_FIELD_DESC);
        oprot.writeI32(struct.max_compaction_threshold);
        oprot.writeFieldEnd();
      }
      if (struct.isSetRow_cache_save_period_in_seconds()) {
        oprot.writeFieldBegin(ROW_CACHE_SAVE_PERIOD_IN_SECONDS_FIELD_DESC);
        oprot.writeI32(struct.row_cache_save_period_in_seconds);
        oprot.writeFieldEnd();
      }
      if (struct.isSetKey_cache_save_period_in_seconds()) {
        oprot.writeFieldBegin(KEY_CACHE_SAVE_PERIOD_IN_SECONDS_FIELD_DESC);
        oprot.writeI32(struct.key_cache_save_period_in_seconds);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMemtable_flush_after_mins()) {
        oprot.writeFieldBegin(MEMTABLE_FLUSH_AFTER_MINS_FIELD_DESC);
        oprot.writeI32(struct.memtable_flush_after_mins);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMemtable_throughput_in_mb()) {
        oprot.writeFieldBegin(MEMTABLE_THROUGHPUT_IN_MB_FIELD_DESC);
        oprot.writeI32(struct.memtable_throughput_in_mb);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMemtable_operations_in_millions()) {
        oprot.writeFieldBegin(MEMTABLE_OPERATIONS_IN_MILLIONS_FIELD_DESC);
        oprot.writeDouble(struct.memtable_operations_in_millions);
        oprot.writeFieldEnd();
      }
      if (struct.isSetReplicate_on_write()) {
        oprot.writeFieldBegin(REPLICATE_ON_WRITE_FIELD_DESC);
        oprot.writeBool(struct.replicate_on_write);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMerge_shards_chance()) {
        oprot.writeFieldBegin(MERGE_SHARDS_CHANCE_FIELD_DESC);
        oprot.writeDouble(struct.merge_shards_chance);
        oprot.writeFieldEnd();
      }
      if (struct.key_validation_class != null) {
        if (struct.isSetKey_validation_class()) {
          oprot.writeFieldBegin(KEY_VALIDATION_CLASS_FIELD_DESC);
          oprot.writeString(struct.key_validation_class);
          oprot.writeFieldEnd();
        }
      }
      if (struct.row_cache_provider != null) {
        if (struct.isSetRow_cache_provider()) {
          oprot.writeFieldBegin(ROW_CACHE_PROVIDER_FIELD_DESC);
          oprot.writeString(struct.row_cache_provider);
          oprot.writeFieldEnd();
        }
      }
      if (struct.key_alias != null) {
        if (struct.isSetKey_alias()) {
          oprot.writeFieldBegin(KEY_ALIAS_FIELD_DESC);
          oprot.writeBinary(struct.key_alias);
          oprot.writeFieldEnd();
        }
      }
      if (struct.compaction_strategy != null) {
        if (struct.isSetCompaction_strategy()) {
          oprot.writeFieldBegin(COMPACTION_STRATEGY_FIELD_DESC);
          oprot.writeString(struct.compaction_strategy);
          oprot.writeFieldEnd();
        }
      }
      if (struct.compaction_strategy_options != null) {
        if (struct.isSetCompaction_strategy_options()) {
          oprot.writeFieldBegin(COMPACTION_STRATEGY_OPTIONS_FIELD_DESC);
          {
            oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.compaction_strategy_options.size()));
            for (Map.Entry<String, String> _iter125 : struct.compaction_strategy_options.entrySet())
            {
              oprot.writeString(_iter125.getKey());
              oprot.writeString(_iter125.getValue());
            }
            oprot.writeMapEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetRow_cache_keys_to_save()) {
        oprot.writeFieldBegin(ROW_CACHE_KEYS_TO_SAVE_FIELD_DESC);
        oprot.writeI32(struct.row_cache_keys_to_save);
        oprot.writeFieldEnd();
      }
      if (struct.compression_options != null) {
        if (struct.isSetCompression_options()) {
          oprot.writeFieldBegin(COMPRESSION_OPTIONS_FIELD_DESC);
          {
            oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.compression_options.size()));
            for (Map.Entry<String, String> _iter126 : struct.compression_options.entrySet())
            {
              oprot.writeString(_iter126.getKey());
              oprot.writeString(_iter126.getValue());
            }
            oprot.writeMapEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetBloom_filter_fp_chance()) {
        oprot.writeFieldBegin(BLOOM_FILTER_FP_CHANCE_FIELD_DESC);
        oprot.writeDouble(struct.bloom_filter_fp_chance);
        oprot.writeFieldEnd();
      }
      if (struct.caching != null) {
        if (struct.isSetCaching()) {
          oprot.writeFieldBegin(CACHING_FIELD_DESC);
          oprot.writeString(struct.caching);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetDclocal_read_repair_chance()) {
        oprot.writeFieldBegin(DCLOCAL_READ_REPAIR_CHANCE_FIELD_DESC);
        oprot.writeDouble(struct.dclocal_read_repair_chance);
        oprot.writeFieldEnd();
      }
      if (struct.isSetPopulate_io_cache_on_flush()) {
        oprot.writeFieldBegin(POPULATE_IO_CACHE_ON_FLUSH_FIELD_DESC);
        oprot.writeBool(struct.populate_io_cache_on_flush);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMemtable_flush_period_in_ms()) {
        oprot.writeFieldBegin(MEMTABLE_FLUSH_PERIOD_IN_MS_FIELD_DESC);
        oprot.writeI32(struct.memtable_flush_period_in_ms);
        oprot.writeFieldEnd();
      }
      if (struct.isSetDefault_time_to_live()) {
        oprot.writeFieldBegin(DEFAULT_TIME_TO_LIVE_FIELD_DESC);
        oprot.writeI32(struct.default_time_to_live);
        oprot.writeFieldEnd();
      }
      if (struct.isSetIndex_interval()) {
        oprot.writeFieldBegin(INDEX_INTERVAL_FIELD_DESC);
        oprot.writeI32(struct.index_interval);
        oprot.writeFieldEnd();
      }
      if (struct.speculative_retry != null) {
        if (struct.isSetSpeculative_retry()) {
          oprot.writeFieldBegin(SPECULATIVE_RETRY_FIELD_DESC);
          oprot.writeString(struct.speculative_retry);
          oprot.writeFieldEnd();
        }
      }
      if (struct.triggers != null) {
        if (struct.isSetTriggers()) {
          oprot.writeFieldBegin(TRIGGERS_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.triggers.size()));
            for (TriggerDef _iter127 : struct.triggers)
            {
              _iter127.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      if (struct.cells_per_row_to_cache != null) {
        if (struct.isSetCells_per_row_to_cache()) {
          oprot.writeFieldBegin(CELLS_PER_ROW_TO_CACHE_FIELD_DESC);
          oprot.writeString(struct.cells_per_row_to_cache);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetMin_index_interval()) {
        oprot.writeFieldBegin(MIN_INDEX_INTERVAL_FIELD_DESC);
        oprot.writeI32(struct.min_index_interval);
        oprot.writeFieldEnd();
      }
      if (struct.isSetMax_index_interval()) {
        oprot.writeFieldBegin(MAX_INDEX_INTERVAL_FIELD_DESC);
        oprot.writeI32(struct.max_index_interval);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CfDefTupleSchemeFactory implements SchemeFactory {
    public CfDefTupleScheme getScheme() {
      return new CfDefTupleScheme();
    }
  }

  private static class CfDefTupleScheme extends TupleScheme<CfDef> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CfDef struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeString(struct.keyspace);
      oprot.writeString(struct.name);
      BitSet optionals = new BitSet();
      if (struct.isSetColumn_type()) {
        optionals.set(0);
      }
      if (struct.isSetComparator_type()) {
        optionals.set(1);
      }
      if (struct.isSetSubcomparator_type()) {
        optionals.set(2);
      }
      if (struct.isSetComment()) {
        optionals.set(3);
      }
      if (struct.isSetRead_repair_chance()) {
        optionals.set(4);
      }
      if (struct.isSetColumn_metadata()) {
        optionals.set(5);
      }
      if (struct.isSetGc_grace_seconds()) {
        optionals.set(6);
      }
      if (struct.isSetDefault_validation_class()) {
        optionals.set(7);
      }
      if (struct.isSetId()) {
        optionals.set(8);
      }
      if (struct.isSetMin_compaction_threshold()) {
        optionals.set(9);
      }
      if (struct.isSetMax_compaction_threshold()) {
        optionals.set(10);
      }
      if (struct.isSetKey_validation_class()) {
        optionals.set(11);
      }
      if (struct.isSetKey_alias()) {
        optionals.set(12);
      }
      if (struct.isSetCompaction_strategy()) {
        optionals.set(13);
      }
      if (struct.isSetCompaction_strategy_options()) {
        optionals.set(14);
      }
      if (struct.isSetCompression_options()) {
        optionals.set(15);
      }
      if (struct.isSetBloom_filter_fp_chance()) {
        optionals.set(16);
      }
      if (struct.isSetCaching()) {
        optionals.set(17);
      }
      if (struct.isSetDclocal_read_repair_chance()) {
        optionals.set(18);
      }
      if (struct.isSetMemtable_flush_period_in_ms()) {
        optionals.set(19);
      }
      if (struct.isSetDefault_time_to_live()) {
        optionals.set(20);
      }
      if (struct.isSetSpeculative_retry()) {
        optionals.set(21);
      }
      if (struct.isSetTriggers()) {
        optionals.set(22);
      }
      if (struct.isSetCells_per_row_to_cache()) {
        optionals.set(23);
      }
      if (struct.isSetMin_index_interval()) {
        optionals.set(24);
      }
      if (struct.isSetMax_index_interval()) {
        optionals.set(25);
      }
      if (struct.isSetRow_cache_size()) {
        optionals.set(26);
      }
      if (struct.isSetKey_cache_size()) {
        optionals.set(27);
      }
      if (struct.isSetRow_cache_save_period_in_seconds()) {
        optionals.set(28);
      }
      if (struct.isSetKey_cache_save_period_in_seconds()) {
        optionals.set(29);
      }
      if (struct.isSetMemtable_flush_after_mins()) {
        optionals.set(30);
      }
      if (struct.isSetMemtable_throughput_in_mb()) {
        optionals.set(31);
      }
      if (struct.isSetMemtable_operations_in_millions()) {
        optionals.set(32);
      }
      if (struct.isSetReplicate_on_write()) {
        optionals.set(33);
      }
      if (struct.isSetMerge_shards_chance()) {
        optionals.set(34);
      }
      if (struct.isSetRow_cache_provider()) {
        optionals.set(35);
      }
      if (struct.isSetRow_cache_keys_to_save()) {
        optionals.set(36);
      }
      if (struct.isSetPopulate_io_cache_on_flush()) {
        optionals.set(37);
      }
      if (struct.isSetIndex_interval()) {
        optionals.set(38);
      }
      oprot.writeBitSet(optionals, 39);
      if (struct.isSetColumn_type()) {
        oprot.writeString(struct.column_type);
      }
      if (struct.isSetComparator_type()) {
        oprot.writeString(struct.comparator_type);
      }
      if (struct.isSetSubcomparator_type()) {
        oprot.writeString(struct.subcomparator_type);
      }
      if (struct.isSetComment()) {
        oprot.writeString(struct.comment);
      }
      if (struct.isSetRead_repair_chance()) {
        oprot.writeDouble(struct.read_repair_chance);
      }
      if (struct.isSetColumn_metadata()) {
        {
          oprot.writeI32(struct.column_metadata.size());
          for (ColumnDef _iter128 : struct.column_metadata)
          {
            _iter128.write(oprot);
          }
        }
      }
      if (struct.isSetGc_grace_seconds()) {
        oprot.writeI32(struct.gc_grace_seconds);
      }
      if (struct.isSetDefault_validation_class()) {
        oprot.writeString(struct.default_validation_class);
      }
      if (struct.isSetId()) {
        oprot.writeI32(struct.id);
      }
      if (struct.isSetMin_compaction_threshold()) {
        oprot.writeI32(struct.min_compaction_threshold);
      }
      if (struct.isSetMax_compaction_threshold()) {
        oprot.writeI32(struct.max_compaction_threshold);
      }
      if (struct.isSetKey_validation_class()) {
        oprot.writeString(struct.key_validation_class);
      }
      if (struct.isSetKey_alias()) {
        oprot.writeBinary(struct.key_alias);
      }
      if (struct.isSetCompaction_strategy()) {
        oprot.writeString(struct.compaction_strategy);
      }
      if (struct.isSetCompaction_strategy_options()) {
        {
          oprot.writeI32(struct.compaction_strategy_options.size());
          for (Map.Entry<String, String> _iter129 : struct.compaction_strategy_options.entrySet())
          {
            oprot.writeString(_iter129.getKey());
            oprot.writeString(_iter129.getValue());
          }
        }
      }
      if (struct.isSetCompression_options()) {
        {
          oprot.writeI32(struct.compression_options.size());
          for (Map.Entry<String, String> _iter130 : struct.compression_options.entrySet())
          {
            oprot.writeString(_iter130.getKey());
            oprot.writeString(_iter130.getValue());
          }
        }
      }
      if (struct.isSetBloom_filter_fp_chance()) {
        oprot.writeDouble(struct.bloom_filter_fp_chance);
      }
      if (struct.isSetCaching()) {
        oprot.writeString(struct.caching);
      }
      if (struct.isSetDclocal_read_repair_chance()) {
        oprot.writeDouble(struct.dclocal_read_repair_chance);
      }
      if (struct.isSetMemtable_flush_period_in_ms()) {
        oprot.writeI32(struct.memtable_flush_period_in_ms);
      }
      if (struct.isSetDefault_time_to_live()) {
        oprot.writeI32(struct.default_time_to_live);
      }
      if (struct.isSetSpeculative_retry()) {
        oprot.writeString(struct.speculative_retry);
      }
      if (struct.isSetTriggers()) {
        {
          oprot.writeI32(struct.triggers.size());
          for (TriggerDef _iter131 : struct.triggers)
          {
            _iter131.write(oprot);
          }
        }
      }
      if (struct.isSetCells_per_row_to_cache()) {
        oprot.writeString(struct.cells_per_row_to_cache);
      }
      if (struct.isSetMin_index_interval()) {
        oprot.writeI32(struct.min_index_interval);
      }
      if (struct.isSetMax_index_interval()) {
        oprot.writeI32(struct.max_index_interval);
      }
      if (struct.isSetRow_cache_size()) {
        oprot.writeDouble(struct.row_cache_size);
      }
      if (struct.isSetKey_cache_size()) {
        oprot.writeDouble(struct.key_cache_size);
      }
      if (struct.isSetRow_cache_save_period_in_seconds()) {
        oprot.writeI32(struct.row_cache_save_period_in_seconds);
      }
      if (struct.isSetKey_cache_save_period_in_seconds()) {
        oprot.writeI32(struct.key_cache_save_period_in_seconds);
      }
      if (struct.isSetMemtable_flush_after_mins()) {
        oprot.writeI32(struct.memtable_flush_after_mins);
      }
      if (struct.isSetMemtable_throughput_in_mb()) {
        oprot.writeI32(struct.memtable_throughput_in_mb);
      }
      if (struct.isSetMemtable_operations_in_millions()) {
        oprot.writeDouble(struct.memtable_operations_in_millions);
      }
      if (struct.isSetReplicate_on_write()) {
        oprot.writeBool(struct.replicate_on_write);
      }
      if (struct.isSetMerge_shards_chance()) {
        oprot.writeDouble(struct.merge_shards_chance);
      }
      if (struct.isSetRow_cache_provider()) {
        oprot.writeString(struct.row_cache_provider);
      }
      if (struct.isSetRow_cache_keys_to_save()) {
        oprot.writeI32(struct.row_cache_keys_to_save);
      }
      if (struct.isSetPopulate_io_cache_on_flush()) {
        oprot.writeBool(struct.populate_io_cache_on_flush);
      }
      if (struct.isSetIndex_interval()) {
        oprot.writeI32(struct.index_interval);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CfDef struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.keyspace = iprot.readString();
      struct.setKeyspaceIsSet(true);
      struct.name = iprot.readString();
      struct.setNameIsSet(true);
      BitSet incoming = iprot.readBitSet(39);
      if (incoming.get(0)) {
        struct.column_type = iprot.readString();
        struct.setColumn_typeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.comparator_type = iprot.readString();
        struct.setComparator_typeIsSet(true);
      }
      if (incoming.get(2)) {
        struct.subcomparator_type = iprot.readString();
        struct.setSubcomparator_typeIsSet(true);
      }
      if (incoming.get(3)) {
        struct.comment = iprot.readString();
        struct.setCommentIsSet(true);
      }
      if (incoming.get(4)) {
        struct.read_repair_chance = iprot.readDouble();
        struct.setRead_repair_chanceIsSet(true);
      }
      if (incoming.get(5)) {
        {
          org.apache.thrift.protocol.TList _list132 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.column_metadata = new ArrayList<ColumnDef>(_list132.size);
          for (int _i133 = 0; _i133 < _list132.size; ++_i133)
          {
            ColumnDef _elem134;
            _elem134 = new ColumnDef();
            _elem134.read(iprot);
            struct.column_metadata.add(_elem134);
          }
        }
        struct.setColumn_metadataIsSet(true);
      }
      if (incoming.get(6)) {
        struct.gc_grace_seconds = iprot.readI32();
        struct.setGc_grace_secondsIsSet(true);
      }
      if (incoming.get(7)) {
        struct.default_validation_class = iprot.readString();
        struct.setDefault_validation_classIsSet(true);
      }
      if (incoming.get(8)) {
        struct.id = iprot.readI32();
        struct.setIdIsSet(true);
      }
      if (incoming.get(9)) {
        struct.min_compaction_threshold = iprot.readI32();
        struct.setMin_compaction_thresholdIsSet(true);
      }
      if (incoming.get(10)) {
        struct.max_compaction_threshold = iprot.readI32();
        struct.setMax_compaction_thresholdIsSet(true);
      }
      if (incoming.get(11)) {
        struct.key_validation_class = iprot.readString();
        struct.setKey_validation_classIsSet(true);
      }
      if (incoming.get(12)) {
        struct.key_alias = iprot.readBinary();
        struct.setKey_aliasIsSet(true);
      }
      if (incoming.get(13)) {
        struct.compaction_strategy = iprot.readString();
        struct.setCompaction_strategyIsSet(true);
      }
      if (incoming.get(14)) {
        {
          org.apache.thrift.protocol.TMap _map135 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.compaction_strategy_options = new HashMap<String,String>(2*_map135.size);
          for (int _i136 = 0; _i136 < _map135.size; ++_i136)
          {
            String _key137;
            String _val138;
            _key137 = iprot.readString();
            _val138 = iprot.readString();
            struct.compaction_strategy_options.put(_key137, _val138);
          }
        }
        struct.setCompaction_strategy_optionsIsSet(true);
      }
      if (incoming.get(15)) {
        {
          org.apache.thrift.protocol.TMap _map139 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.compression_options = new HashMap<String,String>(2*_map139.size);
          for (int _i140 = 0; _i140 < _map139.size; ++_i140)
          {
            String _key141;
            String _val142;
            _key141 = iprot.readString();
            _val142 = iprot.readString();
            struct.compression_options.put(_key141, _val142);
          }
        }
        struct.setCompression_optionsIsSet(true);
      }
      if (incoming.get(16)) {
        struct.bloom_filter_fp_chance = iprot.readDouble();
        struct.setBloom_filter_fp_chanceIsSet(true);
      }
      if (incoming.get(17)) {
        struct.caching = iprot.readString();
        struct.setCachingIsSet(true);
      }
      if (incoming.get(18)) {
        struct.dclocal_read_repair_chance = iprot.readDouble();
        struct.setDclocal_read_repair_chanceIsSet(true);
      }
      if (incoming.get(19)) {
        struct.memtable_flush_period_in_ms = iprot.readI32();
        struct.setMemtable_flush_period_in_msIsSet(true);
      }
      if (incoming.get(20)) {
        struct.default_time_to_live = iprot.readI32();
        struct.setDefault_time_to_liveIsSet(true);
      }
      if (incoming.get(21)) {
        struct.speculative_retry = iprot.readString();
        struct.setSpeculative_retryIsSet(true);
      }
      if (incoming.get(22)) {
        {
          org.apache.thrift.protocol.TList _list143 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.triggers = new ArrayList<TriggerDef>(_list143.size);
          for (int _i144 = 0; _i144 < _list143.size; ++_i144)
          {
            TriggerDef _elem145;
            _elem145 = new TriggerDef();
            _elem145.read(iprot);
            struct.triggers.add(_elem145);
          }
        }
        struct.setTriggersIsSet(true);
      }
      if (incoming.get(23)) {
        struct.cells_per_row_to_cache = iprot.readString();
        struct.setCells_per_row_to_cacheIsSet(true);
      }
      if (incoming.get(24)) {
        struct.min_index_interval = iprot.readI32();
        struct.setMin_index_intervalIsSet(true);
      }
      if (incoming.get(25)) {
        struct.max_index_interval = iprot.readI32();
        struct.setMax_index_intervalIsSet(true);
      }
      if (incoming.get(26)) {
        struct.row_cache_size = iprot.readDouble();
        struct.setRow_cache_sizeIsSet(true);
      }
      if (incoming.get(27)) {
        struct.key_cache_size = iprot.readDouble();
        struct.setKey_cache_sizeIsSet(true);
      }
      if (incoming.get(28)) {
        struct.row_cache_save_period_in_seconds = iprot.readI32();
        struct.setRow_cache_save_period_in_secondsIsSet(true);
      }
      if (incoming.get(29)) {
        struct.key_cache_save_period_in_seconds = iprot.readI32();
        struct.setKey_cache_save_period_in_secondsIsSet(true);
      }
      if (incoming.get(30)) {
        struct.memtable_flush_after_mins = iprot.readI32();
        struct.setMemtable_flush_after_minsIsSet(true);
      }
      if (incoming.get(31)) {
        struct.memtable_throughput_in_mb = iprot.readI32();
        struct.setMemtable_throughput_in_mbIsSet(true);
      }
      if (incoming.get(32)) {
        struct.memtable_operations_in_millions = iprot.readDouble();
        struct.setMemtable_operations_in_millionsIsSet(true);
      }
      if (incoming.get(33)) {
        struct.replicate_on_write = iprot.readBool();
        struct.setReplicate_on_writeIsSet(true);
      }
      if (incoming.get(34)) {
        struct.merge_shards_chance = iprot.readDouble();
        struct.setMerge_shards_chanceIsSet(true);
      }
      if (incoming.get(35)) {
        struct.row_cache_provider = iprot.readString();
        struct.setRow_cache_providerIsSet(true);
      }
      if (incoming.get(36)) {
        struct.row_cache_keys_to_save = iprot.readI32();
        struct.setRow_cache_keys_to_saveIsSet(true);
      }
      if (incoming.get(37)) {
        struct.populate_io_cache_on_flush = iprot.readBool();
        struct.setPopulate_io_cache_on_flushIsSet(true);
      }
      if (incoming.get(38)) {
        struct.index_interval = iprot.readI32();
        struct.setIndex_intervalIsSet(true);
      }
    }
  }

}
