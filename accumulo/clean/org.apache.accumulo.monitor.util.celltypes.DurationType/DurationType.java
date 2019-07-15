import org.apache.accumulo.monitor.util.celltypes.*;


import org.apache.accumulo.core.util.Duration;

public class DurationType extends NumberType<Long> {
  private static final long serialVersionUID = 1L;
  private Long errMin;
  private Long errMax;

  public DurationType() {
    this(null, null);
  }

  public DurationType(Long errMin, Long errMax) {
    this.errMin = errMin;
    this.errMax = errMax;
  }

  @Override
  public String format(Object obj) {
    if (obj == null)
      return "-";
    Long millis = (Long) obj;
    if (errMin != null && errMax != null) {
      String numbers = Duration.format(millis);
      if (millis < errMin || millis > errMax)
        return "<span class='error'>" + numbers + "</span>";
      return numbers;
    }
    return Duration.format(millis);
  }

}
