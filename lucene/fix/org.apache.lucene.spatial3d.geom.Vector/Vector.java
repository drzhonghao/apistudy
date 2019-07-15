

import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.PlanetModel;


public class Vector {
	public static final double MINIMUM_RESOLUTION = 1.0E-12;

	public static final double MINIMUM_ANGULAR_RESOLUTION = (Math.PI) * (Vector.MINIMUM_RESOLUTION);

	public static final double MINIMUM_RESOLUTION_SQUARED = (Vector.MINIMUM_RESOLUTION) * (Vector.MINIMUM_RESOLUTION);

	public static final double MINIMUM_RESOLUTION_CUBED = (Vector.MINIMUM_RESOLUTION_SQUARED) * (Vector.MINIMUM_RESOLUTION);

	public final double x;

	public final double y;

	public final double z;

	private static final double MINIMUM_GRAM_SCHMIDT_ENVELOPE = (Vector.MINIMUM_RESOLUTION) * 0.5;

	public Vector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector(final Vector A, final double BX, final double BY, final double BZ) {
		this(A.x, A.y, A.z, BX, BY, BZ);
	}

	public Vector(final double AX, final double AY, final double AZ, final double BX, final double BY, final double BZ) {
		final double thisX = (AY * BZ) - (AZ * BY);
		final double thisY = (AZ * BX) - (AX * BZ);
		final double thisZ = (AX * BY) - (AY * BX);
		final double magnitude = Vector.magnitude(thisX, thisY, thisZ);
		if (magnitude == 0.0) {
			throw new IllegalArgumentException("Degenerate/parallel vector constructed");
		}
		final double inverseMagnitude = 1.0 / magnitude;
		double normalizeX = thisX * inverseMagnitude;
		double normalizeY = thisY * inverseMagnitude;
		double normalizeZ = thisZ * inverseMagnitude;
		int i = 0;
		while (true) {
			final double currentDotProdA = ((AX * normalizeX) + (AY * normalizeY)) + (AZ * normalizeZ);
			final double currentDotProdB = ((BX * normalizeX) + (BY * normalizeY)) + (BZ * normalizeZ);
			if (((Math.abs(currentDotProdA)) < (Vector.MINIMUM_GRAM_SCHMIDT_ENVELOPE)) && ((Math.abs(currentDotProdB)) < (Vector.MINIMUM_GRAM_SCHMIDT_ENVELOPE))) {
				break;
			}
			final double currentVectorX;
			final double currentVectorY;
			final double currentVectorZ;
			final double currentDotProd;
			if ((Math.abs(currentDotProdA)) > (Math.abs(currentDotProdB))) {
				currentVectorX = AX;
				currentVectorY = AY;
				currentVectorZ = AZ;
				currentDotProd = currentDotProdA;
			}else {
				currentVectorX = BX;
				currentVectorY = BY;
				currentVectorZ = BZ;
				currentDotProd = currentDotProdB;
			}
			normalizeX = normalizeX - (currentDotProd * currentVectorX);
			normalizeY = normalizeY - (currentDotProd * currentVectorY);
			normalizeZ = normalizeZ - (currentDotProd * currentVectorZ);
			final double correctedMagnitude = Vector.magnitude(normalizeX, normalizeY, normalizeZ);
			final double inverseCorrectedMagnitude = 1.0 / correctedMagnitude;
			normalizeX = normalizeX * inverseCorrectedMagnitude;
			normalizeY = normalizeY * inverseCorrectedMagnitude;
			normalizeZ = normalizeZ * inverseCorrectedMagnitude;
			if ((i++) > 10) {
				throw new IllegalArgumentException("Plane could not be constructed! Could not find a normal vector.");
			}
		} 
		this.x = normalizeX;
		this.y = normalizeY;
		this.z = normalizeZ;
	}

	public Vector(final Vector A, final Vector B) {
		this(A, B.x, B.y, B.z);
	}

	public static double magnitude(final double x, final double y, final double z) {
		return Math.sqrt((((x * x) + (y * y)) + (z * z)));
	}

	public Vector normalize() {
		double denom = magnitude();
		if (denom < (Vector.MINIMUM_RESOLUTION))
			return null;

		double normFactor = 1.0 / denom;
		return new Vector(((x) * normFactor), ((y) * normFactor), ((z) * normFactor));
	}

