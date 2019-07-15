import org.apache.karaf.log.core.internal.*;


import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.log.core.LogEventFormatter;
import org.apache.karaf.log.core.internal.layout.PatternConverter;
import org.apache.karaf.log.core.internal.layout.PatternParser;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

public class LogEventFormatterImpl implements LogEventFormatter {

    private String pattern;
    private Map<Integer, String> level2Color = new HashMap<>();

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setColor(int level, String color) {
        if (color != null && color.length() > 0) {
            this.level2Color.put(level, color);
        } else {
            this.level2Color.remove(level);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.karaf.log.core.internal.LogEventFormatter#format(org.ops4j.pax.logging.spi.PaxLoggingEvent, java.lang.String, boolean)
     */
    @Override
    public String format(PaxLoggingEvent event, String overridenPattern, boolean noColor) {
        final PatternConverter cnv = new PatternParser(overridenPattern != null ? overridenPattern : pattern).parse();
        String color = getColor(event, noColor);
        StringBuffer sb = new StringBuffer();
        color(sb, color);
        for (PatternConverter pc = cnv; pc != null; pc = pc.next) {
            pc.format(sb, event);
        }
        if (event.getThrowableStrRep() != null) {
            for (String r : event.getThrowableStrRep()) {
                sb.append(r).append('\n');
            }
        }
        color(sb, "0");
        return sb.toString();
    }

    private void color(StringBuffer sb, String color) {
        if (color != null) {
            sb.append((char)27);
            sb.append('[');
            sb.append(color);
            sb.append('m');
        }
    }

    private String getColor(PaxLoggingEvent event, boolean noColor) {
        if (!noColor && event != null && event.getLevel() != null && event.getLevel().toString() != null) {
            return level2Color.get(event.getLevel().toInt());
        } else {
            return null;
        }
    }

}
