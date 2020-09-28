package com.controller;

import TTCEPackage.BiDa;
import TTCEPackage.K7X0Util;
import TTCEPackage.UnsignedByteByReference;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.entity.DoorCard;
import com.entity.Record;
import com.entity.TblTxnp;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.service.IRecordService;
import com.service.ITblTxnpService;
import com.utils.Card.PrintUtil;
import com.utils.DaPuWeiDa;
import com.utils.EmailUtil;
import com.utils.Http.HttpUtil;
import com.utils.ReturnNullOrKong;
import com.utils.Returned2.AutoLog;
import com.utils.Returned2.SetResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.utils.Returned2.Result;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.*;

import static TTCEPackage.K7X0Util.check;


/**
 * 卡Controller
 */
@Slf4j
@Api(tags = "制读卡")
@RestController
@RequestMapping("/zzj/card")
public class CardController {

    @Value("${sdk.ComHandle}")
    private Integer comHandle;


    @Value("${print.phone}")
    private String phone;
    @Value("${print.leave}")
    private String leave;
    @Value("${print.wifiname}")
    private String wifiname;
    @Value("${print.wifipass}")
    private String wifipass;

    @Value("${dapuPort}")
    private int dapuPort;
    @Value("${Email.HOST}")
    private String HOST;

    @Value("${Email.FROM}")
    private String FROM;

    @Value("${Email.AFFIXNAME}")
    private String AFFIXNAME;

    @Value("${Email.USER}")
    private String USER;

    @Value("${Email.PWD}")
    private String PWD;

    @Value("${Email.SUBJECT}")
    private String SUBJECT;

    @Value("${Email.cardEmail}")
    private String cardEmail;

    @Autowired
    private RabbitHelper rabbitHelper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String cardQueue = "cardQueue";

    private final String checkoutQueue = "checkoutQueue";
    @Autowired
    private ITblTxnpService tblTxnpService;

    @Autowired
    private IRecordService iRecordService;//发卡记录

    /**
     * 发送自助机无卡邮件
     */
    @ApiOperation(value = "发送自助机无卡邮件")
    @RequestMapping(value = "/sendEmailNoCard", method = RequestMethod.GET)
    public Result<?> sendEmailNoCard(String TO) {
        String[] TOS=TO.split(",");
        EmailUtil.send("自助机无卡，请快速补充！",
                HOST,FROM,"",AFFIXNAME,USER,PWD,"无卡通知",TOS);
        return Result.ok("成功");
    }


    @RequestMapping("/testCardRabbit")
    public void testCardRabbit() {
        String cardStr = "设备即将无卡请检测。";
        rabbitHelper.startThread(this.rabbitTemplate,cardQueue,cardStr);
    }
    @RequestMapping("/checkoutQueue")
    public void checkoutQueue() {
        String checkoutStr = "有客人离店，请处理。";
        rabbitHelper.startThread(this.rabbitTemplate,checkoutQueue,checkoutStr);
    }

    @GetMapping(value = "/testSendOne")
    public Result<Object> testSendOne() throws InterruptedException {
        // 打开发卡机
        K7X0Util.open(comHandle);
        Thread.sleep(2000);
        K7X0Util.send();
        return Result.ok("成功");
    }


    @GetMapping(value = "/checks")
    public Result<Object> checks() {
        Result<Object> result = new Result<>();
        log.info("check()方法");
        try {
            // 打开发卡机
            log.info("检测发卡机是否有卡");
            K7X0Util.open(comHandle);
            Thread.sleep(2000);
            // 检测发卡机是否预空
            boolean isEmpty = check(2, 0x31);
            if (isEmpty) {
                log.info("发卡机卡箱预空,即将无卡");
            }
            Thread.sleep(2000);
            // 检测发卡机是否有卡
            isEmpty = check(3, 0x38);
            if (isEmpty) {
                log.info("sendCard()方法结束return:{卡箱已空}");
                sendEmailNoCard(cardEmail);
                return SetResultUtil.setErrorMsgResult(result, "发卡机卡箱已空");
            }
            log.info("check()方法结束");
            return SetResultUtil.setSuccessResult(result, "有卡");
        } catch (Exception e) {
            log.error("sendCard()出现异常error:{}", e.getMessage());
            K7X0Util.regain();
            return SetResultUtil.setExceptionResult(result, "sendCard");
        }
    }


