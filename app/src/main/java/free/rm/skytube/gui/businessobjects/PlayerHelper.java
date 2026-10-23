package free.rm.skytube.gui.businessobjects;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.source.ProgressiveMediaSource;

public final class PlayerHelper {
    private static final long PREFERRED_CACHE_SIZE = 64L * 1024 * 1024;
    private static final long PREFERRED_FILE_SIZE = 2L * 1024 * 1024;

    private PlayerHelper() {
    }

    public static long getPreferredCacheSize() {
        return PREFERRED_CACHE_SIZE;
    }

    public static long getPreferredFileSize() {
        return PREFERRED_FILE_SIZE;
    }

    public static int getProgressiveLoadIntervalBytes(@NonNull Context context) {
        return ProgressiveMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES;
    }
}
