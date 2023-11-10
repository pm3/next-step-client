package io.aston.nextstep.model;

public record Event(EventType type, String name, Workflow workflow, Task task) {
}