    /**
     * 发卡
     */
    @ApiOperation(value = "发卡", notes = "发卡-sendCard")
    @GetMapping(value = "/sendCard")
    public Result<Object> sendCard(int num, String roomno, String begin_,
                                   String end_, boolean flag,String accnt,String state) {
        Result<Object> result = new Result<>();
        log.info("进入sendCard()方法");
        try {
            // 打开发卡机
            log.info("检测发卡机是否有卡");
            K7X0Util.open(comHandle);
            // 检测发卡机是否预空
            boolean isEmpty = check(2, 0x31);
            if (isEmpty) {
                String str = "发卡机卡箱预空,即将无卡";
                log.info("发卡机卡箱预空,即将无卡");
                rabbitHelper.startThread(this.rabbitTemplate,cardQueue,str);
            }
            // 检测发卡机是否有卡
            isEmpty = check(3, 0x38);
            if (isEmpty) {
                // 发卡失败
                log.info("发卡失败失败数据加入数据库中");
                log.info("sendCard()方法结束return:{卡箱已空}");
                sendEmailNoCard(cardEmail);
                return SetResultUtil.setErrorMsgResult(result, "发卡机卡箱已空");
            }
            while (check(3, 0x31)) {
                System.out.println("##########有卡未取出");
                return SetResultUtil.setExceptionResult(result, "读卡位置有卡未取出");
            }
            for (int i = 0; i < num; i++) {
                System.out.println("发送到读卡位置");
                // 将卡片发送到读卡位置
                int key=K7X0Util.sendToReadToReturn(comHandle);
                //读卡
                if (0 != key){
                    return Result.error("失败");
                }
                /**
                 * 写卡
                 */
                Result<Object> writeResult=null;
                if (i==0&&flag){
                    writeResult=NewKeyCard(roomno,begin_,end_);
                }else{
                    writeResult=DuplicateKeyCard(roomno,begin_,end_);
                }
                if (writeResult.getCode()==200){
                    System.out.println("开始发卡");
                    Thread.sleep(1000);
                    log.info("开始发卡");
                    //写卡成功
                    //发送到收卡位置
                    K7X0Util.sendCardToTake(comHandle);
                    log.info("打印小票需要的数据");
                    //打印小票无 早餐数据

                    PrintUtil pu = new PrintUtil();
                    pu.print(roomno,phone,wifiname,wifipass,begin_,end_,accnt);
                    Thread.sleep(1000);
                }else{
                    K7X0Util.regainCard(comHandle);
                    //记录发卡
                    Record record=new Record();
                    record.setOrderId(accnt);
                    record.setSendTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    record.setSendNum(num+"");
                    record.setRoomNum(roomno);
                    record.setState("0");
                    iRecordService.save(record);
                    return SetResultUtil.setExceptionResult(result, "写卡失败");
                }
            }
            if(!StringUtils.isEmpty(state) &&"0".equals(state)){
                //记录发卡
                Record record=new Record();
                record.setOrderId(accnt);
                record.setSendTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                record.setSendNum(num+"");
                record.setRoomNum(roomno);
                record.setState("1");
                iRecordService.save(record);
            }
            log.info("sendCard()方法结束");
            return SetResultUtil.setSuccessResult(result, "发卡成功");
        } catch (Exception e) {
            log.error("sendCard()出现异常error:{}", e.getMessage());
            K7X0Util.regain();
            return SetResultUtil.setExceptionResult(result, "sendCard");
        }
    }

