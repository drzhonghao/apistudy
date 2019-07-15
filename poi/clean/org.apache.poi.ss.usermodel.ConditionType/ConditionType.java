import org.apache.poi.ss.usermodel.*;


import java.util.HashMap;
import java.util.Map;

/**
 * Represents a type of a conditional formatting rule
 */
public class ConditionType {
    private static Map<Integer,ConditionType> lookup = new HashMap<>();
    
    /**
     * This conditional formatting rule compares a cell value
     * to a formula calculated result, using an operator
     */
    public static final ConditionType CELL_VALUE_IS = 
            new ConditionType(1, "cellIs");

    /**
     * This conditional formatting rule contains a formula to evaluate.
     * When the formula result is true, the cell is highlighted.
     */
    public static final ConditionType FORMULA = 
            new ConditionType(2, "expression");
    
    /**
     * This conditional formatting rule contains a color scale,
     * with the cell background set according to a gradient.
     */
    public static final ConditionType COLOR_SCALE = 
            new ConditionType(3, "colorScale");
    
    /**
     * This conditional formatting rule sets a data bar, with the
     *  cell populated with bars based on their values
     */
    public static final ConditionType DATA_BAR = 
            new ConditionType(4, "dataBar");
    
    /**
     * This conditional formatting rule that files the values
     */
    public static final ConditionType FILTER = 
            new ConditionType(5, null);
    
    /**
     * This conditional formatting rule sets a data bar, with the
     *  cell populated with bars based on their values
     */
    public static final ConditionType ICON_SET = 
            new ConditionType(6, "iconSet");
    
    
    public final byte id;
    public final String type;

    public String toString() {
        return id + " - " + type;
    }
    
    
    public static ConditionType forId(byte id) {
        return forId((int)id);
    }
    public static ConditionType forId(int id) {
        return lookup.get(id);
    }
        
    private ConditionType(int id, String type) {
        this.id = (byte)id; this.type = type;
        lookup.put(id, this);
    }
}
