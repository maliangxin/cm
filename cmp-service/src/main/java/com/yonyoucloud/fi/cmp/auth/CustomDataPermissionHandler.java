package com.yonyoucloud.fi.cmp.auth;

import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.permissions.ICustomDataPermissionHandler;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.springframework.stereotype.Component;

/**
 * @author: rtsungc
 * @version 1.0
 * @createTime 2022/10/25 17:02
 * @description
 */
@Component
public class CustomDataPermissionHandler implements ICustomDataPermissionHandler {

    @Override
    public boolean forceNoAuthWithNull(BillContext billContext) {
        return false;
    }

    @Override
    public boolean forceNoAuthWithDefaultValue(BillContext billContext) {
        return false;
    }

    /**
     扩展实现列表，表头时，只返回主表数据，去掉因为子表权限而产生的重复问题
     */
    @Override
    public boolean forceUseAllChildDataPermission(BillContext billContext) {
        String billnum = billContext.getBillnum();
        // TODO: 2024/8/16 为啥原来要去掉权限 
        if(IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum) || IBillNumConstant.BANKRECONCILIATION.equals(billnum)
                || IBillNumConstant.CMP_BILLCLAIM_CARD.equals(billnum) || IBillNumConstant.CMP_MYBILLCLAIM_LIST.equals(billnum)){//保留银行对账单、认领单的子表数据权限，其他的不保留
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean forceUseUnionMultiRole() {
        /**
         * 控制多角色时 权限取并集，默认为false
         * true 给海康提供 多角色时会有多个in条件拼接 导致sql冗长 有效率问题 公有云改为false
         */
        //        return true;
        return false;
    }

    @Override
    public boolean forceDeduplicationOfData(BillContext billContext) {
        return true;
    }
}