package ru.dude.cache18;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * блокировка для безопасной очистки
 * Lock for safe clear
 */
public class CasheLocker {

    private final AtomicBoolean isLock = new AtomicBoolean();

    private final Phaser phaser = new Phaser(1);

    /**
     * Блокирует поток, если установлена блокировка
     * Block thread if lock is enable
     */
    public void awaitIfLock(){
        if (isLock.get()){
            phaser.awaitAdvance(phaser.getPhase());
        }
    }

    /**
     * Устанавливает блокировку
     * Set lock
     */
    public void lock(){
        isLock.set(true);

    }

    /**
     * Снимает блокировку, возобновляет все потоки, заблокированные во время блокировки
     * Release lock and resume all waiting threads
     */
    public void release(){
        if (isLock.get()) {
            isLock.set(false);
            phaser.arrive();
        }
    }

    /**
     * Уничтожает внутренние объекты
     * Destroy inner objects
     */
    public void terminate() {
        phaser.forceTermination();
    }
}