    /**
     * 不打印发卡
     */
    @ApiOperation(value = "发卡", notes = "发卡-sendCardNoPaper")
    @GetMapping(value = "/sendCardNoPaper")
    public Result<Object> sendCardNoPaper(int num, String roomno, String begin_, String end_,boolean flag) {
        Result<Object> result = new Result<>();
        log.info("进入sendCard()方法");
        try {
            // 打开发卡机
            log.info("检测发卡机是否有卡");
            K7X0Util.open(comHandle);
            // 检测发卡机是否预空
            boolean isEmpty = check(2, 0x31);
            if (isEmpty) {
                String str = "发卡机卡箱预空,即将无卡";
                log.info("发卡机卡箱预空,即将无卡");
                rabbitHelper.startThread(this.rabbitTemplate,cardQueue,str);
            }
            // 检测发卡机是否有卡
            isEmpty = check(3, 0x38);
            if (isEmpty) {
                // 发卡失败
                log.info("发卡失败失败数据加入数据库中");
                log.info("sendCard()方法结束return:{卡箱已空}");
                sendEmailNoCard(cardEmail);
                return SetResultUtil.setErrorMsgResult(result, "发卡机卡箱已空");
            }
            while (check(3, 0x31)) {
                System.out.println("##########有卡未取出");
                return SetResultUtil.setExceptionResult(result, "读卡位置有卡未取出");
            }
            for (int i = 0; i < num; i++) {
                System.out.println("发送到读卡位置");
                // 将卡片发送到读卡位置
                int key=K7X0Util.sendToReadToReturn(comHandle);
                //读卡
                if (0 != key){
                    return Result.error("失败");
                }
                /**
                 * 写卡
                 */
                Result<Object> writeResult=null;
                if (i==0&&flag){
                    writeResult=NewKeyCard(roomno,begin_,end_);
                }else{
                    writeResult=DuplicateKeyCard(roomno,begin_,end_);
                }
                if (writeResult.getCode()==200){
                    System.out.println("开始发卡");
                    Thread.sleep(1000);
                    log.info("开始发卡");
                    //写卡成功
                    //发送到收卡位置
                    K7X0Util.sendCardToTake(comHandle);
                    log.info("打印小票需要的数据");
                    Thread.sleep(1000);
                }else{
                    K7X0Util.regainCard(comHandle);
                    return SetResultUtil.setExceptionResult(result, "写卡失败");
                }
            }
            log.info("sendCard()方法结束");
            return SetResultUtil.setSuccessResult(result, "发卡成功");
        } catch (Exception e) {
            log.error("sendCard()出现异常error:{}", e.getMessage());
            K7X0Util.regain();
            return SetResultUtil.setExceptionResult(result, "sendCard");
        }
    }


    /**
     * 退卡
     */
    @ApiOperation(value = "退卡", notes = "退卡-Recoverycard")
    @GetMapping(value = "/Recoverycard")
    public Result<Object> Recoverycard() throws InterruptedException {

        Result<Object> result = new Result<>();
        log.info("Recoverycard()方法");
        // 将卡片发送到读卡位置
        int key = K7X0Util.sendTakeToRead(comHandle);
        //读卡
        if (0 == key) {
            result = ReadCard();
            Thread.sleep(500);
            return result;
        } else {
            return Result.error("失败");
        }
    }


    /**
     * testOpenAndClose
     */
    @ApiOperation(value = "testOpenAndClose", notes = "testOpenAndClose")
    @GetMapping(value = "/testOpenAndClose")
    public Result<Object> testOpenAndClose() {
        System.out.println("打开发卡机串口");
        if(!K7X0Util.open(comHandle)){
            System.out.println("发卡器串口打开失败+=========>com:"+comHandle);
            return Result.error("发卡机串口打开失败");
        }
        System.out.println("发卡机打开串口成功");
        System.out.println("关闭发卡机串口");
        K7X0Util.colse(K7X0Util.ComHandle);
        return Result.ok("成功");
    }



    /**
     *  从读卡位置发到取卡位置
     */
    @ApiOperation(value = "sendCardToTake", notes = "sendCardToTake")
    @GetMapping(value = "/sendCardToTake")
    public Result<Object> sendCardToTake() {
        K7X0Util.sendCardToTake(comHandle);
        return Result.ok("成功");
    }

