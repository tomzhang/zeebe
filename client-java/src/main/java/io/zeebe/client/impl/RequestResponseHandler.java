/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.impl;

import org.agrona.DirectBuffer;

import io.zeebe.client.clustering.impl.ClientTopologyManager;
import io.zeebe.protocol.clientapi.MessageHeaderDecoder;
import io.zeebe.transport.RemoteAddress;
import io.zeebe.util.buffer.BufferWriter;

public interface RequestResponseHandler extends BufferWriter
{

    boolean handlesResponse(MessageHeaderDecoder responseHeader);

    Object getResult(DirectBuffer buffer, int offset, int blockLength, int version);

    RemoteAddress getTarget(ClientTopologyManager currentTopology);

    String describeRequest();

}
