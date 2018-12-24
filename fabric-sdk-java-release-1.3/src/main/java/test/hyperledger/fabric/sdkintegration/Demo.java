package test.hyperledger.fabric.sdkintegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Demo {
    //private static E2eText e2eText = new E2eText();
    private static E2eText e2eText;


    public static void main(String[] args) {
        //FileHelper fh = new FileHelper("/Users/apple/Desktop/e2e.txt");
      //  E2eText e2eText = fh.getObjFromFile();
        E2eText e2eText = new E2eText();
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        String aa = "";
        aa += "123123\n";
        aa += "qweqwe\n";
        System.out.println(aa);
//        try{
//            e2eText.checkConfig();
//            try {
//                e2eText.setup();
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        Scanner sc = new Scanner(System.in);
        while(true) {
                System.out.println("1、下发证书 "+" 2、请求证书");
                int flag = Integer.parseInt(sc.nextLine());
                if(flag == 1){
                    System.out.println("请输入一对汽车公钥及证书包的键值");
                    String a = sc.nextLine();
                   // System.out.println(a);
                    String b = sc.nextLine();
                  //  System.out.println(a +"," + b);
                   e2eText.set(new String[]{a, b});
                }
                else{
                    System.out.println("请输入要汽车公钥key");
                    String c = sc.nextLine();
                   // System.out.println(c);
                    System.out.println("证书包为：" + e2eText.query(c));
                }

        }
//        for(int i = 0; i < 10; ++ i) {
//            String str = RandomNum.createRandomString(100);
//            e2eText.set(new String[]{"dd" + i, str});
//           // System.out.println(i + ": " + str);
//            list1.add(str);
//        }
//
//        for(int i = 0; i < 10; ++ i){
//            list2.add(e2eText.query("dd" + i));
//        }
//        System.out.println("实际值");
//        for(String s : list1){
//            System.out.println(s);
//        }
//        System.out.println("查出值");
//        for(String s : list2){
//            System.out.println(s);
//        }


       // fh.saveObjToFile(e2eText);
    }

}
