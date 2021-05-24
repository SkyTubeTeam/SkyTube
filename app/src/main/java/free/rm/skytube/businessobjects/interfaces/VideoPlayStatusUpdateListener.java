package free.rm.skytube.businessobjects.interfaces;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;

/**
 * Interface that is used to alert {@link free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter} that a video's playback status
 * has been updated. This is needed in order to set the watch status after a video has been viewed via Chromecast. Since previously, this was
 * happening in MainActivity's onResume, when playing a video through Chromecast, this onResume method is never called, since the Activity
 * is never left.
 */
public interface VideoPlayStatusUpdateListener {
	void onVideoStatusUpdated(CardData videoChanged);
}
