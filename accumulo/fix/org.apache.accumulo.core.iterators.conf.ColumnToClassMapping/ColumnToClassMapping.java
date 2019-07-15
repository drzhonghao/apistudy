

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.iterators.conf.ColumnSet;
import org.apache.accumulo.core.iterators.conf.ColumnUtil;
import org.apache.accumulo.core.iterators.conf.ColumnUtil.ColFamHashKey;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.apache.accumulo.start.classloader.vfs.ContextManager;
import org.apache.hadoop.io.Text;


public class ColumnToClassMapping<K> {
	private HashMap<ColumnUtil.ColFamHashKey, K> objectsCF;

	private HashMap<ColumnUtil.ColHashKey, K> objectsCol;

	public ColumnToClassMapping() {
		objectsCF = new HashMap<>();
		objectsCol = new HashMap<>();
	}

	public ColumnToClassMapping(Map<String, String> objectStrings, Class<? extends K> c) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		this(objectStrings, c, null);
	}

	public ColumnToClassMapping(Map<String, String> objectStrings, Class<? extends K> c, String context) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		this();
		for (Map.Entry<String, String> entry : objectStrings.entrySet()) {
			String column = entry.getKey();
			String className = entry.getValue();
			Pair<Text, Text> pcic = ColumnSet.decodeColumns(column);
			Class<?> clazz;
			if ((context != null) && (!(context.equals(""))))
				clazz = AccumuloVFSClassLoader.getContextManager().getClassLoader(context).loadClass(className);
			else
				clazz = AccumuloVFSClassLoader.loadClass(className, c);

			@SuppressWarnings("unchecked")
			K inst = ((K) (clazz.newInstance()));
			if ((pcic.getSecond()) == null) {
				addObject(pcic.getFirst(), inst);
			}else {
				addObject(pcic.getFirst(), pcic.getSecond(), inst);
			}
		}
	}

	protected void addObject(Text colf, K obj) {
	}

	protected void addObject(Text colf, Text colq, K obj) {
	}

	public K getObject(Key key) {
		K obj = null;
		if ((objectsCol.size()) > 0) {
			if (obj != null) {
				return obj;
			}
		}
		if ((objectsCF.size()) > 0) {
		}
		return obj;
	}

	public boolean isEmpty() {
		return ((objectsCol.size()) == 0) && ((objectsCF.size()) == 0);
	}
}

