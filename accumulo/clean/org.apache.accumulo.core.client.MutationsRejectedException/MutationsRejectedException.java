import org.apache.accumulo.core.client.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.security.SecurityErrorCode;
import org.apache.accumulo.core.data.ConstraintViolationSummary;
import org.apache.accumulo.core.data.TabletId;
import org.apache.accumulo.core.data.impl.TabletIdImpl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Communicate the failed mutations of a BatchWriter back to the client.
 *
 */
public class MutationsRejectedException extends AccumuloException {
  private static final long serialVersionUID = 1L;

  private List<ConstraintViolationSummary> cvsl;
  private Map<TabletId,Set<SecurityErrorCode>> af;
  private Collection<String> es;
  private int unknownErrors;

  private static <K,V,L> Map<L,V> transformKeys(Map<K,V> map, Function<K,L> keyFunction) {
    HashMap<L,V> ret = new HashMap<>();
    for (Entry<K,V> entry : map.entrySet()) {
      ret.put(keyFunction.apply(entry.getKey()), entry.getValue());
    }

    return ret;
  }

  /**
   * @param cvsList
   *          list of constraint violations
   * @param hashMap
   *          authorization failures
   * @param serverSideErrors
   *          server side errors
   * @param unknownErrors
   *          number of unknown errors
   *
   * @deprecated since 1.6.0, see
   *             {@link #MutationsRejectedException(Instance, List, Map, Collection, int, Throwable)}
   */
  @Deprecated
  public MutationsRejectedException(List<ConstraintViolationSummary> cvsList,
      HashMap<org.apache.accumulo.core.data.KeyExtent,Set<SecurityErrorCode>> hashMap,
      Collection<String> serverSideErrors, int unknownErrors, Throwable cause) {
    super(
        "# constraint violations : " + cvsList.size() + "  security codes: " + hashMap.values()
            + "  # server errors " + serverSideErrors.size() + " # exceptions " + unknownErrors,
        cause);
    this.cvsl = cvsList;
    this.af = transformKeys(hashMap, TabletIdImpl.KE_2_TID_OLD);
    this.es = serverSideErrors;
    this.unknownErrors = unknownErrors;
  }

  /**
   * @param cvsList
   *          list of constraint violations
   * @param hashMap
   *          authorization failures
   * @param serverSideErrors
   *          server side errors
   * @param unknownErrors
   *          number of unknown errors
   *
   * @deprecated since 1.7.0 see
   *             {@link #MutationsRejectedException(Instance, List, Map, Collection, int, Throwable)}
   */
  @Deprecated
  public MutationsRejectedException(Instance instance, List<ConstraintViolationSummary> cvsList,
      HashMap<org.apache.accumulo.core.data.KeyExtent,Set<SecurityErrorCode>> hashMap,
      Collection<String> serverSideErrors, int unknownErrors, Throwable cause) {
    super(
        "# constraint violations : " + cvsList.size() + "  security codes: "
            + format(transformKeys(hashMap, TabletIdImpl.KE_2_TID_OLD), instance)
            + "  # server errors " + serverSideErrors.size() + " # exceptions " + unknownErrors,
        cause);
    this.cvsl = cvsList;
    this.af = transformKeys(hashMap, TabletIdImpl.KE_2_TID_OLD);
    this.es = serverSideErrors;
    this.unknownErrors = unknownErrors;
  }

  /**
   *
   * @param cvsList
   *          list of constraint violations
   * @param hashMap
   *          authorization failures
   * @param serverSideErrors
   *          server side errors
   * @param unknownErrors
   *          number of unknown errors
   *
   * @since 1.7.0
   */
  public MutationsRejectedException(Instance instance, List<ConstraintViolationSummary> cvsList,
      Map<TabletId,Set<SecurityErrorCode>> hashMap, Collection<String> serverSideErrors,
      int unknownErrors, Throwable cause) {
    super("# constraint violations : " + cvsList.size() + "  security codes: "
        + format(hashMap, instance) + "  # server errors " + serverSideErrors.size()
        + " # exceptions " + unknownErrors, cause);
    this.cvsl = cvsList;
    this.af = hashMap;
    this.es = serverSideErrors;
    this.unknownErrors = unknownErrors;
  }

  private static String format(Map<TabletId,Set<SecurityErrorCode>> hashMap, Instance instance) {
    Map<String,Set<SecurityErrorCode>> result = new HashMap<>();

    for (Entry<TabletId,Set<SecurityErrorCode>> entry : hashMap.entrySet()) {
      String tableInfo = Tables.getPrintableTableInfoFromId(instance,
          entry.getKey().getTableId().toString());

      if (!result.containsKey(tableInfo)) {
        result.put(tableInfo, new HashSet<SecurityErrorCode>());
      }

      result.get(tableInfo).addAll(hashMap.get(entry.getKey()));
    }

    return result.toString();
  }

  /**
   * @return the internal list of constraint violations
   */
  public List<ConstraintViolationSummary> getConstraintViolationSummaries() {
    return cvsl;
  }

  /**
   * @return the internal list of authorization failures
   * @deprecated since 1.5, see {@link #getAuthorizationFailuresMap()}
   */
  @Deprecated
  public List<org.apache.accumulo.core.data.KeyExtent> getAuthorizationFailures() {
    return new ArrayList<>(Collections2.transform(af.keySet(), TabletIdImpl.TID_2_KE_OLD));
  }

  /**
   * @return the internal mapping of keyextent mappings to SecurityErrorCode
   * @since 1.5.0
   * @deprecated since 1.7.0 see {@link #getSecurityErrorCodes()}
   */
  @Deprecated
  // @formatter:off
  public Map<org.apache.accumulo.core.data.KeyExtent,Set<SecurityErrorCode>>
    getAuthorizationFailuresMap() {
    return transformKeys(af, TabletIdImpl.TID_2_KE_OLD);
  }
  // @formatter:on

  /**
   * @return the internal mapping of TabletID to SecurityErrorCodes
   */
  public Map<TabletId,Set<SecurityErrorCode>> getSecurityErrorCodes() {
    return af;
  }

  /**
   *
   * @return A list of servers that had internal errors when mutations were written
   *
   */
  public Collection<String> getErrorServers() {
    return es;
  }

  /**
   *
   * @return a count of unknown exceptions that occurred during processing
   */
  public int getUnknownExceptions() {
    return unknownErrors;
  }
}
