

import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.composite.CompositeSpatialStrategy;
import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.PackedQuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTreeFactory;
import org.apache.lucene.spatial.serialized.SerializedDVStrategy;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;


public class SpatialDocMaker extends DocMaker {
	public static final String SPATIAL_FIELD = "spatial";

	private static Map<Integer, SpatialStrategy> spatialStrategyCache = new HashMap<>();

	private SpatialStrategy strategy;

	private SpatialDocMaker.ShapeConverter shapeConverter;

	public static SpatialStrategy getSpatialStrategy(int roundNumber) {
		SpatialStrategy result = SpatialDocMaker.spatialStrategyCache.get(roundNumber);
		if (result == null) {
			throw new IllegalStateException("Strategy should have been init'ed by SpatialDocMaker by now");
		}
		return result;
	}

	protected SpatialStrategy makeSpatialStrategy(final Config config) {
		Map<String, String> configMap = new AbstractMap<String, String>() {
			@Override
			public Set<Map.Entry<String, String>> entrySet() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String get(Object key) {
				return config.get(("spatial." + key), null);
			}
		};
		SpatialContext ctx = SpatialContextFactory.makeSpatialContext(configMap, null);
		return makeSpatialStrategy(config, configMap, ctx);
	}

	protected SpatialStrategy makeSpatialStrategy(final Config config, Map<String, String> configMap, SpatialContext ctx) {
		final String strategyName = config.get("spatial.strategy", "rpt");
		switch (strategyName) {
			case "rpt" :
				return makeRPTStrategy(SpatialDocMaker.SPATIAL_FIELD, config, configMap, ctx);
			case "composite" :
				return makeCompositeStrategy(config, configMap, ctx);
			default :
				throw new IllegalStateException(("Unknown spatial.strategy: " + strategyName));
		}
	}

	protected RecursivePrefixTreeStrategy makeRPTStrategy(String spatialField, Config config, Map<String, String> configMap, SpatialContext ctx) {
		SpatialPrefixTree grid = SpatialPrefixTreeFactory.makeSPT(configMap, null, ctx);
		RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, spatialField);
		strategy.setPointsOnly(config.get("spatial.docPointsOnly", false));
		final boolean pruneLeafyBranches = config.get("spatial.pruneLeafyBranches", true);
		if (grid instanceof PackedQuadPrefixTree) {
			((PackedQuadPrefixTree) (grid)).setPruneLeafyBranches(pruneLeafyBranches);
			strategy.setPruneLeafyBranches(false);
		}else {
			strategy.setPruneLeafyBranches(pruneLeafyBranches);
		}
		int prefixGridScanLevel = config.get("query.spatial.prefixGridScanLevel", (-4));
		if (prefixGridScanLevel < 0)
			prefixGridScanLevel = (grid.getMaxLevels()) + prefixGridScanLevel;

		strategy.setPrefixGridScanLevel(prefixGridScanLevel);
		double distErrPct = config.get("spatial.distErrPct", 0.025);
		strategy.setDistErrPct(distErrPct);
		return strategy;
	}

	protected SerializedDVStrategy makeSerializedDVStrategy(String spatialField, Config config, Map<String, String> configMap, SpatialContext ctx) {
		return new SerializedDVStrategy(ctx, spatialField);
	}

	protected SpatialStrategy makeCompositeStrategy(Config config, Map<String, String> configMap, SpatialContext ctx) {
		final CompositeSpatialStrategy strategy = new CompositeSpatialStrategy(SpatialDocMaker.SPATIAL_FIELD, makeRPTStrategy(((SpatialDocMaker.SPATIAL_FIELD) + "_rpt"), config, configMap, ctx), makeSerializedDVStrategy(((SpatialDocMaker.SPATIAL_FIELD) + "_sdv"), config, configMap, ctx));
		strategy.setOptimizePredicates(config.get("query.spatial.composite.optimizePredicates", true));
		return strategy;
	}

	@Override
	public void setConfig(Config config, ContentSource source) {
		super.setConfig(config, source);
		SpatialStrategy existing = SpatialDocMaker.spatialStrategyCache.get(config.getRoundNumber());
		if (existing == null) {
			strategy = makeSpatialStrategy(config);
			SpatialDocMaker.spatialStrategyCache.put(config.getRoundNumber(), strategy);
			shapeConverter = SpatialDocMaker.makeShapeConverter(strategy, config, "doc.spatial.");
			System.out.println(("Spatial Strategy: " + (strategy)));
		}
	}

	public static SpatialDocMaker.ShapeConverter makeShapeConverter(final SpatialStrategy spatialStrategy, Config config, String configKeyPrefix) {
		final double radiusDegrees = config.get((configKeyPrefix + "radiusDegrees"), 0.0);
		final double plusMinus = config.get((configKeyPrefix + "radiusDegreesRandPlusMinus"), 0.0);
		final boolean bbox = config.get((configKeyPrefix + "bbox"), false);
		return new SpatialDocMaker.ShapeConverter() {
			@Override
			public Shape convert(Shape shape) {
				if ((shape instanceof Point) && ((radiusDegrees != 0.0) || (plusMinus != 0.0))) {
					Point point = ((Point) (shape));
					double radius = radiusDegrees;
					if (plusMinus > 0.0) {
						Random random = new Random(point.hashCode());
						radius += (((random.nextDouble()) * 2) * plusMinus) - plusMinus;
						radius = Math.abs(radius);
					}
					shape = spatialStrategy.getSpatialContext().makeCircle(point, radius);
				}
				if (bbox)
					shape = shape.getBoundingBox();

				return shape;
			}
		};
	}

	public interface ShapeConverter {
		Shape convert(Shape shape);
	}

	@Override
	public Document makeDocument() throws Exception {
		DocMaker.DocState docState = getDocState();
		Document doc = super.makeDocument();
		String shapeStr = doc.getField(DocMaker.BODY_FIELD).stringValue();
		return doc;
	}

	public static Shape makeShapeFromString(SpatialStrategy strategy, String name, String shapeStr) {
		if ((shapeStr != null) && ((shapeStr.length()) > 0)) {
			try {
				return strategy.getSpatialContext().readShapeFromWkt(shapeStr);
			} catch (Exception e) {
				System.err.println((((("Shape " + name) + " wasn't parseable: ") + e) + "  (skipping it)"));
				return null;
			}
		}
		return null;
	}

	@Override
	public Document makeDocument(int size) throws Exception {
		throw new UnsupportedOperationException();
	}
}

