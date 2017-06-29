package org.camunda.tngp.client.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.camunda.tngp.client.clustering.impl.ClientTopologyManager;
import org.camunda.tngp.client.impl.cmd.AbstractCmdImpl;
import org.camunda.tngp.transport.ChannelManager;
import org.camunda.tngp.transport.requestresponse.client.TransportConnectionPool;
import org.camunda.tngp.util.actor.Actor;


public class ClientCommandManager implements Actor
{
    protected final ManyToOneConcurrentArrayQueue<Runnable> commandQueue = new ManyToOneConcurrentArrayQueue<>(100);
    protected final Consumer<Runnable> commandConsumer = Runnable::run;

    protected final List<ClientCommandController<?>> commandControllers = new ArrayList<>();

    protected final ChannelManager channelManager;
    protected final TransportConnectionPool connectionPool;
    protected final ClientTopologyManager topologyManager;

    public ClientCommandManager(final ChannelManager channelManager, final TransportConnectionPool connectionPool, final ClientTopologyManager topologyManager)
    {
        this.channelManager = channelManager;
        this.connectionPool = connectionPool;
        this.topologyManager = topologyManager;
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = commandQueue.drain(commandConsumer);

        final Iterator<ClientCommandController<?>> iterator = commandControllers.iterator();
        while (iterator.hasNext())
        {
            final ClientCommandController controller = iterator.next();

            if (!controller.isClosed())
            {
                workCount = controller.doWork();
            }
            else
            {
                iterator.remove();
            }
        }

        return workCount;
    }

    public <R> CompletableFuture<R> executeAsync(final AbstractCmdImpl<R> command)
    {
        command.getRequestWriter().validate();

        final CompletableFuture<R> future = new CompletableFuture<>();

        commandQueue.add(() ->
        {
            final ClientCommandController<R> controller = new ClientCommandController<>(channelManager, connectionPool, topologyManager, command, future);
            commandControllers.add(controller);
        });

        return future;
    }

    public <R> R execute(final AbstractCmdImpl<R> command)
    {
        try
        {
            return executeAsync(command).get();
        }
        catch (final InterruptedException e)
        {
            throw new RuntimeException("Interrupted while executing command");
        }
        catch (final ExecutionException e)
        {
            throw (RuntimeException) e.getCause();
        }
    }
}