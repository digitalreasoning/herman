herman
======

A modification to the service loader pattern that uses embedded jars to create an isolated classloader for the service implementation.


Basic Use
---------

```java

Iterable<MyServiceInterface> implementations =
    IsolatedServiceLoader.builder(MyServiceInterface.class)
                         .negativeFilters("com.example.myapi.internal.*")
                         .positiveFilters("com.example.myapi.*")
                         .build();
```
