package ru.dude.cache18;

import java.util.concurrent.*;

/**
 * Пример использования
 */
public class CacheTest {

    public static void main(String [] args) throws Exception{

        final Cashe18 cashe18 = new Cashe18();

        cashe18.put("one","XAASDASDD1234ASd45ASd");
        cashe18.put("two","545ASDASD7");
        cashe18.put("three","524565aascascasdqauya");

        cashe18.putIfAbsent("sum", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "123" + "456";
            }
        });

        System.out.println(">>>two =" +cashe18.get("two"));
        System.out.println(">>>sum =" +cashe18.get("sum"));

        cashe18.terminate();
    }

}
