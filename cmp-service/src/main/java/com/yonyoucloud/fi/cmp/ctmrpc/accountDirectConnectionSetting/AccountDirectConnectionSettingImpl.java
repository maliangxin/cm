package com.yonyoucloud.fi.cmp.ctmrpc.accountDirectConnectionSetting;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpAccountDirectConnectionSettingService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountDirectConnectionSettingImpl implements CtmCmpAccountDirectConnectionSettingService {
    /**
     * 查询银企联启用状态为启用，账户状态为启用，有customNo的账户直联状态配置
     * @param commonQueryData
     * @return
     * @throws Exception
     */
    @Override
    public List<BankAccountSetting> queryAccountDirectConnectionSettings(CommonRequestDataVo commonQueryData) throws Exception {
        List <String> enterpriseBankAccounts = commonQueryData.getIds();
        QuerySchema querySchema = QuerySchema.create().addSelect("customNo,enterpriseBankAccount,openFlag,empower");
        QueryConditionGroup group = new QueryConditionGroup();
        if(CollectionUtils.isNotEmpty(enterpriseBankAccounts)){
            group.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        }
        querySchema.addCondition(group);
        return MetaDaoHelper.queryObject(BankAccountSetting.ENTITY_NAME, querySchema, null);
    }
}
