package com.controller;

import com.alibaba.druid.util.StringUtils;
import com.common.Invoiqr;
import com.common.ResponseData;
import com.utils.Card.CLibraryUtil;
import com.utils.Card.PrintUtil;
import com.utils.Card.leavePrintUtil;
import com.utils.Card.leavewuPrintUtil;
import com.utils.DaPuWeiDa;
import com.utils.Returned3.R;
import com.yang.serialport.manager.SerialPortManager;
import gnu.io.PortInUseException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags="打印相关功能")
@RestController
@RequestMapping("/print")
public class PrintController {

    @Value("${qrDir}")
    private String qrDir;
    @Value("${Invoiqr.appCode}")
    private String appCode;
    @Value("${Invoiqr.taxpayerCode}")
    private String taxpayerCode;
    @Value("${Invoiqr.keyStorePath}")
    private String keyStorePath;
    @Value("${Invoiqr.keyStoreAbner}")
    private String keyStoreAbner;
    @Value("${Invoiqr.keyStorePassWord}")
    private String keyStorePassWord;
    @Value("${Invoiqr.facadeUrl}")
    private String facadeUrl;


    @Value("${print.phone}")
    private String phone;
    @Value("${dapuPort}")
    private int dapuPort;
    @Value("${print.leave}")
    private String leave;
    /**
     * test 打印测试
     */
    @ApiOperation(value = "打印测试", httpMethod = "GET")
    @RequestMapping(value = "/testPrint", method = RequestMethod.GET)
    public R testPrint() throws PortInUseException {
        log.info("打印小票需要的数据");
        DaPuWeiDa.printCheckIn(dapuPort,"3303",leave,"he", phone,"2020-07-04");
        return R.ok();
    }


    /**
     * 离店打印小票
     *
     * @param reservationNumber 订单号
     * @param stime             预计入住时间
     * @param ztime             预计离店时间
     * @param roomNum           房间号
     * @return
     */
    @RequestMapping(value = "/updatechckInPerson", method = RequestMethod.GET)
    @ApiOperation(value = "离店打印小票", httpMethod = "GET")
    public R updatechckInPerson(@RequestParam(name = "reservationNumber", required = true)
                                @ApiParam(name = "reservationNumber", value = "订单号")
                                        String reservationNumber, String stime, String ztime, String roomNum) {
        log.info("进入updatechckInPerson()方法reservationNumber:{}", reservationNumber);
        if (org.apache.commons.lang.StringUtils.isEmpty(reservationNumber) || org.apache.commons.lang.StringUtils.isEmpty(stime) || org.apache.commons.lang.StringUtils.isEmpty(ztime) || org.apache.commons.lang.StringUtils.isEmpty(roomNum)) {
            return R.error("请输入必须值");
        }
        try {
            leavePrintUtil pu = new leavePrintUtil();
            leavewuPrintUtil wupu = new leavewuPrintUtil();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String orderNo = null;
            Date kstime = null;
            Date jstime = null;
            //打印小票
            kstime = sdf.parse(stime);
            jstime = sdf.parse(ztime);
            log.info("打印小票无发票");
            wupu.print(roomNum, null, kstime, jstime, new Date());
            return R.ok();
        } catch (Exception e) {
            log.error("updatechckInPerson()方法出现异常error:{}", e.getMessage());
            return R.error("打印退房小票失败!");
        }
    }

    @ApiOperation(value = "testInvoice", httpMethod = "GET")
    @RequestMapping(value = "/testInvoice", method = RequestMethod.GET)
    public R testInvoice(String amount,String reservationNumber) throws Exception {
        Invoiqr i = new Invoiqr();
        //打印电子发票开票
        Map s = i.getCheckInPerson(amount, qrDir, appCode, taxpayerCode, keyStorePath,
                keyStoreAbner, keyStorePassWord, facadeUrl, reservationNumber);
        String orderNo = (String) s.get("orderNo");
        String url=(String) s.get("url");
        return  R.ok();
    }

    @ApiOperation(value = "quiry_order", httpMethod = "GET")
    @RequestMapping(value = "/quiry_order", method = RequestMethod.GET)
    public R quiry_order(String orderNo) throws Exception {
        Invoiqr i = new Invoiqr();
        //打印电子发票开票
        String s = i.quiry_order(orderNo, appCode, taxpayerCode, keyStorePath,
                keyStoreAbner, keyStorePassWord, facadeUrl);
        System.out.println("s:"+s);
        return  R.ok();
    }


    /**
     * 离店打印普票
     *
     * @param reservationNumber 订单号
     * @param stime             预计入住时间
     * @param ztime             预计离店时间
     * @param amount            开普票金额
     * @param roomNum           房间号
     * @return
     */
    @ApiOperation(value = "离店打印发票", httpMethod = "GET")
    @RequestMapping(value = "/invoice", method = RequestMethod.GET)
    public R invoice(
            @RequestParam(name = "reservationNumber", required = true) @ApiParam(name = "reservationNumber", value = "订单号") String reservationNumber,
            String stime, String ztime, String amount, String roomNum) throws Exception {
        if (org.apache.commons.lang.StringUtils.isEmpty(reservationNumber) || org.apache.commons.lang.StringUtils.isEmpty(stime) || org.apache.commons.lang.StringUtils.isEmpty(ztime) ||
                org.apache.commons.lang.StringUtils.isEmpty(amount) || org.apache.commons.lang.StringUtils.isEmpty(roomNum)) {
            return R.error("请输入开票必须值!");
        }
        if (amount.equals("0")) {
            return R.error("金额不能为零");
        }
        leavePrintUtil pu = new leavePrintUtil();
        leavewuPrintUtil wupu = new leavewuPrintUtil();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        log.info("根据amount是否打印发票");
        String orderNo = null;
        Date kstime = null;
        Date jstime = null;
        int stamptype = 0;
        Invoiqr i = new Invoiqr();
        //打印电子发票开票
        Map s = i.getCheckInPerson(amount, qrDir, appCode, taxpayerCode, keyStorePath,
                keyStoreAbner, keyStorePassWord, facadeUrl, reservationNumber);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        String dateString = formatter.format(new Date());
        String imgurl;
        orderNo = (String) s.get("orderNo");
        imgurl = (String) s.get("filePath");
        kstime = sdf.parse(stime);
        jstime = sdf.parse(ztime);
        stamptype = 1;
        pu.print(roomNum, imgurl, kstime, jstime, new Date(), orderNo);
        try {

        } catch (Exception e) {
            e.printStackTrace();
            return R.error("打印发票失败!");
        }
        return R.ok();
    }




    /**
     * 查询电子发票订单状态
     *
     * @param orderNo 电子发票订单号
     * @return 状态码 以及说明
     * @throws Exception
     */
    @ApiOperation(value = "查询电子发票订单状态")
    @RequestMapping(value = "/invoiceQuery", method = RequestMethod.GET)
    @ResponseBody
    public R invoiceQuery(String orderNo) {
        if (StringUtils.isEmpty(orderNo)) {
            return R.error("请输入发票订单号");
        }
        Invoiqr i = new Invoiqr();
        try {
            ResponseData responseData = i.quiry_order(orderNo);
            return R.ok("Response", responseData);
        } catch (Exception e) {
            log.error("invoiceQuery()方法异常:查询失败");
        }
        return R.error();
    }


}
