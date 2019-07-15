

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.spatial.spatial4j.Geo3dCircleShape;
import org.apache.lucene.spatial.spatial4j.Geo3dPointShape;
import org.apache.lucene.spatial.spatial4j.Geo3dRectangleShape;
import org.apache.lucene.spatial.spatial4j.Geo3dShape;
import org.apache.lucene.spatial.spatial4j.Geo3dSpatialContextFactory;
import org.apache.lucene.spatial3d.geom.GeoAreaShape;
import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoCircle;
import org.apache.lucene.spatial3d.geom.GeoPointShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.io.BinaryCodec;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeCollection;


public class Geo3dBinaryCodec extends BinaryCodec {
	private PlanetModel planetModel;

	@SuppressWarnings("unchecked")
	public Geo3dBinaryCodec(SpatialContext ctx, SpatialContextFactory factory) {
		super(ctx, factory);
		planetModel = ((Geo3dSpatialContextFactory) (factory)).planetModel;
	}

	@Override
	public Shape readShape(DataInput dataInput) throws IOException {
		SerializableObject serializableObject = SerializableObject.readObject(planetModel, ((InputStream) (dataInput)));
		if (serializableObject instanceof GeoAreaShape) {
			GeoAreaShape shape = ((GeoAreaShape) (serializableObject));
			return new Geo3dShape<>(shape, ctx);
		}
		throw new IllegalArgumentException(("trying to read a not supported shape: " + (serializableObject.getClass())));
	}

	@Override
	public void writeShape(DataOutput dataOutput, Shape s) throws IOException {
		if (s instanceof Geo3dShape) {
			Geo3dShape geoAreaShape = ((Geo3dShape) (s));
		}else {
			throw new IllegalArgumentException(("trying to write a not supported shape: " + (s.getClass().getName())));
		}
	}

	@Override
	public Point readPoint(DataInput dataInput) throws IOException {
		SerializableObject serializableObject = SerializableObject.readObject(planetModel, ((InputStream) (dataInput)));
		if (serializableObject instanceof GeoPointShape) {
			GeoPointShape shape = ((GeoPointShape) (serializableObject));
			return new Geo3dPointShape(shape, ctx);
		}
		throw new IllegalArgumentException(("trying to read a not supported point shape: " + (serializableObject.getClass())));
	}

	@Override
	public void writePoint(DataOutput dataOutput, Point pt) throws IOException {
		writeShape(dataOutput, pt);
	}

	@Override
	public Rectangle readRect(DataInput dataInput) throws IOException {
		SerializableObject serializableObject = SerializableObject.readObject(planetModel, ((InputStream) (dataInput)));
		if (serializableObject instanceof GeoBBox) {
			GeoBBox shape = ((GeoBBox) (serializableObject));
			return new Geo3dRectangleShape(shape, ctx);
		}
		throw new IllegalArgumentException(("trying to read a not supported rectangle shape: " + (serializableObject.getClass())));
	}

	@Override
	public void writeRect(DataOutput dataOutput, Rectangle r) throws IOException {
		writeShape(dataOutput, r);
	}

	@Override
	public Circle readCircle(DataInput dataInput) throws IOException {
		SerializableObject serializableObject = SerializableObject.readObject(planetModel, ((InputStream) (dataInput)));
		if (serializableObject instanceof GeoCircle) {
			GeoCircle shape = ((GeoCircle) (serializableObject));
			return new Geo3dCircleShape(shape, ctx);
		}
		throw new IllegalArgumentException(("trying to read a not supported circle shape: " + (serializableObject.getClass())));
	}

	@Override
	public void writeCircle(DataOutput dataOutput, Circle c) throws IOException {
		writeShape(dataOutput, c);
	}

	@Override
	public ShapeCollection readCollection(DataInput dataInput) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeCollection(DataOutput dataOutput, ShapeCollection col) throws IOException {
		throw new UnsupportedOperationException();
	}
}

