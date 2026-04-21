/*
 * SkyTube
 * Copyright (C) 2026  SkyTube Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

/**
 * Holds the audio language/quality and subtitle options built from {@link StreamInfo} and applies
 * the selected ones through ExoPlayer's constraint-based track selection parameters.
 *
 * <p>The manager deliberately does not inspect ExoPlayer's mapped track groups. All metadata comes
 * from the stream information collected by {@link DatasourceBuilder} when playback starts, so a
 * local file simply produces an empty (bare-bone) manager.</p>
 */
public class TrackSelectionManager {
    public static final int NO_BITRATE = 0;

    private final DefaultTrackSelector trackSelector;
    private final List<AudioTrackInfo> audioTracks;
    private final List<AudioTrackInfo> audioLanguages = new ArrayList<>();
    private final List<AudioTrackInfo> audioQualities = new ArrayList<>();
    private final List<SubtitleTrackInfo> subtitleTracks;
    private int currentAudioLanguageIndex = -1;
    private int currentAudioQualityIndex = -1;
    private int currentSubtitleTrackIndex = -1;

    /**
     * The minimal information a selector needs to display an option.
     */
    public interface SelectableTrackInfo {
        String getLanguage();

        String getDisplayName();
    }

    /**
     * An audio option built from an {@link AudioStream}.
     */
    public static class AudioTrackInfo implements SelectableTrackInfo {
        final String language;
        final String displayName;
        final int bitrate;
        final boolean translated;

        public AudioTrackInfo(String language, String displayName, int bitrate) {
            this(language, displayName, bitrate, false);
        }

        public AudioTrackInfo(String language, String displayName, int bitrate, boolean translated) {
            this.language = language;
            this.displayName = displayName;
            this.bitrate = bitrate;
            this.translated = translated;
        }

