/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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
package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.extractor.StreamingService;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;

/**
 * Base class for an activity that receives an intent from other apps in order to open/bookmark/etc a video.
 * This Activity uses a transparent theme, and finishes right away, so as not to take focus from the sharing app.
 */
abstract class ExternalUrlActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent() != null) {
            String textData = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            ContentId content = SkyTubeApp.parseUrl(this, textData, false);
            if (content != null && content.getType() == StreamingService.LinkType.STREAM) {
                handleVideoContent(content);
            } else {
                handleTextData(textData);
            }
        }
    }

    abstract void handleVideoContent(ContentId content);

    @StringRes
    abstract int invalidUrlErrorMessage();

    protected void invalidUrlError() {
        Toast.makeText(this, invalidUrlErrorMessage(), Toast.LENGTH_LONG).show();
        finish();
    }

    protected void handleTextData(String textData) {
        SkyTubeApp.openUrl(this, textData, false);
        finish();
    }

}
