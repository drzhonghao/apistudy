

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.Vector;


public class PlanetModel implements SerializableObject {
	public static final PlanetModel SPHERE = new PlanetModel(1.0, 1.0);

	public static final double WGS84_MEAN = 6371008.7714;

	public static final double WGS84_POLAR = 6356752.314245;

	public static final double WGS84_EQUATORIAL = 6378137.0;

	public static final PlanetModel WGS84 = new PlanetModel(((PlanetModel.WGS84_EQUATORIAL) / (PlanetModel.WGS84_MEAN)), ((PlanetModel.WGS84_POLAR) / (PlanetModel.WGS84_MEAN)));

	public final double ab;

	public final double c;

	public final double inverseAb;

	public final double inverseC;

	public final double inverseAbSquared;

	public final double inverseCSquared;

	public final double flattening;

	public final double squareRatio;

	public final double scale;

	public final double inverseScale;

	public final GeoPoint NORTH_POLE;

	public final GeoPoint SOUTH_POLE;

	public final GeoPoint MIN_X_POLE;

	public final GeoPoint MAX_X_POLE;

	public final GeoPoint MIN_Y_POLE;

	public final GeoPoint MAX_Y_POLE;

	public final double minimumPoleDistance;

	public PlanetModel(final double ab, final double c) {
		this.ab = ab;
		this.c = c;
		this.inverseAb = 1.0 / ab;
		this.inverseC = 1.0 / c;
		this.flattening = (ab - c) * (inverseAb);
		this.squareRatio = ((ab * ab) - (c * c)) / (c * c);
		this.inverseAbSquared = (inverseAb) * (inverseAb);
		this.inverseCSquared = (inverseC) * (inverseC);
		this.NORTH_POLE = new GeoPoint(c, 0.0, 0.0, 1.0, ((Math.PI) * 0.5), 0.0);
		this.SOUTH_POLE = new GeoPoint(c, 0.0, 0.0, (-1.0), ((-(Math.PI)) * 0.5), 0.0);
		this.MIN_X_POLE = new GeoPoint(ab, (-1.0), 0.0, 0.0, 0.0, (-(Math.PI)));
		this.MAX_X_POLE = new GeoPoint(ab, 1.0, 0.0, 0.0, 0.0, 0.0);
		this.MIN_Y_POLE = new GeoPoint(ab, 0.0, (-1.0), 0.0, 0.0, ((-(Math.PI)) * 0.5));
		this.MAX_Y_POLE = new GeoPoint(ab, 0.0, 1.0, 0.0, 0.0, ((Math.PI) * 0.5));
		this.scale = ((2.0 * ab) + c) / 3.0;
		this.inverseScale = 1.0 / (scale);
		this.minimumPoleDistance = Math.min(surfaceDistance(NORTH_POLE, SOUTH_POLE), surfaceDistance(MIN_X_POLE, MAX_X_POLE));
	}

	public PlanetModel(final InputStream inputStream) throws IOException {
		this(SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	@Override
	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, ab);
		SerializableObject.writeDouble(outputStream, c);
	}

	public boolean isSphere() {
		return (this.ab) == (this.c);
	}

	public double getMinimumMagnitude() {
		return Math.min(this.ab, this.c);
	}

	public double getMaximumMagnitude() {
		return Math.max(this.ab, this.c);
	}

	public double getMinimumXValue() {
		return -(this.ab);
	}

	public double getMaximumXValue() {
		return this.ab;
	}

	public double getMinimumYValue() {
		return -(this.ab);
	}

	public double getMaximumYValue() {
		return this.ab;
	}

	public double getMinimumZValue() {
		return -(this.c);
	}

	public double getMaximumZValue() {
		return this.c;
	}

	public boolean pointOnSurface(final Vector v) {
		return pointOnSurface(v.x, v.y, v.z);
	}

