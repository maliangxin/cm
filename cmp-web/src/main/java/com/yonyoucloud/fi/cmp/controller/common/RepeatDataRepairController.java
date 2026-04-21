package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataServiceImpl;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 重复数据检查及修复工具类
 */
@Controller
@RequestMapping("/repeat")
@Slf4j
public class RepeatDataRepairController extends BaseController {

    final String BANKRECONCILIATIONMAPPER = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";

    @Autowired
    CtmCmpCheckRepeatDataServiceImpl checkRepeatDataService;

    @GetMapping("/check")
    public void checkRepeat() throws Exception {
        String checkRepeat = AppContext.getEnvConfig("cmp.bankreconciliation.checkRepeat", "0");
        if ("1".equals(checkRepeat)) {
            log.error("开始验重任务。。。。。。。。。。。。。");
            checkBankreconciliation();
        }
    }

    /**
     * 历史数据修复
     */
    @GetMapping("/repairdd")
    public void repairDatadd(String startDate, String endDate, HttpServletResponse response) throws Exception {
        repairBankDealDetails(startDate, endDate);
        renderJson(response, ResultMessage.success());
    }

    @GetMapping("/repairbr")
    public void repairDatabr(String startDate, String endDate, HttpServletResponse response) throws Exception {
        repairBankReconciliation(startDate, endDate);
        renderJson(response, ResultMessage.success());
    }

    /**
     * 修复银行账户交易明细
     *
     * @param startDate
     * @param endDate
     * @throws Exception
     */
    private void repairBankDealDetails(String startDate, String endDate) throws Exception {
        log.error("银行账户交易明细数据修复任务开始。");
        QuerySchema schema = QuerySchema.create().addSelect("*");//
        QueryConditionGroup condition = new QueryConditionGroup(ConditionOperator.and);
        if (startDate != null && endDate != null) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").between(startDate, endDate)));//单据日期
        }
        schema.addCondition(condition);
        List<BankDealDetail> bizObjects = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, schema, null);
        List<BankDealDetail> updateBankDealDetails = new ArrayList<>();
        if (bizObjects.size() > 0) {
            for (BankDealDetail bankDealDetail : bizObjects) {
                if (StringUtils.isEmpty(bankDealDetail.getConcat_info())) {
                    String concat_info = checkRepeatDataService.formatConctaInfoBankDealDetail(bankDealDetail);
                    bankDealDetail.fillConcatInfo(concat_info);
                    updateBankDealDetails.add(bankDealDetail);
                }
            }
        }
        if (updateBankDealDetails.size() > 0) {
            //更新账户交易明细
            EntityTool.setUpdateStatus(updateBankDealDetails);
            MetaDaoHelper.update(BankDealDetail.ENTITY_NAME, updateBankDealDetails);
            log.error("银行账户交易明细数据修复任务结束，共修复数据银行账户交易明细数据：" + updateBankDealDetails.size());
        }
    }

    /**
     * BankReconciliation
     *
     * @param startDate
     * @param endDate
     * @throws Exception
     */
    private void repairBankReconciliation(String startDate, String endDate) throws Exception {
        log.error("银行对账单数据修复任务开始。");
        QuerySchema schema = QuerySchema.create().addSelect("*");//
        QueryConditionGroup condition = new QueryConditionGroup(ConditionOperator.and);
        if (startDate != null && endDate != null) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_date").between(startDate, endDate)));//单据日期
        }
        schema.addCondition(condition);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        List<BankReconciliation> updateBankReconciliations = new ArrayList<>();
        if (bankReconciliations.size() > 0) {
            for (BankReconciliation bankReconciliation : bankReconciliations) {
                if (StringUtils.isEmpty(bankReconciliation.getConcat_info())) {
                    String concat_info = checkRepeatDataService.formatConctaInfoBankReconciliation(bankReconciliation);
                    bankReconciliation.setConcat_info(concat_info);
                    updateBankReconciliations.add(bankReconciliation);
                }
            }
        }
        if (updateBankReconciliations.size() > 0) {
            //更新账户银行对账单信息
            EntityTool.setUpdateStatus(updateBankReconciliations);
            CommonSaveUtils.updateBankReconciliation(updateBankReconciliations);
            log.error("银行对账单数据修复任务结束，共修复数据银行对账单数据：" + updateBankReconciliations.size());
        }
    }

    /**
     * 查询银行对账单重复数据
     * 根据拼接的concat_info验重
     */
    public void checkBankreconciliation() throws Exception {
        List<String> list = SqlHelper.selectList(BANKRECONCILIATIONMAPPER + ".queryRepeatInfo");
        if (list != null && list.size() > 0) {
            log.error("检测出来重复数据：" + list.size());
            SqlHelper.update(BANKRECONCILIATIONMAPPER + ".updateRepeatInfo", list);
        }
    }


}