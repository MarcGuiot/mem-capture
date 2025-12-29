package org.globsframework.memory;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AllocationTransformerAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            File agentFile = new File(System.getProperty("mem.profiler", "target/memory-profiler-1.1-SNAPSHOT.jar"));
            if (agentFile.exists()) {
                inst.appendToBootstrapClassLoaderSearch(new java.util.jar.JarFile(agentFile));
            }
        } catch (Exception e) {
            System.err.println("Failed to append to bootstrap class loader search: " + e.getMessage());
        }

        AllocationRecorderUtil.init(inst);

        SignalHandler handler = signal -> AllocationRecorderUtil.toggle();
        try {
            Signal.handle(new Signal("USR1"), handler);
        } catch (Exception e) {
            System.err.println("Failed to register USR1 signal handler: " + e.getMessage());
        }

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                // Don't instrument system classes or our own recorder to avoid infinite loops
                if (className == null ||
                    className.startsWith("java/lang/ThreadLocal") ||
                    className.startsWith("jdk/") ||
                    className.startsWith("sun/") ||
                    className.startsWith("com/sun/") ||
                    className.startsWith("org/globsframework/memory/")) {
                    return null;
                }

                ClassReader cr = new ClassReader(classfileBuffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                AllocationTrackingTransformer transformer = new AllocationTrackingTransformer(cw);
                cr.accept(transformer, 0);
                return cw.toByteArray();
            }
        });
    }
}
