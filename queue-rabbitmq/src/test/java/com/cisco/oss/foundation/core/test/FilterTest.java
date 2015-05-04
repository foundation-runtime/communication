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

package com.cisco.oss.foundation.core.test;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.filter.impl.FilterParser;
import org.hornetq.core.filter.impl.Identifier;
import org.hornetq.core.filter.impl.ParseException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yaogen on 10/03/2015.
 */
public class FilterTest {
    String filter = "pointer='/households[householdId=?]/userProfiles[userProfileId=?]' OR pointer LIKE '/households[householdId=?]/userProfiles[userProfileId=?]/preferences/%' OR pointer='/households[householdId=?]/userProfiles[userProfileId=?]/favoriteChannels' OR pointer LIKE '/households[householdId=?]/devices[deviceId=?]%' OR pointer LIKE '/households[householdId=?]/preferences/%' OR pointer='/households[householdId=?]/householdStatus' OR pointer='/households[householdId=?]/locale' OR pointer='/households[householdId=?]' OR pointer LIKE '/households[householdId=?]/enabledServices%' OR pointer = '/households[householdId=?]/numTitles' OR pointer = '/households[householdId=?]/authorizations/subscriptions[authorizationId=?]' OR pointer = '/households[householdId=?]/authorizations/titles[purchaseId=?]'";

    @Test
    public void testFilterParser(){
        Map<SimpleString, Identifier> identifierMap = new HashMap<>();
        try {
            Object parsed = FilterParser.doParse(new SimpleString(filter), identifierMap);
            int i=0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
