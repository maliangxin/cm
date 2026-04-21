package com.yonyoucloud.fi.cmp.common.service.cancelsettle;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.QueryBillNodeParam;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cmp.api.ctmrpc.cancelsettle.CmpCancelSettleService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchTransferAccountCancelSettleServiceImpl;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CmpCancelSettleServiceImpl implements CmpCancelSettleService {

    @Override
    public BillDetailNode getBillNode(QueryBillNodeParam queryBillNodeParam) {
        //入参为当前操作结算单的上游，返回值upperNodes要构造本次上游的上游返回
        String billTypeId = queryBillNodeParam.getBillTypeId();
        List<String> billDetailIds = queryBillNodeParam.getBillDetailIds();
        CmpBusinessBillType businessBillType = CmpBusinessBillType.find(billTypeId);
        CancelSettlementServiceEnum cancelSettlementServiceEnum = CancelSettlementServiceEnum.find(businessBillType);
        //这点需要路由给下游单据
        ICmpOperationService server = null;
        try {
            server = cancelSettlementServiceEnum.getClazz().newInstance();
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102232"),e.getMessage());
        }
        return  server.buildCancelSettleNode(billTypeId,billDetailIds);
    }

    @Override
    public void handle(BillBaseNode billBaseNode, String reason) throws Exception {
        if(ObjectUtils.isEmpty(billBaseNode)){
            throw new Exception("billBaseNode is null");
        }

        CmpBusinessBillType businessBillType = CmpBusinessBillType.find(billBaseNode.getBillTypeId());
        CancelSettlementServiceEnum cancelSettlementServiceEnum = CancelSettlementServiceEnum.find(businessBillType);
        ICmpOperationService server = AppContext.getBean(cancelSettlementServiceEnum.getClazz());
        server.handleCancelSettle(cancelSettlementServiceEnum, billBaseNode, reason);
    }
}
