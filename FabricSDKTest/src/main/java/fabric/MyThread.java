package fabric;

public class MyThread extends Thread {
    private int id;
    private E2eMainProcess e2eMainProcess;
    private long t1, t2;
    public MyThread(int a, E2eMainProcess e){
        id = a;
        e2eMainProcess = e;
    }
    @Override
    public void run(){
//        String result = e2eMainProcess.queryTest("zju"+id);
//        if(result.equals("edu"+id))
//          System.out.println("ok");
       // System.out.println(System.currentTimeMillis());
      //  t1 = System.currentTimeMillis();
        e2eMainProcess.setTest(new String[]{"a"+id, "ab"+id});
       // t2 = System.currentTimeMillis();
        //System.out.println("t1: "+ t1 + "\n" + "t2: " + t2);
        //System.out.println(id + "时间为：" + (t2 - t1) + "ms");
      //  String result = e2eMainProcess.queryTest("a"+id);

       // System.out.println(id + "查询时间为：" + (t2 - t1) + "ms");
       // System.out.println(System.currentTimeMillis());
    }
}
