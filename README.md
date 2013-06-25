herman
======

A modification to the service loader pattern that uses embedded jars to create an isolated classloader for the service implementation.


Basic Use
---------

```java

Iterable<MyServiceInterface> implementations =  IsolatedServiceSet.loader(MyServiceInterface.class)
												                  .negativeFilters(new String[] {
												                     "com.example.myapi.internal.*"
												                  })
												                  .positiveFilters(new String[] {
												                     "com.example.myapi.*"
												                  }).load();
```
