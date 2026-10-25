/*
 * SkyTube
 * Copyright (C) 2026  SkyTube contributors
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

package free.rm.skytube.businessobjects.download;

import androidx.annotation.NonNull;

/**
 * A download that has been completed.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.FinishedMission}.</p>
 */
public class FinishedMission extends Mission {
    private static final long serialVersionUID = 1L;

    public FinishedMission() {
        super();
    }

    public FinishedMission(@NonNull DownloadMission mission) {
        super(mission.getId(), mission.getSource(), mission.getLength(), mission.getTimestamp(),
                mission.getKind(), mission.getTargetFile());
    }
}