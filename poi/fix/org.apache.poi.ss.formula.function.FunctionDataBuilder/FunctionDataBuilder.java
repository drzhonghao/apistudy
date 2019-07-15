

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;


final class FunctionDataBuilder {
	private int _maxFunctionIndex;

	private final Map<String, FunctionMetadata> _functionDataByName;

	private final Map<Integer, FunctionMetadata> _functionDataByIndex;

	private final Set<Integer> _mutatingFunctionIndexes;

	public FunctionDataBuilder(int sizeEstimate) {
		_maxFunctionIndex = -1;
		_functionDataByName = new HashMap<>(((sizeEstimate * 3) / 2));
		_functionDataByIndex = new HashMap<>(((sizeEstimate * 3) / 2));
		_mutatingFunctionIndexes = new HashSet<>();
	}

	public void add(int functionIndex, String functionName, int minParams, int maxParams, byte returnClassCode, byte[] parameterClassCodes, boolean hasFootnote) {
		Integer indexKey = Integer.valueOf(functionIndex);
		if (functionIndex > (_maxFunctionIndex)) {
			_maxFunctionIndex = functionIndex;
		}
		FunctionMetadata prevFM;
		prevFM = _functionDataByName.get(functionName);
		if (prevFM != null) {
			if ((!hasFootnote) || (!(_mutatingFunctionIndexes.contains(indexKey)))) {
				throw new RuntimeException((("Multiple entries for function name '" + functionName) + "'"));
			}
			_functionDataByIndex.remove(Integer.valueOf(prevFM.getIndex()));
		}
		prevFM = _functionDataByIndex.get(indexKey);
		if (prevFM != null) {
			if ((!hasFootnote) || (!(_mutatingFunctionIndexes.contains(indexKey)))) {
				throw new RuntimeException((("Multiple entries for function index (" + functionIndex) + ")"));
			}
			_functionDataByName.remove(prevFM.getName());
		}
		if (hasFootnote) {
			_mutatingFunctionIndexes.add(indexKey);
		}
	}

	public FunctionMetadataRegistry build() {
		FunctionMetadata[] jumbledArray = new FunctionMetadata[_functionDataByName.size()];
		_functionDataByName.values().toArray(jumbledArray);
		FunctionMetadata[] fdIndexArray = new FunctionMetadata[(_maxFunctionIndex) + 1];
		for (int i = 0; i < (jumbledArray.length); i++) {
			FunctionMetadata fd = jumbledArray[i];
			fdIndexArray[fd.getIndex()] = fd;
		}
		return null;
	}
}

