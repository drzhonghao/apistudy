

import org.apache.poi.hpsf.IllegalPropertySetDataException;
import org.apache.poi.hpsf.TypedPropertyValue;
import org.apache.poi.hpsf.Variant;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;


@Internal
public class Array {
	static class ArrayDimension {
		private long _size;

		@SuppressWarnings("unused")
		private int _indexOffset;

		void read(LittleEndianByteArrayInputStream lei) {
			_size = lei.readUInt();
			_indexOffset = lei.readInt();
		}
	}

	static class ArrayHeader {
		private Array.ArrayDimension[] _dimensions;

		private int _type;

		void read(LittleEndianByteArrayInputStream lei) {
			_type = lei.readInt();
			long numDimensionsUnsigned = lei.readUInt();
			if (!((1 <= numDimensionsUnsigned) && (numDimensionsUnsigned <= 31))) {
				String msg = ("Array dimension number " + numDimensionsUnsigned) + " is not in [1; 31] range";
				throw new IllegalPropertySetDataException(msg);
			}
			int numDimensions = ((int) (numDimensionsUnsigned));
			_dimensions = new Array.ArrayDimension[numDimensions];
			for (int i = 0; i < numDimensions; i++) {
				Array.ArrayDimension ad = new Array.ArrayDimension();
				ad.read(lei);
				_dimensions[i] = ad;
			}
		}

		long getNumberOfScalarValues() {
			long result = 1;
			for (Array.ArrayDimension dimension : _dimensions) {
				result *= dimension._size;
			}
			return result;
		}

		int getType() {
			return _type;
		}
	}

	private final Array.ArrayHeader _header = new Array.ArrayHeader();

	private TypedPropertyValue[] _values;

	public void read(LittleEndianByteArrayInputStream lei) {
		_header.read(lei);
		long numberOfScalarsLong = _header.getNumberOfScalarValues();
		if (numberOfScalarsLong > (Integer.MAX_VALUE)) {
			String msg = ("Sorry, but POI can't store array of properties with size of " + numberOfScalarsLong) + " in memory";
			throw new UnsupportedOperationException(msg);
		}
		int numberOfScalars = ((int) (numberOfScalarsLong));
		_values = new TypedPropertyValue[numberOfScalars];
		int paddedType = ((_header._type) == (Variant.VT_VARIANT)) ? 0 : _header._type;
		for (int i = 0; i < numberOfScalars; i++) {
			TypedPropertyValue typedPropertyValue = new TypedPropertyValue(paddedType, null);
			typedPropertyValue.read(lei);
			_values[i] = typedPropertyValue;
			if (paddedType != 0) {
			}
		}
	}

	public TypedPropertyValue[] getValues() {
		return _values;
	}
}

