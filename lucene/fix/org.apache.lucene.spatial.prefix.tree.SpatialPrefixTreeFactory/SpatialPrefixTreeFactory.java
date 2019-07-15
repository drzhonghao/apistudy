

import java.util.Map;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;


public abstract class SpatialPrefixTreeFactory {
	private static final double DEFAULT_GEO_MAX_DETAIL_KM = 0.001;

	public static final String PREFIX_TREE = "prefixTree";

	public static final String MAX_LEVELS = "maxLevels";

	public static final String MAX_DIST_ERR = "maxDistErr";

	protected Map<String, String> args;

	protected SpatialContext ctx;

	protected Integer maxLevels;

	public static SpatialPrefixTree makeSPT(Map<String, String> args, ClassLoader classLoader, SpatialContext ctx) {
		SpatialPrefixTreeFactory instance;
		String cname = args.get(SpatialPrefixTreeFactory.PREFIX_TREE);
		if (cname == null)
			cname = (ctx.isGeo()) ? "geohash" : "quad";

		if ("geohash".equalsIgnoreCase(cname)) {
		}else
			if ("quad".equalsIgnoreCase(cname)) {
			}else
				if ("packedQuad".equalsIgnoreCase(cname)) {
				}else
					if ("s2".equalsIgnoreCase(cname)) {
					}else {
						try {
							Class<?> c = classLoader.loadClass(cname);
							instance = ((SpatialPrefixTreeFactory) (c.newInstance()));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}



		instance = null;
		instance.init(args, ctx);
		instance = null;
		return instance.newSPT();
	}

	protected void init(Map<String, String> args, SpatialContext ctx) {
		this.args = args;
		this.ctx = ctx;
		initMaxLevels();
	}

	protected void initMaxLevels() {
		String mlStr = args.get(SpatialPrefixTreeFactory.MAX_LEVELS);
		if (mlStr != null) {
			maxLevels = Integer.valueOf(mlStr);
			return;
		}
		double degrees;
		String maxDetailDistStr = args.get(SpatialPrefixTreeFactory.MAX_DIST_ERR);
		if (maxDetailDistStr == null) {
			if (!(ctx.isGeo())) {
				return;
			}
			degrees = DistanceUtils.dist2Degrees(SpatialPrefixTreeFactory.DEFAULT_GEO_MAX_DETAIL_KM, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		}else {
			degrees = Double.parseDouble(maxDetailDistStr);
		}
		maxLevels = getLevelForDistance(degrees);
	}

	protected abstract int getLevelForDistance(double degrees);

	protected abstract SpatialPrefixTree newSPT();
}