	public static boolean crossProductEvaluateIsZero(final Vector A, final Vector B, final Vector point) {
		final double thisX = ((A.y) * (B.z)) - ((A.z) * (B.y));
		final double thisY = ((A.z) * (B.x)) - ((A.x) * (B.z));
		final double thisZ = ((A.x) * (B.y)) - ((A.y) * (B.x));
		final double magnitude = Vector.magnitude(thisX, thisY, thisZ);
		if (magnitude == 0.0) {
			return true;
		}
		final double inverseMagnitude = 1.0 / magnitude;
		double normalizeX = thisX * inverseMagnitude;
		double normalizeY = thisY * inverseMagnitude;
		double normalizeZ = thisZ * inverseMagnitude;
		int i = 0;
		while (true) {
			final double currentDotProdA = (((A.x) * normalizeX) + ((A.y) * normalizeY)) + ((A.z) * normalizeZ);
			final double currentDotProdB = (((B.x) * normalizeX) + ((B.y) * normalizeY)) + ((B.z) * normalizeZ);
			if (((Math.abs(currentDotProdA)) < (Vector.MINIMUM_GRAM_SCHMIDT_ENVELOPE)) && ((Math.abs(currentDotProdB)) < (Vector.MINIMUM_GRAM_SCHMIDT_ENVELOPE))) {
				break;
			}
			final double currentVectorX;
			final double currentVectorY;
			final double currentVectorZ;
			final double currentDotProd;
			if ((Math.abs(currentDotProdA)) > (Math.abs(currentDotProdB))) {
				currentVectorX = A.x;
				currentVectorY = A.y;
				currentVectorZ = A.z;
				currentDotProd = currentDotProdA;
			}else {
				currentVectorX = B.x;
				currentVectorY = B.y;
				currentVectorZ = B.z;
				currentDotProd = currentDotProdB;
			}
			normalizeX = normalizeX - (currentDotProd * currentVectorX);
			normalizeY = normalizeY - (currentDotProd * currentVectorY);
			normalizeZ = normalizeZ - (currentDotProd * currentVectorZ);
			final double correctedMagnitude = Vector.magnitude(normalizeX, normalizeY, normalizeZ);
			final double inverseCorrectedMagnitude = 1.0 / correctedMagnitude;
			normalizeX = normalizeX * inverseCorrectedMagnitude;
			normalizeY = normalizeY * inverseCorrectedMagnitude;
			normalizeZ = normalizeZ * inverseCorrectedMagnitude;
			if ((i++) > 10) {
				throw new IllegalArgumentException("Plane could not be constructed! Could not find a normal vector.");
			}
		} 
		return (Math.abs((((normalizeX * (point.x)) + (normalizeY * (point.y))) + (normalizeZ * (point.z))))) < (Vector.MINIMUM_RESOLUTION);
	}

	public double dotProduct(final Vector v) {
		return (((this.x) * (v.x)) + ((this.y) * (v.y))) + ((this.z) * (v.z));
	}

	public double dotProduct(final double x, final double y, final double z) {
		return (((this.x) * x) + ((this.y) * y)) + ((this.z) * z);
	}

	public boolean isWithin(final Membership[] bounds, final Membership... moreBounds) {
		for (final Membership bound : bounds) {
		}
		for (final Membership bound : moreBounds) {
		}
		return true;
	}

	public Vector translate(final double xOffset, final double yOffset, final double zOffset) {
		return new Vector(((x) - xOffset), ((y) - yOffset), ((z) - zOffset));
	}

	public Vector rotateXY(final double angle) {
		return rotateXY(Math.sin(angle), Math.cos(angle));
	}

	public Vector rotateXY(final double sinAngle, final double cosAngle) {
		return new Vector((((x) * cosAngle) - ((y) * sinAngle)), (((x) * sinAngle) + ((y) * cosAngle)), z);
	}

	public Vector rotateXZ(final double angle) {
		return rotateXZ(Math.sin(angle), Math.cos(angle));
	}

	public Vector rotateXZ(final double sinAngle, final double cosAngle) {
		return new Vector((((x) * cosAngle) - ((z) * sinAngle)), y, (((x) * sinAngle) + ((z) * cosAngle)));
	}

	public Vector rotateZY(final double angle) {
		return rotateZY(Math.sin(angle), Math.cos(angle));
	}

