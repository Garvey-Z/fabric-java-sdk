package fabric;

public class TestDemo {
    public static void main(String[] args) {
        int count = 20;
        E2eMainProcess e2eMainProcess1 = new E2eMainProcess();
//        E2eMainProcess e2eMainProcess2 = new E2eMainProcess();
//       // e2eMainProcess.run("crlchannel", "crl_cc");
        e2eMainProcess1.run("certchannel","cert_cc");
//        e2eMainProcess2.run("certchannel","cert_cc");

      System.out.println(e2eMainProcess1.setTest(new String[]{"qwe","asda"}));
     //  System.out.println(e2eMainProcess.queryTest("asd"));
//        System.out.println(e2eMainProcess.queryTest("zju_test"));
       // int a[] = new int[100];
        E2eMainProcess e2eMainProcess[] = new E2eMainProcess[1000];
        for(int i = 0; i < count; ++i) {
          //  e2eMainProcess[i] = new E2eMainProcess();
            //e2eMainProcess[i].run("certchannel","crl_cc");
        }

        MyThread a[] = new MyThread[1000];
        for(int i = 0; i < count; ++ i) {
          //  e2eMainProcess.setTest(new String[]{"zju"+ i, "edu" + i});
            e2eMainProcess[i] = new E2eMainProcess();
            e2eMainProcess[i].run("certchannel","cert_cc");
            a[i] = new MyThread(i, e2eMainProcess[i]);
           // a[i].run();
        }
//        a[0] = new MyThread(0, e2eMainProcess1);
//        a[1] = new MyThread(0, e2eMainProcess2);
        for(int i = 0; i < count; ++ i){
//
           a[i].start();
        }
    }


}
