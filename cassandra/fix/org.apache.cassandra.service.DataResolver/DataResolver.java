

import com.codahale.metrics.Meter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadQuery;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterators;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowDiffListener;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.db.transform.EmptyPartitionsDiscarder;
import org.apache.cassandra.db.transform.Filter;
import org.apache.cassandra.db.transform.FilteredPartitions;
import org.apache.cassandra.db.transform.MorePartitions;
import org.apache.cassandra.db.transform.MoreRows;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.ExcludingBounds;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.exceptions.ReadTimeoutException;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.AsyncOneResponse;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.service.ReadCallback;
import org.apache.cassandra.service.ResponseResolver;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.concurrent.Accumulator;
import org.slf4j.Logger;

import static org.apache.cassandra.net.MessagingService.Verb.READ_REPAIR;


public class DataResolver extends ResponseResolver {
	private static final boolean DROP_OVERSIZED_READ_REPAIR_MUTATIONS = Boolean.getBoolean("cassandra.drop_oversized_readrepair_mutations");

	@VisibleForTesting
	final List<AsyncOneResponse> repairResults = Collections.synchronizedList(new ArrayList<>());

	private final long queryStartNanoTime;

	private final boolean enforceStrictLiveness;

	DataResolver(Keyspace keyspace, ReadCommand command, ConsistencyLevel consistency, int maxResponseCount, long queryStartNanoTime) {
		super(keyspace, command, consistency, maxResponseCount);
		this.queryStartNanoTime = queryStartNanoTime;
		this.enforceStrictLiveness = command.metadata().enforceStrictLiveness();
	}

	public PartitionIterator getData() {
		ReadResponse response = responses.iterator().next().payload;
		return UnfilteredPartitionIterators.filter(response.makeIterator(command), command.nowInSec());
	}

	public boolean isDataPresent() {
		return !(responses.isEmpty());
	}

	public void compareResponses() {
		try (PartitionIterator iterator = resolve()) {
			PartitionIterators.consume(iterator);
		}
	}

	public PartitionIterator resolve() {
		int count = responses.size();
		List<UnfilteredPartitionIterator> iters = new ArrayList<>(count);
		InetAddress[] sources = new InetAddress[count];
		for (int i = 0; i < count; i++) {
			MessageIn<ReadResponse> msg = responses.get(i);
			iters.add(msg.payload.makeIterator(command));
			sources[i] = msg.from;
		}
		DataLimits.Counter mergedResultCounter = command.limits().newCounter(command.nowInSec(), true, command.selectsFullPartition(), enforceStrictLiveness);
		UnfilteredPartitionIterator merged = mergeWithShortReadProtection(iters, sources, mergedResultCounter);
		FilteredPartitions filtered = FilteredPartitions.filter(merged, new Filter(command.nowInSec(), command.metadata().enforceStrictLiveness()));
		PartitionIterator counted = Transformation.apply(filtered, mergedResultCounter);
		return command.isForThrift() ? counted : Transformation.apply(counted, new EmptyPartitionsDiscarder());
	}

	private UnfilteredPartitionIterator mergeWithShortReadProtection(List<UnfilteredPartitionIterator> results, InetAddress[] sources, DataLimits.Counter mergedResultCounter) {
		if ((results.size()) == 1)
			return results.get(0);

		if (!(command.limits().isUnlimited()))
			for (int i = 0; i < (results.size()); i++)
				results.set(i, extendWithShortReadProtection(results.get(i), sources[i], mergedResultCounter));


		return UnfilteredPartitionIterators.merge(results, command.nowInSec(), new DataResolver.RepairMergeListener(sources));
	}

	private class RepairMergeListener implements UnfilteredPartitionIterators.MergeListener {
		private final InetAddress[] sources;

		private RepairMergeListener(InetAddress[] sources) {
			this.sources = sources;
		}

		public UnfilteredRowIterators.MergeListener getRowMergeListener(DecoratedKey partitionKey, List<UnfilteredRowIterator> versions) {
			return new DataResolver.RepairMergeListener.MergeListener(partitionKey, columns(versions), isReversed(versions));
		}

