/**
 * 
 */
package com.cisco.vss.foundation.http.apache.test;

import com.cisco.vss.foundation.configuration.FoundationConfigurationListenerRegistry;
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
