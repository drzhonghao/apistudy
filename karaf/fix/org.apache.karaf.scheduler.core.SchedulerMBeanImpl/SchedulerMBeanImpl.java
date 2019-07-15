

import javax.management.MBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;


public class SchedulerMBeanImpl extends StandardMBean {
	public TabularData getJobs() throws MBeanException {
		try {
			CompositeType jobType = new CompositeType("Job", "Scheduler job", new String[]{ "Job", "Schedule" }, new String[]{ "Job Name", "Job Scheduling" }, new OpenType[]{ SimpleType.STRING, SimpleType.STRING });
			TabularType tableType = new TabularType("Jobs", "Tables of all jobs", jobType, new String[]{ "Job" });
			TabularData table = new TabularDataSupport(tableType);
			return table;
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void trigger(String name, boolean background) throws MBeanException {
		try {
			if (background) {
			}else {
			}
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}

	public void unschedule(String name) throws MBeanException {
		try {
		} catch (Exception e) {
			throw new MBeanException(null, e.toString());
		}
	}
}

