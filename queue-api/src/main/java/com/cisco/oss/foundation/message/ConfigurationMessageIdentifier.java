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

package com.cisco.oss.foundation.message;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import org.apache.commons.configuration.Configuration;


public class ConfigurationMessageIdentifier implements MessageIdentifier {

    public static final String MESSAGE_IDENTIFIER_PROPERTY = "messageHandling.messageIdentifierProperty";

    private final String identifierProperty;

    public ConfigurationMessageIdentifier() {
        Configuration config = ConfigurationFactory.getConfiguration();
        identifierProperty = config.getString(MESSAGE_IDENTIFIER_PROPERTY);
    }

    public String getIdentifier(Message message) {

        if (null == identifierProperty) {
            return null;
        }

        Object identifierPropertyValue = message.getProperties().get(identifierProperty);
        return identifierPropertyValue != null ? identifierPropertyValue.toString(): null;
    }

}
