package free.rm.skytube.gui.businessobjects;

import android.content.Context;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.interfaces.PlaybackStateListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;

/**
 * MediaSessionCompat wrapper.
 * manages the mediaSession and controls YoutubePlayerFragmentInterface
 * upon media session events.
 */
public final class YoutubePlayerMediaSession {
    private final MediaSessionCompat mediaSession;


    public YoutubePlayerMediaSession(Context context) {
        mediaSession = new MediaSessionCompat(context, context.getString(R.string.app_name));
    }


    public void setActive(boolean active) {
        mediaSession.setActive(active);
    }


    public void release() {
        mediaSession.release();
    }


    public void bindToPlayer(YouTubePlayerFragmentInterface player) {
        bindPlayerControlCallbacks(player);
        bindPlayerStateListener(player);
        setActive(true);
    }


    private void bindPlayerControlCallbacks(YouTubePlayerFragmentInterface player) {
        MediaSessionCompat.Callback callback = createMediaSessionCallback(player);
        mediaSession.setCallback(callback);
    }


    private void bindPlayerStateListener(YouTubePlayerFragmentInterface player) {
        PlaybackStateListener playbackStateListener = createPlaybackStateListener(mediaSession);
        player.setPlaybackStateListener(playbackStateListener);
    }


    private MediaSessionCompat.Callback createMediaSessionCallback(YouTubePlayerFragmentInterface player) {
        return new MediaSessionCompat.Callback() {
            private final YouTubePlayerFragmentInterface playerFragment = player;

            @Override
            public void onPlay() {
                playerFragment.play();
            }

            @Override
            public void onPause() {
                playerFragment.pause();
            }

            @Override
            public void onStop() {
                playerFragment.pause();
            }
        };
    }


    private PlaybackStateListener createPlaybackStateListener(MediaSessionCompat session) {
        return new PlaybackStateListener() {
            private final MediaSessionCompat mediaSession = session;

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
                setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
            }


            private void setMediaPlaybackState(int state) {
                PlaybackStateCompat playbackState = buildPlaybackState(state);
                mediaSession.setPlaybackState(playbackState);
            }


            private PlaybackStateCompat buildPlaybackState(int state) {
                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
                stateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
                stateBuilder.setActions(getPlaybackStateActions(state));
                return stateBuilder.build();
            }


            private long getPlaybackStateActions (int state) {
                if (state == PlaybackStateCompat.STATE_ERROR || state == PlaybackStateCompat.STATE_NONE) {
                    return 0;
                }

                long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE;

                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    actions |= PlaybackStateCompat.ACTION_PAUSE;
                } else {
                    actions |= PlaybackStateCompat.ACTION_PLAY;
                }

                return actions;
            }
        };
    }
}