	public Vector rotateZY(final double sinAngle, final double cosAngle) {
		return new Vector(x, (((z) * sinAngle) + ((y) * cosAngle)), (((z) * cosAngle) - ((y) * sinAngle)));
	}

	public double linearDistanceSquared(final Vector v) {
		double deltaX = (this.x) - (v.x);
		double deltaY = (this.y) - (v.y);
		double deltaZ = (this.z) - (v.z);
		return ((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ);
	}

	public double linearDistanceSquared(final double x, final double y, final double z) {
		double deltaX = (this.x) - x;
		double deltaY = (this.y) - y;
		double deltaZ = (this.z) - z;
		return ((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ);
	}

	public double linearDistance(final Vector v) {
		return Math.sqrt(linearDistanceSquared(v));
	}

	public double linearDistance(final double x, final double y, final double z) {
		return Math.sqrt(linearDistanceSquared(x, y, z));
	}

	public double normalDistanceSquared(final Vector v) {
		double t = dotProduct(v);
		double deltaX = ((this.x) * t) - (v.x);
		double deltaY = ((this.y) * t) - (v.y);
		double deltaZ = ((this.z) * t) - (v.z);
		return ((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ);
	}

	public double normalDistanceSquared(final double x, final double y, final double z) {
		double t = dotProduct(x, y, z);
		double deltaX = ((this.x) * t) - x;
		double deltaY = ((this.y) * t) - y;
		double deltaZ = ((this.z) * t) - z;
		return ((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ);
	}

	public double normalDistance(final Vector v) {
		return Math.sqrt(normalDistanceSquared(v));
	}

	public double normalDistance(final double x, final double y, final double z) {
		return Math.sqrt(normalDistanceSquared(x, y, z));
	}

	public double magnitude() {
		return Vector.magnitude(x, y, z);
	}

	public boolean isNumericallyIdentical(final double otherX, final double otherY, final double otherZ) {
		final double deltaX = (x) - otherX;
		final double deltaY = (y) - otherY;
		final double deltaZ = (z) - otherZ;
		return (((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ)) < (Vector.MINIMUM_RESOLUTION_SQUARED);
	}

	public boolean isNumericallyIdentical(final Vector other) {
		final double deltaX = (x) - (other.x);
		final double deltaY = (y) - (other.y);
		final double deltaZ = (z) - (other.z);
		return (((deltaX * deltaX) + (deltaY * deltaY)) + (deltaZ * deltaZ)) < (Vector.MINIMUM_RESOLUTION_SQUARED);
	}

	public boolean isParallel(final double otherX, final double otherY, final double otherZ) {
		final double thisX = ((y) * otherZ) - ((z) * otherY);
		final double thisY = ((z) * otherX) - ((x) * otherZ);
		final double thisZ = ((x) * otherY) - ((y) * otherX);
		return (((thisX * thisX) + (thisY * thisY)) + (thisZ * thisZ)) < (Vector.MINIMUM_RESOLUTION_SQUARED);
	}

	public boolean isParallel(final Vector other) {
		final double thisX = ((y) * (other.z)) - ((z) * (other.y));
		final double thisY = ((z) * (other.x)) - ((x) * (other.z));
		final double thisZ = ((x) * (other.y)) - ((y) * (other.x));
		return (((thisX * thisX) + (thisY * thisY)) + (thisZ * thisZ)) < (Vector.MINIMUM_RESOLUTION_SQUARED);
	}

	static double computeDesiredEllipsoidMagnitude(final PlanetModel planetModel, final double x, final double y, final double z) {
		return 1.0 / (Math.sqrt(((((x * x) * (planetModel.inverseAbSquared)) + ((y * y) * (planetModel.inverseAbSquared))) + ((z * z) * (planetModel.inverseCSquared)))));
	}

	static double computeDesiredEllipsoidMagnitude(final PlanetModel planetModel, final double z) {
		return 1.0 / (Math.sqrt((((1.0 - (z * z)) * (planetModel.inverseAbSquared)) + ((z * z) * (planetModel.inverseCSquared)))));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Vector))
			return false;

		Vector other = ((Vector) (o));
		return (((other.x) == (x)) && ((other.y) == (y))) && ((other.z) == (z));
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(y);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(z);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return ((((("[X=" + (x)) + ", Y=") + (y)) + ", Z=") + (z)) + "]";
	}
}

