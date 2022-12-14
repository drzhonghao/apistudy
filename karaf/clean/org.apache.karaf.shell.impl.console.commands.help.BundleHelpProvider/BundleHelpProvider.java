import org.apache.karaf.shell.impl.console.commands.help.*;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.AnsiPrintingWikiVisitor;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiParser;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiVisitor;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class BundleHelpProvider implements HelpProvider {

    @Override
    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("bundle|")) {
                path = path.substring("bundle|".length());
            } else {
                return null;
            }
        }

        if (path.matches("[0-9]*")) {
            long id = Long.parseLong(path);
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            Bundle bundle = bundleContext.getBundle(id);
            if (bundle != null) {
                String title = ShellUtil.getBundleName(bundle);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                ps.println("\n" + title);
                ps.println(ShellUtil.getUnderlineString(title));
                URL bundleInfo = bundle.getEntry("OSGI-INF/bundle.info");
                if (bundleInfo != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(bundleInfo.openStream()))) {
                        int maxSize = 80;
                        Terminal terminal = session.getTerminal();
                        if (terminal != null) {
                            maxSize = terminal.getWidth();
                        }
                        WikiVisitor visitor = new AnsiPrintingWikiVisitor(ps, maxSize);
                        WikiParser parser = new WikiParser(visitor);
                        parser.parse(reader);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                ps.close();
                return baos.toString();
            }
        }
        return null;
    }

}
