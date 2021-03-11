package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.interfaces.PlaybackStateListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;


public final class MediaSessionHandler {
    private YouTubePlayerFragmentInterface playerFragment;
    private final MediaSessionCompat mediaSession;

    public MediaSessionHandler(Context context) {
        mediaSession = new MediaSessionCompat(context, context.getString(R.string.app_name));
        setupMediaSession();
    }

    public void attachToPlayer(YouTubePlayerFragmentInterface playerFragment) {
        this.playerFragment = playerFragment;
        PlaybackStateListener playbackStateListener = createPlaybackStateListener();
        playerFragment.setPlaybackStateListener(playbackStateListener);
    }

    public void setActive(boolean active) {
        mediaSession.setActive(active);
    }

    private void setupMediaSession() {
        MediaSessionCompat.Callback callback = createMediaSessionCallback();
        mediaSession.setCallback(callback);
        setActive(true);
    }

    private MediaSessionCompat.Callback createMediaSessionCallback() {
        return new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (playerFragment != null) {
                    playerFragment.play();
                }
            }

            @Override
            public void onPause() {
                if (playerFragment != null) {
                    playerFragment.pause();
                }
            }
        };
    }

    private PlaybackStateListener createPlaybackStateListener() {
        return new PlaybackStateListener() {
            @Override
            public void started() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }

            @Override
            public void paused() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }

            @Override
            public void ended() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }

            private void setMediaPlaybackState(int state) {
                PlaybackStateCompat playbackState = buildPlaybackState(state);
                mediaSession.setPlaybackState(playbackState);
            }

            private PlaybackStateCompat buildPlaybackState(int state) {
                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
                stateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
                setPlaybackStateActions(stateBuilder, state);
                return stateBuilder.build();
            }

            private void setPlaybackStateActions(PlaybackStateCompat.Builder builder, int state) {
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    builder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
                } else {
                    builder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
                }
            }
        };
    }
}
