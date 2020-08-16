package com.zsoltsafrany.needle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Use this class to execute tasks on the UI/main or on a background thread. You can execute them concurrently or
 * serially. You can even define different levels of concurrency for different task types.
 *
 * @see #onMainThread()
 * @see #onBackgroundThread()
 * @see com.zsoltsafrany.needle.CancelableTask
 * @see com.zsoltsafrany.needle.UiRelatedTask
 * @see com.zsoltsafrany.needle.UiRelatedProgressTask
 * @see <a href="http://github.com/ZsoltSafrany/needle">Needle on GitHub</a>
 */
public class Needle {

    public static final int DEFAULT_POOL_SIZE = 3;
    public static final String DEFAULT_TASK_TYPE = "default";
    public static final long DEFAULT_STACK_SIZE = 0L; //0 means default for current JVM, depends on JVM

    private static Executor sMainThreadExecutor = new MainThreadExecutor();

    /**
     * Use for tasks that need to run on the UI/main thread.
     */
    public static Executor onMainThread() {
        return sMainThreadExecutor;
    }

    /**
     * Use for tasks that don't need the UI/main thread and should be executed on a background thread.
     */
    public static BackgroundThreadExecutor onBackgroundThread() {
        return new ExecutorObtainer();
    }

    static class ExecutorObtainer implements BackgroundThreadExecutor {

        private static Map<ExecutorId, Executor> sCachedExecutors = new HashMap<ExecutorId, Executor>();

        private int mDesiredThreadPoolSize = DEFAULT_POOL_SIZE;
        private String mDesiredTaskType = DEFAULT_TASK_TYPE;
        private long mThreadStackSize = DEFAULT_STACK_SIZE;

        @Override
        public BackgroundThreadExecutor serially() {
            withThreadPoolSize(1);
            return this;
        }

        @Override
        public BackgroundThreadExecutor withTaskType(String taskType) {
            if (taskType == null) {
                throw new IllegalArgumentException("Task type cannot be null");
            }
            mDesiredTaskType = taskType;
            return this;
        }

        @Override
        public BackgroundThreadExecutor withThreadPoolSize(int poolSize) {
            if (poolSize < 1) {
                throw new IllegalArgumentException("Thread pool size cannot be less than 1");
            }
            mDesiredThreadPoolSize = poolSize;
            return this;
        }

        @Override
        public BackgroundThreadExecutor withThreadStackSize(long stackSize) {
            if (stackSize < 0L) {
                throw new IllegalArgumentException("Thread stack size cannot be less than 0");
            }
            mThreadStackSize = stackSize;
            return this;
        }


        @Override
        public void execute(Runnable runnable) {
            if(runnable instanceof Preparable)
                ((Preparable) runnable).prepareOnUi();
            getExecutor().execute(runnable);
        }

        Executor getExecutor() {
            final ExecutorId executorId = new ExecutorId(mDesiredThreadPoolSize, mDesiredTaskType);
            synchronized (ExecutorObtainer.class) {
                Executor executor = sCachedExecutors.get(executorId);
                if (executor == null) {
                    ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(mDesiredThreadPoolSize, mDesiredThreadPoolSize,
                            0L, TimeUnit.MILLISECONDS,  new LinkedBlockingQueue<Runnable>());
                    threadPoolExecutor.setThreadFactory(new NeedleThreadFactory(mThreadStackSize));
                    executor = threadPoolExecutor;
                    //executor = Executors.newFixedThreadPool(mDesiredThreadPoolSize);
                    sCachedExecutors.put(executorId, executor);
                }
                return executor;
            }
        }
    }

    private static class ExecutorId {
        private final int mThreadPoolSize;
        private final String mTaskType;

        private ExecutorId(int threadPoolSize, String taskType) {
            mThreadPoolSize = threadPoolSize;
            mTaskType = taskType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExecutorId executorId = (ExecutorId) o;
            if (mThreadPoolSize != executorId.mThreadPoolSize) return false;
            if (!mTaskType.equals(executorId.mTaskType)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return 31 * mThreadPoolSize + mTaskType.hashCode();
        }
    }

    private static class NeedleThreadFactory implements ThreadFactory {
        private static ThreadGroup threadGroup=new ThreadGroup("NeedleGroup");
        private static AtomicInteger atomicInteger=new AtomicInteger(0);
        private long mStackSize=0L;

        NeedleThreadFactory(long stackSize) {
            mStackSize = stackSize;
        }
        public Thread newThread(Runnable r) {
            return new Thread(threadGroup, r,
                    Integer.valueOf(atomicInteger.addAndGet(1)).toString()+"@needle",
                    mStackSize);
        }
    }
}
