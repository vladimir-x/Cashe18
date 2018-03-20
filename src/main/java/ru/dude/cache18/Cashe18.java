package ru.dude.cache18;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.*;

/**
 * Потокобезопасный кэш с фукнцией самоочистки.
 * Поддерживает вставку вычисляемых (Callable) значений.
 * При очистке удаляются наиболее старые и редко используемые элементы.
 *
 * TODO: Нет защиты от мнгновенного переполнения (OutOfMemryException).
 * TODO: Нет ручного переопределения функции сравнения элементов кэша на устаревание.
 *
 * Демонстрационный вариант. Для использования в реальных проектах, пока не пригоден.
 */
public class Cashe18 {

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

    // настройка очистки
    private final TimeUnit clearedTimeUnit;
    // настройка очистки
    private final Long clearedPeriod;

    // блокировщик для очистки
    private final CasheLocker cleanLock = new CasheLocker();

    // шедулер для очистки
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

    /**
     * Конструктор по умолчанию
     */
    public Cashe18() {
        this.clearedTimeUnit = TimeUnit.HOURS;
        this.clearedPeriod = 1L;
        this.maxSize = DEFAULT_MAX_SIZE;
        makeCleanTask();
    }

    /**
     * Коснтруктор с настройками
     * @param maxSize - максимальыйн размер кэша. -1 = не ограничено
     * @param clearTimeUnit - еденица времени для расписания очистки
     * @param clearPeriod - период времени  для расписания очистки
     */
    public Cashe18(Long maxSize,TimeUnit clearTimeUnit,Long clearPeriod) {
        this.clearedTimeUnit = clearTimeUnit;
        this.clearedPeriod = clearPeriod;
        this.maxSize = maxSize;
        makeCleanTask();
    }


    /**
     * Поместить или перезаписать
     * Put or rewrite
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
     * Поместить если такого key не существовало
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
     * Поместить или перезаписать
     * Put or rewrite
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
     * Поместить если такого key не существовало
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
     * ПОлучить значение по ключу key. Возвращает null если значения не существовало или вычисление было прервано.
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

    /**
     * Задача для очистки
     */
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

            cleanService.scheduleWithFixedDelay(runnable, clearedPeriod, clearedPeriod, clearedTimeUnit);

        }
    }

    /**
     * Метод очистки
     */
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


    /**
     * Вывод содержимого кэша
     * @param ps
     */
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

    /**
     * Размер кэша
     * @return
     */
    public int getSize(){
        return hashMap.size();
    }

    /**
     * Уничтожает внутренние объекты
     * Destroy inner objects
     */
    public void terminate() {
        futureExecutor.shutdownNow();
        cleanService.shutdownNow();
        cleanLock.terminate();
    }
}