    /**
     *  从读卡位置发送到读卡箱
     */
    @ApiOperation(value = "regain", notes = "regain")
    @GetMapping(value = "/regain")
    public Result<Object> regain() {
        if(!K7X0Util.open(comHandle)){
            System.out.println("发卡器串口打开失败+=========>com:"+comHandle);
            return Result.error("打开串口失败");
        }
        K7X0Util.regain();
        return Result.ok("成功");
    }




    @ApiOperation(value = "补卡", notes = "补卡")
    @GetMapping(value = "/Scard")
    public Result<Object> Scard() throws InterruptedException {
        Result<Object> result = new Result<>();
        // 回收到发卡箱
        log.info("进入sendCard()方法");
        boolean flag = K7X0Util.regainCardZiDong(comHandle);
        if (!flag) {
            return SetResultUtil.setErrorMsgResult(result, "失败");
        }
        Thread.sleep(1000);
        return Result.ok("成功");
    }
    /**
     * 检测发卡位置是否有卡
     */
    @ApiOperation(value = "检测发卡是否有卡", httpMethod = "GET")
    @RequestMapping(value = "/checkTureCard", method = RequestMethod.GET)
    public Result<Object> checkTureCard() {
        Result<Object> result = new Result<>();
        //打开发卡器
        K7X0Util.open(comHandle);
        boolean isEmpty = K7X0Util.check(3, 0x35);//0x35
        System.out.println("isEmpty:" + isEmpty);
        SetResultUtil.setSuccessResult(result, "检测是否有卡", isEmpty);
        return result;
    }



    private static String cardUrl="http://localhost:10086/api/readcard/";
    private static String CardNo="1";
    private static String MakeWay="1";

    /**
     * 必达 门锁接口调用
     *
     */
    @AutoLog(value = "制作新卡（实体卡）")
    @ApiOperation(value="制作新卡（实体卡）-NewKeyCard", notes="制作新卡（实体卡）-NewKeyCard")
    @GetMapping(value = "/NewKeyCard")
    public Result<Object> NewKeyCard(String roomNumber,String checkInTime,String checkOutTime) throws ParseException {
        Map<String,String> map=new HashMap<>();
        map.put("CheckInDate",checkInTime);
        map.put("CheckOutDate",checkOutTime);
        map.put("RoomNo",roomNumber);
        map.put("CardNo",CardNo);
        map.put("MakeWay",MakeWay);
        String param= HttpUtil.getMapToString(map);
        String returnResult= HttpUtil.sendPost(cardUrl+"makecardbd",param);
        if (returnResult!=null){
            JSONObject jsonObj = JSONObject.parseObject(returnResult);
            System.out.println(jsonObj);
            log.info("jsonObj:"+jsonObj.toString());
            String IsSuccess=jsonObj.getString("IsSuccess");
            if ("true".equals(IsSuccess)){
                return  Result.ok();
            }else {
                returnResult= HttpUtil.sendPost(cardUrl+"makecardbd",param);
                if (returnResult!=null){
                    jsonObj = JSONObject.parseObject(returnResult);
                    System.out.println(jsonObj);
                    log.info("jsonObj:"+jsonObj.toString());
                    IsSuccess=jsonObj.getString("IsSuccess");
                    if ("true".equals(IsSuccess)) {
                        return Result.ok();
                    }else {
                        return Result.error("写卡失败");
                    }
                }else {
                    return Result.error("写卡失败");
                }
            }
        }else{
            return Result.error("写卡失败");
        }
    }

