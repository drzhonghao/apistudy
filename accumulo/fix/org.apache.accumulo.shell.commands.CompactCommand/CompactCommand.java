

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.compaction.CompactionSettings;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.TableOperation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;


public class CompactCommand extends TableOperation {
	private Option noFlushOption;

	private Option waitOpt;

	private Option profileOpt;

	private Option cancelOpt;

	private Option strategyOpt;

	private Option strategyConfigOpt;

	private Option enameOption;

	private Option epathOption;

	private Option sizeLtOption;

	private Option sizeGtOption;

	private Option minFilesOption;

	private Option outBlockSizeOpt;

	private Option outHdfsBlockSizeOpt;

	private Option outIndexBlockSizeOpt;

	private Option outCompressionOpt;

	private Option outReplication;

	private Option enoSampleOption;

	private CompactionConfig compactionConfig = null;

	private boolean cancel = false;

	@Override
	public String description() {
		return "Initiates a major compaction on tablets within the specified range" + (((((" that have one or more files. If no file selection options are" + " specified, then all files will be compacted. Options that configure") + " output settings are only applied to this compaction and not later") + " compactions. If multiple concurrent user initiated compactions specify") + " iterators or a compaction strategy, then all but one will fail to") + " start.");
	}

	@Override
	protected void doTableOp(final Shell shellState, final String tableName) throws AccumuloException, AccumuloSecurityException {
		if (cancel) {
			try {
				shellState.getConnector().tableOperations().cancelCompaction(tableName);
				Shell.log.info(("Compaction canceled for table " + tableName));
			} catch (TableNotFoundException e) {
				throw new AccumuloException(e);
			}
		}else {
			try {
				if (compactionConfig.getWait()) {
					Shell.log.info("Compacting table ...");
				}
				for (IteratorSetting iteratorSetting : compactionConfig.getIterators()) {
				}
				shellState.getConnector().tableOperations().compact(tableName, compactionConfig);
				Shell.log.info((((("Compaction of table " + tableName) + " ") + (compactionConfig.getWait() ? "completed" : "started")) + " for given range"));
			} catch (Exception ex) {
				throw new AccumuloException(ex);
			}
		}
	}

	private void put(CommandLine cl, Map<String, String> opts, Option opt, CompactionSettings setting) {
		if (cl.hasOption(opt.getLongOpt()))
			setting.put(opts, cl.getOptionValue(opt.getLongOpt()));

	}

	private Map<String, String> getConfigurableCompactionStrategyOpts(CommandLine cl) {
		Map<String, String> opts = new HashMap<>();
		put(cl, opts, enoSampleOption, CompactionSettings.SF_NO_SAMPLE);
		put(cl, opts, enameOption, CompactionSettings.SF_NAME_RE_OPT);
		put(cl, opts, epathOption, CompactionSettings.SF_PATH_RE_OPT);
		put(cl, opts, sizeLtOption, CompactionSettings.SF_LT_ESIZE_OPT);
		put(cl, opts, sizeGtOption, CompactionSettings.SF_GT_ESIZE_OPT);
		put(cl, opts, minFilesOption, CompactionSettings.MIN_FILES_OPT);
		put(cl, opts, outCompressionOpt, CompactionSettings.OUTPUT_COMPRESSION_OPT);
		put(cl, opts, outBlockSizeOpt, CompactionSettings.OUTPUT_BLOCK_SIZE_OPT);
		put(cl, opts, outHdfsBlockSizeOpt, CompactionSettings.OUTPUT_HDFS_BLOCK_SIZE_OPT);
		put(cl, opts, outIndexBlockSizeOpt, CompactionSettings.OUTPUT_INDEX_BLOCK_SIZE_OPT);
		put(cl, opts, outReplication, CompactionSettings.OUTPUT_REPLICATION_OPT);
		return opts;
	}

