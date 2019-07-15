import org.apache.karaf.kar.command.*;


import org.apache.karaf.kar.KarService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "kar", name = "list", description = "List the installed KAR files.")
@Service
public class ListKarCommand implements Action {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Reference
    private KarService karService;

    @Override
    public Object execute() throws Exception {

        ShellTable table = new ShellTable();
        table.column("KAR Name");

        for (String karName : karService.list()) {
            table.addRow().addContent(karName);
        }

        table.print(System.out, !noFormat);

        return null;
    }
    
}
