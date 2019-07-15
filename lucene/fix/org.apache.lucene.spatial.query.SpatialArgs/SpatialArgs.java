

import org.apache.lucene.spatial.query.SpatialOperation;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceCalculator;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;


public class SpatialArgs {
	public static final double DEFAULT_DISTERRPCT = 0.025;

	private SpatialOperation operation;

	private Shape shape;

	private Double distErrPct;

	private Double distErr;

	public SpatialArgs(SpatialOperation operation, Shape shape) {
		if ((operation == null) || (shape == null))
			throw new NullPointerException("operation and shape are required");

		this.operation = operation;
		this.shape = shape;
	}

	public static double calcDistanceFromErrPct(Shape shape, double distErrPct, SpatialContext ctx) {
		if ((distErrPct < 0) || (distErrPct > 0.5)) {
			throw new IllegalArgumentException((("distErrPct " + distErrPct) + " must be between [0 to 0.5]"));
		}
		if ((distErrPct == 0) || (shape instanceof Point)) {
			return 0;
		}
		Rectangle bbox = shape.getBoundingBox();
		Point ctr = bbox.getCenter();
		double y = ((ctr.getY()) >= 0) ? bbox.getMaxY() : bbox.getMinY();
		double diagonalDist = ctx.getDistCalc().distance(ctr, bbox.getMaxX(), y);
		return diagonalDist * distErrPct;
	}

	public double resolveDistErr(SpatialContext ctx, double defaultDistErrPct) {
		if ((distErr) != null)
			return distErr;

		double distErrPct = ((this.distErrPct) != null) ? this.distErrPct : defaultDistErrPct;
		return SpatialArgs.calcDistanceFromErrPct(shape, distErrPct, ctx);
	}

	public void validate() throws IllegalArgumentException {
		if (((distErr) != null) && ((distErrPct) != null))
			throw new IllegalArgumentException("Only distErr or distErrPct can be specified.");

	}

	@Override
	public String toString() {
		return null;
	}

	public SpatialOperation getOperation() {
		return operation;
	}

	public void setOperation(SpatialOperation operation) {
		this.operation = operation;
	}

	public Shape getShape() {
		return shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Double getDistErrPct() {
		return distErrPct;
	}

	public void setDistErrPct(Double distErrPct) {
		if (distErrPct != null)
			this.distErrPct = distErrPct;

	}

	public Double getDistErr() {
		return distErr;
	}

	public void setDistErr(Double distErr) {
		this.distErr = distErr;
	}
}

