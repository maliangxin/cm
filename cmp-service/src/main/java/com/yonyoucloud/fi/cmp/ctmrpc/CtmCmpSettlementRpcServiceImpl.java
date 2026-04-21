package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.common.CtmCmpSettlementRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.SettleCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @Author: mal
 * @Description: 本类用于提供rpc接口供stwb判断是否日结
 * @Date: Created in  2022/8/1 11:05
 * @Version v1.0
 */
@Service
@Slf4j
public class CtmCmpSettlementRpcServiceImpl implements CtmCmpSettlementRpcService {

    @Autowired
    SettlementService settlementService;

    @Override
    public boolean checkSettle(CommonRequestDataVo param) throws Exception {
        String accentity = param.getAccentity();
        Date dzDate = param.getDzdate();
        log.info("CtmCmpSettlementRpcService.isSettle accentity = {}, dzDate = {}",accentity,dzDate.toString());
        //最大日结日期
        Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
        return SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, dzDate);
    }

    @Override
    public void updateSettleStatusOfJournal(List<String> ids) throws Exception {





    }
}
