/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.newpipe;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.stream.Description;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import free.rm.skytube.R;

public class NewPipeUtils {
    private NewPipeUtils() {}

    public static String filterHtml(@Nullable String content) {
        if (content == null) {
            return "";
        }
        return Jsoup.clean(content, "", Safelist.basic(), new Document.OutputSettings().prettyPrint(false));
    }

    public static String filterHtml(Description description) {
        String result;
        if (description.type() == Description.Type.HTML) {
            result = filterHtml(description.content());
        } else {
            result = description.content();
        }
        return result;
    }

    @Nullable
    public static String getThumbnailUrl(List<Image> images) {
        return images.stream().max(Comparator.comparing(Image::getWidth)).map(Image::getUrl).orElse(null);
    }

    @Nullable
    public static String getThumbnailUrl(InfoItem comment) {
        return getThumbnailUrl(comment.getThumbnails());
    }

    /**
     * Translates extractor failures into messages that explain what the user can do.
     *
     * @param context   used to resolve the localized message
     * @param throwable the failure raised while extracting the stream
     * @return a user-facing explanation, or {@code null} when the failure has no message
     */
    @Nullable
    public static String getUserFriendlyErrorMessage(@NonNull Context context,
                                                     @Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof ReCaptchaException) {
            return context.getString(R.string.error_video_captcha_required);
        }

        final String message = throwable.getMessage();
        if (message != null && message.contains("LOGIN_REQUIRED")) {
            final String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
            if (lowerCaseMessage.contains("age")) {
                return context.getString(R.string.error_video_age_restricted);
            }
            return context.getString(R.string.error_video_login_required);
        }
        return message;
    }
}