    @AutoLog(value = "复制卡（实体卡）")
    @ApiOperation(value="复制卡（实体卡）-DuplicateKeyCard", notes="复制卡（实体卡）-DuplicateKeyCard")
    @GetMapping(value = "/DuplicateKeyCard")
    public Result<Object> DuplicateKeyCard(String roomNumber,String checkInTime,String checkOutTime) throws ParseException {
        Map<String,String> map=new HashMap<>();
        map.put("CheckInDate",checkInTime);
        map.put("CheckOutDate",checkOutTime);
        map.put("RoomNo",roomNumber);
        map.put("CardNo",CardNo);
        map.put("MakeWay",MakeWay);
        String param= HttpUtil.getMapToString(map);
        String returnResult= HttpUtil.sendPost(cardUrl+"copycardbd",param);
        if (returnResult!=null){
            JSONObject jsonObj = JSONObject.parseObject(returnResult);
            System.out.println(jsonObj);
            log.info("jsonObj:"+jsonObj.toString());
            String IsSuccess=jsonObj.getString("IsSuccess");
            if ("true".equals(IsSuccess)){
                return  Result.ok();
            }else {
                returnResult= HttpUtil.sendPost(cardUrl+"copycardbd",param);
                if (returnResult!=null){
                    jsonObj = JSONObject.parseObject(returnResult);
                    System.out.println(jsonObj);
                    log.info("jsonObj:"+jsonObj.toString());
                    IsSuccess=jsonObj.getString("IsSuccess");
                    if ("true".equals(IsSuccess)) {
                        return Result.ok();
                    }else {
                        return Result.error("写卡失败");
                    }
                }else {
                    return Result.error("写卡失败");
                }
            }
        }else{
            return Result.error("写卡失败");
        }
    }



    @AutoLog(value = "读卡（实体卡）")
    @ApiOperation(value="读卡（实体卡）-ReadCard", notes="读卡（实体卡）-ReadCard")
    @GetMapping(value = "/ReadCard")
    public Result<Object> ReadCard(){
        String returnResult= HttpUtil.sendPost(cardUrl+"readcardbd",null);
        if (returnResult!=null){
            JSONObject jsonObj = JSONObject.parseObject(returnResult);
            System.out.println(jsonObj);
            String IsSuccess=jsonObj.getString("IsSuccess");
            if ("true".equals(IsSuccess)){
                String Roomno=jsonObj.getString("RoomNo").substring(2,6);
                String EndTime=jsonObj.getString("CheckOutTime");
                String BeginTime=jsonObj.getString("CheckInTime");
                EndTime=new SimpleDateFormat("yy").format(new Date())+EndTime;
                BeginTime=new SimpleDateFormat("yy").format(new Date())+BeginTime;
                if ("0".equals(Roomno.substring(0,1))){
                    Roomno=Roomno.substring(1);
                }
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("RoomNo",Roomno);
                jsonObject.put("EndTime",EndTime);
                jsonObject.put("BeginTime",BeginTime);
                log.info("jsonObject:"+jsonObject);
                return  Result.ok(jsonObject);
            }else {
                returnResult= HttpUtil.sendPost(cardUrl+"readcardbd",null);
                if (returnResult!=null) {
                    jsonObj = JSONObject.parseObject(returnResult);
                    System.out.println(jsonObj);
                    IsSuccess = jsonObj.getString("IsSuccess");
                    if ("true".equals(IsSuccess)) {
                        String Roomno=jsonObj.getString("RoomNo").substring(2,6);
                        String EndTime=jsonObj.getString("CheckOutTime");
                        String BeginTime=jsonObj.getString("CheckInTime");
                        EndTime=new SimpleDateFormat("yy").format(new Date())+EndTime;
                        BeginTime=new SimpleDateFormat("yy").format(new Date())+BeginTime;
                        if ("0".equals(Roomno.substring(0,1))){
                            Roomno=Roomno.substring(1);
                        }
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put("RoomNo",Roomno);
                        jsonObject.put("EndTime",EndTime);
                        jsonObject.put("BeginTime",BeginTime);
                        log.info("jsonObject:"+jsonObject);
                        return  Result.ok(jsonObject);
                    } else {
                        return Result.error("读卡失败");
                    }
                }else {
                    return Result.error("读卡失败");
                }
            }
        }else{
            return Result.error("读卡失败");
        }
    }



}

