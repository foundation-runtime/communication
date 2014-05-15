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

package com.cisco.vss.foundtion.threading.load.test;


import com.cisco.vss.foundation.queue.management.MessageIdentifier;

import javax.servlet.http.HttpServletRequest;

public class SimpleIdentifier implements MessageIdentifier {

	@Override
	public String getIdentifier(Object message) {
		HttpServletRequest sr = (HttpServletRequest) message;
		System.out.println((String) sr.getHeader("id"));
		return  (String) sr.getAttribute("id");
	}
}
