package com.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.entity.Record;
import com.mapper.RecordMapper;
import com.service.IRecordService;
import org.springframework.stereotype.Service;

/**
 * @Description: 发卡记录表
 * @Author: jeecg-boot
 * @Date:   2019-09-16
 * @Version: V1.0
 */
@Service
public class RecordServiceImpl extends ServiceImpl<RecordMapper, Record> implements IRecordService {

}
