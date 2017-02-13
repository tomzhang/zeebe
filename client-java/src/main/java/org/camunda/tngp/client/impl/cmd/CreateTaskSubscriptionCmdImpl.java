package org.camunda.tngp.client.impl.cmd;

import static org.camunda.tngp.util.EnsureUtil.ensureGreaterThan;
import static org.camunda.tngp.util.EnsureUtil.ensureGreaterThanOrEqual;
import static org.camunda.tngp.util.EnsureUtil.ensureNotNull;

import java.util.function.Function;

import org.camunda.tngp.client.impl.ClientCmdExecutor;
import org.camunda.tngp.client.impl.cmd.taskqueue.TaskSubscription;
import org.camunda.tngp.client.impl.data.MsgPackConverter;
import org.camunda.tngp.protocol.clientapi.ControlMessageType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateTaskSubscriptionCmdImpl extends AbstractControlMessageCmd<TaskSubscription, Long>
{
    protected static final Function<TaskSubscription, Long> RESPONSE_HANDLER = TaskSubscription::getId;

    protected final TaskSubscription subscription = new TaskSubscription();
    protected final MsgPackConverter msgPackConverter = new MsgPackConverter();

    private long topicId = -1L;
    private String taskType;
    private long lockDuration = -1L;
    private long lockOwner = -1L;
    private int initialCredits = -1;

    public CreateTaskSubscriptionCmdImpl(ClientCmdExecutor cmdExecutor, final ObjectMapper objectMapper)
    {
        super(cmdExecutor, objectMapper, TaskSubscription.class, ControlMessageType.ADD_TASK_SUBSCRIPTION, RESPONSE_HANDLER);
    }

    public CreateTaskSubscriptionCmdImpl lockOwner(int lockOwner)
    {
        this.lockOwner = lockOwner;
        return this;
    }

    public CreateTaskSubscriptionCmdImpl topicId(long topicId)
    {
        this.topicId = topicId;
        return this;
    }

    public CreateTaskSubscriptionCmdImpl initialCredits(int initialCredits)
    {
        this.initialCredits = initialCredits;
        return this;
    }

    public CreateTaskSubscriptionCmdImpl lockDuration(long lockDuration)
    {
        this.lockDuration = lockDuration;
        return this;
    }

    public CreateTaskSubscriptionCmdImpl taskType(String taskType)
    {
        this.taskType = taskType;
        return this;
    }

    @Override
    public void validate()
    {
        ensureGreaterThanOrEqual("topic id", topicId, 0);
        ensureNotNull("task type", taskType);
        ensureGreaterThan("lock duration", lockDuration, 0);
        ensureGreaterThanOrEqual("lock owner", lockOwner, 0);
        ensureGreaterThan("initial credits", initialCredits, 0);
    }

    @Override
    public void reset()
    {
        topicId = -1;
        taskType = null;
        lockDuration = -1L;
        lockOwner = -1L;
        initialCredits = -1;
    }

    @Override
    protected Object writeCommand()
    {
        subscription.setTopicId(topicId);
        subscription.setTaskType(taskType);
        subscription.setLockDuration(lockDuration);
        subscription.setLockOwner(lockOwner);
        subscription.setCredits(initialCredits);

        return subscription;
    }

}
