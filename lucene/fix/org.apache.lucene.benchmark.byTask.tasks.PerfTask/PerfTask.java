

import java.io.PrintStream;
import java.util.Locale;
import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.stats.Points;
import org.apache.lucene.benchmark.byTask.utils.Config;


public abstract class PerfTask implements Cloneable {
	static final int DEFAULT_LOG_STEP = 1000;

	private PerfRunData runData;

	private String name;

	private int depth = 0;

	protected int logStep;

	private int logStepCount = 0;

	private int maxDepthLogStart = 0;

	private boolean disableCounting = false;

	protected String params = null;

	private boolean runInBackground;

	private int deltaPri;

	private int algLineNum = 0;

	protected static final String NEW_LINE = System.getProperty("line.separator");

	private PerfTask() {
		name = getClass().getSimpleName();
		if (name.endsWith("Task")) {
			name = name.substring(0, ((name.length()) - 4));
		}
	}

	public void setRunInBackground(int deltaPri) {
		runInBackground = true;
		this.deltaPri = deltaPri;
	}

	public boolean getRunInBackground() {
		return runInBackground;
	}

	public int getBackgroundDeltaPriority() {
		return deltaPri;
	}

	protected volatile boolean stopNow;

	public void stopNow() {
		stopNow = true;
	}

	public PerfTask(PerfRunData runData) {
		this();
		this.runData = runData;
		Config config = runData.getConfig();
		this.maxDepthLogStart = config.get("task.max.depth.log", 0);
		String logStepAtt = "log.step";
		String taskLogStepAtt = "log.step." + (name);
		if ((config.get(taskLogStepAtt, null)) != null) {
			logStepAtt = taskLogStepAtt;
		}
		logStep = config.get(logStepAtt, PerfTask.DEFAULT_LOG_STEP);
		if ((logStep) <= 0) {
			logStep = Integer.MAX_VALUE;
		}
	}

	@Override
	protected PerfTask clone() throws CloneNotSupportedException {
		return ((PerfTask) (super.clone()));
	}

	public void close() throws Exception {
	}

	public final int runAndMaybeStats(boolean reportStats) throws Exception {
		if ((!reportStats) || (shouldNotRecordStats())) {
			setup();
			int count = doLogic();
			count = (disableCounting) ? 0 : count;
			tearDown();
			return count;
		}
		if ((reportStats && ((depth) <= (maxDepthLogStart))) && (!(shouldNeverLogAtStart()))) {
			System.out.println(("------------> starting task: " + (getName())));
		}
		setup();
		Points pnts = runData.getPoints();
		int count = doLogic();
		count = (disableCounting) ? 0 : count;
		tearDown();
		return count;
	}

	public abstract int doLogic() throws Exception;

	public String getName() {
		if ((params) == null) {
			return name;
		}
		return new StringBuilder(name).append('(').append(params).append(')').toString();
	}

	protected void setName(String name) {
		this.name = name;
	}

	public PerfRunData getRunData() {
		return runData;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	String getPadding() {
		char[] c = new char[4 * (getDepth())];
		for (int i = 0; i < (c.length); i++)
			c[i] = ' ';

		return new String(c);
	}

	@Override
	public String toString() {
		String padd = getPadding();
		StringBuilder sb = new StringBuilder(padd);
		if (disableCounting) {
			sb.append('-');
		}
		sb.append(getName());
		if (getRunInBackground()) {
			sb.append(" &");
			int x = getBackgroundDeltaPriority();
			if (x != 0) {
				sb.append(x);
			}
		}
		return sb.toString();
	}

	int getMaxDepthLogStart() {
		return maxDepthLogStart;
	}

	protected String getLogMessage(int recsCount) {
		return ("processed " + recsCount) + " records";
	}

	protected boolean shouldNeverLogAtStart() {
		return false;
	}

	protected boolean shouldNotRecordStats() {
		return false;
	}

	public void setup() throws Exception {
	}

	public void tearDown() throws Exception {
		if (((++(logStepCount)) % (logStep)) == 0) {
			double time = ((System.currentTimeMillis()) - (runData.getStartTimeMillis())) / 1000.0;
			System.out.println((((((String.format(Locale.ROOT, "%7.2f", time)) + " sec --> ") + (Thread.currentThread().getName())) + " ") + (getLogMessage(logStepCount))));
		}
	}

	public boolean supportsParams() {
		return false;
	}

	public void setParams(String params) {
		if (!(supportsParams())) {
			throw new UnsupportedOperationException(((getName()) + " does not support command line parameters."));
		}
		this.params = params;
	}

	public String getParams() {
		return params;
	}

	public boolean isDisableCounting() {
		return disableCounting;
	}

	public void setDisableCounting(boolean disableCounting) {
		this.disableCounting = disableCounting;
	}

	public void setAlgLineNum(int algLineNum) {
		this.algLineNum = algLineNum;
	}

	public int getAlgLineNum() {
		return algLineNum;
	}
}

