import org.apache.poi.sl.usermodel.*;


import java.util.Objects;

import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.util.Internal;


/**
 * Helper class for ColorStyle - not part of the API / implementation may change any time
 */
@Internal
public abstract class AbstractColorStyle implements ColorStyle {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorStyle)) {
            return false;
        }
        return Objects.equals(DrawPaint.applyColorTransform(this), DrawPaint.applyColorTransform((ColorStyle)o));
    }

    @Override
    public int hashCode() {
        return DrawPaint.applyColorTransform(this).hashCode();
    }

}
