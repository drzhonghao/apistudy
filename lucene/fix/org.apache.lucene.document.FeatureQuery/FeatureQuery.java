

import org.apache.lucene.search.Query;


final class FeatureQuery extends Query {
	private final String fieldName = null;

	private final String featureName = null;

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || ((getClass()) != (obj.getClass()))) {
			return false;
		}
		FeatureQuery that = ((FeatureQuery) (obj));
		return false;
	}

	@Override
	public int hashCode() {
		int h = getClass().hashCode();
		h = (31 * h) + (fieldName.hashCode());
		h = (31 * h) + (featureName.hashCode());
		return h;
	}

	@Override
	public String toString(String field) {
		return null;
	}
}

