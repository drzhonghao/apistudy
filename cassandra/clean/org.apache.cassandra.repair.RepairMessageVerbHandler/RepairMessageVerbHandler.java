import org.apache.cassandra.repair.*;


import java.net.InetAddress;
import java.util.*;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.messages.*;
import org.apache.cassandra.service.ActiveRepairService;

/**
 * Handles all repair related message.
 *
 * @since 2.0
 */
public class RepairMessageVerbHandler implements IVerbHandler<RepairMessage>
{
    private static final Logger logger = LoggerFactory.getLogger(RepairMessageVerbHandler.class);
    public void doVerb(final MessageIn<RepairMessage> message, final int id)
    {
        // TODO add cancel/interrupt message
        RepairJobDesc desc = message.payload.desc;
        try
        {
            switch (message.payload.messageType)
            {
                case PREPARE_MESSAGE:
                    PrepareMessage prepareMessage = (PrepareMessage) message.payload;
                    logger.debug("Preparing, {}", prepareMessage);
                    List<ColumnFamilyStore> columnFamilyStores = new ArrayList<>(prepareMessage.cfIds.size());
                    for (UUID cfId : prepareMessage.cfIds)
                    {
                        ColumnFamilyStore columnFamilyStore = ColumnFamilyStore.getIfExists(cfId);
                        if (columnFamilyStore == null)
                        {
                            logErrorAndSendFailureResponse(String.format("Table with id %s was dropped during prepare phase of repair",
                                                                         cfId.toString()), message.from, id);
                            return;
                        }
                        columnFamilyStores.add(columnFamilyStore);
                    }
                    ActiveRepairService.instance.registerParentRepairSession(prepareMessage.parentRepairSession,
                                                                             message.from,
                                                                             columnFamilyStores,
                                                                             prepareMessage.ranges,
                                                                             prepareMessage.isIncremental,
                                                                             prepareMessage.timestamp,
                                                                             prepareMessage.isGlobal);
                    MessagingService.instance().sendReply(new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE), id, message.from);
                    break;

                case SNAPSHOT:
                    logger.debug("Snapshotting {}", desc);
                    final ColumnFamilyStore cfs = ColumnFamilyStore.getIfExists(desc.keyspace, desc.columnFamily);
                    if (cfs == null)
                    {
                        logErrorAndSendFailureResponse(String.format("Table %s.%s was dropped during snapshot phase of repair",
                                                                     desc.keyspace, desc.columnFamily), message.from, id);
                        return;
                    }

                    ActiveRepairService.ParentRepairSession prs = ActiveRepairService.instance.getParentRepairSession(desc.parentSessionId);
                    if (prs.isGlobal)
                    {
                        prs.maybeSnapshot(cfs.metadata.cfId, desc.parentSessionId);
                    }
                    else
                    {
                        cfs.snapshot(desc.sessionId.toString(), new Predicate<SSTableReader>()
                        {
                            public boolean apply(SSTableReader sstable)
                            {
                                return sstable != null &&
                                       !sstable.metadata.isIndex() && // exclude SSTables from 2i
                                       new Bounds<>(sstable.first.getToken(), sstable.last.getToken()).intersects(desc.ranges);
                            }
                        }, true, false); //ephemeral snapshot, if repair fails, it will be cleaned next startup
                    }
                    logger.debug("Enqueuing response to snapshot request {} to {}", desc.sessionId, message.from);
                    MessagingService.instance().sendReply(new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE), id, message.from);
                    break;

                case VALIDATION_REQUEST:
                    ValidationRequest validationRequest = (ValidationRequest) message.payload;
                    logger.debug("Validating {}", validationRequest);
                    // trigger read-only compaction
                    ColumnFamilyStore store = ColumnFamilyStore.getIfExists(desc.keyspace, desc.columnFamily);
                    if (store == null)
                    {
                        logger.error("Table {}.{} was dropped during snapshot phase of repair", desc.keyspace, desc.columnFamily);
                        MessagingService.instance().sendOneWay(new ValidationComplete(desc).createMessage(), message.from);
                        return;
                    }

                    Validator validator = new Validator(desc, message.from, validationRequest.gcBefore);
                    CompactionManager.instance.submitValidation(store, validator);
                    break;

                case SYNC_REQUEST:
                    // forwarded sync request
                    SyncRequest request = (SyncRequest) message.payload;
                    logger.debug("Syncing {}", request);
                    long repairedAt = ActiveRepairService.UNREPAIRED_SSTABLE;
                    if (desc.parentSessionId != null && ActiveRepairService.instance.getParentRepairSession(desc.parentSessionId) != null)
                        repairedAt = ActiveRepairService.instance.getParentRepairSession(desc.parentSessionId).getRepairedAt();

                    StreamingRepairTask task = new StreamingRepairTask(desc, request, repairedAt);
                    task.run();
                    break;

                case ANTICOMPACTION_REQUEST:
                    AnticompactionRequest anticompactionRequest = (AnticompactionRequest) message.payload;
                    logger.debug("Got anticompaction request {}", anticompactionRequest);
                    ListenableFuture<?> compactionDone = ActiveRepairService.instance.doAntiCompaction(anticompactionRequest.parentRepairSession, anticompactionRequest.successfulRanges);
                    compactionDone.addListener(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            MessagingService.instance().sendReply(new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE), id, message.from);
                        }
                    }, MoreExecutors.directExecutor());
                    break;

                case CLEANUP:
                    logger.debug("cleaning up repair");
                    CleanupMessage cleanup = (CleanupMessage) message.payload;
                    ActiveRepairService.instance.removeParentRepairSession(cleanup.parentRepairSession);
                    MessagingService.instance().sendReply(new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE), id, message.from);
                    break;

                default:
                    ActiveRepairService.instance.handleMessage(message.from, message.payload);
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error("Got error, removing parent repair session");
            if (desc != null && desc.parentSessionId != null)
                ActiveRepairService.instance.removeParentRepairSession(desc.parentSessionId);
            throw new RuntimeException(e);
        }
    }

    private void logErrorAndSendFailureResponse(String errorMessage, InetAddress to, int id)
    {
        logger.error(errorMessage);
        MessageOut reply = new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE)
                               .withParameter(MessagingService.FAILURE_RESPONSE_PARAM, MessagingService.ONE_BYTE);
        MessagingService.instance().sendReply(reply, id, to);
    }
}
