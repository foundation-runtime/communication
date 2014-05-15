/*
 * Copyright 2014 Cisco Systems, Inc.
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

package com.cisco.vss.foundation.message;

import java.util.Map;

/**
 * Basic message POJO. This Message types obscures the concrete underlying message type as it exposes simple String
 * payload or binary that can be transformed by clients to any other type.
 * Created by Yair Ogen on 24/04/2014.
 */
public interface Message {

    /**
     * get the message payload as complete string
     * @return the message payload as complete string
     */
    String getPayloadAsString();

    /**
     * get the message payload as a byte array
     * @return the message payload as a byte array
     */
    byte[] getPayloadAsBytes();

    /**
     * get any optional message properties.
     * @return any optional message properties.
     */
    Map<String,Object> getProperties();
}
