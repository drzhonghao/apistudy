

import java.math.BigInteger;
import java.util.Calendar;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LocaleUtil;


public class PropertyValue {
	private MAPIProperty property;

	private Types.MAPIType actualType;

	private long flags;

	protected byte[] data;

	public PropertyValue(MAPIProperty property, long flags, byte[] data) {
		this(property, flags, data, property.usualType);
	}

	public PropertyValue(MAPIProperty property, long flags, byte[] data, Types.MAPIType actualType) {
		this.property = property;
		this.flags = flags;
		this.data = data;
		this.actualType = actualType;
	}

	public MAPIProperty getProperty() {
		return property;
	}

	public long getFlags() {
		return flags;
	}

	public Object getValue() {
		return data;
	}

	public byte[] getRawValue() {
		return data;
	}

	public Types.MAPIType getActualType() {
		return actualType;
	}

	public void setRawValue(byte[] value) {
		this.data = value;
	}

	@Override
	public String toString() {
		Object v = getValue();
		if (v == null) {
			return "(No value available)";
		}
		if (v instanceof byte[]) {
		}else {
			return v.toString();
		}
		return null;
	}

	public static class NullPropertyValue extends PropertyValue {
		public NullPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.NULL);
		}

		@Override
		public Void getValue() {
			return null;
		}
	}

	public static class BooleanPropertyValue extends PropertyValue {
		public BooleanPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.BOOLEAN);
		}

		@Override
		public Boolean getValue() {
			short val = LittleEndian.getShort(data);
			return val > 0;
		}

		public void setValue(boolean value) {
			if ((data.length) != 2) {
				data = new byte[2];
			}
			if (value) {
				LittleEndian.putShort(data, 0, ((short) (1)));
			}
		}
	}

	public static class ShortPropertyValue extends PropertyValue {
		public ShortPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.SHORT);
		}

		@Override
		public Short getValue() {
			return LittleEndian.getShort(data);
		}

		public void setValue(short value) {
			if ((data.length) != 2) {
				data = new byte[2];
			}
			LittleEndian.putShort(data, 0, value);
		}
	}

	public static class LongPropertyValue extends PropertyValue {
		public LongPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.LONG);
		}

		@Override
		public Integer getValue() {
			return LittleEndian.getInt(data);
		}

		public void setValue(int value) {
			if ((data.length) != 4) {
				data = new byte[4];
			}
			LittleEndian.putInt(data, 0, value);
		}
	}

	public static class LongLongPropertyValue extends PropertyValue {
		public LongLongPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.LONG_LONG);
		}

		@Override
		public Long getValue() {
			return LittleEndian.getLong(data);
		}

		public void setValue(long value) {
			if ((data.length) != 8) {
				data = new byte[8];
			}
			LittleEndian.putLong(data, 0, value);
		}
	}

	public static class FloatPropertyValue extends PropertyValue {
		public FloatPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.FLOAT);
		}

		@Override
		public Float getValue() {
			return LittleEndian.getFloat(data);
		}

		public void setValue(float value) {
			if ((data.length) != 4) {
				data = new byte[4];
			}
			LittleEndian.putFloat(data, 0, value);
		}
	}

	public static class DoublePropertyValue extends PropertyValue {
		public DoublePropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.DOUBLE);
		}

		@Override
		public Double getValue() {
			return LittleEndian.getDouble(data);
		}

		public void setValue(double value) {
			if ((data.length) != 8) {
				data = new byte[8];
			}
			LittleEndian.putDouble(data, 0, value);
		}
	}

	public static class CurrencyPropertyValue extends PropertyValue {
		private static final BigInteger SHIFT = BigInteger.valueOf(10000);

		public CurrencyPropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.CURRENCY);
		}

		@Override
		public BigInteger getValue() {
			long unshifted = LittleEndian.getLong(data);
			return BigInteger.valueOf(unshifted).divide(PropertyValue.CurrencyPropertyValue.SHIFT);
		}

		public void setValue(BigInteger value) {
			if ((data.length) != 8) {
				data = new byte[8];
			}
			long shifted = value.multiply(PropertyValue.CurrencyPropertyValue.SHIFT).longValue();
			LittleEndian.putLong(data, 0, shifted);
		}
	}

	public static class TimePropertyValue extends PropertyValue {
		private static final long OFFSET = (((1000L * 60L) * 60L) * 24L) * ((365L * 369L) + 89L);

		public TimePropertyValue(MAPIProperty property, long flags, byte[] data) {
			super(property, flags, data, Types.TIME);
		}

		@Override
		public Calendar getValue() {
			long time = LittleEndian.getLong(data);
			time = ((time / 10) / 1000) - (PropertyValue.TimePropertyValue.OFFSET);
			Calendar timeC = LocaleUtil.getLocaleCalendar();
			timeC.setTimeInMillis(time);
			return timeC;
		}

		public void setValue(Calendar value) {
			if ((data.length) != 8) {
				data = new byte[8];
			}
			long time = value.getTimeInMillis();
			time = ((time + (PropertyValue.TimePropertyValue.OFFSET)) * 10) * 1000;
			LittleEndian.putLong(data, 0, time);
		}
	}
}

