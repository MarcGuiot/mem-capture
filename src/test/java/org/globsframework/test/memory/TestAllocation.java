package org.globsframework.test.memory;

import java.util.ArrayList;
import java.util.List;

public class TestAllocation {


    // start "test" with
    // -javaagent:target/memory-profiler-1.1-SNAPSHOT.jar
    // -Dmem.profiler=target/memory-profiler-1.1-SNAPSHOT.jar
    // -DALLOCATION_TRACING=true

    public static void main(String[] args) throws InterruptedException {
        Integer a = 100;
        Test test = new Test("dfff");
        System.out.println(test.toString("a"));
        List<Test> list = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            list.add(new Test("a " + i));
        }
        System.out.println(list.get(100).toString("b"));
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
