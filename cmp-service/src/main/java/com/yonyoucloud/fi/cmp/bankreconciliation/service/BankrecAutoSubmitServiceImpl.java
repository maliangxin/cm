package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankrecAutoSubmitService;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 银行对账单提前入账智能规则生成资金首付款单自动提交接口
 * PS：湖南建投项目需求开发
 * author wq
 * date 2023年10月19日10:03:03
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
public class BankrecAutoSubmitServiceImpl implements BankrecAutoSubmitService {
    @Autowired
    private IFIBillService fiBillService;
    @Override
    public void autoSubmit(String fullname, List<String> ids, String billnum) throws Exception {
        if (ids == null || ids.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100605"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00130", "主键不能为空") /* "主键不能为空" */);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.appendQueryCondition(QueryCondition.name("id").in(ids));
        List<Map<String, Object>> bizObjects = MetaDaoHelper.query(fullname, querySchema);
        if (bizObjects == null || bizObjects.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100604"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012E", "根据主表主键未查到数据") /* "根据主表主键未查到数据" */);
        }
        //这块得每个单据分开提交，后面的FundPaymentSubmitRule 不支持多个数据一起提交
        for(Map<String, Object> bizObject : bizObjects){
            BillDataDto bill = new BillDataDto();
            bill.setBillnum(billnum);
            bill.setData(bizObject);
            //settleflag为true才会传结算  submit规则类 FundCollectionSubmitRule 会判断是否有审批流这里不需要处理
            //有审批流的话，提交走submit，审批走audit，没有审批流只走submit，不会走audit
            fiBillService.executeUpdate(OperationTypeEnum.SUBMIT.getValue(), bill);
        }
//        if ("cmp.fundcollection.FundCollection".equals(fullname)) {
//            for (Map<String, Object> bizObjectElem : bizObject) {
//                if (null != bizObjectElem.get("isWfControlled") && (Boolean) bizObjectElem.get("isWfControlled")) {
//                    //不知道为什么到提交就变成3了，这里改回去，保证能正常生成凭证
//                    bizObjectElem.put("voucherstatus", 2);
//                    Map<String, Object> paramMap = new HashMap<>();
//                    paramMap.put("param",bill);
//                    BillContext billContext = new BillContext();
//                    billContext.setCardKey("cmp_fundcollection");
//                    billContext.setSubid("CM");
//                    billContext.setBilltype("ArchiveList");
//                    billContext.setEntityCode(billnum);
//                    billContext.setAction("submit");
//                    billContext.setBillnum(billnum);
//                    billContext.setFullname(fullname);
//                    BillBiz.executeRule("audit", billContext, paramMap);
//                    break;
//                }
//            }
//        } else if ("cmp.fundpayment.FundPayment".equals(fullname)) {
//            for (Map<String, Object> bizObjectElem : bizObject) {
//                if (null != bizObjectElem.get("isWfControlled") && !(Boolean) bizObjectElem.get("isWfControlled")) {
//                    //不知道为什么到提交就变成3了，这里改回去，保证能正常生成凭证
//                    bizObjectElem.put("voucherstatus", 2);
//                    Map<String, Object> paramMap = new HashMap<>();
//                    paramMap.put("param", bill);
//                    BillContext billContext = new BillContext();
//                    billContext.setCardKey("cmp_fundpayment");
//                    billContext.setSubid("CM");
//                    billContext.setBilltype("ArchiveList");
//                    billContext.setEntityCode(billnum);
//                    billContext.setAction("submit");
//                    billContext.setBillnum(billnum);
//                    billContext.setFullname(fullname);
//                    BillBiz.executeRule("audit", billContext, paramMap);
//                    break;
//                }
//            }
//        }
    }
}
