/*
 * SkyTube
 * Copyright (C) 2020 Zsombor Gegesy
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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import free.rm.skytube.businessobjects.AsyncTaskParallel;

/**
 * Helper class to run tasks on a separate threadpool, where all the network heavy jobs could run effectively, without starving the main async threadpool.
 */
public class NetworkAccess {

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "NetworkAccessTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>(128);

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_TIME = 30;

    private static final ThreadPoolExecutor executor;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        executor = threadPoolExecutor;
    }

    public static <Params, Progress, Result> void runOnNetworkPool(AsyncTaskParallel<Params, Progress, Result> task, Params... params) {
        task.executeOnExecutor(executor, params);
    }

    public static <Progress, Result> void runOnNetworkPool(AsyncTaskParallel<Void, Progress, Result> task) {
        task.executeOnExecutor(executor, (Void[]) null);
    }

}
