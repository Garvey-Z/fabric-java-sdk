package fabric;

public class TestDemo {
    public static void main(String[] args) {
        E2eMainProcess e2eMainProcess = new E2eMainProcess();
        e2eMainProcess.run();
    //  System.out.println(e2eMainProcess.setTest(new String[]{"bb","qq"}));
       System.out.println(e2eMainProcess.queryTest("aaa"));
        System.out.println(e2eMainProcess.queryTest("bb"));
    }
}
