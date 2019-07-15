

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.benchmark.byTask.stats.TaskStats;
import org.apache.lucene.benchmark.byTask.tasks.PerfTask;
import org.apache.lucene.benchmark.byTask.utils.Config;


public class Points {
	private ArrayList<TaskStats> points = new ArrayList<>();

	private int nextTaskRunNum = 0;

	private TaskStats currentStats;

	public Points(Config config) {
	}

	public List<TaskStats> taskStats() {
		return points;
	}

	public synchronized TaskStats markTaskStart(PerfTask task, int round) {
		return null;
	}

	public TaskStats getCurrentStats() {
		return currentStats;
	}

	private synchronized int nextTaskRunNum() {
		return (nextTaskRunNum)++;
	}

	public synchronized void markTaskEnd(TaskStats stats, int count) {
		int numParallelTasks = ((nextTaskRunNum) - 1) - (stats.getTaskRunNum());
	}

	public void clearData() {
		points.clear();
	}
}

