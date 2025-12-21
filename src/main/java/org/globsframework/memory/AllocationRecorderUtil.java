package org.globsframework.memory;

import java.lang.instrument.Instrumentation;


public class AllocationRecorderUtil {
    // Avoid recursion by using a ThreadLocal flag
    private static final ThreadLocal<Boolean> IN_RECORDER = ThreadLocal.withInitial(() -> false);
    private static Instrumentation instrumentation;

    public static void setInstrumentation(Instrumentation inst) {
        instrumentation = inst;
    }

    public static void record(String className) {
        if (IN_RECORDER.get()) return;
        IN_RECORDER.set(true);
        try {
            AllocationEvent event = new AllocationEvent(className, -1);
            event.commit();
        } finally {
            IN_RECORDER.set(false);
        }
    }

    public static void recordArray(Object array, String type) {
        if (IN_RECORDER.get()) return;
        IN_RECORDER.set(true);
        try {
            long size = -1;
            if (instrumentation != null && array != null) {
                size = instrumentation.getObjectSize(array);
            }
            AllocationEvent event = new AllocationEvent(type, size);
            event.commit();
        } finally {
            IN_RECORDER.set(false);
        }
    }
}
