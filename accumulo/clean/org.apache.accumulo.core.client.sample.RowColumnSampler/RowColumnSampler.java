import org.apache.accumulo.core.client.sample.AbstractHashSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.*;


import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;

import com.google.common.collect.ImmutableSet;

/**
 * This sampler can hash any subset of a Key's fields. The fields that hashed for the sample are
 * determined by the configuration options passed in {@link #init(SamplerConfiguration)}. The
 * following key values are valid options.
 *
 * <ul>
 * <li>row=true|false
 * <li>family=true|false
 * <li>qualifier=true|false
 * <li>visibility=true|false
 * </ul>
 *
 * <p>
 * If not specified in the options, fields default to false.
 *
 * <p>
 * To determine what options are valid for hashing see {@link AbstractHashSampler}
 *
 * <p>
 * To configure Accumulo to generate sample data on one thousandth of the column qualifiers, the
 * following SamplerConfiguration could be created and used to configure a table.
 *
 * <pre>
 * <code>
 * new SamplerConfiguration(RowColumnSampler.class.getName()).setOptions(
 *   ImmutableMap.of("hasher","murmur3_32","modulus","1009","qualifier","true"));
 * </code>
 * </pre>
 *
 * <p>
 * With this configuration, if a column qualifier is selected then all key values contains that
 * column qualifier will end up in the sample data.
 *
 * @since 1.8.0
 */

public class RowColumnSampler extends AbstractHashSampler {

  private boolean row = true;
  private boolean family = true;
  private boolean qualifier = true;
  private boolean visibility = true;

  private static final Set<String> VALID_OPTIONS = ImmutableSet.of("row", "family", "qualifier",
      "visibility");

  private boolean hashField(SamplerConfiguration config, String field) {
    String optValue = config.getOptions().get(field);
    if (optValue != null) {
      return Boolean.parseBoolean(optValue);
    }

    return false;
  }

  @Override
  protected boolean isValidOption(String option) {
    return super.isValidOption(option) || VALID_OPTIONS.contains(option);
  }

  @Override
  public void init(SamplerConfiguration config) {
    super.init(config);

    row = hashField(config, "row");
    family = hashField(config, "family");
    qualifier = hashField(config, "qualifier");
    visibility = hashField(config, "visibility");

    if (!row && !family && !qualifier && !visibility) {
      throw new IllegalStateException("Must hash at least one key field");
    }
  }

  private void putByteSquence(ByteSequence data, DataOutput hasher) throws IOException {
    hasher.write(data.getBackingArray(), data.offset(), data.length());
  }

  @Override
  protected void hash(DataOutput hasher, Key k) throws IOException {
    if (row) {
      putByteSquence(k.getRowData(), hasher);
    }

    if (family) {
      putByteSquence(k.getColumnFamilyData(), hasher);
    }

    if (qualifier) {
      putByteSquence(k.getColumnQualifierData(), hasher);
    }

    if (visibility) {
      putByteSquence(k.getColumnVisibilityData(), hasher);
    }
  }
}
