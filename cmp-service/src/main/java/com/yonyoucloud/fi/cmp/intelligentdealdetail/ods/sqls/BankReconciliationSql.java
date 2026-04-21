package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls;

import com.yonyou.ucf.mdd.ext.core.AppContext;

/**
 * @Author guoyangy
 * @Date 2024/8/9 10:30
 * @Description todo
 * @Version 1.0
 */
public class BankReconciliationSql{

    public static final String TOTALCOUNT="totalcount";
    public static final String CONDITION="processstatus";

    //2,根据processstatus查询所有流水id
    public static final String GETALLBANKRECONCILIATIONIDBYPROCESSSTATUS="SELECT id FROM CMP_BANKRECONCILIATION WHERE "+CONDITION+"=? AND ytenant_id='"+ AppContext.getYTenantId()+"' order by id desc limit 100000";

    //1.根据processstatus查询流水总数量
    public static final String TOTALCOUNTBYCONCATINFO="SELECT COUNT(id) as "+TOTALCOUNT+" FROM CMP_BANKRECONCILIATION WHERE "+CONDITION+"=? AND ytenant_id='"+ AppContext.getYTenantId()+"'";

}
