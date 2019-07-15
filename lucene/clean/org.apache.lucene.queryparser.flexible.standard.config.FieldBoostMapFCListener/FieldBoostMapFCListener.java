import org.apache.lucene.queryparser.flexible.standard.config.*;


import java.util.Map;

import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.core.config.FieldConfig;
import org.apache.lucene.queryparser.flexible.core.config.FieldConfigListener;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;

/**
 * This listener listens for every field configuration request and assign a
 * {@link ConfigurationKeys#BOOST} to the
 * equivalent {@link FieldConfig} based on a defined map: fieldName -&gt; boostValue stored in
 * {@link ConfigurationKeys#FIELD_BOOST_MAP}.
 * 
 * @see ConfigurationKeys#FIELD_BOOST_MAP
 * @see ConfigurationKeys#BOOST
 * @see FieldConfig
 * @see FieldConfigListener
 */
public class FieldBoostMapFCListener implements FieldConfigListener {

  private QueryConfigHandler config = null;
  
  public FieldBoostMapFCListener(QueryConfigHandler config) {
    this.config = config;
  }

  @Override
  public void buildFieldConfig(FieldConfig fieldConfig) {
    Map<String, Float> fieldBoostMap = this.config.get(ConfigurationKeys.FIELD_BOOST_MAP);
    
    if (fieldBoostMap != null) {
      Float boost = fieldBoostMap.get(fieldConfig.getField());

      if (boost != null) {
        fieldConfig.set(ConfigurationKeys.BOOST, boost);
      }

    }
  }

}
