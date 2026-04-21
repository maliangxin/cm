package com.yonyoucloud.fi.cmp.autocorrsetting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2025/4/17 20:38
 */
@Service
@Slf4j
public class AsyncCorrServiceImpl implements AsyncCorrService{

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void asyncConfirmCorrOpration(List corrIds, List dcFlags, String uid) {
        CorrOperationService corrOperationService = applicationContext.getBean(CorrOperationService.class);
        corrOperationService.asyncConfirmCorrOpration(corrIds, dcFlags, uid);
    }

    @Override
    public void asyncRefuseCorrOpration(List corrIds, String uid) {
        CorrOperationService corrOperationService = applicationContext.getBean(CorrOperationService.class);
        corrOperationService.asyncRefuseCorrOpration(corrIds, uid);
    }
}
