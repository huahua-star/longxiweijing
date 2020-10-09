package com.controller;

import com.utils.EmailUtil;
import com.utils.Returned2.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Api(tags = "email")
@RestController
@RequestMapping("/email")
public class EmailController {

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

    @Value("${reservationMonthUrl}")
    private String reservationMonthUrl;


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

    /*退房发送邮件*/
    @ApiOperation(value = "退房发送邮件")
    @RequestMapping(value = "/sendEmailCheckOut", method = RequestMethod.GET)
    public Result<?> sendEmailCheckOut(String accnt,String roomNo) {
        //客房邮箱
        String TO="longxikefang2020@163.com";
        String[] TOS=TO.split(",");
        EmailUtil.send("账号为"+accnt+"，房间号为"+roomNo+"的客人已在自助机上办理退房。",
                HOST,FROM,"",AFFIXNAME,USER,PWD,"客人退房提醒。",TOS);
        return Result.ok("成功");
    }
}
