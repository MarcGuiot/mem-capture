package org.globsframework.memory;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AllocationTransformerAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            // In Java 9+, the bootstrap class loader doesn't use the classpath.
            // We need to explicitly append our jar to it so that system classes can find our classes.
            String agentJarPath = System.getProperty("java.class.path").split(java.io.File.pathSeparator)[0];
            // If we're running as an agent, the first element might not be our JAR.
            // Let's try to find it more reliably.
            java.io.File agentFile = null;
            if (agentArgs != null && new java.io.File(agentArgs).exists()) {
                agentFile = new java.io.File(agentArgs);
            } else {
                // Try searching in the classpath for our JAR name
                for (String path : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
                    if (path.contains("memory-profiler")) {
                        agentFile = new java.io.File(path);
                        break;
                    }
                }
            }

            if (agentFile != null && agentFile.exists()) {
                inst.appendToBootstrapClassLoaderSearch(new java.util.jar.JarFile(agentFile));
            } else {
                 // Final fallback for the test environment
                 agentFile = new java.io.File("target/memory-profiler-1.0-SNAPSHOT.jar");
                 if (agentFile.exists()) {
                     inst.appendToBootstrapClassLoaderSearch(new java.util.jar.JarFile(agentFile));
                 }
            }
        } catch (Exception e) {
            System.err.println("Failed to append to bootstrap class loader search: " + e.getMessage());
        }

        AllocationRecorderUtil.setInstrumentation(inst);
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                // Don't instrument system classes or our own recorder to avoid infinite loops
                if (className == null ||
                    className.startsWith("java/") ||
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
