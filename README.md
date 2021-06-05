# Micrometer `@Timed` + AspectJ compile-time weaving

In https://github.com/micrometer-metrics/micrometer/issues/1149 and on StackOverflow, an FAQ about Micrometer's `@Timed` annotation is,
why it works with Spring AOP, but not when using Micrometer as an aspect library for native AspectJ in the context of compile-time weaving (CTW),
e.g. with AspectJ Maven Plugin. It can be made to work with load-time weaving (LTW) when providing an `aop.xml` pointing to `TimedAspect`,
but in a CTW the aspect never kicks in.

The reason is that the aspect has been compiled with Javac, not with the AspectJ compiler (AJC), which is necessary to "finish" the Java class,
i.e. to enhance its byte code in order to be a full AspectJ aspect. The LTW agent does that on the fly during class-loading, but in a CTW context
you need to explicitly tell AJC to do post-compile weaving (a.k.a. binary weaving) on the Micrometer library, producing newly woven class files.
This is done by putting Micrometer on AJC's _**inpath**_ in order to make sure that its class files are being transformed and written to the target
directory. The inpath in AspectJ Maven is configured via `<weaveDependencies>`. There are at least two ways to do this:

  * You can either create your own woven version of the library in a separate Maven module and then use that module instead of Micrometer.
    In that case, you need to make sure to exclude the original Micrometer library in the consuming module, in order to make sure that the unwoven
    class files are not on the classpath anymore and accidentally used.

  * The way shown here in this example project is a single-module approach, building an executable uber JAR with Maven Shade. The Micrometer class
    files are not a re-usable library like in the first approach, but it is nice for demonstration purposes, because we can just run the sample
    application and check its output:

```text
$ mvn clean package

...
[INFO] --- aspectj-maven-plugin:1.12.6:compile (default) @ SO_AJ_MicrometerTimed_67803726 ---
[INFO] Showing AJC message detail for messages of types: [error, warning, fail]
[INFO] Join point 'method-execution(void de.scrum_master.app.Application.doSomething())' in Type 'de.scrum_master.app.Application' (Application.java:23) advised by around advice from 'io.micrometer.core.aop.TimedAspect' (micrometer-core-1.7.0.jar!TimedAspect.class(from TimedAspect.java))
...
[INFO] --- maven-shade-plugin:3.2.4:shade (default) @ SO_AJ_MicrometerTimed_67803726 ---
[INFO] Including org.hdrhistogram:HdrHistogram:jar:2.1.12 in the shaded jar.
[INFO] Including org.latencyutils:LatencyUtils:jar:2.0.3 in the shaded jar.
[INFO] Including org.aspectj:aspectjrt:jar:1.9.6 in the shaded jar.
[INFO] Excluding io.micrometer:micrometer-core:jar:1.7.0 from the shaded jar.
[INFO] Replacing original artifact with shaded artifact.
[INFO] Replacing C:\Users\me\java-src\SO_AJ_MicrometerTimed_67803726\target\SO_AJ_MicrometerTimed_67803726-1.0-SNAPSHOT.jar with C:\Users\me\java-src\SO_AJ_MicrometerTimed_67803726\target\SO_AJ_MicrometerTimed_67803726-1.0-SNAPSHOT-shaded.jar
[INFO] Dependency-reduced POM written at: C:\Users\me\java-src\SO_AJ_MicrometerTimed_67803726\target\dependency-reduced-pom.xml
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------

$ java -jar target/SO_AJ_MicrometerTimed_67803726-1.0-SNAPSHOT.jar

Juni 05, 2021 1:12:27 PM io.micrometer.core.instrument.push.PushMeterRegistry start
INFO: publishing metrics for LoggingMeterRegistry every 1m
Juni 05, 2021 1:13:00 PM io.micrometer.core.instrument.logging.LoggingMeterRegistry lambda$publish$5
INFO: method.timed{class=de.scrum_master.app.Application,exception=none,method=doSomething} throughput=0.166667/s mean=0.11842469s max=0.2146482s
```

Please specifically note those log lines (line breaks inserted for better readability):

```text
Join point 'method-execution(void de.scrum_master.app.Application.doSomething())'
  in Type 'de.scrum_master.app.Application' (Application.java:23)
  advised by around advice from 'io.micrometer.core.aop.TimedAspect'
  (micrometer-core-1.7.0.jar!TimedAspect.class(from TimedAspect.java))
```

The above is proof that the `@Timed` annotation actually causes Micrometer's `TimedAspect` to be woven into our application code. And here are
the measurements created by the aspect for the sample application:

```text
method.timed
  {class=de.scrum_master.app.Application,exception=none,method=doSomething}
  throughput=0.166667/s mean=0.11842469s max=0.2146482s
```
