# CACHE18

Простая реализация потокобезопасного кэшера для Java SE приложений.

Поддерживает:
* потокобезопасные вставки и извлечение данных по ключу типа String
* вставку вычисляемых значений ( Callable<E> )
* очистку кэша по расписанию с приоритетом наиболее устаревших и не популярных записей

Не поддерживает:
* Нет защиты от переполнения памяти
* Нет механизма переопределения функции приоритета очистки

В связи с существующими недостатками использование в реальных проектах не рекомендуется. 
Лучше обратите внимение на существующие популярные кэшеры.


## Пример использования

		Cashe18 cashe18 = new Cashe18();

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
		
		