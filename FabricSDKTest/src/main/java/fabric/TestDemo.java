package fabric;

public class TestDemo {
    public static void main(String[] args) {
        E2eMainProcess e2eMainProcess = new E2eMainProcess();
        e2eMainProcess.run();
    //   System.out.println(e2eMainProcess.setTest(new String[]{"qaz","qqqweqwe"}));
       System.out.println(e2eMainProcess.queryTest("a"));
    }
}
