package org.camunda.tngp.broker.clustering.raft;

import org.camunda.tngp.broker.clustering.channel.Endpoint;
import org.camunda.tngp.broker.clustering.raft.controller.ReplicationController;
import org.camunda.tngp.broker.clustering.raft.controller.VoteController;
import org.camunda.tngp.broker.logstreams.BrokerEventMetadata;
import org.camunda.tngp.logstreams.impl.log.index.LogBlockIndex;
import org.camunda.tngp.logstreams.log.BufferedLogStreamReader;
import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.logstreams.log.LogStreamReader;
import org.camunda.tngp.logstreams.log.LoggedEvent;
import org.camunda.tngp.logstreams.log.StreamContext;

/**
 * A member is a reference to another node involved in the raft protocol.
 */
public class Member
{
    private final Raft raft;
    private final Endpoint endpoint;

    private final LogStreamReader logStreamReader;
    private final BrokerEventMetadata metadata;

    private final ReplicationController replicationController;
    private final VoteController voteController;

    private int failures = 0;

    private long currentEntryPosition = -1L;
    private int currentEntryTerm = -1;
    private long matchPosition = -1L;

    private long lastContact;

    public Member()
    {
        this.endpoint = new Endpoint();
        this.raft = null;
        this.logStreamReader = null;
        this.metadata = null;
        this.replicationController = null;
        this.voteController = null;
    }

    public Member(final Endpoint endpoint, final RaftContext context)
    {
        this.endpoint = new Endpoint();
        this.endpoint.wrap(endpoint);

        this.raft = context.getRaft();

        this.logStreamReader = new BufferedLogStreamReader(raft.stream());
        this.metadata = new BrokerEventMetadata();

        this.replicationController = new ReplicationController(context, this);
        this.voteController = new VoteController(context, this);
    }

    public Endpoint endpoint()
    {
        return endpoint;
    }

    public void resetReaderToLastEntry()
    {
        currentEntryPosition = -1L;
        currentEntryTerm = -1;
        matchPosition = -1L;

        logStreamReader.seekToLastEvent();

        if (logStreamReader.hasNext())
        {
            final LoggedEvent lastEntry = logStreamReader.next();
            metadata.wrap(lastEntry.getMetadata(), lastEntry.getMetadataOffset(), lastEntry.getMetadataLength());

            currentEntryPosition = lastEntry.getPosition();
            currentEntryTerm = metadata.getRaftTermId();
        }
    }

    public void resetReaderToPreviousEntry(long position)
    {
        if (position < currentEntryPosition)
        {
            currentEntryPosition = -1;
            currentEntryTerm = -1;

            if (position < 0)
            {
                logStreamReader.seekToFirstEvent();
                return;
            }

            final Raft raft = this.raft;
            final LogStream stream = raft.stream();
            final StreamContext context = stream.getContext();
            final LogBlockIndex blockIndex = context.getBlockIndex();

            long blockPosition = blockIndex.lookupBlockPosition(position);
            if (blockPosition == position)
            {
                blockPosition = blockIndex.lookupBlockPosition(position - 1);
            }

            if (blockPosition >= 0)
            {
                logStreamReader.seek(blockPosition);
            }
            else
            {
                logStreamReader.seekToFirstEvent();
            }


            while (logStreamReader.hasNext())
            {
                final LoggedEvent curr = logStreamReader.next();
                metadata.wrap(curr.getMetadata(), curr.getMetadataOffset(), curr.getMetadataLength());

                if (curr.getPosition() < position)
                {
                    currentEntryPosition = curr.getPosition();
                    currentEntryTerm = metadata.getRaftTermId();
                }
                else
                {
                    break;
                }
            }

            if (currentEntryPosition >= 0)
            {
                logStreamReader.seek(this.currentEntryPosition);
                logStreamReader.next();
            }
            else
            {
                logStreamReader.seekToFirstEvent();
            }
        }
    }

    public boolean hasNextEntry()
    {
        return logStreamReader.hasNext();
    }

    public LoggedEvent nextEntry()
    {
        final LoggedEvent nextEntry = logStreamReader.next();
        nextEntry.readMetadata(metadata);

        currentEntryPosition = nextEntry.getPosition();
        currentEntryTerm = metadata.getRaftTermId();

        return nextEntry;
    }

    public long currentEntryPosition()
    {
        return currentEntryPosition;
    }

    public Member currentEntryPosition(final long currentEntryPosition)
    {
        this.currentEntryPosition = currentEntryPosition;
        return this;
    }

    public int currentEntryTerm()
    {
        return currentEntryTerm;
    }

    public Member currentEntryTerm(final int currentEntryTerm)
    {
        this.currentEntryTerm = currentEntryTerm;
        return this;
    }

    public long lastContact()
    {
        return lastContact;
    }

    public Member lastContact(final long lastContact)
    {
        this.lastContact = lastContact;
        return this;
    }

    public boolean hasFailures()
    {
        return failures > 0;
    }

    public void incrementFailures()
    {
        failures++;
    }

    public Member failures(int failures)
    {
        if (failures < 0)
        {
            throw new IllegalArgumentException();
        }
        this.failures = failures;
        return this;
    }

    public ReplicationController getReplicationController()
    {
        return replicationController;
    }

    public VoteController getVoteController()
    {
        return voteController;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Member other = (Member) obj;
        if (endpoint == null)
        {
            if (other.endpoint != null)
            {
                return false;
            }
        }
        else if (!endpoint.equals(other.endpoint))
        {
            return false;
        }
        return true;
    }

}