package ru.dude.cache18;

import java.util.concurrent.*;

public class CacheTest {

    public static void main(String [] args) throws Exception{



        final Cashe18 cashe18 = new Cashe18(5L);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int j=0;j<100;++j) {
            for (int i = 0; i <100; ++i) {
                final int k = i;
                final int finalJ = j;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        cashe18.putIfAbsent("key="+ finalJ, k);
                    }
                };
                executor.execute(r);
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);

        System.out.println(">>>GET =" +cashe18.get("key="+ 61));
        System.out.println(">>>GET =" +cashe18.get("key="+ 61));
        System.out.println(">>>GET =" +cashe18.get("key="+ 61));

        cashe18.put("one","XAASDASDD1234ASd45ASd");
        cashe18.put("two","545ASDASD7");
        cashe18.put("three","524565aascascasdqauya");



        System.out.println(">>>two =" +cashe18.get("two"));

        //cashe18.printAll( System.out);

        startTick();


        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cashe18.printAll( System.out);

        cashe18.terminate();
    }

    private static void startTick(){
        new Thread() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Thread.sleep(500);
                        System.out.print(".");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }
}