	public boolean pointOnSurface(final double x, final double y, final double z) {
		return (Math.abs(((((((x * x) * (inverseAb)) * (inverseAb)) + (((y * y) * (inverseAb)) * (inverseAb))) + (((z * z) * (inverseC)) * (inverseC))) - 1.0))) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean pointOutside(final Vector v) {
		return pointOutside(v.x, v.y, v.z);
	}

	public boolean pointOutside(final double x, final double y, final double z) {
		return ((((((x * x) + (y * y)) * (inverseAb)) * (inverseAb)) + (((z * z) * (inverseC)) * (inverseC))) - 1.0) > (Vector.MINIMUM_RESOLUTION);
	}

	public GeoPoint createSurfacePoint(final Vector vector) {
		return createSurfacePoint(vector.x, vector.y, vector.z);
	}

	public GeoPoint createSurfacePoint(final double x, final double y, final double z) {
		final double t = Math.sqrt((1.0 / ((((x * x) * (inverseAbSquared)) + ((y * y) * (inverseAbSquared))) + ((z * z) * (inverseCSquared)))));
		return new GeoPoint((t * x), (t * y), (t * z));
	}

	public GeoPoint bisection(final GeoPoint pt1, final GeoPoint pt2) {
		final double A0 = ((pt1.x) + (pt2.x)) * 0.5;
		final double B0 = ((pt1.y) + (pt2.y)) * 0.5;
		final double C0 = ((pt1.z) + (pt2.z)) * 0.5;
		final double denom = ((((inverseAbSquared) * A0) * A0) + (((inverseAbSquared) * B0) * B0)) + (((inverseCSquared) * C0) * C0);
		if (denom < (Vector.MINIMUM_RESOLUTION)) {
			return null;
		}
		final double t = Math.sqrt((1.0 / denom));
		return new GeoPoint((t * A0), (t * B0), (t * C0));
	}

	public double surfaceDistance(final GeoPoint pt1, final GeoPoint pt2) {
		final double L = (pt2.getLongitude()) - (pt1.getLongitude());
		final double U1 = Math.atan(((1.0 - (flattening)) * (Math.tan(pt1.getLatitude()))));
		final double U2 = Math.atan(((1.0 - (flattening)) * (Math.tan(pt2.getLatitude()))));
		final double sinU1 = Math.sin(U1);
		final double cosU1 = Math.cos(U1);
		final double sinU2 = Math.sin(U2);
		final double cosU2 = Math.cos(U2);
		final double dCosU1CosU2 = cosU1 * cosU2;
		final double dCosU1SinU2 = cosU1 * sinU2;
		final double dSinU1SinU2 = sinU1 * sinU2;
		final double dSinU1CosU2 = sinU1 * cosU2;
		double lambda = L;
		double lambdaP = (Math.PI) * 2.0;
		int iterLimit = 0;
		double cosSqAlpha;
		double sinSigma;
		double cos2SigmaM;
		double cosSigma;
		double sigma;
		double sinAlpha;
		double C;
		double sinLambda;
		double cosLambda;
		do {
			sinLambda = Math.sin(lambda);
			cosLambda = Math.cos(lambda);
			sinSigma = Math.sqrt((((cosU2 * sinLambda) * (cosU2 * sinLambda)) + ((dCosU1SinU2 - (dSinU1CosU2 * cosLambda)) * (dCosU1SinU2 - (dSinU1CosU2 * cosLambda)))));
			if (sinSigma == 0.0) {
				return 0.0;
			}
			cosSigma = dSinU1SinU2 + (dCosU1CosU2 * cosLambda);
			sigma = Math.atan2(sinSigma, cosSigma);
			sinAlpha = (dCosU1CosU2 * sinLambda) / sinSigma;
			cosSqAlpha = 1.0 - (sinAlpha * sinAlpha);
			cos2SigmaM = cosSigma - ((2.0 * dSinU1SinU2) / cosSqAlpha);
			if (Double.isNaN(cos2SigmaM))
				cos2SigmaM = 0.0;

			C = (((flattening) / 16.0) * cosSqAlpha) * (4.0 + ((flattening) * (4.0 - (3.0 * cosSqAlpha))));
			lambdaP = lambda;
			lambda = L + ((((1.0 - C) * (flattening)) * sinAlpha) * (sigma + ((C * sinSigma) * (cos2SigmaM + ((C * cosSigma) * ((-1.0) + ((2.0 * cos2SigmaM) * cos2SigmaM)))))));
		} while (((Math.abs((lambda - lambdaP))) >= (Vector.MINIMUM_RESOLUTION)) && ((++iterLimit) < 100) );
		final double uSq = cosSqAlpha * (this.squareRatio);
		final double A = 1.0 + ((uSq / 16384.0) * (4096.0 + (uSq * ((-768.0) + (uSq * (320.0 - (175.0 * uSq)))))));
		final double B = (uSq / 1024.0) * (256.0 + (uSq * ((-128.0) + (uSq * (74.0 - (47.0 * uSq))))));
		final double deltaSigma = (B * sinSigma) * (cos2SigmaM + ((B / 4.0) * ((cosSigma * ((-1.0) + ((2.0 * cos2SigmaM) * cos2SigmaM))) - ((((B / 6.0) * cos2SigmaM) * ((-3.0) + ((4.0 * sinSigma) * sinSigma))) * ((-3.0) + ((4.0 * cos2SigmaM) * cos2SigmaM))))));
		return (((c) * (inverseScale)) * A) * (sigma - deltaSigma);
	}

	public GeoPoint surfacePointOnBearing(final GeoPoint from, final double dist, final double bearing) {
		double lat = from.getLatitude();
		double lon = from.getLongitude();
		double sinα1 = Math.sin(bearing);
		double cosα1 = Math.cos(bearing);
		double tanU1 = (1.0 - (flattening)) * (Math.tan(lat));
		double cosU1 = 1.0 / (Math.sqrt((1.0 + (tanU1 * tanU1))));
		double sinU1 = tanU1 * cosU1;
		double σ1 = Math.atan2(tanU1, cosα1);
		double sinα = cosU1 * sinα1;
		double cosSqα = 1.0 - (sinα * sinα);
		double uSq = cosSqα * (squareRatio);
		double A = 1.0 + ((uSq / 16384.0) * (4096.0 + (uSq * ((-768.0) + (uSq * (320.0 - (175.0 * uSq)))))));
		double B = (uSq / 1024.0) * (256.0 + (uSq * ((-128.0) + (uSq * (74.0 - (47.0 * uSq))))));
		double cos2σM;
		double sinσ;
		double cosσ;
		double Δσ;
		double σ = dist / (((c) * (inverseScale)) * A);
		double σʹ;
		double iterations = 0;
		do {
			cos2σM = Math.cos(((2.0 * σ1) + σ));
			sinσ = Math.sin(σ);
			cosσ = Math.cos(σ);
			Δσ = (B * sinσ) * (cos2σM + ((B / 4.0) * ((cosσ * ((-1.0) + ((2.0 * cos2σM) * cos2σM))) - ((((B / 6.0) * cos2σM) * ((-3.0) + ((4.0 * sinσ) * sinσ))) * ((-3.0) + ((4.0 * cos2σM) * cos2σM))))));
			σʹ = σ;
			σ = (dist / (((c) * (inverseScale)) * A)) + Δσ;
		} while (((Math.abs((σ - σʹ))) >= (Vector.MINIMUM_RESOLUTION)) && ((++iterations) < 100) );
		double x = (sinU1 * sinσ) - ((cosU1 * cosσ) * cosα1);
		double φ2 = Math.atan2(((sinU1 * cosσ) + ((cosU1 * sinσ) * cosα1)), ((1.0 - (flattening)) * (Math.sqrt(((sinα * sinα) + (x * x))))));
		double λ = Math.atan2((sinσ * sinα1), ((cosU1 * cosσ) - ((sinU1 * sinσ) * cosα1)));
		double C = (((flattening) / 16.0) * cosSqα) * (4.0 + ((flattening) * (4.0 - (3.0 * cosSqα))));
		double L = λ - ((((1.0 - C) * (flattening)) * sinα) * (σ + ((C * sinσ) * (cos2σM + ((C * cosσ) * ((-1.0) + ((2.0 * cos2σM) * cos2σM)))))));
		double λ2 = (((lon + L) + (3.0 * (Math.PI))) % (2.0 * (Math.PI))) - (Math.PI);
		return null;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PlanetModel))
			return false;

		final PlanetModel other = ((PlanetModel) (o));
		return ((ab) == (other.ab)) && ((c) == (other.c));
	}

	@Override
	public int hashCode() {
		return (Double.hashCode(ab)) + (Double.hashCode(c));
	}

	@Override
	public String toString() {
		if (this.equals(PlanetModel.SPHERE)) {
			return "PlanetModel.SPHERE";
		}else
			if (this.equals(PlanetModel.WGS84)) {
				return "PlanetModel.WGS84";
			}else {
				return ((("PlanetModel(ab=" + (ab)) + " c=") + (c)) + ")";
			}

	}
}

