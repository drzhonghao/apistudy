import org.apache.karaf.diagnostic.common.*;


import java.io.OutputStreamWriter;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

/**
 * Dump provider which add file named features.txt with informations
 * about installed features and repositories.
 */
public class FeaturesDumpProvider extends TextDumpProvider {

    /**
     * Feature service.
     */
    private final FeaturesService features;

    /**
     * Creates new dump entry witch contains information about
     * karaf features.
     * 
     * @param features Feature service.
     */
    public FeaturesDumpProvider(FeaturesService features) {
        super("features.txt");
        this.features = features;
    }

    /**
     * {@inheritDoc}
     */
    protected void writeDump(OutputStreamWriter outputStreamWriter) throws Exception {
        // creates header
        outputStreamWriter.write("Repositories:\n");

        // list repositories
        for (Repository repo : features.listRepositories()) {
            outputStreamWriter.write(repo.getURI() + " (" + repo.getName() + ")\n");
        }

        // list features
        outputStreamWriter.write("\nfeatures:\n");
        for (Feature feature : features.listFeatures()) {
            outputStreamWriter.write(feature.getName() + " " + feature.getVersion());
            outputStreamWriter.write(" installed: " + features.isInstalled(feature));
            outputStreamWriter.write("\nBundles:\n");
            for (BundleInfo bundle : feature.getBundles()) {
                outputStreamWriter.write("\t" + bundle.getLocation());
                if (bundle.getStartLevel() != 0) {
                    outputStreamWriter.write(" start level " + bundle.getStartLevel());
                }
                outputStreamWriter.write("\n\n");
            }
        }

        // flush & close stream
        outputStreamWriter.close();
    }

}
