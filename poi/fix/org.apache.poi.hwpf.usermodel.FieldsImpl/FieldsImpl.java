

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.hwpf.model.PlexOfField;
import org.apache.poi.hwpf.usermodel.Fields;
import org.apache.poi.util.Internal;


@Internal
public class FieldsImpl implements Fields {
	private static int binarySearch(List<PlexOfField> list, int startIndex, int endIndex, int requiredStartOffset) {
		FieldsImpl.checkIndexForBinarySearch(list.size(), startIndex, endIndex);
		int low = startIndex;
		int mid = -1;
		int high = endIndex - 1;
		int result = 0;
		while (low <= high) {
			mid = (low + high) >>> 1;
			int midStart = list.get(mid).getFcStart();
			if (midStart == requiredStartOffset) {
				return mid;
			}else
				if (midStart < requiredStartOffset) {
					low = mid + 1;
				}else {
					high = mid - 1;
				}

		} 
		if (mid < 0) {
			int insertPoint = endIndex;
			for (int index = startIndex; index < endIndex; index++) {
				if (requiredStartOffset < (list.get(index).getFcStart())) {
					insertPoint = index;
				}
			}
			return (-insertPoint) - 1;
		}
		return (-mid) - (result >= 0 ? 1 : 2);
	}

	private static void checkIndexForBinarySearch(int length, int start, int end) {
		if (start > end) {
			throw new IllegalArgumentException();
		}
		if ((length < end) || (0 > start)) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	private FieldsImpl.PlexOfFieldComparator comparator = new FieldsImpl.PlexOfFieldComparator();

	private static final class PlexOfFieldComparator implements Serializable , Comparator<PlexOfField> {
		public int compare(PlexOfField o1, PlexOfField o2) {
			int thisVal = o1.getFcStart();
			int anotherVal = o2.getFcStart();
			return Integer.compare(thisVal, anotherVal);
		}
	}
}

