package ru.dude.cache18;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Элемент кэша
 * Cache element
 */
public class CashedItem implements Comparable<CashedItem>{

    /**
     * Ключ
     */
    private final String key;

    /**
     * значение
     */
    private Future<? extends Object> future;

    /**
     * Количество извлечений из кэша
     * Extract counter
     */
    private final AtomicLong extractCounter;

    /**
     * Метка последнего извлечения
     * Last access mark
     */
    private final AtomicLong lastExtract;


    /**
     * Коснтруктор с отложенной установкой значения
     * @param key
     */
    public CashedItem(String key) {
        this.key = key;

        extractCounter  = new AtomicLong(1L);
        lastExtract = new AtomicLong(new Date().getTime());
    }

    /**
     * Коснтруктор с установкой ключа и значения
     * @param key
     * @param future
     */
    public CashedItem(String key, Future<? extends Object> future) {
        this.key = key;
        this.future = future;

        extractCounter  = new AtomicLong(1L);
        lastExtract = new AtomicLong(new Date().getTime());
    }

    /**
     * Зрегистрирвоать факт извлечения
     */
    public void markExtact(){
        extractCounter.incrementAndGet();
        lastExtract.set(new Date().getTime());
    }

    /**
     * Ключ
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * отложенная установка значения
     * @param future
     */
    public void setFuture(Future<? extends Object> future) {
        this.future = future;
    }

    /**
     * Значение
     * @return
     */
    public Future<? extends Object> getFuture() {
        return future;
    }


    /**
     * количество извлечений
     * @return
     */
    public long getExtractCount(){
        return  extractCounter.get();
    }

    /**
     * Компаратор, для выделения устаревших и не популярных данных
     * @param o
     * @return
     */
    @Override
    public int compareTo(CashedItem o) {

        if (o == null) return -1;
        long mx = Math.max(lastExtract.get() , o.lastExtract.get());
        double k = ((double) lastExtract.get()) / mx;
        double ko = ((double) o.lastExtract.get()) / mx;

        double t = k * extractCounter.get();
        double to = ko * o.extractCounter.get();

        return Double.compare(t,to);
    }
}
