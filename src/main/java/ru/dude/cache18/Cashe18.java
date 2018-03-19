package ru.dude.cache18;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.*;

/**
 */
public class Cashe18 {

    private static final Long CLEARED_HOUR = 1L;

    public static final Long DEFAULT_MAX_SIZE = 50000L;

    public static final Long UNBOUNDED_SIZE = -1L;


    /**
     * Хранение данных: быстрая вставка, выборка
     * Data holder
     */
    private final ConcurrentHashMap<String, CashedItem> hashMap = new ConcurrentHashMap<String, CashedItem>();

    /**
     * Очистка
     * Clear
     */
    private final CasheLocker cleanLock = new CasheLocker();

    private final ScheduledExecutorService cleanService = Executors.newSingleThreadScheduledExecutor();

    /**
     * Execute callable
     * Выполнение callable
     */
    private final ExecutorService futureExecutor =Executors.newFixedThreadPool(2);

    /**
     * Максимальный размер кэша
     * Cashe maximum size
     */
    private final Long maxSize;

    public Cashe18() {
        this.maxSize = UNBOUNDED_SIZE;
        makeCleanTask();
    }

    public Cashe18(Long maxSize) {
        this.maxSize = maxSize;
        makeCleanTask();
    }


    /**
     * put or rewrite
     * @param key
     * @param value
     * @param <E>
     */
    public <E> void put(String key, final E value) {

        Callable<E> callable = new Callable<E>() {
            @Override
            public E call() throws Exception {
                return value;
            }
        };
        put(key, callable);
    }

    /**
     * Put only if not exist
     * @param key
     * @param value
     * @param <E>
     */
    public <E> void putIfAbsent(String key, final E value) {

        Callable<E> callable = new Callable<E>() {
            @Override
            public E call() throws Exception {
                return value;
            }
        };
        putIfAbsent(key, callable);
    }

    /**
     * put or rewrite
     *
     * @param key
     * @param callable
     * @param <E>
     */
    public <E extends Object> void put(String key, Callable<E> callable) {

        cleanLock.awaitIfLock();
        // cleanCondition.await();
        CashedItem ci = new CashedItem(key);
        CashedItem before = hashMap.put(key, ci);
        if (before != null) {
            //prevous
            before.getFuture().cancel(true);
        }
        ci.setFuture(futureExecutor.submit(callable));

    }


    /**
     * Put only if not exist
     *
     * @param key
     * @param callable
     * @param <E>
     */
    public <E extends Object> void putIfAbsent(String key, Callable<E> callable) {

        cleanLock.awaitIfLock();

        CashedItem ci = new CashedItem(key);
        CashedItem before = hashMap.putIfAbsent(key, ci);
        if (before == null) {
            ci.setFuture(futureExecutor.submit(callable));
        }
    }

    /**
     * get value or null if not exist, or execution has been canselled
     *
     * @param key
     * @param <E>
     * @return
     */
    public <E> E get(String key) {

        cleanLock.awaitIfLock();

        CashedItem ci = hashMap.get(key);
        try {
            if (ci!=null){
                ci.markExtact();
                return (E) ci.getFuture().get();
            }
            return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }


    private void makeCleanTask() {
        if (maxSize != UNBOUNDED_SIZE) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        promptClear();
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
            };

            cleanService.scheduleWithFixedDelay(runnable, CLEARED_HOUR, CLEARED_HOUR, TimeUnit.HOURS);

        }
    }

    private void promptClear() {
        if (maxSize != UNBOUNDED_SIZE) {
            cleanLock.lock();
            try {
                while (hashMap.size() > maxSize) {

                    final PriorityQueue<CashedItem> pqueue = new PriorityQueue(hashMap.values());
                    while(pqueue.size()>maxSize){
                        CashedItem ci = pqueue.poll();
                        hashMap.remove(ci.getKey());
                    }
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                cleanLock.release();
            }
        }
    }


    public void printAll(PrintStream ps) {
        Iterator<Map.Entry<String, CashedItem>> it = hashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CashedItem> next = it.next();
            String key = next.getKey();
            Future<?> future = next.getValue().getFuture();
            if (!future.isCancelled()) {
                try {
                    ps.println(key + ":" + future.get() + "("+next.getValue().getExtractCount() + ")");
                } catch (InterruptedException e) {
                    ps.println(key + ":null");
                } catch (ExecutionException e) {
                    ps.println(key + ":null");
                }
            }
        }
    }

    public int getSize(){
        return hashMap.size();
    }

    public void terminate() {
        futureExecutor.shutdownNow();
        cleanService.shutdownNow();
        cleanLock.terminate();
    }
}
