package com.cisco.vss.foundation.http.server;

import com.cisco.vss.foundation.configuration.ConfigurationFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TraceWrapper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceWrapper.class);
	
	public static String toStringWithBody(String data, String ndsBodyStatusHeader, byte[] body, int bodyLimit, String contentType, List<String> contentTypes, String characterEncoding, String bodySuffix, boolean forceBinaryPrint) {

		int dataLength = data.length();
		final StringBuilder builder = new StringBuilder(dataLength + bodyLimit);
		builder.append(data);
		builder.append("\n");
		boolean isText = false;
		if (body.length > 0) {			
			boolean bodyEncrypted = (ndsBodyStatusHeader != null) && (ndsBodyStatusHeader.equalsIgnoreCase("Encrypted"));

			if (!forceBinaryPrint && !bodyEncrypted && contentType != null) {
				for (String type : contentTypes) {
					if (contentType.toLowerCase().contains(type)) {
						String enc = characterEncoding;
						if (enc == null) {
							enc = "UTF-8";
						}

						try {
							builder.append(new String(body, enc));
							isText = true;
							break;
						} catch (UnsupportedEncodingException e) {
							LOGGER.trace("problem appending string body with {} encoding. error is: {}", enc, e);
							break;
						}
					}
				}
			}

			if (!isText) {
				builder.append(Hex.encodeHex(body));
			}
			if (builder.length() > dataLength + bodyLimit) {
				builder.setLength(dataLength + bodyLimit);
				builder.replace(builder.length() - bodySuffix.length(), builder.length(), bodySuffix);
			}
		}
		return builder.toString();
		
	}
	
	public static List<String> getContentTypes(String serviceName) {
		List<String> types = new ArrayList<String>(5);
		String baseKey = serviceName + ".http.traceFilter.textContentTypes";
		Configuration subset = ConfigurationFactory.getConfiguration().subset(baseKey);
		Iterator<String> keys = subset.getKeys();
		while(keys.hasNext()){
			String key = keys.next();
			types.add(subset.getString(key));
		}
		return types;
	}

}
