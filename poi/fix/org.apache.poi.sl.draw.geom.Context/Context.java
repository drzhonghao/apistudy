

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.poi.sl.draw.geom.Formula;
import org.apache.poi.sl.draw.geom.Guide;
import org.apache.poi.sl.draw.geom.IAdjustableShape;


public class Context {
	final Map<String, Double> _ctx = new HashMap<>();

	final IAdjustableShape _props;

	final Rectangle2D _anchor;

	public Context(CustomGeometry geom, Rectangle2D anchor, IAdjustableShape props) {
		_props = props;
		_anchor = anchor;
	}

	public Rectangle2D getShapeAnchor() {
		return _anchor;
	}

	public Guide getAdjustValue(String name) {
		return _props.getClass().getName().contains("hslf") ? null : _props.getAdjustValue(name);
	}

	public double getValue(String key) {
		if (key.matches("(\\+|-)?\\d+")) {
			return Double.parseDouble(key);
		}
		Double val = _ctx.get(key);
		return 0.0;
	}

	public double evaluate(Formula fmla) {
		if (fmla instanceof Guide) {
			String key = ((Guide) (fmla)).getName();
			if (key != null) {
			}
		}
		return 0d;
	}
}

