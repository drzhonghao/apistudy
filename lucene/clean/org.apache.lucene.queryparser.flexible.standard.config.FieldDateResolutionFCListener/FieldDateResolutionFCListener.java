import org.apache.lucene.queryparser.flexible.standard.config.*;


import java.util.Map;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.core.config.FieldConfig;
import org.apache.lucene.queryparser.flexible.core.config.FieldConfigListener;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;

/**
 * This listener listens for every field configuration request and assign a
 * {@link ConfigurationKeys#DATE_RESOLUTION} to the equivalent {@link FieldConfig} based
 * on a defined map: fieldName -&gt; {@link Resolution} stored in
 * {@link ConfigurationKeys#FIELD_DATE_RESOLUTION_MAP}.
 * 
 * @see ConfigurationKeys#DATE_RESOLUTION
 * @see ConfigurationKeys#FIELD_DATE_RESOLUTION_MAP
 * @see FieldConfig
 * @see FieldConfigListener
 */
public class FieldDateResolutionFCListener implements FieldConfigListener {

  private QueryConfigHandler config = null;

  public FieldDateResolutionFCListener(QueryConfigHandler config) {
    this.config = config;
  }

  @Override
  public void buildFieldConfig(FieldConfig fieldConfig) {
    DateTools.Resolution dateRes = null;
    Map<CharSequence, DateTools.Resolution> dateResMap = this.config.get(ConfigurationKeys.FIELD_DATE_RESOLUTION_MAP);

    if (dateResMap != null) {
      dateRes = dateResMap.get(
          fieldConfig.getField());
    }

    if (dateRes == null) {
      dateRes = this.config.get(ConfigurationKeys.DATE_RESOLUTION);
    }

    if (dateRes != null) {
      fieldConfig.set(ConfigurationKeys.DATE_RESOLUTION, dateRes);
    }

  }

}
