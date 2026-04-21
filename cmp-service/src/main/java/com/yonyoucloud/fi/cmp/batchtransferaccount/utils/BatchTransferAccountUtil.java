package com.yonyoucloud.fi.cmp.batchtransferaccount.utils;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyoucloud.ctm.stwb.datasettled.OppAccTypeEnum;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;

/**
 * @Description
 * @Author hanll
 * @Date 2025/9/5-19:34
 */
public class BatchTransferAccountUtil {
    // 保存-新增，保存-编辑
    public static final String SAVE_SAVE = "SAVE:SAVE";
    // 保存-删除
    public static final String SAVE_DELETE = "SAVE:DELETE";

    // 提交-提交
    public static final String SUBMIT_SUBMIT  = "SUBMIT:SUBMIT";
    // 提交-撤回
    public static final String SUBMIT_CANCEL_SUBMIT  = "SUBMIT:CANCEL_SUBMIT";
    // 提交-撤回审批
    public static final String SUBMIT_APPROVE_UNSUBMIT  = "SUBMIT:APPROVE_UNSUBMIT";
    // 提交-审批
    public static final String SUBMIT_APPROVE_PASS  = "SUBMIT:APPROVE_PASS";
    // 提交-结算成功
    public static final String SUBMIT_SETTLE_SUCCESS  = "SUBMIT:SETTLE_SUCCESS";
    // 提交-审批退回
    public static final String SUBMIT_APPROVE_UNPASS  = "SUBMIT:APPROVE_UNPASS";
    // 提交-结算止付
    public static final String SAVE_SUBMIT_AUDIT_SETTLE_SUCCESS_STOP_SETTLE  = "SAVE:SUBMIT:AUDIT:SETTLE_SUCCESS:STOP_SETTLE";

    // 审核-审核通过
    public static final String AUDIT_APPROVE_PASS  = "AUDIT:APPROVE_PASS";
    // 审核-撤回
    public static final String AUDIT_SAVE_SUBMIT_APPROVE_UNSUBMIT  = "AUDIT:SAVE:SUBMIT:APPROVE_UNSUBMIT";
    // 审核-审批 审核不通过
    public static final String AUDIT_SAVE_SUBMIT_APPROVE_UNPASS  = "AUDIT:SAVE:SUBMIT:APPROVE_UNPASS";
    // 审核-结算止付
    public static final String AUDIT_STOP_SETTLE  = "AUDIT:STOP_SETTLE";
    // 审核-终止
    public static final String AUDIT_SAVE_SUBMIT_TERMINATE  = "AUDIT:SAVE:SUBMIT:TERMINATE";

    // 结算成功-结算成功
    public static final String SETTLE_SUCCESS_SETTLE_SUCCESS  = "SETTLE_SUCCESS:SETTLE_SUCCESS";
    // 结算成功-取消结算
    public static final String SETTLE_SUCCESS_CANCEL_SETTLE  = "SETTLE_SUCCESS:CANCEL_SETTLE";


    /**
     * 获取账户类型
     * @param enterpriseAccountId  银行账户id
     * @return
     * @throws Exception
     */
    public static String getAcctType(String enterpriseAccountId) throws Exception {
        if (StringUtils.isNotEmpty(enterpriseAccountId)) {
            EnterpriseBankQueryService enterpriseBankQueryService = CtmAppContext.getBean(EnterpriseBankQueryService.class);
            // 银行账户
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(enterpriseAccountId);
            Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
            // 开户类型是其他金融机构开户 虚拟账户类型
            if (acctOpenType == AcctopenTypeEnum.OtherFinancial.getValue()) {
                return String.valueOf(OppAccTypeEnum.ENTERPRISE_VIRTUAL_ACC.getValue());
            } else {
                return String.valueOf(OppAccTypeEnum.ENTERPRISE_BANK_ACC.getValue());
            }
        } else {
            // 现金账户
            return String.valueOf(OppAccTypeEnum.ENTERPRISE_CASH_ACC.getValue());
        }
    }

    /**
     * 过滤企业现金账户
     * @param billDataDto
     * @param bizObject
     */
    public static void filterCashAccount(BillDataDto billDataDto, BizObject bizObject) {
        if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISECASHACCT.equals(billDataDto.getrefCode()) && bizObject != null) {
            Object currency = bizObject.getJavaObject("currency", Object.class);
            if (currency == null) {
                return;
            }
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                billDataDto.setCondition(conditon);
            }
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
        }
    }
}
