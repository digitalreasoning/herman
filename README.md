herman
======

A modification to the service loader pattern that uses embedded jars to create an isolated classloader for the service implementation.


Basic Use
---------

```java

Iterable<MyServiceInterface> implementations =
    IsolatedServiceLoader.builder(MyServiceInterface.class)
                         .excludes("com.example.myapi.internal.*")
                         .includes("com.example.myapi.*")
                         .build();
```

[![Build Status](https://travis-ci.org/digitalreasoning/herman.png?branch-master)](https://travis-ci.org/digitalrasoning/herman.png?branch=master)