        @Override
        public String getLanguage() {
            return language;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * A subtitle option. Unlike audio, subtitles have neither a bitrate nor a translated flag.
     */
    public static class SubtitleTrackInfo implements SelectableTrackInfo {
        final String language;
        final String displayName;

        public SubtitleTrackInfo(String language, String displayName) {
            this.language = language;
            this.displayName = displayName;
        }

        @Override
        public String getLanguage() {
            return language;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    public TrackSelectionManager(DefaultTrackSelector trackSelector,
                                 List<AudioTrackInfo> audioTracks,
                                 List<SubtitleTrackInfo> subtitleTracks) {
        this.trackSelector = trackSelector;
        this.audioTracks = audioTracks == null ? new ArrayList<>() : new ArrayList<>(audioTracks);
        this.subtitleTracks = subtitleTracks == null
                ? new ArrayList<>() : new ArrayList<>(subtitleTracks);
        buildAudioLanguageOptions();
        selectDefaults();
    }

    private void buildAudioLanguageOptions() {
        audioLanguages.clear();
        final Set<String> seenLanguages = new LinkedHashSet<>();
        // Add original languages first so a translated variant never replaces an original one.
        addAudioLanguageOptions(seenLanguages, false);
        addAudioLanguageOptions(seenLanguages, true);

        final String preferredLanguage = getPreferredAudioTrackFromSettings();
        Collections.sort(audioLanguages, (a, b) -> {
            if (a.translated != b.translated) {
                return a.translated ? 1 : -1;
            }
            final boolean aPreferred = isPreferredLanguage(a.language, preferredLanguage);
            final boolean bPreferred = isPreferredLanguage(b.language, preferredLanguage);
            if (aPreferred != bPreferred) {
                return aPreferred ? -1 : 1;
            }
            return a.displayName.compareToIgnoreCase(b.displayName);
        });
    }

    private void addAudioLanguageOptions(Set<String> seenLanguages, boolean translated) {
        for (AudioTrackInfo track : audioTracks) {
            if (track.translated != translated) {
                continue;
            }
            final String key = track.language == null ? "" : track.language;
            if (!seenLanguages.add(key)) {
                continue;
            }
            audioLanguages.add(new AudioTrackInfo(track.language,
                    getLanguageDisplayName(track.language, translated),
                    getHighestBitrate(track.language), translated));
        }
    }

    private void buildAudioQualityOptions(String language) {
        audioQualities.clear();
        if (!hasLanguage(language)) {
            return;
        }
        final Set<Integer> bitrates = new LinkedHashSet<>();
        for (AudioTrackInfo track : audioTracks) {
            if (sameLanguage(track.language, language)) {
                bitrates.add(track.bitrate);
            }
        }
        final List<Integer> sortedBitrates = new ArrayList<>(bitrates);
        Collections.sort(sortedBitrates, Comparator.reverseOrder());
        for (int bitrate : sortedBitrates) {
            audioQualities.add(new AudioTrackInfo(language, formatBitrate(bitrate), bitrate));
        }
    }

    private void selectDefaults() {
        int languageIndex = findAudioLanguageIndex(getPreferredAudioTrackFromSettings());
        if (languageIndex < 0) {
            languageIndex = findAudioLanguageIndex(Locale.getDefault().getLanguage());
        }
        if (languageIndex < 0 && !audioLanguages.isEmpty()) {
            languageIndex = 0;
        }
        if (languageIndex >= 0) {
            setAudioLanguage(languageIndex, false);
        } else {
            applyAudioSelection(null, NO_BITRATE);
        }

        final boolean preferEnabled = isPreferredSubtitleEnabledFromSettings();
        final String savedLanguage = getPreferredSubtitleTrackFromSettings();
        int subtitleIndex = -1;
        if (preferEnabled && savedLanguage != null) {
            subtitleIndex = findSubtitleIndex(savedLanguage);
        }
        if (preferEnabled && subtitleIndex < 0) {
            subtitleIndex = findSubtitleIndex(Locale.getDefault().getLanguage());
        }
        if (subtitleIndex >= 0) {
            setSubtitleTrack(subtitleIndex, false);
        } else {
            disableSubtitles();
        }
    }

    private int findAudioLanguageIndex(String language) {
        if (language == null) {
            return -1;
        }
        final String normalized = normalizeLanguage(language);
        for (int i = 0; i < audioLanguages.size(); i++) {
            if (sameLanguage(normalized, audioLanguages.get(i).language)) {
                return i;
            }
        }
        return -1;
    }

    private int findSubtitleIndex(String language) {
        if (language == null) {
            return -1;
        }
        final String normalized = normalizeLanguage(language);
        for (int i = 0; i < subtitleTracks.size(); i++) {
            if (sameLanguage(normalized, subtitleTracks.get(i).language)) {
                return i;
            }
        }
        return -1;
    }

    private int getHighestBitrate(String language) {
        int highest = NO_BITRATE;
        for (AudioTrackInfo track : audioTracks) {
            if (sameLanguage(track.language, language) && track.bitrate > highest) {
                highest = track.bitrate;
            }
        }
        return highest;
    }

    private boolean hasLanguage(String language) {
        for (AudioTrackInfo track : audioTracks) {
            if (sameLanguage(track.language, language)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks the closest available quality for a language: an exact match first, then the best
     * quality that is not higher than the requested one, and finally the lowest available quality
     * when every track is worse than the requested one.
     */
    static int findBestAudioTrackIndex(List<AudioTrackInfo> tracks, String language, int preferredBitrate) {
        int highestIndex = -1;
        int lowestIndex = -1;
        int lowerIndex = -1;

        for (int i = 0; i < tracks.size(); i++) {
            final AudioTrackInfo track = tracks.get(i);
            if (!sameLanguage(track.language, language)) {
                continue;
            }
            if (preferredBitrate > NO_BITRATE && track.bitrate == preferredBitrate) {
                return i;
            }
            if (highestIndex < 0 || track.bitrate > tracks.get(highestIndex).bitrate) {
                highestIndex = i;
            }
            if (lowestIndex < 0 || track.bitrate < tracks.get(lowestIndex).bitrate) {
                lowestIndex = i;
            }
            if (track.bitrate > NO_BITRATE && track.bitrate < preferredBitrate
                    && (lowerIndex < 0 || track.bitrate > tracks.get(lowerIndex).bitrate)) {
                lowerIndex = i;
            }
        }

        if (preferredBitrate <= NO_BITRATE) {
            return highestIndex;
        }
        return lowerIndex >= 0 ? lowerIndex : lowestIndex;
    }

    private static boolean sameLanguage(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equalsIgnoreCase(second);
    }

    private static boolean isPreferredLanguage(String language, String preferredLanguage) {
        return preferredLanguage != null && sameLanguage(language, preferredLanguage);
    }

    static String normalizeLanguage(String language) {
        if (language == null || language.isEmpty() || "und".equalsIgnoreCase(language)) {
            return null;
        }
        final String base = language.split("[-_]", 2)[0];
        return base.toLowerCase(Locale.US);
    }

    static String getLanguageDisplayName(String languageCode) {
        return getLanguageDisplayName(languageCode, false);
    }

    static String getLanguageDisplayName(String languageCode, boolean translated) {
        String displayName = SkyTubeApp.getStr(R.string.audio_language_unknown);
        if (languageCode != null && !languageCode.isEmpty()) {
            try {
                final String name = new Locale(languageCode).getDisplayLanguage(Locale.getDefault());
                displayName = name.isEmpty() ? languageCode : name;
            } catch (Exception e) {
                displayName = languageCode;
            }
        }
        return translated
                ? SkyTubeApp.getStr(R.string.audio_language_translated_pattern, displayName)
                : displayName;
    }

    static String formatBitrate(int bitrate) {
        if (bitrate <= NO_BITRATE) {
            return SkyTubeApp.getStr(R.string.audio_quality_unknown);
        }
        if (bitrate >= 1000) {
            return SkyTubeApp.getStr(R.string.audio_quality_bitrate_kbps, bitrate / 1000);
        }
        return SkyTubeApp.getStr(R.string.audio_quality_bitrate_bps, bitrate);
    }

    /**
     * Returns the languages for which YouTube supplied an auto-generated dub. NewPipe keeps that
     * flag on the YouTube specific {@link ItagItem}, not on {@link AudioStream} itself.
     */
    static Set<String> findAutoTranslatedAudioLanguages(StreamInfo streamInfo) {
        final Set<String> languages = new HashSet<>();
        if (streamInfo == null || streamInfo.getAudioStreams() == null) {
            return languages;
        }
        for (AudioStream audioStream : streamInfo.getAudioStreams()) {
            if (audioStream == null) {
                continue;
            }
            final ItagItem itagItem = audioStream.getItagItem();
            if (itagItem == null || !itagItem.isAutoGenerated()) {
                continue;
            }
            final String language = normalizeLanguage(itagItem.getAudioLocale() != null
                    ? itagItem.getAudioLocale().getLanguage()
                    : (audioStream.getAudioLocale() != null
                        ? audioStream.getAudioLocale().getLanguage() : null));
            if (language != null) {
                languages.add(language);
            }
        }
        return languages;
    }

    public List<AudioTrackInfo> getAvailableAudioLanguages() {
        return Collections.unmodifiableList(audioLanguages);
    }

    public List<AudioTrackInfo> getAvailableAudioQualities() {
        return Collections.unmodifiableList(audioQualities);
    }

    public List<SubtitleTrackInfo> getAvailableSubtitleTracks() {
        return Collections.unmodifiableList(subtitleTracks);
    }

    public int getCurrentAudioLanguageIndex() {
        return currentAudioLanguageIndex;
    }

    public int getCurrentAudioQualityIndex() {
        return currentAudioQualityIndex;
    }

    public int getCurrentSubtitleTrackIndex() {
        return currentSubtitleTrackIndex;
    }

    public boolean setAudioLanguage(int index) {
        return setAudioLanguage(index, true);
    }

    /**
     * @param savePreference whether the language should become the default for future videos
     */
    public boolean setAudioLanguage(int index, boolean savePreference) {
        if (index < 0 || index >= audioLanguages.size()) {
            return false;
        }

        final String language = audioLanguages.get(index).language;
        final int trackIndex = findBestAudioTrackIndex(
                audioTracks, language, getPreferredAudioQualityFromSettings());
        final int bitrate = trackIndex >= 0 ? audioTracks.get(trackIndex).bitrate : NO_BITRATE;
        applyAudioSelection(language, bitrate);

        if (savePreference && language != null) {
            saveAudioTrackPreference(language);
        }
        currentAudioLanguageIndex = index;
        buildAudioQualityOptions(language);
        currentAudioQualityIndex = findQualityIndex(bitrate);
        return true;
    }

    public boolean setAudioQuality(int index) {
        return setAudioQuality(index, true);
    }

    /**
     * @param savePreference whether the quality should be kept when switching languages
     */
    public boolean setAudioQuality(int index, boolean savePreference) {
        if (index < 0 || index >= audioQualities.size()) {
            return false;
        }
        final int bitrate = audioQualities.get(index).bitrate;
        applyAudioSelection(getCurrentAudioLanguage(), bitrate);
        if (savePreference) {
            saveAudioQualityPreference(bitrate);
        }
        currentAudioQualityIndex = index;
        return true;
    }

    private void applyAudioSelection(String language, int bitrate) {
        if (trackSelector == null) {
            return;
        }
        final int maxBitrate = bitrate > NO_BITRATE ? bitrate : Integer.MAX_VALUE;
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setPreferredAudioLanguage(language)
                .setMaxAudioBitrate(maxBitrate)
                .build());
    }

    private int findQualityIndex(int bitrate) {
        for (int i = 0; i < audioQualities.size(); i++) {
            if (audioQualities.get(i).bitrate == bitrate) {
                return i;
            }
        }
        return -1;
    }

    private String getCurrentAudioLanguage() {
        if (currentAudioLanguageIndex >= 0 && currentAudioLanguageIndex < audioLanguages.size()) {
            return audioLanguages.get(currentAudioLanguageIndex).language;
        }
        return null;
    }

    public boolean setSubtitleTrack(int index) {
        return setSubtitleTrack(index, true);
    }

    public boolean setSubtitleTrack(int index, boolean savePreference) {
        if (index < 0) {
            disableSubtitles();
            return true;
        }
        if (index >= subtitleTracks.size()) {
            return false;
        }
        final String language = subtitleTracks.get(index).language;
        applySubtitleSelection(language);
        currentSubtitleTrackIndex = index;
        if (savePreference) {
            saveSubtitleTrackPreference(language);
            saveSubtitleEnabledPreference(true);
        }
        return true;
    }

    private void applySubtitleSelection(String language) {
        if (trackSelector == null) {
            return;
        }
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setPreferredTextLanguage(language)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, language == null)
                .build());
    }

    public boolean disableSubtitles() {
        applySubtitleSelection(null);
        currentSubtitleTrackIndex = -1;
        return true;
    }

    private String getPreferredAudioTrackFromSettings() {
        if (SkyTubeApp.getSettings() == null) {
            return null;
        }
        return SkyTubeApp.getSettings().getPreferredAudioTrack();
    }

    private int getPreferredAudioQualityFromSettings() {
        if (SkyTubeApp.getSettings() == null) {
            return NO_BITRATE;
        }
        return SkyTubeApp.getSettings().getPreferredAudioQuality();
    }

    private String getPreferredSubtitleTrackFromSettings() {
        if (SkyTubeApp.getSettings() == null) {
            return null;
        }
        return SkyTubeApp.getSettings().getPreferredSubtitleTrack();
    }

    private boolean isPreferredSubtitleEnabledFromSettings() {
        return SkyTubeApp.getSettings() != null && SkyTubeApp.getSettings().getPreferredSubtitleEnabled();
    }

    private void saveAudioTrackPreference(String languageCode) {
        if (languageCode != null && SkyTubeApp.getSettings() != null) {
            SkyTubeApp.getSettings().setPreferredAudioTrack(languageCode);
        }
    }

    private void saveAudioQualityPreference(int bitrate) {
        if (SkyTubeApp.getSettings() != null) {
            SkyTubeApp.getSettings().setPreferredAudioQuality(bitrate);
        }
    }

    private void saveSubtitleTrackPreference(String languageCode) {
        if (languageCode != null && SkyTubeApp.getSettings() != null) {
            SkyTubeApp.getSettings().setPreferredSubtitleTrack(languageCode);
        }
    }

    public void saveSubtitleEnabledPreference(boolean enabled) {
        if (SkyTubeApp.getSettings() != null) {
            SkyTubeApp.getSettings().setPreferredSubtitleEnabled(enabled);
        }
    }
}
