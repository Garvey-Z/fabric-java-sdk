package fabric;

public class TestDemo {
    public static void main(String[] args) {
        E2eMainProcess e2eMainProcess = new E2eMainProcess();
       //System.out.println(e2eMainProcess.run("peer0.org2.example.com"));
       System.out.println(e2eMainProcess.queryTest("aaa"));
       System.out.println(e2eMainProcess.queryTest("bb"));
    }
}
