import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationObserver;
import org.apache.accumulo.core.conf.*;


import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A configuration that can be observed. Handling of observers is thread-safe.
 */
public abstract class ObservableConfiguration extends AccumuloConfiguration {

  private Set<ConfigurationObserver> observers;

  /**
   * Creates a new observable configuration.
   */
  public ObservableConfiguration() {
    observers = Collections.synchronizedSet(new java.util.HashSet<ConfigurationObserver>());
  }

  /**
   * Adds an observer.
   *
   * @param co
   *          observer
   * @throws NullPointerException
   *           if co is null
   */
  public void addObserver(ConfigurationObserver co) {
    requireNonNull(co);
    observers.add(co);
  }

  /**
   * Removes an observer.
   *
   * @param co
   *          observer
   */
  public void removeObserver(ConfigurationObserver co) {
    observers.remove(co);
  }

  /**
   * Gets the current set of observers. The returned collection is a snapshot, and changes to it do
   * not reflect back to the configuration.
   *
   * @return observers
   */
  public Collection<ConfigurationObserver> getObservers() {
    return snapshot(observers);
  }

  private static Collection<ConfigurationObserver> snapshot(
      Collection<ConfigurationObserver> observers) {
    Collection<ConfigurationObserver> c = new java.util.ArrayList<>();
    synchronized (observers) {
      c.addAll(observers);
    }
    return c;
  }

  /**
   * Expires all observers.
   */
  public void expireAllObservers() {
    Collection<ConfigurationObserver> copy = snapshot(observers);
    for (ConfigurationObserver co : copy)
      co.sessionExpired();
  }

  /**
   * Notifies all observers that a property changed.
   *
   * @param key
   *          configuration property key
   */
  public void propertyChanged(String key) {
    Collection<ConfigurationObserver> copy = snapshot(observers);
    for (ConfigurationObserver co : copy)
      co.propertyChanged(key);
  }

  /**
   * Notifies all observers that properties changed.
   */
  public void propertiesChanged() {
    Collection<ConfigurationObserver> copy = snapshot(observers);
    for (ConfigurationObserver co : copy)
      co.propertiesChanged();
  }
}
