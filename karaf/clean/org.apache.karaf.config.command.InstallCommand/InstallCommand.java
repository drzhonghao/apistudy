import org.apache.karaf.config.command.*;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Command(scope = "config", name = "install", description = "Install a cfg file in the Karaf etc folder.")
@Service
public class InstallCommand implements Action {

    @Argument(index = 0, name = "url", description = "The URL of the cfg file.", required = true, multiValued = false)
    private String url;

    @Argument(index = 1, name = "finalname", description = "Final name of the cfg file", required = true, multiValued = false)
    private String finalname;

    @Option(name = "-o", aliases = { "--override" }, description = "Override the target cfg file", required = false, multiValued = false)
    private boolean override;

    @Override
    public Object execute() throws Exception {
        File etcFolder = new File(System.getProperty("karaf.etc"));
        File file = new File(etcFolder, finalname);
        if (file.exists()) {
            if (!override) {
                throw new IllegalArgumentException("Configuration file {} already exists " + finalname);
            } else {
                System.out.println("Overriding configuration file " + finalname);
            }
        } else {
            System.out.println("Creating configuration file " + finalname);
        }

        try (InputStream is = new BufferedInputStream(new URL(url).openStream())) {
            if (!file.exists()) {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                file.createNewFile();
            }
            try (FileOutputStream fop = new FileOutputStream(file)) {
                StreamUtils.copy(is, fop);
            }
        } catch (RuntimeException | MalformedURLException e) {
            throw e;
        }
        return null;
    }

}
