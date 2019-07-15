

import com.google.common.base.Preconditions;
import org.apache.accumulo.core.conf.AccumuloConfiguration;


class SizeType {
	public String convert(String str) {
		long size = AccumuloConfiguration.getMemoryInBytes(str);
		Preconditions.checkArgument((size > 0));
		return Long.toString(size);
	}
}

