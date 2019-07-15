

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.cql3.functions.AbstractFunction;
import org.apache.cassandra.cql3.functions.FunctionName;
import org.apache.cassandra.cql3.functions.UDAggregate;
import org.apache.cassandra.cql3.functions.UDFunction;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.UserType;
import org.apache.cassandra.exceptions.AlreadyExistsException;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.Functions;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.service.MigrationListener;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.WrappedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.net.MessagingService.Verb.DEFINITIONS_UPDATE;


public class MigrationManager {
	private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);

	public static final MigrationManager instance = new MigrationManager();

	private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

	public static final int MIGRATION_DELAY_IN_MS = 60000;

	private static final int MIGRATION_TASK_WAIT_IN_SECONDS = Integer.parseInt(System.getProperty("cassandra.migration_task_wait_in_seconds", "1"));

	private final List<MigrationListener> listeners = new CopyOnWriteArrayList<>();

	private MigrationManager() {
	}

	public void register(MigrationListener listener) {
		listeners.add(listener);
	}

	public void unregister(MigrationListener listener) {
		listeners.remove(listener);
	}

	public static void scheduleSchemaPull(InetAddress endpoint, EndpointState state) {
		UUID schemaVersion = state.getSchemaVersion();
		if ((!(endpoint.equals(FBUtilities.getBroadcastAddress()))) && (schemaVersion != null))
			MigrationManager.maybeScheduleSchemaPull(schemaVersion, endpoint);

	}

	private static void maybeScheduleSchemaPull(final UUID theirVersion, final InetAddress endpoint) {
		if ((Schema.instance.getVersion()) == null) {
			MigrationManager.logger.debug("Not pulling schema from {}, because local schama version is not known yet", endpoint);
			return;
		}
		if (Schema.instance.isSameVersion(theirVersion)) {
			MigrationManager.logger.debug(("Not pulling schema from {}, because schema versions match: " + "local/real={}, local/compatible={}, remote={}"), endpoint, Schema.schemaVersionToString(Schema.instance.getRealVersion()), Schema.schemaVersionToString(Schema.instance.getAltVersion()), Schema.schemaVersionToString(theirVersion));
			return;
		}
		if (!(MigrationManager.shouldPullSchemaFrom(endpoint))) {
			MigrationManager.logger.debug("Not pulling schema because versions match or shouldPullSchemaFrom returned false");
			return;
		}
		if ((Schema.instance.isEmpty()) || ((MigrationManager.runtimeMXBean.getUptime()) < (MigrationManager.MIGRATION_DELAY_IN_MS))) {
			MigrationManager.logger.debug(("Immediately submitting migration task for {}, " + "schema versions: local/real={}, local/compatible={}, remote={}"), endpoint, Schema.schemaVersionToString(Schema.instance.getRealVersion()), Schema.schemaVersionToString(Schema.instance.getAltVersion()), Schema.schemaVersionToString(theirVersion));
			MigrationManager.submitMigrationTask(endpoint);
		}else {
			Runnable runnable = () -> {
				UUID epSchemaVersion = Gossiper.instance.getSchemaVersion(endpoint);
				if (epSchemaVersion == null) {
					MigrationManager.logger.debug("epState vanished for {}, not submitting migration task", endpoint);
					return;
				}
				if (Schema.instance.isSameVersion(epSchemaVersion)) {
					MigrationManager.logger.debug("Not submitting migration task for {} because our versions match ({})", endpoint, epSchemaVersion);
					return;
				}
				MigrationManager.logger.debug("submitting migration task for {}, schema version mismatch: local/real={}, local/compatible={}, remote={}", endpoint, Schema.schemaVersionToString(Schema.instance.getRealVersion()), Schema.schemaVersionToString(Schema.instance.getAltVersion()), Schema.schemaVersionToString(epSchemaVersion));
				MigrationManager.submitMigrationTask(endpoint);
			};
			ScheduledExecutors.nonPeriodicTasks.schedule(runnable, MigrationManager.MIGRATION_DELAY_IN_MS, TimeUnit.MILLISECONDS);
		}
	}

	private static Future<?> submitMigrationTask(InetAddress endpoint) {
		return null;
	}

	public static boolean shouldPullSchemaFrom(InetAddress endpoint) {
		return ((MessagingService.instance().knowsVersion(endpoint)) && (MigrationManager.is30Compatible(MessagingService.instance().getRawVersion(endpoint)))) && (!(Gossiper.instance.isGossipOnlyMember(endpoint)));
	}

	private static boolean is30Compatible(int version) {
		return (version == (MessagingService.current_version)) || (version == (MessagingService.VERSION_3014));
	}

	public static boolean isReadyForBootstrap() {
		return false;
	}

	public static void waitUntilReadyForBootstrap() {
		CountDownLatch completionLatch;
	}

	public void notifyCreateKeyspace(KeyspaceMetadata ksm) {
		for (MigrationListener listener : listeners)
			listener.onCreateKeyspace(ksm.name);

	}

	public void notifyCreateColumnFamily(CFMetaData cfm) {
		for (MigrationListener listener : listeners)
			listener.onCreateColumnFamily(cfm.ksName, cfm.cfName);

	}

	public void notifyCreateView(ViewDefinition view) {
		for (MigrationListener listener : listeners)
			listener.onCreateView(view.ksName, view.viewName);

	}

	public void notifyCreateUserType(UserType ut) {
		for (MigrationListener listener : listeners)
			listener.onCreateUserType(ut.keyspace, ut.getNameAsString());

	}

	public void notifyCreateFunction(UDFunction udf) {
		for (MigrationListener listener : listeners)
			listener.onCreateFunction(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public void notifyCreateAggregate(UDAggregate udf) {
		for (MigrationListener listener : listeners)
			listener.onCreateAggregate(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public void notifyUpdateKeyspace(KeyspaceMetadata ksm) {
		for (MigrationListener listener : listeners)
			listener.onUpdateKeyspace(ksm.name);

	}

	public void notifyUpdateColumnFamily(CFMetaData cfm, boolean columnsDidChange) {
		for (MigrationListener listener : listeners)
			listener.onUpdateColumnFamily(cfm.ksName, cfm.cfName, columnsDidChange);

	}

	public void notifyUpdateView(ViewDefinition view, boolean columnsDidChange) {
		for (MigrationListener listener : listeners)
			listener.onUpdateView(view.ksName, view.viewName, columnsDidChange);

	}

	public void notifyUpdateUserType(UserType ut) {
		for (MigrationListener listener : listeners)
			listener.onUpdateUserType(ut.keyspace, ut.getNameAsString());

		Schema.instance.getKSMetaData(ut.keyspace).functions.udfs().forEach(( f) -> f.userTypeUpdated(ut.keyspace, ut.getNameAsString()));
	}

	public void notifyUpdateFunction(UDFunction udf) {
		for (MigrationListener listener : listeners)
			listener.onUpdateFunction(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public void notifyUpdateAggregate(UDAggregate udf) {
		for (MigrationListener listener : listeners)
			listener.onUpdateAggregate(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public void notifyDropKeyspace(KeyspaceMetadata ksm) {
		for (MigrationListener listener : listeners)
			listener.onDropKeyspace(ksm.name);

	}

	public void notifyDropColumnFamily(CFMetaData cfm) {
		for (MigrationListener listener : listeners)
			listener.onDropColumnFamily(cfm.ksName, cfm.cfName);

	}

	public void notifyDropView(ViewDefinition view) {
		for (MigrationListener listener : listeners)
			listener.onDropView(view.ksName, view.viewName);

	}

	public void notifyDropUserType(UserType ut) {
		for (MigrationListener listener : listeners)
			listener.onDropUserType(ut.keyspace, ut.getNameAsString());

	}

	public void notifyDropFunction(UDFunction udf) {
		for (MigrationListener listener : listeners)
			listener.onDropFunction(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public void notifyDropAggregate(UDAggregate udf) {
		for (MigrationListener listener : listeners)
			listener.onDropAggregate(udf.name().keyspace, udf.name().name, udf.argTypes());

	}

	public static void announceNewKeyspace(KeyspaceMetadata ksm) throws ConfigurationException {
		MigrationManager.announceNewKeyspace(ksm, false);
	}

	public static void announceNewKeyspace(KeyspaceMetadata ksm, boolean announceLocally) throws ConfigurationException {
		MigrationManager.announceNewKeyspace(ksm, FBUtilities.timestampMicros(), announceLocally);
	}

	public static void announceNewKeyspace(KeyspaceMetadata ksm, long timestamp, boolean announceLocally) throws ConfigurationException {
		ksm.validate();
		if ((Schema.instance.getKSMetaData(ksm.name)) != null)
			throw new AlreadyExistsException(ksm.name);

		MigrationManager.logger.info("Create new Keyspace: {}", ksm);
		MigrationManager.announce(SchemaKeyspace.makeCreateKeyspaceMutation(ksm, timestamp), announceLocally);
	}

	public static void announceNewColumnFamily(CFMetaData cfm) throws ConfigurationException {
		MigrationManager.announceNewColumnFamily(cfm, false);
	}

	public static void announceNewColumnFamily(CFMetaData cfm, boolean announceLocally) throws ConfigurationException {
		MigrationManager.announceNewColumnFamily(cfm, announceLocally, true);
	}

	public static void forceAnnounceNewColumnFamily(CFMetaData cfm) throws ConfigurationException {
		MigrationManager.announceNewColumnFamily(cfm, false, false, 0);
	}

	private static void announceNewColumnFamily(CFMetaData cfm, boolean announceLocally, boolean throwOnDuplicate) throws ConfigurationException {
		MigrationManager.announceNewColumnFamily(cfm, announceLocally, throwOnDuplicate, FBUtilities.timestampMicros());
	}

	private static void announceNewColumnFamily(CFMetaData cfm, boolean announceLocally, boolean throwOnDuplicate, long timestamp) throws ConfigurationException {
		cfm.validate();
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(cfm.ksName);
		if (ksm == null)
			throw new ConfigurationException(String.format("Cannot add table '%s' to non existing keyspace '%s'.", cfm.cfName, cfm.ksName));
		else
			if (throwOnDuplicate && ((ksm.getTableOrViewNullable(cfm.cfName)) != null))
				throw new AlreadyExistsException(cfm.ksName, cfm.cfName);


		MigrationManager.logger.info("Create new table: {}", cfm);
		MigrationManager.announce(SchemaKeyspace.makeCreateTableMutation(ksm, cfm, timestamp), announceLocally);
	}

	public static void announceNewView(ViewDefinition view, boolean announceLocally) throws ConfigurationException {
		view.metadata.validate();
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(view.ksName);
		if (ksm == null)
			throw new ConfigurationException(String.format("Cannot add table '%s' to non existing keyspace '%s'.", view.viewName, view.ksName));
		else
			if ((ksm.getTableOrViewNullable(view.viewName)) != null)
				throw new AlreadyExistsException(view.ksName, view.viewName);


		MigrationManager.logger.info("Create new view: {}", view);
		MigrationManager.announce(SchemaKeyspace.makeCreateViewMutation(ksm, view, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceNewType(UserType newType, boolean announceLocally) {
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(newType.keyspace);
		MigrationManager.announce(SchemaKeyspace.makeCreateTypeMutation(ksm, newType, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceNewFunction(UDFunction udf, boolean announceLocally) {
		MigrationManager.logger.info("Create scalar function '{}'", udf.name());
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(udf.name().keyspace);
		MigrationManager.announce(SchemaKeyspace.makeCreateFunctionMutation(ksm, udf, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceNewAggregate(UDAggregate udf, boolean announceLocally) {
		MigrationManager.logger.info("Create aggregate function '{}'", udf.name());
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(udf.name().keyspace);
		MigrationManager.announce(SchemaKeyspace.makeCreateAggregateMutation(ksm, udf, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceKeyspaceUpdate(KeyspaceMetadata ksm) throws ConfigurationException {
		MigrationManager.announceKeyspaceUpdate(ksm, false);
	}

	public static void announceKeyspaceUpdate(KeyspaceMetadata ksm, boolean announceLocally) throws ConfigurationException {
		ksm.validate();
		KeyspaceMetadata oldKsm = Schema.instance.getKSMetaData(ksm.name);
		if (oldKsm == null)
			throw new ConfigurationException(String.format("Cannot update non existing keyspace '%s'.", ksm.name));

		MigrationManager.logger.info("Update Keyspace '{}' From {} To {}", ksm.name, oldKsm, ksm);
		MigrationManager.announce(SchemaKeyspace.makeCreateKeyspaceMutation(ksm.name, ksm.params, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceColumnFamilyUpdate(CFMetaData cfm) throws ConfigurationException {
		MigrationManager.announceColumnFamilyUpdate(cfm, false);
	}

	public static void announceColumnFamilyUpdate(CFMetaData cfm, boolean announceLocally) throws ConfigurationException {
		MigrationManager.announceColumnFamilyUpdate(cfm, null, announceLocally);
	}

	public static void announceColumnFamilyUpdate(CFMetaData cfm, Collection<ViewDefinition> views, boolean announceLocally) throws ConfigurationException {
		cfm.validate();
		CFMetaData oldCfm = Schema.instance.getCFMetaData(cfm.ksName, cfm.cfName);
		if (oldCfm == null)
			throw new ConfigurationException(String.format("Cannot update non existing table '%s' in keyspace '%s'.", cfm.cfName, cfm.ksName));

		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(cfm.ksName);
		oldCfm.validateCompatibility(cfm);
		long timestamp = FBUtilities.timestampMicros();
		MigrationManager.logger.info("Update table '{}/{}' From {} To {}", cfm.ksName, cfm.cfName, oldCfm, cfm);
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeUpdateTableMutation(ksm, oldCfm, cfm, timestamp);
		if (views != null)
			views.forEach(( view) -> MigrationManager.addViewUpdateToMutationBuilder(view, builder));

		MigrationManager.announce(builder, announceLocally);
	}

	public static void announceViewUpdate(ViewDefinition view, boolean announceLocally) throws ConfigurationException {
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(view.ksName);
		long timestamp = FBUtilities.timestampMicros();
		Mutation.SimpleBuilder builder = SchemaKeyspace.makeCreateKeyspaceMutation(ksm.name, ksm.params, timestamp);
		MigrationManager.addViewUpdateToMutationBuilder(view, builder);
		MigrationManager.announce(builder, announceLocally);
	}

	private static void addViewUpdateToMutationBuilder(ViewDefinition view, Mutation.SimpleBuilder builder) {
		view.metadata.validate();
		ViewDefinition oldView = Schema.instance.getView(view.ksName, view.viewName);
		if (oldView == null)
			throw new ConfigurationException(String.format("Cannot update non existing materialized view '%s' in keyspace '%s'.", view.viewName, view.ksName));

		oldView.metadata.validateCompatibility(view.metadata);
		MigrationManager.logger.info("Update view '{}/{}' From {} To {}", view.ksName, view.viewName, oldView, view);
		SchemaKeyspace.makeUpdateViewMutation(builder, oldView, view);
	}

	public static void announceTypeUpdate(UserType updatedType, boolean announceLocally) {
		MigrationManager.logger.info("Update type '{}.{}' to {}", updatedType.keyspace, updatedType.getNameAsString(), updatedType);
		MigrationManager.announceNewType(updatedType, announceLocally);
	}

	public static void announceKeyspaceDrop(String ksName) throws ConfigurationException {
		MigrationManager.announceKeyspaceDrop(ksName, false);
	}

	public static void announceKeyspaceDrop(String ksName, boolean announceLocally) throws ConfigurationException {
		KeyspaceMetadata oldKsm = Schema.instance.getKSMetaData(ksName);
		if (oldKsm == null)
			throw new ConfigurationException(String.format("Cannot drop non existing keyspace '%s'.", ksName));

		MigrationManager.logger.info("Drop Keyspace '{}'", oldKsm.name);
		MigrationManager.announce(SchemaKeyspace.makeDropKeyspaceMutation(oldKsm, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceColumnFamilyDrop(String ksName, String cfName) throws ConfigurationException {
		MigrationManager.announceColumnFamilyDrop(ksName, cfName, false);
	}

	public static void announceColumnFamilyDrop(String ksName, String cfName, boolean announceLocally) throws ConfigurationException {
		CFMetaData oldCfm = Schema.instance.getCFMetaData(ksName, cfName);
		if (oldCfm == null)
			throw new ConfigurationException(String.format("Cannot drop non existing table '%s' in keyspace '%s'.", cfName, ksName));

		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(ksName);
		MigrationManager.logger.info("Drop table '{}/{}'", oldCfm.ksName, oldCfm.cfName);
		MigrationManager.announce(SchemaKeyspace.makeDropTableMutation(ksm, oldCfm, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceViewDrop(String ksName, String viewName, boolean announceLocally) throws ConfigurationException {
		ViewDefinition view = Schema.instance.getView(ksName, viewName);
		if (view == null)
			throw new ConfigurationException(String.format("Cannot drop non existing materialized view '%s' in keyspace '%s'.", viewName, ksName));

		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(ksName);
		MigrationManager.logger.info("Drop table '{}/{}'", view.ksName, view.viewName);
		MigrationManager.announce(SchemaKeyspace.makeDropViewMutation(ksm, view, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceTypeDrop(UserType droppedType) {
		MigrationManager.announceTypeDrop(droppedType, false);
	}

	public static void announceTypeDrop(UserType droppedType, boolean announceLocally) {
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(droppedType.keyspace);
		MigrationManager.announce(SchemaKeyspace.dropTypeFromSchemaMutation(ksm, droppedType, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceFunctionDrop(UDFunction udf, boolean announceLocally) {
		MigrationManager.logger.info("Drop scalar function overload '{}' args '{}'", udf.name(), udf.argTypes());
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(udf.name().keyspace);
		MigrationManager.announce(SchemaKeyspace.makeDropFunctionMutation(ksm, udf, FBUtilities.timestampMicros()), announceLocally);
	}

	public static void announceAggregateDrop(UDAggregate udf, boolean announceLocally) {
		MigrationManager.logger.info("Drop aggregate function overload '{}' args '{}'", udf.name(), udf.argTypes());
		KeyspaceMetadata ksm = Schema.instance.getKSMetaData(udf.name().keyspace);
		MigrationManager.announce(SchemaKeyspace.makeDropAggregateMutation(ksm, udf, FBUtilities.timestampMicros()), announceLocally);
	}

	private static void announce(Mutation.SimpleBuilder schema, boolean announceLocally) {
		List<Mutation> mutations = Collections.singletonList(schema.build());
		if (announceLocally)
			SchemaKeyspace.mergeSchema(mutations);
		else
			FBUtilities.waitOnFuture(MigrationManager.announce(mutations));

	}

	private static void pushSchemaMutation(InetAddress endpoint, Collection<Mutation> schema) {
		MessageOut<Collection<Mutation>> msg = new MessageOut<>(DEFINITIONS_UPDATE, schema, MigrationManager.MigrationsSerializer.instance);
		MessagingService.instance().sendOneWay(msg, endpoint);
	}

	private static Future<?> announce(final Collection<Mutation> schema) {
		Future<?> f = StageManager.getStage(Stage.MIGRATION).submit(new WrappedRunnable() {
			protected void runMayThrow() throws ConfigurationException {
				SchemaKeyspace.mergeSchemaAndAnnounceVersion(schema);
			}
		});
		for (InetAddress endpoint : Gossiper.instance.getLiveMembers()) {
			if (((!(endpoint.equals(FBUtilities.getBroadcastAddress()))) && (MessagingService.instance().knowsVersion(endpoint))) && (MigrationManager.is30Compatible(MessagingService.instance().getRawVersion(endpoint))))
				MigrationManager.pushSchemaMutation(endpoint, schema);

		}
		return f;
	}

	public static void passiveAnnounce(UUID version, boolean compatible) {
		Gossiper.instance.addLocalApplicationState(ApplicationState.SCHEMA, StorageService.instance.valueFactory.schema(version));
		MigrationManager.logger.debug("Gossiping my {} schema version {}", (compatible ? "3.0 compatible" : "3.11"), Schema.schemaVersionToString(version));
	}

	public static void resetLocalSchema() {
		MigrationManager.logger.info("Starting local schema reset...");
		MigrationManager.logger.debug("Truncating schema tables...");
		SchemaKeyspace.truncate();
		MigrationManager.logger.debug("Clearing local schema keyspace definitions...");
		Schema.instance.clear();
		Set<InetAddress> liveEndpoints = Gossiper.instance.getLiveMembers();
		liveEndpoints.remove(FBUtilities.getBroadcastAddress());
		for (InetAddress node : liveEndpoints) {
			if (MigrationManager.shouldPullSchemaFrom(node)) {
				MigrationManager.logger.debug("Requesting schema from {}", node);
				FBUtilities.waitOnFuture(MigrationManager.submitMigrationTask(node));
				break;
			}
		}
		MigrationManager.logger.info("Local schema reset is complete.");
	}

	public static class MigrationsSerializer implements IVersionedSerializer<Collection<Mutation>> {
		public static MigrationManager.MigrationsSerializer instance = new MigrationManager.MigrationsSerializer();

		public void serialize(Collection<Mutation> schema, DataOutputPlus out, int version) throws IOException {
			out.writeInt(schema.size());
			for (Mutation mutation : schema)
				Mutation.serializer.serialize(mutation, out, version);

		}

		public Collection<Mutation> deserialize(DataInputPlus in, int version) throws IOException {
			int count = in.readInt();
			Collection<Mutation> schema = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				schema.add(Mutation.serializer.deserialize(in, version));

			return schema;
		}

		public long serializedSize(Collection<Mutation> schema, int version) {
			int size = TypeSizes.sizeof(schema.size());
			for (Mutation mutation : schema)
				size += Mutation.serializer.serializedSize(mutation, version);

			return size;
		}
	}
}

