/*
 * SkyTube
 * Copyright (C) 2026 SkyTube Team
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

package free.rm.skytube.gui.businessobjects.updates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import free.rm.skytube.R;

/**
 * Unit tests for OpenFDroidTask.
 */
@ExtendWith(MockitoExtension.class)
class OpenFDroidTaskTest {

    @Mock
    private Context context;

    @Mock
    private PackageManager packageManager;

    @InjectMocks
    private OpenFDroidTask openFDroidTask;

    @BeforeEach
    void setUp() {
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    void testDoInBackground_fdroidInstalled_returnsTrue() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenReturn(null); // PackageInfo object, null is fine for this test

        // Act
        Boolean result = openFDroidTask.doInBackground();

        // Assert
        assertTrue(result);
        verify(packageManager).getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES);
    }

    @Test
    void testDoInBackground_fdroidNotInstalled_returnsFalse() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenThrow(new PackageManager.NameNotFoundException());

        // Act
        Boolean result = openFDroidTask.doInBackground();

        // Assert
        assertFalse(result);
        verify(packageManager).getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES);
    }

    @Test
    void testOnPostExecute_fdroidInstalled_opensFdroidApp() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenReturn(null);

        // Act
        openFDroidTask.doInBackground();
        openFDroidTask.onPostExecute(true);

        // Assert
        verify(context).startActivity(any(Intent.class));
    }

    @Test
    void testOnPostExecute_fdroidNotInstalled_opensWebPage() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenThrow(new PackageManager.NameNotFoundException());

        // Act
        openFDroidTask.doInBackground();
        openFDroidTask.onPostExecute(false);

        // Assert
        verify(context).startActivity(any(Intent.class));
    }

    @Test
    void testOnPostExecute_fdroidAppLaunchFailed_fallsBackToWebPage() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenReturn(null);
        doThrow(new RuntimeException()).when(context).startActivity(any(Intent.class));

        // Act
        openFDroidTask.doInBackground();
        openFDroidTask.onPostExecute(true);

        // Assert
        // Should call startActivity twice - once for F-Droid app, once for fallback web page
        verify(context, times(2)).startActivity(any(Intent.class));
    }

    @Test
    void testOnPostExecute_webPageLaunchFailed_attemptsToOpen() throws Exception {
        // Arrange
        when(packageManager.getPackageInfo("org.fdroid.fdroid", PackageManager.GET_ACTIVITIES))
                .thenThrow(new PackageManager.NameNotFoundException());
        doThrow(new RuntimeException()).when(context).startActivity(any(Intent.class));

        // Act
        openFDroidTask.doInBackground();
        openFDroidTask.onPostExecute(false);

        // Assert
        // Should at least attempt to open the web page
        verify(context).startActivity(any(Intent.class));
    }
}