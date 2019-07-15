import org.apache.lucene.queryparser.flexible.standard.config.*;


import java.text.NumberFormat;

import org.apache.lucene.index.PointValues;

/**
 * This class holds the configuration used to parse numeric queries and create
 * {@link PointValues} queries.
 * 
 * @see PointValues
 * @see NumberFormat
 */
public class PointsConfig {
    
  private NumberFormat format;
  
  private Class<? extends Number> type;
  
  /**
   * Constructs a {@link PointsConfig} object.
   * 
   * @param format
   *          the {@link NumberFormat} used to parse a {@link String} to
   *          {@link Number}
   * @param type
   *          the numeric type used to index the numeric values
   * 
   * @see PointsConfig#setNumberFormat(NumberFormat)
   */
  public PointsConfig(NumberFormat format, Class<? extends Number> type) {
    setNumberFormat(format);
    setType(type);    
  }
  
  /**
   * Returns the {@link NumberFormat} used to parse a {@link String} to
   * {@link Number}
   * 
   * @return the {@link NumberFormat} used to parse a {@link String} to
   *         {@link Number}
   */
  public NumberFormat getNumberFormat() {
    return format;
  }
  
  /**
   * Returns the numeric type used to index the numeric values
   * 
   * @return the numeric type used to index the numeric values
   */
  public Class<? extends Number> getType() {
    return type;
  }
  
  /**
   * Sets the numeric type used to index the numeric values
   * 
   * @param type the numeric type used to index the numeric values
   */
  public void setType(Class<? extends Number> type) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null!");
    }
    if (Integer.class.equals(type) == false &&
        Long.class.equals(type) == false &&
        Float.class.equals(type) == false &&
        Double.class.equals(type) == false) {
      throw new IllegalArgumentException("unsupported numeric type: " + type);
    }
    this.type = type;
  }
  
  /**
   * Sets the {@link NumberFormat} used to parse a {@link String} to
   * {@link Number}
   * 
   * @param format
   *          the {@link NumberFormat} used to parse a {@link String} to
   *          {@link Number}, must not be <code>null</code>
   */
  public void setNumberFormat(NumberFormat format) {    
    if (format == null) {
      throw new IllegalArgumentException("format must not be null!");
    } 
    this.format = format;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + format.hashCode();
    result = prime * result + type.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PointsConfig other = (PointsConfig) obj;
    if (!format.equals(other.format)) return false;
    if (!type.equals(other.type)) return false;
    return true;
  }
}
