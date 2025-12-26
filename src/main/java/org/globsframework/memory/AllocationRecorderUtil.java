package org.globsframework.memory;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;

public class AllocationRecorderUtil {
    // Avoid recursion by using a ThreadLocal flag
    private static final ThreadLocal<Boolean> IN_RECORDER = ThreadLocal.withInitial(() -> false);
    public static final byte[] ARRAY = "array: ".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CLASS = "class: ".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RET = "\n".getBytes(StandardCharsets.UTF_8);
    public static final int MAX_STACK = 20;
    private static Instrumentation instrumentation;
    private static OutputStream outputStream;

    public static void init(Instrumentation inst) {
        instrumentation = inst;
        try {
            outputStream = new BufferedOutputStream(
                    new FileOutputStream(System.getProperty("OUTPUT_MEM", "memory.out")));
        } catch (FileNotFoundException e) {
        }
    }

    public static void record(String className) {
        if (IN_RECORDER.get()) {
            return;
        }
        IN_RECORDER.set(true);
        try {
            synchronized (outputStream) {
                final byte[] bytes = className.getBytes(StandardCharsets.UTF_8);
                outputStream.write(CLASS);
                outputStream.write(bytes);
                outputStream.write(RET);
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (int i = 2; i < Math.min(stackTrace.length, MAX_STACK); i++) {
                    outputStream.write(stackTrace[i].toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(RET);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            } finally {
            IN_RECORDER.set(false);
        }
    }

    public static void recordArray(Object array, String type) {
        if (IN_RECORDER.get()) {
            return;
        }
        IN_RECORDER.set(true);
        try {
//            long size = -1;
//            if (instrumentation != null && array != null) {
//                size = instrumentation.getObjectSize(array);
//            }
            synchronized (outputStream) {
                final byte[] bytes = type.getBytes(StandardCharsets.UTF_8);
                outputStream.write(ARRAY);
                outputStream.write(bytes);
                outputStream.write(RET);
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (int i = 2; i < Math.min(stackTrace.length, MAX_STACK); i++) {
                    outputStream.write(stackTrace[i].toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(RET);
                }
                outputStream.flush();

//            AllocationEvent event = new AllocationEvent(type, size);
//            event.commit();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            IN_RECORDER.set(false);
        }
    }
}