		private PartitionColumns columns(List<UnfilteredRowIterator> versions) {
			Columns statics = Columns.NONE;
			Columns regulars = Columns.NONE;
			for (UnfilteredRowIterator iter : versions) {
				if (iter == null)
					continue;

				PartitionColumns cols = iter.columns();
				statics = statics.mergeTo(cols.statics);
				regulars = regulars.mergeTo(cols.regulars);
			}
			return new PartitionColumns(statics, regulars);
		}

		private boolean isReversed(List<UnfilteredRowIterator> versions) {
			for (UnfilteredRowIterator iter : versions) {
				if (iter == null)
					continue;

				return iter.isReverseOrder();
			}
			assert false : "Expected at least one iterator";
			return false;
		}

		public void close() {
			try {
				FBUtilities.waitOnFutures(repairResults, DatabaseDescriptor.getWriteRpcTimeout());
			} catch (TimeoutException ex) {
				int blockFor = consistency.blockFor(keyspace);
				if (Tracing.isTracing())
					Tracing.trace("Timed out while read-repairing after receiving all {} data and digest responses", blockFor);
				else
					ResponseResolver.logger.debug("Timeout while read-repairing after receiving all {} data and digest responses", blockFor);

				throw new ReadTimeoutException(consistency, (blockFor - 1), blockFor, true);
			}
		}

		private class MergeListener implements UnfilteredRowIterators.MergeListener {
			private final DecoratedKey partitionKey;

			private final PartitionColumns columns;

			private final boolean isReversed;

			private final PartitionUpdate[] repairs = new PartitionUpdate[sources.length];

			private final Row.Builder[] currentRows = new Row.Builder[sources.length];

			private final RowDiffListener diffListener;

			private DeletionTime partitionLevelDeletion;

			private DeletionTime mergedDeletionTime;

			private final DeletionTime[] sourceDeletionTime = new DeletionTime[sources.length];

			private final ClusteringBound[] markerToRepair = new ClusteringBound[sources.length];

			private MergeListener(DecoratedKey partitionKey, PartitionColumns columns, boolean isReversed) {
				this.partitionKey = partitionKey;
				this.columns = columns;
				this.isReversed = isReversed;
				this.diffListener = new RowDiffListener() {
					public void onPrimaryKeyLivenessInfo(int i, Clustering clustering, LivenessInfo merged, LivenessInfo original) {
						if ((merged != null) && (!(merged.equals(original))))
							currentRow(i, clustering).addPrimaryKeyLivenessInfo(merged);

					}

					public void onDeletion(int i, Clustering clustering, Row.Deletion merged, Row.Deletion original) {
						if ((merged != null) && (!(merged.equals(original))))
							currentRow(i, clustering).addRowDeletion(merged);

					}

					public void onComplexDeletion(int i, Clustering clustering, ColumnDefinition column, DeletionTime merged, DeletionTime original) {
						if ((merged != null) && (!(merged.equals(original))))
							currentRow(i, clustering).addComplexDeletion(column, merged);

					}

					public void onCell(int i, Clustering clustering, Cell merged, Cell original) {
						if (((merged != null) && (!(merged.equals(original)))) && (isQueried(merged)))
							currentRow(i, clustering).addCell(merged);

					}

					private boolean isQueried(Cell cell) {
						ColumnDefinition column = cell.column();
						ColumnFilter filter = command.columnFilter();
						return column.isComplex() ? filter.fetchedCellIsQueried(column, cell.path()) : filter.fetchedColumnIsQueried(column);
					}
				};
			}

			private PartitionUpdate update(int i) {
				if ((repairs[i]) == null)
					repairs[i] = new PartitionUpdate(command.metadata(), partitionKey, columns, 1);

				return repairs[i];
			}

			private DeletionTime partitionLevelRepairDeletion(int i) {
				return (repairs[i]) == null ? DeletionTime.LIVE : repairs[i].partitionLevelDeletion();
			}

			private Row.Builder currentRow(int i, Clustering clustering) {
				if ((currentRows[i]) == null) {
					currentRows[i] = BTreeRow.sortedBuilder();
					currentRows[i].newRow(clustering);
				}
				return currentRows[i];
			}

			public void onMergedPartitionLevelDeletion(DeletionTime mergedDeletion, DeletionTime[] versions) {
				this.partitionLevelDeletion = mergedDeletion;
				for (int i = 0; i < (versions.length); i++) {
					if (mergedDeletion.supersedes(versions[i]))
						update(i).addPartitionDeletion(mergedDeletion);

				}
			}

