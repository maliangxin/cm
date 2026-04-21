package com.yonyoucloud.fi.cmp.auth;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.permissions.ICustomOrgPermissionsHandler;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主组织权限过滤的控制类，针对参照过滤和列表查询
 *
 * @author maliangn
 * @since 2023-10-19
 */
@Component
@Slf4j
public class CustomMasterOrgPermissionsHandler implements ICustomOrgPermissionsHandler {
    /**
     * 需要跳出权限的billnum
     */
    static List<String> billnumList = new ArrayList<>();
    /**
     * 需要跳出权限的字段
     */
    static List<String> fieldList = new ArrayList<>();
    static List<String> referFullNameList = new ArrayList<>();
    static List<String> characterDefList =  new ArrayList<>();

    static {
        billnumList.add(IBillNumConstant.CMP_BILLCLAIMCENTER_LIST);
        billnumList.add(IBillNumConstant.CMP_MYBILLCLAIM_LIST);
        billnumList.add(IBillNumConstant.CMP_BILLCLAIM_CARD);
        billnumList.add(IBillNumConstant.CMP_BILLCLAIMCENTER);
        billnumList.add(IBillNumConstant.BANKRECONCILIATIONLIST);
        //余额调节表日记账和对账单跳出主组织权限
        billnumList.add(IBillNumConstant.BALANCEADJUSTRESULT_BANK);
        billnumList.add(IBillNumConstant.BALANCEADJUSTRESULT_JOURAL);

        billnumList.add(IBillNumConstant.BANKRECONCILIATION);

        billnumList.add(IBillNumConstant.CMP_BANKRECONCILIATIONSETTING);

        fieldList.add(IFieldConstant.BANKACCOUNT_NAME);
        fieldList.add(IFieldConstant.ACTUALCLAIMACCENTIRY_NAME);
        fieldList.add(IFieldConstant.USEORG_NAME);
        fieldList.add(IFieldConstant.USEORG_NAME_SECOND);
        fieldList.add(IFieldConstant.ACCENTITY_NAME);
        fieldList.add(IFieldConstant.DEPT_NAME);
        referFullNameList.add("org.func.BaseOrg");
        characterDefList.add("cmp.receivebill.ReceiveBillCharacterDef");
    }

    @Autowired
    AutoConfigService autoConfigService;

    /**
     * 主组织过滤控制 - 参照
     *
     * @param billContext
     * @param billDataDto
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeReferMasterOrgPermissionExt(BillContext billContext, BillDataDto billDataDto) throws Exception {

        String billnum = billContext.getBillnum();
        Boolean agentFlag = autoConfigService.getEnableBizDelegationMode();// 是否开启结算中心代理模式
        Boolean inoutFlag = autoConfigService.getUnifiedIEModelWhenClaim();// 是否开启统收统支模式

        if (billnumList.contains(billnum) && fieldList.contains(billDataDto.getKey()) && (agentFlag || inoutFlag)) {
            return false;
        }

        // 银行对账设置的企业银行账户需要跳出权限
        if (IBillNumConstant.CMP_BANKRECONCILIATIONSETTING.equals(billnum) && "bankaccount_name".equals(billDataDto.getKey())) {
            return false;
        }

        // 银行对账设置子表授权使用组织跳出主组织权限
        if (IBillNumConstant.CMP_BANKRECONCILIATIONSETTING.equals(billnum) && "useorg_name".equals(billDataDto.getKey())) {
            return false;
        }

        //银行对账单期初未达项跳出权限控制
        if (IBillNumConstant.CMP_BANKRECONCILIATIONWDLIST.equals(billnum) && fieldList.contains(billDataDto.getKey())) {
            return false;
        }

        //期初余额设置跳出权限控制
        if (IBillNumConstant.CMP_OPENINGOUTSTANDING_CARD.equals(billnum) && fieldList.contains(billDataDto.getKey())) {
            return false;
        }

        //判断是不是特征，需不需要跳过组织权限过滤
        if(isSkipOrgFilter(billDataDto)){
            return false;
        }

        return true;
    }

    /**
     * 判断要不要跳过组织过滤
     * @param billDataDto
     * @return
     */
    boolean isSkipOrgFilter(BillDataDto billDataDto) {
        boolean skipFlag = false;
        try {
            String referDataFullName = billDataDto.getRefEntity().getCDataClass_FullName();
            if (referFullNameList.contains(referDataFullName) && isCharacterDef(billDataDto)) {
                skipFlag = true;
            }
        }catch (Exception e){
            log.error("判断要不要跳过组织过滤异常",e);
        }
        return skipFlag;
    }

    /**
     * 判断是不是特征
     * @param billDataDto
     * @return
     */
    boolean isCharacterDef(BillDataDto billDataDto){
        return characterDefList.contains(billDataDto.getFullname());
    }


    /**
     * 主组织过滤控制 - 列表查询
     *
     * @param billcontext
     * @param querySchema
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeMasterOrgPermissionExt(BillContext billcontext, QuerySchema querySchema) throws Exception {

        String billnum = billcontext.getBillnum();
        if (billnumList.contains(billnum)) {
            return false;
        }
        return true;
    }


}
