package com.yonyoucloud.fi.cmp.bankvouchercheck.rule;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.QueryPagerVo;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceBatchOperationService;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.bankvourchercheck.BankvourchercheckWorkbench;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @description:银企对账工作台生成余额调节表查询规则
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class BankVourhcerCheckBalanceListQueryRule extends AbstractCommonRule {


    @Resource
    private BalanceBatchOperationService balanceBatchOperationService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
        BalanceAdjustResult result = new BalanceAdjustResult();
        if(billDataDto.getCondition() != null) {
            //默认1凭证；2银行日记账
            short reconciliationDataSource = ReconciliationDataSource.Voucher.getValue();
            if ("cmp_bankjournalcheck_balancelist".equals(billnum)){
                reconciliationDataSource = ReconciliationDataSource.BankJournal.getValue();
            }
            QueryPagerVo queryPagerVo = billDataDto.getPage();
            //获取返回页面的page
            Pager pager = new Pager();
            pager.setPageIndex(queryPagerVo.getPageIndex());
            pager.setPageSize(queryPagerVo.getPageSize());
            try {
                List<CtmJSONObject> ctmJSONObjects = balanceBatchOperationService.queryBatchConfirmedBalances(billDataDto.getCondition(),reconciliationDataSource);
                //排序规则： 对账组织（组织编码）正序+银行账户（银行账号）正序的维度
                Collections.sort(ctmJSONObjects, new Comparator<CtmJSONObject>() {
                    @Override
                    public int compare(CtmJSONObject o1, CtmJSONObject o2) {
                        int result = o1.getString("accentity_code").compareTo(o2.getString("accentity_code"));
                        if (result == 0) {
                            // 处理银行账号可能为null的情况
                            String account1 = o1.getString("bankaccount_account");
                            String account2 = o2.getString("bankaccount_account");
                            // 两个都为null则相等
                            if (account1 == null && account2 == null) {
                                return 0;
                            }
                            // account1为null则排到后面
                            if (account1 == null) {
                                return 1;
                            }
                            // account2为null则排到前面
                            if (account2 == null) {
                                return -1;
                            }
                            // 都不为null则正常比较
                            return account1.compareTo(account2);
                        }
                        return result;
                    }
                });
                //银行日记账是假分页，需要将全部数据放回到返回的数据中，用来后续自动对账和余额调节表生成;
                //20250610 余额调节表假分页去掉
//                if (reconciliationDataSource == ReconciliationDataSource.BankJournal.getValue()) {
//                    int pageIndex = pager.getPageIndex(); // 获取当前页码
//                    int pageSize = pager.getPageSize();   // 获取每页大小
//                    int startIndex = (pageIndex - 1) * pageSize; // 计算起始索引
//                    int endIndex = Math.min(startIndex + pageSize, ctmJSONObjects.size()); // 计算结束索引
//                    // 截取 recordList 中的分页数据
//                    List<CtmJSONObject>  pagedRecordList = ctmJSONObjects.subList(startIndex, endIndex);
//                    // 将分页数据设置到 pager 中
//                    pager.setRecordList(pagedRecordList);
//                }else {
//                    pager.setRecordList(ctmJSONObjects);
//                }
//                pager.setRecordCount(ctmJSONObjects.size());
                pager.setRecordList(ctmJSONObjects);
            } catch (Exception e) {
                result.put("generateFailReason",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072F", "生成失败原因") /* "生成失败原因" */);
            }
            ruleResult.setData(pager);
            // 后面的规则都不执行
            ruleResult.setCancel(true);
        }
        return ruleResult;
    }
}