	@Override
	public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws Exception {
		if (cl.hasOption(cancelOpt.getLongOpt())) {
			cancel = true;
			if ((cl.getOptions().length) > 2) {
				throw new IllegalArgumentException("Can not specify other options with cancel");
			}
		}else {
			cancel = false;
		}
		compactionConfig = new CompactionConfig();
		compactionConfig.setFlush((!(cl.hasOption(noFlushOption.getOpt()))));
		compactionConfig.setWait(cl.hasOption(waitOpt.getOpt()));
		compactionConfig.setStartRow(OptUtil.getStartRow(cl));
		compactionConfig.setEndRow(OptUtil.getEndRow(cl));
		if (cl.hasOption(profileOpt.getOpt())) {
			List<IteratorSetting> iterators = shellState.iteratorProfiles.get(cl.getOptionValue(profileOpt.getOpt()));
			if (iterators == null) {
				Shell.log.error((("Profile " + (cl.getOptionValue(profileOpt.getOpt()))) + " does not exist"));
				return -1;
			}
			compactionConfig.setIterators(new ArrayList<>(iterators));
		}
		Map<String, String> configurableCompactOpt = getConfigurableCompactionStrategyOpts(cl);
		if (cl.hasOption(strategyOpt.getOpt())) {
			if ((configurableCompactOpt.size()) > 0)
				throw new IllegalArgumentException("Can not specify compaction strategy with file selection and file output options.");

			CompactionStrategyConfig csc = new CompactionStrategyConfig(cl.getOptionValue(strategyOpt.getOpt()));
			if (cl.hasOption(strategyConfigOpt.getOpt())) {
				Map<String, String> props = new HashMap<>();
				String[] keyVals = cl.getOptionValue(strategyConfigOpt.getOpt()).split(",");
				for (String keyVal : keyVals) {
					String[] sa = keyVal.split("=");
					props.put(sa[0], sa[1]);
				}
				csc.setOptions(props);
			}
			compactionConfig.setCompactionStrategy(csc);
		}
		if ((configurableCompactOpt.size()) > 0) {
			CompactionStrategyConfig csc = new CompactionStrategyConfig("org.apache.accumulo.tserver.compaction.strategies.ConfigurableCompactionStrategy");
			csc.setOptions(configurableCompactOpt);
			compactionConfig.setCompactionStrategy(csc);
		}
		return super.execute(fullCommand, cl, shellState);
	}

	private Option newLAO(String lopt, String desc) {
		return new Option(null, lopt, true, desc);
	}

	@Override
	public Options getOptions() {
		final Options opts = super.getOptions();
		opts.addOption(OptUtil.startRowOpt());
		opts.addOption(OptUtil.endRowOpt());
		noFlushOption = new Option("nf", "noFlush", false, "do not flush table data in memory before compacting.");
		opts.addOption(noFlushOption);
		waitOpt = new Option("w", "wait", false, "wait for compact to finish");
		opts.addOption(waitOpt);
		profileOpt = new Option("pn", "profile", true, "Iterator profile name.");
		profileOpt.setArgName("profile");
		opts.addOption(profileOpt);
		strategyOpt = new Option("s", "strategy", true, "compaction strategy class name");
		opts.addOption(strategyOpt);
		strategyConfigOpt = new Option("sc", "strategyConfig", true, "Key value options for compaction strategy.  Expects <prop>=<value>{,<prop>=<value>}");
		opts.addOption(strategyConfigOpt);
		cancelOpt = new Option(null, "cancel", false, "cancel user initiated compactions");
		opts.addOption(cancelOpt);
		enoSampleOption = new Option(null, "sf-no-sample", false, ("Select files that have no sample data or sample data that differes" + " from the table configuration."));
		opts.addOption(enoSampleOption);
		enameOption = newLAO("sf-ename", ("Select files using regular expression to match file names. Only" + " matches against last part of path."));
		opts.addOption(enameOption);
		epathOption = newLAO("sf-epath", ("Select files using regular expression to match file paths to compact." + " Matches against full path."));
		opts.addOption(epathOption);
		sizeLtOption = newLAO("sf-lt-esize", ("Selects files less than specified size.  Uses the estimated size of" + " file in metadata table. Can use K,M, and G suffixes"));
		opts.addOption(sizeLtOption);
		sizeGtOption = newLAO("sf-gt-esize", ("Selects files greater than specified size. Uses the estimated size of" + " file in metadata table. Can use K,M, and G suffixes"));
		opts.addOption(sizeGtOption);
		minFilesOption = newLAO("min-files", ("Only compacts if at least the specified number of files are selected." + " When no file selection criteria are given, all files are selected."));
		opts.addOption(minFilesOption);
		outBlockSizeOpt = newLAO("out-data-bs", ("Rfile data block size to use for compaction output file. Can use K,M," + " and G suffixes. Uses table settings if not specified."));
		opts.addOption(outBlockSizeOpt);
		outHdfsBlockSizeOpt = newLAO("out-hdfs-bs", ("HDFS block size to use for compaction output file. Can use K,M, and G" + " suffixes. Uses table settings if not specified."));
		opts.addOption(outHdfsBlockSizeOpt);
		outIndexBlockSizeOpt = newLAO("out-index-bs", ("Rfile index block size to use for compaction output file. Can use" + " K,M, and G suffixes. Uses table settings if not specified."));
		opts.addOption(outIndexBlockSizeOpt);
		outCompressionOpt = newLAO("out-compress", ("Compression to use for compaction output file. Either snappy, gz, lzo," + " or none. Uses table settings if not specified."));
		opts.addOption(outCompressionOpt);
		outReplication = newLAO("out-replication", ("HDFS replication to use for compaction output file. Uses table" + " settings if not specified."));
		opts.addOption(outReplication);
		return opts;
	}
}

