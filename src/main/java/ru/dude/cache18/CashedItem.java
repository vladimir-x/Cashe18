package ru.dude.cache18;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Элемент кэша
 * Cache element
 */
public class CashedItem implements Comparable<CashedItem>{
    private final String key;
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


    public CashedItem(String key) {
        this.key = key;

        extractCounter  = new AtomicLong(1L);
        lastExtract = new AtomicLong(new Date().getTime());
    }

    public CashedItem(String key, Future<? extends Object> future) {
        this.key = key;
        this.future = future;

        extractCounter  = new AtomicLong(1L);
        lastExtract = new AtomicLong(new Date().getTime());
    }

    public void markExtact(){
        extractCounter.incrementAndGet();
        lastExtract.set(new Date().getTime());
    }

    public String getKey() {
        return key;
    }

    public void setFuture(Future<? extends Object> future) {
        this.future = future;
    }

    public Future<? extends Object> getFuture() {
        return future;
    }


    public long getExtractCount(){
        return  extractCounter.get();
    }

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
