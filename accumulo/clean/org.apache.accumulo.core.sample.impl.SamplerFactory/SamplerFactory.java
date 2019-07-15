import org.apache.accumulo.core.sample.impl.*;


import java.io.IOException;

import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;

public class SamplerFactory {
  public static Sampler newSampler(SamplerConfigurationImpl config, AccumuloConfiguration acuconf)
      throws IOException {
    String context = acuconf.get(Property.TABLE_CLASSPATH);

    Class<? extends Sampler> clazz;
    try {
      if (context != null && !context.equals(""))
        clazz = AccumuloVFSClassLoader.getContextManager().loadClass(context, config.getClassName(),
            Sampler.class);
      else
        clazz = AccumuloVFSClassLoader.loadClass(config.getClassName(), Sampler.class);

      Sampler sampler = clazz.newInstance();

      sampler.init(config.toSamplerConfiguration());

      return sampler;

    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
