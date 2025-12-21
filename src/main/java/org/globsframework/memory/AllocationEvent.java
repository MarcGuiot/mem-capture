package org.globsframework.memory;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("org.globsframework.memory.AllocationEvent")
@Label("Object Allocation Instrumentation")
@StackTrace(true)
public class AllocationEvent extends Event {
    @Label("Class Name")
    private final String className;

    @Label("Size")
    private final long size;

    public AllocationEvent(String className, long size) {
        this.className = className;
        this.size = size;
    }
}
