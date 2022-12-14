import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.*;


import java.util.Map.Entry;

import org.apache.accumulo.core.constraints.Constraint;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.Shell.Command;
import org.apache.accumulo.shell.ShellCommandException;
import org.apache.accumulo.shell.ShellCommandException.ErrorCode;
import org.apache.accumulo.shell.ShellOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

public class ConstraintCommand extends Command {
  protected Option namespaceOpt;

  @Override
  public int execute(final String fullCommand, final CommandLine cl, final Shell shellState)
      throws Exception {
    final String tableName;
    final String namespace;

    if (cl.hasOption(namespaceOpt.getOpt())) {
      namespace = cl.getOptionValue(namespaceOpt.getOpt());
    } else {
      namespace = null;
    }

    if (cl.hasOption(OptUtil.tableOpt().getOpt()) || !shellState.getTableName().isEmpty()) {
      tableName = OptUtil.getTableOpt(cl, shellState);
    } else {
      tableName = null;
    }

    int i;
    switch (OptUtil.getAldOpt(cl)) {
      case ADD:
        for (String constraint : cl.getArgs()) {
          if (namespace != null) {
            if (!shellState.getConnector().namespaceOperations().testClassLoad(namespace,
                constraint, Constraint.class.getName())) {
              throw new ShellCommandException(ErrorCode.INITIALIZATION_FAILURE,
                  "Servers are unable to load " + constraint + " as type "
                      + Constraint.class.getName());
            }
            i = shellState.getConnector().namespaceOperations().addConstraint(namespace,
                constraint);
            shellState.getReader().println("Added constraint " + constraint + " to namespace "
                + namespace + " with number " + i);
          } else if (tableName != null && !tableName.isEmpty()) {
            if (!shellState.getConnector().tableOperations().testClassLoad(tableName, constraint,
                Constraint.class.getName())) {
              throw new ShellCommandException(ErrorCode.INITIALIZATION_FAILURE,
                  "Servers are unable to load " + constraint + " as type "
                      + Constraint.class.getName());
            }
            i = shellState.getConnector().tableOperations().addConstraint(tableName, constraint);
            shellState.getReader().println(
                "Added constraint " + constraint + " to table " + tableName + " with number " + i);
          } else {
            throw new IllegalArgumentException("Please specify either a table or a namespace");
          }
        }
        break;
      case DELETE:
        for (String constraint : cl.getArgs()) {
          i = Integer.parseInt(constraint);
          if (namespace != null) {
            shellState.getConnector().namespaceOperations().removeConstraint(namespace, i);
            shellState.getReader()
                .println("Removed constraint " + i + " from namespace " + namespace);
          } else if (tableName != null) {
            shellState.getConnector().tableOperations().removeConstraint(tableName, i);
            shellState.getReader().println("Removed constraint " + i + " from table " + tableName);
          } else {
            throw new IllegalArgumentException("Please specify either a table or a namespace");
          }
        }
        break;
      case LIST:
        if (namespace != null) {
          for (Entry<String,Integer> property : shellState.getConnector().namespaceOperations()
              .listConstraints(namespace).entrySet()) {
            shellState.getReader().println(property.toString());
          }
        } else if (tableName != null) {
          for (Entry<String,Integer> property : shellState.getConnector().tableOperations()
              .listConstraints(tableName).entrySet()) {
            shellState.getReader().println(property.toString());
          }
        } else {
          throw new IllegalArgumentException("Please specify either a table or a namespace");
        }
    }

    return 0;
  }

  @Override
  public String description() {
    return "adds, deletes, or lists constraints for a table";
  }

  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }

  @Override
  public String usage() {
    return getName() + " <constraint>{ <constraint>}";
  }

  @Override
  public Options getOptions() {
    final Options o = new Options();
    o.addOptionGroup(OptUtil.addListDeleteGroup("constraint"));

    OptionGroup grp = new OptionGroup();
    grp.addOption(OptUtil.tableOpt("table to add, delete, or list constraints for"));
    namespaceOpt = new Option(ShellOptions.namespaceOption, "namespace", true,
        "name of a namespace to operate on");
    namespaceOpt.setArgName("namespace");
    grp.addOption(namespaceOpt);

    o.addOptionGroup(grp);
    return o;
  }
}
