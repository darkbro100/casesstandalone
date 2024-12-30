package me.paul.util.scheduler;

import lombok.Getter;
import me.paul.CasesStandalone;

public class Async {

  @Getter
  private TaskBuilder builder;

  public Async() {
    builder = TaskBuilder.buildAsync(CasesStandalone.getInstance());
  }

  public static TaskBuilder get() {
    return new Async().getBuilder();
  }
}
