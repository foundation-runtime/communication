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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component
public class ConfigurationMessageIdentifier implements MessageIdentifier {

    public static final String MESSAGE_IDENTIFIER_PROPERTY = "messageHandling.messageIdentifierProperty";



    @Autowired
    private Environment environment;

    @Value("${"+MESSAGE_IDENTIFIER_PROPERTY+"}")
    private String identifierProperty=null;

    public ConfigurationMessageIdentifier() {
    }

    public String getIdentifier(Message message) {

        if (null == identifierProperty) {
            return null;
        }

        Object identifierPropertyValue = message.getProperties().get(identifierProperty);
        return identifierPropertyValue != null ? identifierPropertyValue.toString(): null;
    }

}
