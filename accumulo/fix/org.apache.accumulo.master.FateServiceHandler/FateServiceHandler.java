

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.TableOperationsImpl;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.master.thrift.BulkImportState;
import org.apache.accumulo.core.master.thrift.FateOperation;
import org.apache.accumulo.core.master.thrift.FateService;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.ByteBufferUtil;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.Validator;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.util.TableValidators;
import org.apache.accumulo.server.client.ClientServiceHandler;
import org.apache.accumulo.server.util.TablePropUtil;
import org.apache.hadoop.io.Text;


class FateServiceHandler implements FateService.Iface {
	protected final Master master;

	public FateServiceHandler(Master master) {
		this.master = master;
	}

	@Override
	public long beginFateOperation(TInfo tinfo, TCredentials credentials) throws ThriftSecurityException {
		authenticate(credentials);
		return 0L;
	}

	@Override
	public void executeFateOperation(TInfo tinfo, TCredentials c, long opid, FateOperation op, List<ByteBuffer> arguments, Map<String, String> options, boolean autoCleanup) throws ThriftSecurityException, ThriftTableOperationException {
		authenticate(c);
		switch (op) {
			case NAMESPACE_CREATE :
				{
					TableOperation tableOp = TableOperation.CREATE;
					String namespace = validateNamespaceArgument(arguments.get(0), tableOp, null);
					break;
				}
			case NAMESPACE_RENAME :
				{
					TableOperation tableOp = TableOperation.RENAME;
					String oldName = validateNamespaceArgument(arguments.get(0), tableOp, Namespaces.NOT_DEFAULT.and(Namespaces.NOT_ACCUMULO));
					String newName = validateNamespaceArgument(arguments.get(1), tableOp, null);
					String namespaceId = ClientServiceHandler.checkNamespaceId(master.getInstance(), oldName, tableOp);
					break;
				}
			case NAMESPACE_DELETE :
				{
					TableOperation tableOp = TableOperation.DELETE;
					String namespace = validateNamespaceArgument(arguments.get(0), tableOp, Namespaces.NOT_DEFAULT.and(Namespaces.NOT_ACCUMULO));
					String namespaceId = ClientServiceHandler.checkNamespaceId(master.getInstance(), namespace, tableOp);
					break;
				}
			case TABLE_CREATE :
				{
					TableOperation tableOp = TableOperation.CREATE;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					TimeType timeType = TimeType.valueOf(ByteBufferUtil.toString(arguments.get(1)));
					String namespaceId;
					try {
						namespaceId = Namespaces.getNamespaceId(master.getInstance(), Tables.qualify(tableName).getFirst());
					} catch (NamespaceNotFoundException e) {
						throw new ThriftTableOperationException(null, tableName, tableOp, TableOperationExceptionType.NAMESPACE_NOTFOUND, "");
					}
					break;
				}
			case TABLE_RENAME :
				{
					TableOperation tableOp = TableOperation.RENAME;
					final String oldTableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					String newTableName = validateTableNameArgument(arguments.get(1), tableOp, new Validator<String>() {
						@Override
						public boolean apply(String argument) {
							String oldNamespace = Tables.qualify(oldTableName).getFirst();
							return oldNamespace.equals(Tables.qualify(argument).getFirst());
						}

						@Override
						public String invalidMessage(String argument) {
							return (("Cannot move tables to a new namespace by renaming. The namespace for " + oldTableName) + " does not match ") + argument;
						}
					});
					String tableId = ClientServiceHandler.checkTableId(master.getInstance(), oldTableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canRename;
					canRename = false;
					if (!canRename)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_CLONE :
				{
					TableOperation tableOp = TableOperation.CLONE;
					String srcTableId = validateTableIdArgument(arguments.get(0), tableOp, TableValidators.NOT_ROOT_ID);
					String tableName = validateTableNameArgument(arguments.get(1), tableOp, TableValidators.NOT_SYSTEM);
					String namespaceId;
					try {
						namespaceId = Namespaces.getNamespaceId(master.getInstance(), Tables.qualify(tableName).getFirst());
					} catch (NamespaceNotFoundException e) {
						throw new ThriftTableOperationException(null, tableName, tableOp, TableOperationExceptionType.NAMESPACE_NOTFOUND, "");
					}
					final boolean canCloneTable;
					canCloneTable = false;
					if (!canCloneTable)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					Map<String, String> propertiesToSet = new HashMap<>();
					Set<String> propertiesToExclude = new HashSet<>();
					for (Map.Entry<String, String> entry : options.entrySet()) {
						if (entry.getKey().startsWith(TableOperationsImpl.CLONE_EXCLUDE_PREFIX)) {
							propertiesToExclude.add(entry.getKey().substring(TableOperationsImpl.CLONE_EXCLUDE_PREFIX.length()));
							continue;
						}
						if (!(TablePropUtil.isPropertyValid(entry.getKey(), entry.getValue()))) {
							throw new ThriftTableOperationException(null, tableName, tableOp, TableOperationExceptionType.OTHER, ((("Property or value not valid " + (entry.getKey())) + "=") + (entry.getValue())));
						}
						propertiesToSet.put(entry.getKey(), entry.getValue());
					}
					break;
				}
			case TABLE_DELETE :
				{
					TableOperation tableOp = TableOperation.DELETE;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					final String tableId = ClientServiceHandler.checkTableId(master.getInstance(), tableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canDeleteTable;
					canDeleteTable = false;
					if (!canDeleteTable)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_ONLINE :
				{
					TableOperation tableOp = TableOperation.ONLINE;
					final String tableId = validateTableIdArgument(arguments.get(0), tableOp, TableValidators.NOT_ROOT_ID);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canOnlineOfflineTable;
					canOnlineOfflineTable = false;
					if (!canOnlineOfflineTable)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_OFFLINE :
				{
					TableOperation tableOp = TableOperation.OFFLINE;
					final String tableId = validateTableIdArgument(arguments.get(0), tableOp, TableValidators.NOT_ROOT_ID);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canOnlineOfflineTable;
					canOnlineOfflineTable = false;
					if (!canOnlineOfflineTable)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_MERGE :
				{
					TableOperation tableOp = TableOperation.MERGE;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, null);
					Text startRow = ByteBufferUtil.toText(arguments.get(1));
					Text endRow = ByteBufferUtil.toText(arguments.get(2));
					final String tableId = ClientServiceHandler.checkTableId(master.getInstance(), tableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canMerge;
					canMerge = false;
					if (!canMerge)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_DELETE_RANGE :
				{
					TableOperation tableOp = TableOperation.DELETE_RANGE;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_METADATA);
					Text startRow = ByteBufferUtil.toText(arguments.get(1));
					Text endRow = ByteBufferUtil.toText(arguments.get(2));
					final String tableId = ClientServiceHandler.checkTableId(master.getInstance(), tableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canDeleteRange;
					canDeleteRange = false;
					if (!canDeleteRange)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_BULK_IMPORT :
				{
					TableOperation tableOp = TableOperation.BULK_IMPORT;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					String dir = ByteBufferUtil.toString(arguments.get(1));
					String failDir = ByteBufferUtil.toString(arguments.get(2));
					boolean setTime = Boolean.parseBoolean(ByteBufferUtil.toString(arguments.get(3)));
					final String tableId = ClientServiceHandler.checkTableId(master.getInstance(), tableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canBulkImport;
					canBulkImport = false;
					if (!canBulkImport)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					master.updateBulkImportStatus(dir, BulkImportState.INITIAL);
					break;
				}
			case TABLE_COMPACT :
				{
					TableOperation tableOp = TableOperation.COMPACT;
					String tableId = validateTableIdArgument(arguments.get(0), tableOp, null);
					byte[] startRow = ByteBufferUtil.toBytes(arguments.get(1));
					byte[] endRow = ByteBufferUtil.toBytes(arguments.get(2));
					List<IteratorSetting> iterators = IteratorUtil.decodeIteratorSettings(ByteBufferUtil.toBytes(arguments.get(3)));
					CompactionStrategyConfig compactionStrategy = CompactionStrategyConfigUtil.decode(ByteBufferUtil.toBytes(arguments.get(4)));
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canCompact;
					canCompact = false;
					if (!canCompact)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_CANCEL_COMPACT :
				{
					TableOperation tableOp = TableOperation.COMPACT_CANCEL;
					String tableId = validateTableIdArgument(arguments.get(0), tableOp, null);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canCancelCompact;
					canCancelCompact = false;
					if (!canCancelCompact)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_IMPORT :
				{
					TableOperation tableOp = TableOperation.IMPORT;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					String exportDir = ByteBufferUtil.toString(arguments.get(1));
					String namespaceId;
					try {
						namespaceId = Namespaces.getNamespaceId(master.getInstance(), Tables.qualify(tableName).getFirst());
					} catch (NamespaceNotFoundException e) {
						throw new ThriftTableOperationException(null, tableName, tableOp, TableOperationExceptionType.NAMESPACE_NOTFOUND, "");
					}
					final boolean canImport;
					canImport = false;
					if (!canImport)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			case TABLE_EXPORT :
				{
					TableOperation tableOp = TableOperation.EXPORT;
					String tableName = validateTableNameArgument(arguments.get(0), tableOp, TableValidators.NOT_SYSTEM);
					String exportDir = ByteBufferUtil.toString(arguments.get(1));
					String tableId = ClientServiceHandler.checkTableId(master.getInstance(), tableName, tableOp);
					String namespaceId = getNamespaceIdFromTableId(tableOp, tableId);
					final boolean canExport;
					canExport = false;
					if (!canExport)
						throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

					break;
				}
			default :
				throw new UnsupportedOperationException();
		}
	}

	private String getNamespaceIdFromTableId(TableOperation tableOp, String tableId) throws ThriftTableOperationException {
		String namespaceId;
		try {
			namespaceId = Tables.getNamespaceId(master.getInstance(), tableId);
		} catch (TableNotFoundException e) {
			throw new ThriftTableOperationException(tableId, null, tableOp, TableOperationExceptionType.NOTFOUND, e.getMessage());
		}
		return namespaceId;
	}

	private void throwIfTableMissingSecurityException(ThriftSecurityException e, String tableId, String tableName, TableOperation op) throws ThriftTableOperationException {
		if ((e.isSetCode()) && ((SecurityErrorCode.TABLE_DOESNT_EXIST) == (e.getCode()))) {
			throw new ThriftTableOperationException(tableId, tableName, op, TableOperationExceptionType.NOTFOUND, "Table no longer exists");
		}
	}

	@Override
	public String waitForFateOperation(TInfo tinfo, TCredentials credentials, long opid) throws ThriftSecurityException, ThriftTableOperationException {
		authenticate(credentials);
		return null;
	}

	@Override
	public void finishFateOperation(TInfo tinfo, TCredentials credentials, long opid) throws ThriftSecurityException {
		authenticate(credentials);
	}

	protected void authenticate(TCredentials credentials) throws ThriftSecurityException {
	}

	private String validateTableIdArgument(ByteBuffer tableIdArg, TableOperation op, Validator<String> userValidator) throws ThriftTableOperationException {
		String tableId = (tableIdArg == null) ? null : ByteBufferUtil.toString(tableIdArg);
		try {
			return TableValidators.VALID_ID.and(userValidator).validate(tableId);
		} catch (IllegalArgumentException e) {
			String why = e.getMessage();
			throw new ThriftTableOperationException(tableId, null, op, TableOperationExceptionType.INVALID_NAME, why);
		}
	}

	private String validateTableNameArgument(ByteBuffer tableNameArg, TableOperation op, Validator<String> userValidator) throws ThriftTableOperationException {
		String tableName = (tableNameArg == null) ? null : ByteBufferUtil.toString(tableNameArg);
		return _validateArgument(tableName, op, TableValidators.VALID_NAME.and(userValidator));
	}

	private String validateNamespaceArgument(ByteBuffer namespaceArg, TableOperation op, Validator<String> userValidator) throws ThriftTableOperationException {
		String namespace = (namespaceArg == null) ? null : ByteBufferUtil.toString(namespaceArg);
		return _validateArgument(namespace, op, Namespaces.VALID_NAME.and(userValidator));
	}

	private <T> T _validateArgument(T arg, TableOperation op, Validator<T> validator) throws ThriftTableOperationException {
		try {
			return validator.validate(arg);
		} catch (IllegalArgumentException e) {
			String why = e.getMessage();
			throw new ThriftTableOperationException(null, String.valueOf(arg), op, TableOperationExceptionType.INVALID_NAME, why);
		}
	}
}

