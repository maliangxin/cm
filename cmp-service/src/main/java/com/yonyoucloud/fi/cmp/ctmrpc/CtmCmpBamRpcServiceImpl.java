package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.ctm.inner.rpc.bam.CtmcmpBamRpcService;
import com.yonyoucloud.ctm.inner.vo.common.BamBankAccountSettingReqVo;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CtmCmpBamRpcServiceImpl implements CtmcmpBamRpcService {

    @Override
    public List<Map<String, Object>> queryBankAccountSettingForBam(BamBankAccountSettingReqVo queryDataVO) throws Exception {
        String accEntity = queryDataVO.getAccentity();
        List<String> bankAccountIds = queryDataVO.getBankAccountIds();
        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.account as account,openFlag,customNo,id,openTicketService,accStatus,enableDate,empower");
        if(StringUtils.isNotEmpty(accEntity)){
            schema.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ACCENTITY).eq(accEntity)));
        }
        if(CollectionUtils.isNotEmpty(bankAccountIds)){
            schema.addCondition(QueryConditionGroup.and(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ENTERPRISE_BANK_ACCOUNT).in(bankAccountIds))));
        }
        return MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
    }
}
