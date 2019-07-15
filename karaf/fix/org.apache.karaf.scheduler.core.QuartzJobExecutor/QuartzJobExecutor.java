

import java.io.Serializable;
import java.util.Map;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QuartzJobExecutor implements Job {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuartzJobExecutor.class);

	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDataMap data = context.getJobDetail().getJobDataMap();
		final String contextKey = ((context.getJobDetail().getKey()) != null) ? context.getJobDetail().getKey().toString() : null;
		try {
		} catch (final Throwable t) {
		}
	}

	public static final class JobContextImpl {
		protected final Map<String, Serializable> configuration;

		protected final String name;

		public JobContextImpl(String name, Map<String, Serializable> config) {
			this.name = name;
			this.configuration = config;
		}

		public Map<String, Serializable> getConfiguration() {
			return this.configuration;
		}

		public String getName() {
			return this.name;
		}
	}
}

