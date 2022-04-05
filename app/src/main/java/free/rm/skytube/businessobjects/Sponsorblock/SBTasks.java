package free.rm.skytube.businessobjects.Sponsorblock;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SBTasks {
    private static final String TAG = SBTasks.class.getSimpleName();

    public static class LabelAndColor{
        public final @ColorRes int color;
        public final @StringRes int label;
        LabelAndColor(@ColorRes int color, @StringRes int label) {
            this.color = color;
            this.label = label;
        }
    }
    private static final Map<String, LabelAndColor> categoryMapping;
    static {
        Map<String, LabelAndColor> colors = new HashMap();
        colors.put("sponsor", new LabelAndColor(R.color.sponsorblock_category_sponsor, R.string.sponsorblock_category_sponsor));
        colors.put("selfpromo", new LabelAndColor(R.color.sponsorblock_category_selfpromo, R.string.sponsorblock_category_selfpromo));
        colors.put("interaction", new LabelAndColor(R.color.sponsorblock_category_interaction, R.string.sponsorblock_category_interaction));
        colors.put("music_offtopic", new LabelAndColor(R.color.sponsorblock_category_music_offtopic, R.string.sponsorblock_category_music_offtopic));
        colors.put("intro", new LabelAndColor(R.color.sponsorblock_category_intro, R.string.sponsorblock_category_intro));
        colors.put("outro", new LabelAndColor(R.color.sponsorblock_category_outro, R.string.sponsorblock_category_outro));
        colors.put("preview", new LabelAndColor(R.color.sponsorblock_category_preview, R.string.sponsorblock_category_preview));
        colors.put("filler", new LabelAndColor(R.color.sponsorblock_category_filler, R.string.sponsorblock_category_filler));
        categoryMapping = Collections.unmodifiableMap(colors);
    }

    public static LabelAndColor getLabelAndColor(String category) {
        return categoryMapping.get(category);
    }

    public static Iterable<Map.Entry<String, LabelAndColor>> getAllCategories() {
        return categoryMapping.entrySet();
    }

    /**
     * A task that retrieves information from the Sponsorblock API about a youtube video
     *
     * @param videoId The ID of the youtube video to watch
     */
    public static Maybe<SBVideoInfo> retrieveSponsorblockSegments(@NonNull Context context, @NonNull VideoId videoId) {
        return Maybe.fromCallable(() -> {
            Set<String> filterList = SkyTubeApp.getSettings().getSponsorblockCategories();
            if(filterList.size() == 0) return null; // enabled but all options turned off probably means "turned off but didn't know how to disable"

            StringBuilder query = new StringBuilder("[");
            for(String filterCategory : filterList) {
                query.append("%22" + filterCategory + "%22,");
            }
            query.setLength(query.length() - 1); // remove last comma
            query.append("]");

            String apiUrl = "https://sponsor.ajay.app/api/skipSegments?videoID=" + videoId.getId() + "&categories=" + query;
            Log.d(TAG, "ApiUrl: " + apiUrl);

            try {
                JSONArray sponsorblockInfo = getHTTPJSONArray(apiUrl);
                return new SBVideoInfo(sponsorblockInfo);
            } catch(Exception e) {
                // FileNotFoundException = 404, which the API triggers both if the API call is invalid or no segment was found
                // Hence we just assume that it's usually due to no segment (errors confuse users), and give a log for developers
                Log.w(TAG, "Failed retrieving Sponsorblock info: ", e);
                return null;
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, "Error: " + throwable.getMessage(), throwable);
                    final String msg = (throwable.getCause() != null ? throwable.getCause() : throwable).getMessage();
                    final String toastMsg = context.getString(R.string.could_not_get_sponsorblock, msg);
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
                })
                .subscribeOn(Schedulers.io());
    }

    public static JSONArray getHTTPJSONArray(String urlString) throws IOException, JSONException {
        return new JSONArray(getHTTP(urlString));
    }

    public static String getHTTP(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("User-Agent", "SkyTube-Android-" + BuildConfig.VERSION_CODE);
        urlConnection.setRequestProperty("Accept", "*/*");
        urlConnection.setReadTimeout(10000);
        urlConnection.setConnectTimeout(15000);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        return sb.toString();
    }
}
