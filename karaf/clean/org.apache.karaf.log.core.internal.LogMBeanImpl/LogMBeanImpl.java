import org.apache.karaf.log.core.internal.*;


import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.log.core.LogMBean;
import org.apache.karaf.log.core.LogService;

import java.util.Map;

/**
 * Implementation of the LogMBean.
 */
public class LogMBeanImpl extends StandardMBean implements LogMBean {

    private final LogService logService;

    public LogMBeanImpl(LogService logService) throws NotCompliantMBeanException {
        super(LogMBean.class);
        this.logService = logService;
    }

    @Override
    public String getLevel() {
        return logService.getLevel();
    }

    @Override
    public Map<String, String> getLevel(String logger) {
        return logService.getLevel(logger);
    }

    @Override
    public void setLevel(String level) {
        this.logService.setLevel(level);
    }

    @Override
    public void setLevel(String logger, String level) {
        this.logService.setLevel(logger, level);
    }

}
