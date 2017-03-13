/*
 * Copyright 2015 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.message;

import com.cisco.oss.foundation.flowcontext.FlowContextFactory;
import com.rabbitmq.client.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Basic abstract handler that exposes the foundation message handler but also implements silently the RabbitMQ message handler
 * Created by Yair Ogen on 24/04/2014.
 */
@Component
public abstract class AbstractRabbitMQMessageHandler extends AbstractMessageHandler implements Consumer {

    /**
     * Channel that this consumer is associated with.
     */
//    private final Channel _channel;
    /**
     * Consumer tag for this consumer.
     */
    private volatile String _consumerTag;

    private int channelNumber = -1;

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    protected int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
//     * @param channel the channel to which this consumer is attached
     */
    public AbstractRabbitMQMessageHandler(String consumerName/*Channel channe*/) {
        super(consumerName);
//        _channel = channel;
    }


    /**
     * Stores the most recently passed-in consumerTag - semantically, there should be only one.
     *
     * @see Consumer#handleConsumeOk
     */
    public void handleConsumeOk(String consumerTag) {
        this._consumerTag = consumerTag;
    }

    /**
     * No-op implementation of {@link Consumer#handleCancelOk}.
     *
     * @param consumerTag the defined consumer tag (client- or server-generated)
     */
    public void handleCancelOk(String consumerTag) {
        // no work to do
    }

    /**
     * No-op implementation of {@link Consumer#handleCancel(String)}
     *
     * @param consumerTag the defined consumer tag (client- or server-generated)
     */
    public void handleCancel(String consumerTag) throws IOException {
        // no work to do
    }

    /**
     * No-op implementation of {@link Consumer#handleShutdownSignal}.
     */
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        // no work to do
    }

    /**
     * No-op implementation of {@link Consumer#handleRecoverOk}.
     */
    public void handleRecoverOk(String consumerTag) {
        // no work to do
    }

    /**
     * No-op implementation of {@link Consumer#handleDelivery}.
     */
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
            throws IOException {

        Object fc = properties.getHeaders().get(QueueConstants.FLOW_CONTEXT_HEADER);
        String flowContextStr = fc != null ? fc.toString() : null;
        if (StringUtils.isNotBlank(flowContextStr)) {
            FlowContextFactory.deserializeNativeFlowContext(flowContextStr);
        }
        GetResponse getResponse = new GetResponse(envelope, properties, body, 0);


        Message msg = new RabbitMQMessage(getResponse, consumerTag);
        preMessageProcessing(msg);
        onMessage(msg);
        postMessageProcessing(msg);
    }

    /**


    /**
     * Retrieve the consumer tag.
     *
     * @return the most recently notified consumer tag.
     */
    public String getConsumerTag() {
        return _consumerTag;
    }



    @Override
    public String getServiceDescription() {
        return "RabbitMQ listener";
    }

    @Override
    public String getProtocol() {
        return "RabbitMQ";
    }
}
