

import java.util.Properties;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QuartzScheduler implements Scheduler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String PREFIX = "Apache Karaf Quartz Scheduler ";

	static final String DATA_MAP_OBJECT = "QuartzJobScheduler.Object";

	static final String DATA_MAP_NAME = "QuartzJobScheduler.JobName";

	static final String DATA_MAP_OPTIONS = "QuartzJobScheduler.Options";

	static final String DATA_MAP_CONTEXT = "QuarteJobScheduler.Context";

	static final String DATA_MAP_LOGGER = "QuartzJobScheduler.Logger";

	private volatile Scheduler scheduler;

	public QuartzScheduler(Properties configuration) {
		System.setProperty("org.terracotta.quartz.skipUpdateCheck", Boolean.TRUE.toString());
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(QuartzScheduler.class.getClassLoader());
			scheduler.start();
		} catch (Throwable t) {
			throw new RuntimeException("Unable to create quartz scheduler", t);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	public void deactivate() {
		final Scheduler s = this.scheduler;
		this.scheduler = null;
		this.dispose(s);
	}

	private void dispose(final Scheduler s) {
		if (s != null) {
			try {
				s.shutdown();
			} catch (SchedulerException e) {
				this.logger.debug("Exception during shutdown of scheduler.", e);
			}
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(((QuartzScheduler.PREFIX) + "stopped."));
			}
		}
	}

	private void checkJob(final Object job) throws IllegalArgumentException {
		if ((!(job instanceof Runnable)) && (!(job instanceof Job))) {
			throw new IllegalArgumentException(((("Job object is neither an instance of " + (Runnable.class.getName())) + " nor ") + (Job.class.getName())));
		}
	}

	Scheduler getScheduler() {
		return this.scheduler;
	}

	public boolean unschedule(final String jobName) {
		final Scheduler s = this.scheduler;
		if ((jobName != null) && (s != null)) {
			try {
				final JobKey key = JobKey.jobKey(jobName);
				final JobDetail jobdetail = s.getJobDetail(key);
				if (jobdetail != null) {
					s.deleteJob(key);
					this.logger.debug("Unscheduling job with name {}", jobName);
					return true;
				}
			} catch (final SchedulerException ignored) {
			}
		}
		return false;
	}
}

