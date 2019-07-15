import org.apache.accumulo.core.client.sample.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class encapsultes configuration and options needed to setup and use sampling.
 *
 * @since 1.8.0
 */

public class SamplerConfiguration {

  private String className;
  private Map<String,String> options = new HashMap<>();

  public SamplerConfiguration(Class<? extends Sampler> samplerClass) {
    this(samplerClass.getName());
  }

  public SamplerConfiguration(String samplerClassName) {
    requireNonNull(samplerClassName);
    this.className = samplerClassName;
  }

  public SamplerConfiguration setOptions(Map<String,String> options) {
    requireNonNull(options);
    this.options = new HashMap<>(options.size());

    for (Entry<String,String> entry : options.entrySet()) {
      addOption(entry.getKey(), entry.getValue());
    }

    return this;
  }

  public SamplerConfiguration addOption(String option, String value) {
    checkArgument(option != null, "option is null");
    checkArgument(value != null, "value is null");
    this.options.put(option, value);
    return this;
  }

  public Map<String,String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  public String getSamplerClassName() {
    return className;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SamplerConfiguration) {
      SamplerConfiguration osc = (SamplerConfiguration) o;

      return className.equals(osc.className) && options.equals(osc.options);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return className.hashCode() + 31 * options.hashCode();
  }

  @Override
  public String toString() {
    return className + " " + options;
  }
}
