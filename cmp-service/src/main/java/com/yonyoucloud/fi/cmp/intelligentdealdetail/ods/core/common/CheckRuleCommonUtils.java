package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common;

import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankidentifytype.BankreconciliationIdentifyType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.OdsCommonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CheckRuleCommonUtils {

    /**
     * 处理退款校验规则，过滤出需要校验的银行对账记录
     * 将重复交易或疑似退款的记录添加到待处理列表
     *
     * @param bankReconciliationList 银行对账记录列表
     * @param pendingList 待处理列表，用于存放不符合校验条件的记录
     * @return 符合校验条件的银行对账记录列表
     */
    public static List<BankReconciliation> processRuleForReturn(List<BankReconciliation> bankReconciliationList, List<BankReconciliation> pendingList) {
        List<BankReconciliation> checkList = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            // 判断是否为重复交易：如果重复状态为"疑似重复"或"非确认重复"，则加入无处处理列表
            if (bankReconciliation.getIsRepeat() != null && (BankDealDetailConst.REPEAT_DOUBT == bankReconciliation.getIsRepeat() || bankReconciliation.getIsRepeat() == BankDealDetailConst.REPEAT_CONFIRM)){
                log.error("bank REPEAT_INIT  or  REPEAT_NORMAL :{} REPEAT_INIT status ={}" ,bankReconciliation.getBank_seq_no(),bankReconciliation.getIsRepeat());
                pendingList.add(bankReconciliation);
            }else {
                // 判断是否为疑似退款：如果退款状态为"疑似退款"，则加入无处处理列表
                if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus()==ReFundType.SUSPECTEDREFUND.getValue()){
                    log.error("bank refundstatus_INIT  or  BANK_SEQ_NO :{} refundstatus status ={}" ,bankReconciliation.getBank_seq_no(),bankReconciliation.getRefundstatus());
                    pendingList.add(bankReconciliation);
                }else {
                    checkList.add(bankReconciliation);
                }
            }
        }
        return checkList;
    }

    public static void checkBankReconciliationIdentifyType(BankIdentifyService bankIdentifyService, IFcdsUseSettingInnerService fcdsUseSettingInnerService) throws Exception {
        //step1:查询规则大类
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenant.getId());
        map.put("ytenantId", tenant.getYTenantId());
        StringBuilder messageResult = new StringBuilder();
        boolean mark = false;
        Long count = SqlHelper.selectOne("com.yonyoucloud.fi.ficm.mapper.IdentifyTypeOpenInitDataMapper.queryIdentifyTypeInitData", map);
        if (count != null && count == 0) {
            mark = true;
            messageResult.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079D", "执行失败，请先通过”流水自动辨识匹配规则“节点，进行规则初始化！") /* "执行失败，请先通过”流水自动辨识匹配规则“节点，进行规则初始化！" */);
        }
        Long countForHandler = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.flowhandlesetting.mapper.FlowhandlesettingMapper.queryInitData", map);
        if (countForHandler != null && countForHandler == 0) {
            mark = true;
            messageResult.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079B", "执行失败，请先通过”流水处理规则“节点，进行规则初始化！") /* "执行失败，请先通过”流水处理规则“节点，进行规则初始化！" */);
        }
        Long countForDatesSource = SqlHelper.selectOne("com.yonyoucloud.fi.cmp.fcdsusesetting.mapper.FcdsusesettingMapper.queryInitData", map);
        if (countForDatesSource != null && countForDatesSource == 0) {
            mark = true;
            messageResult.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079C", "执行失败，请先通过”流水处理使用数据源设置“节点，进行规则初始化！") /* "执行失败，请先通过”流水处理使用数据源设置“节点，进行规则初始化！" */);
        }
        if (mark) {
            throw new CtmException(messageResult.toString());
        }
    }

    public static boolean checkBankReconciliationIdentifyType(BankIdentifyService bankIdentifyService) throws Exception {
        //step1:查询规则大类
        IBankDealDetailAccessDao bankDealDetailAccessDao = AppContext.getBean(IBankDealDetailAccessDao.class);
        List<BankreconciliationIdentifyType> bankreconciliationIdentifyTypes = bankDealDetailAccessDao.getBankreconciliationIdentifyTypeListByTenantId();
        if (bankreconciliationIdentifyTypes == null || bankreconciliationIdentifyTypes.size() == 0) {
            return false;
        }
        return true;
    }
}
