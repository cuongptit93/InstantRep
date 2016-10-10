package jp.co.efusion.listhelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by anhdt on 1/5/16.
 */
public class ThreadManager {
    private static final int CORE_POOL_SIZE = 2;

    private static ThreadManager sInstance = null;
    ScheduledThreadPoolExecutor sch;

    public synchronized final static ThreadManager getInstance() {
        if (sInstance == null) {
            sInstance = new ThreadManager();
        }

        return sInstance;
    }

    private ThreadManager() {
        sch = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(CORE_POOL_SIZE);
    }

    public void execTask(Runnable runnable) {
        sch.execute(runnable);
    }

    public ScheduledFuture execRepeatedTask(Runnable runnable, int delayedSeconds) {
        return sch.scheduleAtFixedRate(runnable, delayedSeconds, delayedSeconds, TimeUnit.SECONDS);
    }

    public void removeTask(Runnable runnable) {
        sch.remove(runnable);
    }


}
