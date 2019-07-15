

import java.io.PrintStream;
import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;


@Command(name = "display", scope = "profile", description = "Displays information about the specified profile")
@Service
public class ProfileDisplay implements Action {
	@Option(name = "--overlay", aliases = "-o", description = "Shows the overlay profile settings, taking into account the settings inherited from parent profiles.")
	private Boolean overlay = false;

	@Option(name = "--effective", aliases = "-e", description = "Shows the effective profile settings, taking into account properties substitution.")
	private Boolean effective = false;

	@Option(name = "--display-resources", aliases = "-r", description = "Displays the content of additional profile resources.")
	private Boolean displayResources = false;

	private static void printConfigList(String header, PrintStream out, List<String> list) {
		out.println(header);
		for (String str : list) {
			out.printf("\t%s\n", str);
		}
		out.println();
	}
}

