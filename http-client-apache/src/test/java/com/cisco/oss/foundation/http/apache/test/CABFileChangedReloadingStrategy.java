/**
 * 
 */
package com.cisco.oss.foundation.http.apache.test;

import com.cisco.oss.foundation.configuration.FoundationConfigurationListenerRegistry;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

/**
 * @author Yair Ogen
 * 
 */
public class CABFileChangedReloadingStrategy extends FileChangedReloadingStrategy {

	/**
	 * @see org.apache.commons.configuration.reloading.FileChangedReloadingStrategy#reloadingPerformed()
	 */
	@Override
	public void reloadingPerformed() {
		super.reloadingPerformed();
		FoundationConfigurationListenerRegistry.fireConfigurationChangedEvent();
	}

}
