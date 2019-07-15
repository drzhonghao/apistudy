

import org.apache.lucene.facet.taxonomy.FacetLabel;


public class NameHashIntCacheLRU {
	NameHashIntCacheLRU(int maxCacheSize) {
	}

	Object key(FacetLabel name) {
		return new Long(name.longHashCode());
	}

	Object key(FacetLabel name, int prefixLen) {
		return new Long(name.subpath(prefixLen).longHashCode());
	}
}

