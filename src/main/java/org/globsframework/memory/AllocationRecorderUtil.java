package org.globsframework.memory;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;

public class AllocationRecorderUtil {
    // Avoid recursion by using a ThreadLocal flag
    private static final ThreadLocal<Boolean> IN_RECORDER = ThreadLocal.withInitial(() -> false);
    public static final byte[] ARRAY = "\narray: ".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CLASS = "\nclass: ".getBytes(StandardCharsets.UTF_8);
    public static final byte[] RET = "\n".getBytes(StandardCharsets.UTF_8);
    public static final int MAX_STACK = 20;
    public static final String OUTPUT_MEM = System.getProperty("OUTPUT_MEM", "memory.out");
    public static final byte[] SEPARATOR = "-------\n".getBytes(StandardCharsets.UTF_8);
    private static Instrumentation instrumentation;
    private static volatile OutputStream outputStream = new ByteArrayOutputStream();
    private static volatile boolean running = System.getProperty("ALLOCATION_TRACING") != null;
    private static Clock clock;

    public static void init(Instrumentation inst) {
        clock = Clock.systemDefaultZone();
        ZonedDateTime.now(clock); // force init of internal structure
        instrumentation = inst;
        if (running) {
            newFile();
        }
    }

    private static void newFile() {
        try {
            OutputStream tmp;
            synchronized (outputStream) {
                tmp = outputStream;
                final File file = new File(OUTPUT_MEM);
                if (file.exists()) {
                    file.renameTo(new File(OUTPUT_MEM + "." + System.currentTimeMillis()));
                }
                outputStream = new BufferedOutputStream(new FileOutputStream(OUTPUT_MEM));
            }
            synchronized (tmp) {
                tmp.close();
            }
        } catch (Exception e) {
        }
    }

    private static final java.util.Map<String, Long> sizeCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static sun.misc.Unsafe unsafe;
    static {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {}
    }

    public static void record(String className, Class<?> clazz) {
        if (!running || IN_RECORDER.get()) {
            return;
        }
        IN_RECORDER.set(true);
        try {
            long size = -1;
            if (instrumentation != null && clazz != null) {
                Long cachedSize = sizeCache.get(className);
                if (cachedSize != null) {
                    size = cachedSize;
                } else {
                    if (unsafe != null) {
                        try {
                            Object dummy = unsafe.allocateInstance(clazz);
                            size = instrumentation.getObjectSize(dummy);
                            sizeCache.put(className, size);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            synchronized (outputStream) {
                final OutputStream tmp = outputStream;
                tmp.write(SEPARATOR);
                tmp.write(ZonedDateTime.now(clock).toString().getBytes(StandardCharsets.UTF_8));
                final byte[] bytes = className.getBytes(StandardCharsets.UTF_8);
                tmp.write(CLASS);
                tmp.write(bytes);
                tmp.write(' ');
                tmp.write(Long.toString(size).getBytes(StandardCharsets.UTF_8));
                tmp.write(RET);
                dumpStackTrace(tmp);
            }
        } catch (Throwable e) {
            System.err.println(e.getMessage());
        } finally {
            IN_RECORDER.set(false);
        }
    }

    private static void dumpStackTrace(OutputStream outputStream) throws IOException {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stackTrace.length, MAX_STACK); i++) {
            final String cl = stackTrace[i].getClassName();
            final String methodName = stackTrace[i].getMethodName();
            final int lineNumber = stackTrace[i].getLineNumber();
            outputStream.write((cl + "." + methodName + ":" + lineNumber).getBytes(StandardCharsets.UTF_8));
            outputStream.write(RET);
        }
        outputStream.flush();
    }

    public static void recordArray(Object array, String type) {
        if (!running || IN_RECORDER.get()) {
            return;
        }
        IN_RECORDER.set(true);
        try {
            long size = -1;
            if (instrumentation != null && array != null) {
                size = instrumentation.getObjectSize(array);
            }
            synchronized (outputStream) {
                final OutputStream tmp = outputStream;
                tmp.write(SEPARATOR);
                tmp.write(ZonedDateTime.now(clock).toString().getBytes(StandardCharsets.UTF_8));
                tmp.write(ARRAY);
                tmp.write(type.getBytes(StandardCharsets.UTF_8));
                tmp.write(' ');
                tmp.write(Long.toString(size).getBytes(StandardCharsets.UTF_8));
                tmp.write(RET);
                dumpStackTrace(tmp);

//            AllocationEvent event = new AllocationEvent(type, size);
//            event.commit();
            }
        } catch (Throwable e) {
            System.err.println(e.getMessage());
        } finally {
            IN_RECORDER.set(false);
        }
    }

    public static void toggle() {
        if (!running) {
            newFile();
        }
        running = !running;
        System.out.println("Allocation tracing: " + (running ? "ON" : "OFF"));
    }
}
