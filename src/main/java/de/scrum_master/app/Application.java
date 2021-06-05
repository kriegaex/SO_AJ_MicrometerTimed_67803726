package de.scrum_master.app;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;

import java.util.Random;

public class Application {
  private static final Random RANDOM = new Random();

  public static void main(String[] args) throws InterruptedException {
    Metrics.addRegistry(new LoggingMeterRegistry());
    Application application = new Application();
    for (int i = 0; i < 10; i++) {
      application.doSomething();
      application.doSomethingElse();
    }
    Thread.sleep(2 * 60 * 1000);
  }

  @Timed
  public void doSomething() throws InterruptedException {
    Thread.sleep(RANDOM.nextInt(250));
  }

  public void doSomethingElse() {}
}