			public void onMergedRows(Row merged, Row[] versions) {
				if (merged.isEmpty())
					return;

				Rows.diff(diffListener, merged, versions);
				for (int i = 0; i < (currentRows.length); i++) {
					if ((currentRows[i]) != null)
						update(i).add(currentRows[i].build());

				}
				Arrays.fill(currentRows, null);
			}

			private DeletionTime currentDeletion() {
				return (mergedDeletionTime) == null ? partitionLevelDeletion : mergedDeletionTime;
			}

			public void onMergedRangeTombstoneMarkers(RangeTombstoneMarker merged, RangeTombstoneMarker[] versions) {
				try {
					internalOnMergedRangeTombstoneMarkers(merged, versions);
				} catch (AssertionError e) {
					CFMetaData table = command.metadata();
					String details = String.format("Error merging RTs on %s.%s: merged=%s, versions=%s, sources={%s}, responses:%n %s", table.ksName, table.cfName, (merged == null ? "null" : merged.toString(table)), (('[' + (Joiner.on(", ").join(Iterables.transform(Arrays.asList(versions), ( rt) -> rt == null ? "null" : rt.toString(table))))) + ']'), Arrays.toString(sources), makeResponsesDebugString());
					throw new AssertionError(details, e);
				}
			}

			private String makeResponsesDebugString() {
				return Joiner.on(",\n").join(Iterables.transform(getMessages(), ( m) -> ((m.from) + " => ") + (m.payload.toDebugString(command, partitionKey))));
			}

			private void internalOnMergedRangeTombstoneMarkers(RangeTombstoneMarker merged, RangeTombstoneMarker[] versions) {
				DeletionTime currentDeletion = currentDeletion();
				for (int i = 0; i < (versions.length); i++) {
					RangeTombstoneMarker marker = versions[i];
					if (marker != null)
						sourceDeletionTime[i] = (marker.isOpen(isReversed)) ? marker.openDeletionTime(isReversed) : null;

					if (merged == null) {
						if (marker == null)
							continue;

						assert !(currentDeletion.isLive()) : currentDeletion.toString();
						DeletionTime partitionRepairDeletion = partitionLevelRepairDeletion(i);
						if (((markerToRepair[i]) == null) && (currentDeletion.supersedes(partitionRepairDeletion))) {
							assert (marker.isClose(isReversed)) && (currentDeletion.equals(marker.closeDeletionTime(isReversed))) : String.format("currentDeletion=%s, marker=%s", currentDeletion, marker.toString(command.metadata()));
							if (!((marker.isOpen(isReversed)) && (currentDeletion.equals(marker.openDeletionTime(isReversed)))))
								markerToRepair[i] = marker.closeBound(isReversed).invert();

						}else
							if ((marker.isOpen(isReversed)) && (currentDeletion.equals(marker.openDeletionTime(isReversed)))) {
								closeOpenMarker(i, marker.openBound(isReversed).invert());
							}

					}else {
						if (merged.isClose(isReversed)) {
							if ((markerToRepair[i]) != null)
								closeOpenMarker(i, merged.closeBound(isReversed));

						}
						if (merged.isOpen(isReversed)) {
							DeletionTime newDeletion = merged.openDeletionTime(isReversed);
							DeletionTime sourceDeletion = sourceDeletionTime[i];
							if (!(newDeletion.equals(sourceDeletion)))
								markerToRepair[i] = merged.openBound(isReversed);

						}
					}
				}
				if (merged != null)
					mergedDeletionTime = (merged.isOpen(isReversed)) ? merged.openDeletionTime(isReversed) : null;

			}

			private void closeOpenMarker(int i, ClusteringBound close) {
				ClusteringBound open = markerToRepair[i];
				update(i).add(new RangeTombstone(Slice.make((isReversed ? close : open), (isReversed ? open : close)), currentDeletion()));
				markerToRepair[i] = null;
			}

			public void close() {
				for (int i = 0; i < (repairs.length); i++)
					if (null != (repairs[i]))
						sendRepairMutation(repairs[i], sources[i]);


			}

