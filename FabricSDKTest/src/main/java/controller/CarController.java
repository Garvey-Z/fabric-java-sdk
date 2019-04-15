package controller;

import fabric.E2eMainProcess;
import fabric.TestDemo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CarController {
    //private static E2eText e2eText = new E2eText();
    private E2eMainProcess certE2e, crlE2e;


    public CarController() {
        certE2e = new E2eMainProcess();
        certE2e.run("certchannel", "cert_cc");
        crlE2e = new E2eMainProcess();
        crlE2e.run("crlchannel", "crl_cc");
    }
    @RequestMapping("cert")
    public ModelAndView cert(){
        ModelAndView mav = new ModelAndView("cert");
        return mav;
    }
    @RequestMapping("set_car_cert")
    public ModelAndView set_cert(String car_key, String car_ca){
        String logger[] = new String[100];
        //car_key = car_key.replace("\n", "$");
        car_key = car_key.replace("\r","");
        try {
            logger = certE2e.setTest(new String[]{car_key, car_ca});
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.toString());
        }
        System.out.println(car_key +", "+car_ca);
        ModelAndView mav = new ModelAndView("cert");
        mav.addObject("logger", logger[0]);
        mav.addObject("proposal_time",logger[1]+"ms");
        mav.addObject("block_time", logger[2] +"ms");
        return mav;
    }

    @RequestMapping("get_car_cert")
    public ModelAndView get_cert(String car_key){
        System.out.println("key:");
        System.out.println(car_key);
       // car_key = car_key.replace("\n", "$");
        car_key = car_key.replace("\r","");
       String value =  certE2e.queryTest(car_key);
       if(value == null){
           value = "暂无该车证书包";
       }
       System.out.println("value: \n" + value);
        ModelAndView mav = new ModelAndView("cert");
        mav.addObject("value",value);
        mav.addObject("key",car_key);
        return mav;
    }

    @RequestMapping("crl")
    public ModelAndView crl(){
        ModelAndView mav = new ModelAndView("crl");
        return mav;
    }

    @RequestMapping("set_car_crl")
    public ModelAndView set_crl(String car_key, String car_bad_flag){
        car_key = car_key.replace("\r", "");
        String logger[] = new String[1000];
        try{
            logger = crlE2e.setTest(new String[]{car_key, car_bad_flag});
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.toString());
        }
        ModelAndView mav = new ModelAndView("crl");
        mav.addObject("logger",logger[0]);
        mav.addObject("proposal_time",logger[1]+"ms");
        mav.addObject("block_time", logger[2] +"ms");
        return mav;
    }
    @RequestMapping("get_car_crl")
    public ModelAndView get_crl(String car_key){
        System.out.println(car_key);
        car_key = car_key.replace("\r", "");
        String value = crlE2e.queryTest(car_key);
//        if(value == null){
//            value = "no";
//        }
        ModelAndView mav = new ModelAndView("crl");
        mav.addObject("value",value);
        mav.addObject("key",car_key);
        return mav;
    }
}

