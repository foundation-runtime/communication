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

package com.cisco.vss.foundtion.threading.hornetq.test;

import java.util.ArrayList;
import java.util.List;


public class HornetqMessageContainer {

	private static List<MessageT> hornetqTestMessageList = new ArrayList<MessageT>();
	
	public synchronized static void addTestMessage(MessageT message){
		hornetqTestMessageList.add(message);
	}
	
	public synchronized static List<MessageT> getTestMessageList(){
		return hornetqTestMessageList;
	}
	
	public synchronized static void clearTestMessageList(){
		hornetqTestMessageList = new ArrayList<MessageT>();;
	}
}
