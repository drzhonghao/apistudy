

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.sl.draw.binding.CTAdjPoint2D;
import org.apache.poi.sl.draw.binding.CTPath2D;
import org.apache.poi.sl.draw.binding.CTPath2DArcTo;
import org.apache.poi.sl.draw.binding.CTPath2DClose;
import org.apache.poi.sl.draw.binding.CTPath2DCubicBezierTo;
import org.apache.poi.sl.draw.binding.CTPath2DLineTo;
import org.apache.poi.sl.draw.binding.CTPath2DMoveTo;
import org.apache.poi.sl.draw.binding.CTPath2DQuadBezierTo;
import org.apache.poi.sl.draw.binding.STPathFillMode;
import org.apache.poi.sl.draw.geom.Context;
import org.apache.poi.sl.draw.geom.PathCommand;
import org.apache.poi.sl.usermodel.PaintStyle;

import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.DARKEN;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.DARKEN_LESS;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.LIGHTEN;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.LIGHTEN_LESS;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.NONE;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.NORM;


public class Path {
	private final List<PathCommand> commands;

	PaintStyle.PaintModifier _fill;

	boolean _stroke;

	long _w;

	long _h;

	public Path() {
		this(true, true);
	}

	public Path(boolean fill, boolean stroke) {
		commands = new ArrayList<>();
		_w = -1;
		_h = -1;
		_fill = (fill) ? NORM : NONE;
		_stroke = stroke;
	}

	public Path(CTPath2D spPath) {
		switch (spPath.getFill()) {
			case NONE :
				_fill = NONE;
				break;
			case DARKEN :
				_fill = DARKEN;
				break;
			case DARKEN_LESS :
				_fill = DARKEN_LESS;
				break;
			case LIGHTEN :
				_fill = LIGHTEN;
				break;
			case LIGHTEN_LESS :
				_fill = LIGHTEN_LESS;
				break;
			default :
			case NORM :
				_fill = NORM;
				break;
		}
		_stroke = spPath.isStroke();
		_w = (spPath.isSetW()) ? spPath.getW() : -1;
		_h = (spPath.isSetH()) ? spPath.getH() : -1;
		commands = new ArrayList<>();
		for (Object ch : spPath.getCloseOrMoveToOrLnTo()) {
			if (ch instanceof CTPath2DMoveTo) {
				CTAdjPoint2D pt = ((CTPath2DMoveTo) (ch)).getPt();
			}else
				if (ch instanceof CTPath2DLineTo) {
					CTAdjPoint2D pt = ((CTPath2DLineTo) (ch)).getPt();
				}else
					if (ch instanceof CTPath2DArcTo) {
						CTPath2DArcTo arc = ((CTPath2DArcTo) (ch));
					}else
						if (ch instanceof CTPath2DQuadBezierTo) {
							CTPath2DQuadBezierTo bez = ((CTPath2DQuadBezierTo) (ch));
							CTAdjPoint2D pt1 = bez.getPt().get(0);
							CTAdjPoint2D pt2 = bez.getPt().get(1);
						}else
							if (ch instanceof CTPath2DCubicBezierTo) {
								CTPath2DCubicBezierTo bez = ((CTPath2DCubicBezierTo) (ch));
								CTAdjPoint2D pt1 = bez.getPt().get(0);
								CTAdjPoint2D pt2 = bez.getPt().get(1);
								CTAdjPoint2D pt3 = bez.getPt().get(2);
							}else
								if (ch instanceof CTPath2DClose) {
								}else {
									throw new IllegalStateException(("Unsupported path segment: " + ch));
								}





		}
	}

	public void addCommand(PathCommand cmd) {
		commands.add(cmd);
	}

	public Path2D.Double getPath(Context ctx) {
		Path2D.Double path = new Path2D.Double();
		for (PathCommand cmd : commands) {
			cmd.execute(path, ctx);
		}
		return path;
	}

	public boolean isStroked() {
		return _stroke;
	}

	public boolean isFilled() {
		return (_fill) != (NONE);
	}

	public PaintStyle.PaintModifier getFill() {
		return _fill;
	}

	public long getW() {
		return _w;
	}

	public long getH() {
		return _h;
	}
}

