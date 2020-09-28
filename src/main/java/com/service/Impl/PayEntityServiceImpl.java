package com.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mapper.PayEntityMapper;
import com.entity.PayEntity;
import com.service.PayEntityService;
import org.springframework.stereotype.Service;

@Service
public class PayEntityServiceImpl extends ServiceImpl<PayEntityMapper, PayEntity> implements PayEntityService {
}