			private void sendRepairMutation(PartitionUpdate partition, InetAddress destination) {
				Mutation mutation = new Mutation(partition);
				int messagingVersion = MessagingService.instance().getVersion(destination);
				int mutationSize = ((int) (Mutation.serializer.serializedSize(mutation, messagingVersion)));
				int maxMutationSize = DatabaseDescriptor.getMaxMutationSize();
				if (mutationSize <= maxMutationSize) {
					Tracing.trace("Sending read-repair-mutation to {}", destination);
					MessageOut<Mutation> message = mutation.createMessage(READ_REPAIR);
					repairResults.add(MessagingService.instance().sendRR(message, destination));
					ColumnFamilyStore.metricsFor(command.metadata().cfId).readRepairRequests.mark();
				}else
					if (DataResolver.DROP_OVERSIZED_READ_REPAIR_MUTATIONS) {
						ResponseResolver.logger.debug("Encountered an oversized ({}/{}) read repair mutation for table {}.{}, key {}, node {}", mutationSize, maxMutationSize, command.metadata().ksName, command.metadata().cfName, command.metadata().getKeyValidator().getString(partitionKey.getKey()), destination);
					}else {
						ResponseResolver.logger.warn("Encountered an oversized ({}/{}) read repair mutation for table {}.{}, key {}, node {}", mutationSize, maxMutationSize, command.metadata().ksName, command.metadata().cfName, command.metadata().getKeyValidator().getString(partitionKey.getKey()), destination);
						int blockFor = consistency.blockFor(keyspace);
						Tracing.trace("Timed out while read-repairing after receiving all {} data and digest responses", blockFor);
						throw new ReadTimeoutException(consistency, (blockFor - 1), blockFor, true);
					}

			}
		}
	}

	private UnfilteredPartitionIterator extendWithShortReadProtection(UnfilteredPartitionIterator partitions, InetAddress source, DataLimits.Counter mergedResultCounter) {
		DataLimits.Counter singleResultCounter = command.limits().newCounter(command.nowInSec(), false, command.selectsFullPartition(), enforceStrictLiveness).onlyCount();
		DataResolver.ShortReadPartitionsProtection protection = new DataResolver.ShortReadPartitionsProtection(source, singleResultCounter, mergedResultCounter, queryStartNanoTime);
		if (!(command.isLimitedToOnePartition()))
			partitions = MorePartitions.extend(partitions, protection);

		partitions = Transformation.apply(partitions, protection);
		partitions = Transformation.apply(partitions, singleResultCounter);
		return partitions;
	}

	private class ShortReadPartitionsProtection extends Transformation<UnfilteredRowIterator> implements MorePartitions<UnfilteredPartitionIterator> {
		private final InetAddress source;

		private final DataLimits.Counter singleResultCounter;

		private final DataLimits.Counter mergedResultCounter;

		private DecoratedKey lastPartitionKey;

		private boolean partitionsFetched;

		private final long queryStartNanoTime;

		private ShortReadPartitionsProtection(InetAddress source, DataLimits.Counter singleResultCounter, DataLimits.Counter mergedResultCounter, long queryStartNanoTime) {
			this.source = source;
			this.singleResultCounter = singleResultCounter;
			this.mergedResultCounter = mergedResultCounter;
			this.queryStartNanoTime = queryStartNanoTime;
		}

		@Override
		public UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition) {
			partitionsFetched = true;
			lastPartitionKey = partition.partitionKey();
			DataResolver.ShortReadPartitionsProtection.ShortReadRowsProtection protection = new DataResolver.ShortReadPartitionsProtection.ShortReadRowsProtection(partition.metadata(), partition.partitionKey());
			return Transformation.apply(MoreRows.extend(partition, protection), protection);
		}

		public UnfilteredPartitionIterator moreContents() {
			assert !(mergedResultCounter.isDone());
			assert !(command.limits().isUnlimited());
			assert !(command.isLimitedToOnePartition());
			if ((!(singleResultCounter.isDone())) && ((command.limits().perPartitionCount()) == (DataLimits.NO_LIMIT)))
				return null;

			if (!(partitionsFetched))
				return null;

			partitionsFetched = false;
			int toQuery = ((command.limits().count()) != (DataLimits.NO_LIMIT)) ? (command.limits().count()) - (counted(mergedResultCounter)) : command.limits().perPartitionCount();
			ColumnFamilyStore.metricsFor(command.metadata().cfId).shortReadProtectionRequests.mark();
			Tracing.trace("Requesting {} extra rows from {} for short read protection", toQuery, source);
			PartitionRangeReadCommand cmd = makeFetchAdditionalPartitionReadCommand(toQuery);
			return executeReadCommand(cmd);
		}

