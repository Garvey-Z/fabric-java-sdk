package controller;

import fabric.E2eMainProcess;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CarController {
    //private static E2eText e2eText = new E2eText();
    private E2eMainProcess e2eMainProcess = new E2eMainProcess();
    private String PeerName=null;
    private Map<String, String> peerNameList = new HashMap<>();

    public CarController() {
        peerNameList.put("p1","peer0.org1.example.com");
        peerNameList.put("p2","peer1.org1.example.com");
        peerNameList.put("p3","peer0.org2.example.com");
        peerNameList.put("p4","peer1.org2.example.com");
        File file = new File("1.txt");
        System.out.println(file.getPath());
        System.out.println(file.getAbsolutePath());

    }
    @RequestMapping("index")
    public ModelAndView index(String peerName, HttpServletRequest request) throws IOException {
        ModelAndView mav = new ModelAndView("index");
        System.out.println(request.getServletContext().getRealPath(""));
        System.out.println(peerName);
        if(peerName != PeerName) {
            System.out.println(peerName+ ", " + PeerName);
            e2eMainProcess.run(peerName);
            PeerName = peerName;
        }
        mav.addObject("PeerName", PeerName);
        //   mav.addObject("peerNameList",peerNameList);
        return mav;
    }
    @RequestMapping("get_Car_CA")
    public ModelAndView get(String car_key){

        System.out.println("key:");
        System.out.println(car_key);
        // car_key = car_key.replace("\n", "$");
        car_key = car_key.replace("\r","");
        System.out.println("e2e: " + e2eMainProcess);
        String value =  e2eMainProcess.queryTest(car_key);
        if(value == null){
            value = "暂无该车证书包";
        }
        System.out.println("value: \n" + value);
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("PeerName", PeerName);
        mav.addObject("value",value);
        mav.addObject("key",car_key);
        return mav;
    }
}

