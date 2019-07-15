import org.apache.accumulo.master.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCoordinator {

  private static final Logger log = LoggerFactory.getLogger(EventCoordinator.class);
  long eventCounter = 0;

  synchronized long waitForEvents(long millis, long lastEvent) {
    // Did something happen since the last time we waited?
    if (lastEvent == eventCounter) {
      // no
      if (millis <= 0)
        return eventCounter;
      try {
        wait(millis);
      } catch (InterruptedException e) {
        log.debug("ignoring InterruptedException", e);
      }
    }
    return eventCounter;
  }

  synchronized public void event(String msg, Object... args) {
    log.info(String.format(msg, args));
    eventCounter++;
    notifyAll();
  }

  public Listener getListener() {
    return new Listener();
  }

  public class Listener {
    long lastEvent;

    Listener() {
      lastEvent = eventCounter;
    }

    public void waitForEvents(long millis) {
      lastEvent = EventCoordinator.this.waitForEvents(millis, lastEvent);
    }
  }

}
