

import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.formula.function.FunctionMetadata;


public final class FunctionMetadataRegistry {
	public static final String FUNCTION_NAME_IF = "IF";

	public static final int FUNCTION_INDEX_IF = 1;

	public static final short FUNCTION_INDEX_SUM = 4;

	public static final int FUNCTION_INDEX_CHOOSE = 100;

	public static final short FUNCTION_INDEX_INDIRECT = 148;

	public static final short FUNCTION_INDEX_EXTERNAL = 255;

	private static FunctionMetadataRegistry _instance;

	private final FunctionMetadata[] _functionDataByIndex;

	private final Map<String, FunctionMetadata> _functionDataByName;

	private static FunctionMetadataRegistry getInstance() {
		if ((FunctionMetadataRegistry._instance) == null) {
		}
		return FunctionMetadataRegistry._instance;
	}

	FunctionMetadataRegistry(FunctionMetadata[] functionDataByIndex, Map<String, FunctionMetadata> functionDataByName) {
		_functionDataByIndex = (functionDataByIndex == null) ? null : functionDataByIndex.clone();
		_functionDataByName = functionDataByName;
	}

	Set<String> getAllFunctionNames() {
		return _functionDataByName.keySet();
	}

	public static FunctionMetadata getFunctionByIndex(int index) {
		return FunctionMetadataRegistry.getInstance().getFunctionByIndexInternal(index);
	}

	private FunctionMetadata getFunctionByIndexInternal(int index) {
		return _functionDataByIndex[index];
	}

	public static short lookupIndexByName(String name) {
		FunctionMetadata fd = FunctionMetadataRegistry.getInstance().getFunctionByNameInternal(name);
		if (fd == null) {
			return -1;
		}
		return ((short) (fd.getIndex()));
	}

	private FunctionMetadata getFunctionByNameInternal(String name) {
		return _functionDataByName.get(name);
	}

	public static FunctionMetadata getFunctionByName(String name) {
		return FunctionMetadataRegistry.getInstance().getFunctionByNameInternal(name);
	}
}

