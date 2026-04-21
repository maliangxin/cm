package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmCmpBankReconciliationNewRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.PageRequest;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.PageRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CtmcmpBankReconciliationNewRpcServiceImpl implements CtmCmpBankReconciliationNewRpcService {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Override
    public Pager queryBankReconciliationListByPage(PageRequestDTO<CommonRequestDataVo> pageRequestDTO) throws Exception {
        log.error("分页查询银行交易明细-pageRequestDTO:" + CtmJSONObject.toJSONString(pageRequestDTO));
        CommonRequestDataVo params = pageRequestDTO.getParam();
        PageRequest pageRequest = pageRequestDTO.getPageRequest();
        //会计主体列表
        List<String> accentitys = params.getAccentityList();
        //银行账户列表
        List<String> enterpriseBankAccounts = params.getEnterpriseBankAccountList();
        // 开始日期
        String startDate = params.getStartDate();
        if (startDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100214"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080059", "查询开始日期不能为空") /* "查询开始日期不能为空" */);
        }
        // 结束日期
        String endDate = params.getEndDate();
        if (endDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100215"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080058", "查询结束日期不能为空") /* "查询结束日期不能为空" */);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (!CollectionUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }
        if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(enterpriseBankAccounts));
        }
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        schema.appendQueryCondition(conditionGroup);
        schema.addPager(pageRequest.getPageIndex(), pageRequest.getPageSize());
        Pager pager = MetaDaoHelper.queryByPage(BankReconciliation.ENTITY_NAME, schema);
        log.error("分页查询银行交易明细-查询bip数据库查询数据量为:" + pager.getRecordList().size());
        return pager;
    }

}
