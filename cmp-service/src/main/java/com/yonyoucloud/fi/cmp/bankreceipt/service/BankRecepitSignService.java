package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.Map;

/**
 * desc:银行交易回单统一校验
 * author:wangqiangac
 * date:2023/8/15 18:34
 */
public interface BankRecepitSignService {

    /**
     * 银行回单统一校验接口
     * @param bankElectronicReceipt
     */
    String bankRecepitSign(BankElectronicReceipt bankElectronicReceipt) throws Exception;

    /**
     * 获取企业银行账户是否开通银企联
     * @param enterpriseBankAccount
     * @return
     * @throws Exception
     */
    public default boolean queryOpenFlag(Object enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if (setting != null && setting.size() > 0) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }



}
