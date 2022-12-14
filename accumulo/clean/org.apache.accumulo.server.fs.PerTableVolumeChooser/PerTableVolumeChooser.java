import org.apache.accumulo.server.fs.RandomVolumeChooser;
import org.apache.accumulo.server.fs.VolumeChooserEnvironment;
import org.apache.accumulo.server.fs.*;


import java.util.concurrent.ConcurrentHashMap;

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.conf.TableConfiguration;

/**
 * A {@link VolumeChooser} that delegates to another volume chooser based on the presence of an
 * experimental table property, {@link Property#TABLE_VOLUME_CHOOSER}. If it isn't found, defaults
 * back to {@link RandomVolumeChooser}.
 */
public class PerTableVolumeChooser implements VolumeChooser {

  private final VolumeChooser fallbackVolumeChooser = new RandomVolumeChooser();
  // TODO Add hint of expected size to construction, see ACCUMULO-3410
  /* Track VolumeChooser instances so they can keep state. */
  private final ConcurrentHashMap<String,VolumeChooser> tableSpecificChooser = new ConcurrentHashMap<>();
  // TODO has to be lazily initialized currently because of the reliance on HdfsZooInstance. see
  // ACCUMULO-3411
  private volatile ServerConfigurationFactory serverConfs;

  @Override
  public String choose(VolumeChooserEnvironment env, String[] options) {
    VolumeChooser chooser = null;
    if (env.hasTableId()) {
      // This local variable is an intentional component of the single-check idiom.
      ServerConfigurationFactory localConf = serverConfs;
      if (localConf == null) {
        // If we're under contention when first getting here we'll throw away some initializations.
        localConf = new ServerConfigurationFactory(HdfsZooInstance.getInstance());
        serverConfs = localConf;
      }
      final TableConfiguration tableConf = localConf.getTableConfiguration(env.getTableId());
      chooser = tableSpecificChooser.get(env.getTableId());
      if (chooser == null) {
        VolumeChooser temp = Property.createTableInstanceFromPropertyName(tableConf,
            Property.TABLE_VOLUME_CHOOSER, VolumeChooser.class, fallbackVolumeChooser);
        chooser = tableSpecificChooser.putIfAbsent(env.getTableId(), temp);
        if (chooser == null) {
          chooser = temp;
          // Otherwise, someone else beat us to initializing; use theirs.
        }
      } else if (!(chooser.getClass().getName()
          .equals(tableConf.get(Property.TABLE_VOLUME_CHOOSER)))) {
        // the configuration for this table's chooser has been updated. In the case of failure to
        // instantiate we'll repeat here next call.
        // TODO stricter definition of when the updated property is used, ref ACCUMULO-3412
        VolumeChooser temp = Property.createTableInstanceFromPropertyName(tableConf,
            Property.TABLE_VOLUME_CHOOSER, VolumeChooser.class, fallbackVolumeChooser);
        VolumeChooser last = tableSpecificChooser.replace(env.getTableId(), temp);
        if (chooser.equals(last)) {
          chooser = temp;
        } else {
          // Someone else beat us to updating; use theirs.
          chooser = last;
        }
      }
    } else {
      chooser = fallbackVolumeChooser;
    }

    return chooser.choose(env, options);
  }
}
