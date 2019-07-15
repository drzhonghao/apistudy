

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.StdScheduler;
import org.quartz.utils.Key;


public class KarafStdScheduler extends StdScheduler {
	public KarafStdScheduler(final QuartzScheduler scheduler) {
		super(scheduler);
	}

	@Override
	public Date scheduleJob(final JobDetail jobDetail, final Trigger trigger) throws SchedulerException {
		final Date date = super.scheduleJob(jobDetail, trigger);
		return date;
	}

	@Override
	public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
		if (jobKeys != null) {
			final List<String> contextKeys = new ArrayList<>();
			for (JobKey jobKey : jobKeys) {
				contextKeys.add(jobKey.toString());
			}
			for (String contextKey : contextKeys) {
			}
		}
		return super.deleteJobs(jobKeys);
	}

	@Override
	public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
		final Trigger trigger = getTrigger(triggerKey);
		final String contextKey = ((trigger.getJobKey()) != null) ? trigger.getJobKey().toString() : null;
		if (contextKey != null) {
		}
		return super.unscheduleJob(triggerKey);
	}

	@Override
	public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
		if (triggerKeys != null) {
			final List<String> contextKeys = new ArrayList<>();
			for (TriggerKey triggerKey : triggerKeys) {
				final Trigger trigger = getTrigger(triggerKey);
				final String contextKey = trigger.getJobKey().toString();
				if (contextKey != null) {
					contextKeys.add(contextKey);
				}
			}
			for (String contextKey : contextKeys) {
			}
		}
		return super.unscheduleJobs(triggerKeys);
	}
}

