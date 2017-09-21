package free.rm.skytube.gui.businessobjects;

import java.io.Serializable;

/**
 * Simple data store class to hold information about a channel that has been imported from
 * a YouTube Export xml file.
 */
public class ImportSubscriptionsChannel implements Serializable {
	public String channelName;
	public String channelId;
	public boolean isChecked;

	public ImportSubscriptionsChannel(String channelName, String channelId) {
		this.channelName = channelName;
		this.channelId = channelId;
		isChecked = true;
	}
}
