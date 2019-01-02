package controller;

import fabric.E2eMainProcess;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CarController {
    //private static E2eText e2eText = new E2eText();
    private E2eMainProcess e2eMainProcess;


    public CarController() {
        e2eMainProcess = new E2eMainProcess();
        e2eMainProcess.run();
    }
    @RequestMapping("index")
    public ModelAndView index(){
        ModelAndView mav = new ModelAndView("index");
        return mav;
    }
    @RequestMapping("set_Car_CA")
    public ModelAndView set(String car_key, String car_ca){
        String logger="";
        //car_key = car_key.replace("\n", "$");
        car_key = car_key.replace("\r","");
        try {
            logger = e2eMainProcess.setTest(new String[]{car_key, car_ca});
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.toString());
        }
        System.out.println(car_key +", "+car_ca);
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("logger", logger);
        return mav;
    }

    @RequestMapping("get_Car_CA")
    public ModelAndView get(String car_key){
        System.out.println("key:");
        System.out.println(car_key);
       // car_key = car_key.replace("\n", "$");
        car_key = car_key.replace("\r","");
       String value =  e2eMainProcess.queryTest(car_key);
       if(value == null){
           value = "暂无该车证书包";
       }
       System.out.println("value: \n" + value);
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("value",value);
        mav.addObject("key",car_key);
        return mav;
    }

//    @RequestMapping("getBlockByNumber")
//    public ModelAndView getBlockByNumber(String block_number){
//        String block_value = "";
//        if(!block_number.equals("")) {
//            System.out.println(block_number);
//             block_value = e2eMainProcess.QueryBlockByNumber(Integer.parseInt(block_number));
//        }
//        if(block_value.equals("")){
//            block_value = "暂无该区块";
//        }
//        ModelAndView mav = new ModelAndView("index");
//        mav.addObject("block_value",block_value);
//        return mav;
//    }
}