		private int counted(DataLimits.Counter counter) {
			return command.limits().isGroupByLimit() ? counter.rowCounted() : counter.counted();
		}

		private PartitionRangeReadCommand makeFetchAdditionalPartitionReadCommand(int toQuery) {
			PartitionRangeReadCommand cmd = ((PartitionRangeReadCommand) (command));
			DataLimits newLimits = cmd.limits().forShortReadRetry(toQuery);
			AbstractBounds<PartitionPosition> bounds = cmd.dataRange().keyRange();
			AbstractBounds<PartitionPosition> newBounds = (bounds.inclusiveRight()) ? new Range<>(lastPartitionKey, bounds.right) : new ExcludingBounds<>(lastPartitionKey, bounds.right);
			DataRange newDataRange = cmd.dataRange().forSubRange(newBounds);
			return cmd.withUpdatedLimitsAndDataRange(newLimits, newDataRange);
		}

		private class ShortReadRowsProtection extends Transformation implements MoreRows<UnfilteredRowIterator> {
			private final CFMetaData metadata;

			private final DecoratedKey partitionKey;

			private Clustering lastClustering;

			private int lastCounted = 0;

			private int lastFetched = 0;

			private int lastQueried = 0;

			private ShortReadRowsProtection(CFMetaData metadata, DecoratedKey partitionKey) {
				this.metadata = metadata;
				this.partitionKey = partitionKey;
			}

			@Override
			public Row applyToRow(Row row) {
				lastClustering = row.clustering();
				return row;
			}

			public UnfilteredRowIterator moreContents() {
				assert !(mergedResultCounter.isDoneForPartition());
				assert !(command.limits().isUnlimited());
				if ((!(singleResultCounter.isDoneForPartition())) && ((command.limits().perPartitionCount()) == (DataLimits.NO_LIMIT)))
					return null;

				if ((countedInCurrentPartition(singleResultCounter)) == 0)
					return null;

				if ((Clustering.EMPTY) == (lastClustering))
					return null;

				lastFetched = (countedInCurrentPartition(singleResultCounter)) - (lastCounted);
				lastCounted = countedInCurrentPartition(singleResultCounter);
				if (((lastQueried) > 0) && ((lastFetched) < (lastQueried)))
					return null;

				lastQueried = Math.min(command.limits().count(), command.limits().perPartitionCount());
				ColumnFamilyStore.metricsFor(metadata.cfId).shortReadProtectionRequests.mark();
				Tracing.trace("Requesting {} extra rows from {} for short read protection", lastQueried, source);
				SinglePartitionReadCommand cmd = makeFetchAdditionalRowsReadCommand(lastQueried);
				return UnfilteredPartitionIterators.getOnlyElement(executeReadCommand(cmd), cmd);
			}

			private int countedInCurrentPartition(DataLimits.Counter counter) {
				return command.limits().isGroupByLimit() ? counter.rowCountedInCurrentPartition() : counter.countedInCurrentPartition();
			}

			private SinglePartitionReadCommand makeFetchAdditionalRowsReadCommand(int toQuery) {
				ClusteringIndexFilter filter = command.clusteringIndexFilter(partitionKey);
				if (null != (lastClustering))
					filter = filter.forPaging(metadata.comparator, lastClustering, false);

				return SinglePartitionReadCommand.create(command.isForThrift(), command.metadata(), command.nowInSec(), command.columnFilter(), command.rowFilter(), command.limits().forShortReadRetry(toQuery), partitionKey, filter, command.indexMetadata());
			}
		}

		private UnfilteredPartitionIterator executeReadCommand(ReadCommand cmd) {
			DataResolver resolver = new DataResolver(keyspace, cmd, ConsistencyLevel.ONE, 1, queryStartNanoTime);
			ReadCallback handler = new ReadCallback(resolver, ConsistencyLevel.ONE, cmd, Collections.singletonList(source), queryStartNanoTime);
			if (StorageProxy.canDoLocalRequest(source)) {
			}else
				MessagingService.instance().sendRRWithFailure(cmd.createMessage(MessagingService.current_version), source, handler);

			handler.awaitResults();
			assert (resolver.responses.size()) == 1;
			return resolver.responses.get(0).payload.makeIterator(command);
		}
	}
}

