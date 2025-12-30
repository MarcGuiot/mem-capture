package org.globsframework.test.memory;

import org.junit.jupiter.api.Assertions;
import sun.misc.Signal;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

public class AllocationTest {


    // start "test" with
    // -javaagent:target/memory-profiler-1.1-SNAPSHOT.jar
    // -Dmem.profiler=target/memory-profiler-1.1-SNAPSHOT.jar
    // -DCONFIG_MEM=testConfig.cfg
    // -DOUTPUT_MEM=testOutput.txt



    public static void main(String[] args) throws InterruptedException, IOException {
        Integer a = 100;
        Test test = new Test("dfff");
        System.out.println(test.toString("a"));

        System.setProperty("CONFIG_MEM", "testConfig.cfg");
        System.setProperty("OUTPUT_MEM", "testOutput.cfg");


        final File configFile = new File(System.getProperty("CONFIG_MEM"));
        final File outputMem = new File(System.getProperty("OUTPUT_MEM"));
        configFile.delete();
        outputMem.delete();

        CompletableFuture<Void> start = new CompletableFuture<>();
        CompletableFuture<Void> waitReload = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            List<Test> list = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                list.add(new Test("a " + i));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (i == 20) {
                    start.complete(null);
                    waitReload.join();
                }
            }
            System.out.println(list.get(10).toString("b"));
        });
        Signal.raise(new Signal("USR1"));
        thread.start();
        start.join();
        final FileOutputStream fileOutputStream = new FileOutputStream(configFile);
        fileOutputStream.write("WithStackTrace=true".getBytes());
        Signal.raise(new Signal("USR2"));
        waitReload.complete(null);
        thread.join();

        Signal.raise(new Signal("USR1"));

        outputMem.deleteOnExit();
        configFile.deleteOnExit();

        BufferedReader content = new BufferedReader(new FileReader(outputMem));
        String line;
        while ((line = content.readLine()) != null) {
            if (line.startsWith("java.lang.Thread.run:")) {
                return;
            }
        }
        Assertions.fail("No stack trace found");
    }

    static class Test {
        private final String a;

        public Test(String a) {
            this.a = a;
        }

        public String toString(String b) {
            return a + " " + b;
        }
    }
}
