package org.globsframework.test.memory;

public class TestAllocation {

    public static void main(String[] args) throws InterruptedException {
        Integer a = 100;
        Test test = new Test("dfff");
        System.out.println(test.toString("a"));
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
