package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2025年04月16日 10:32
 * @Description:保存前规则，进行组织权限校验，和账户是否在对应组织下的校验
 */
@Slf4j
@Component("regularbalanceBeforeSaveRule")
public class RegularbalanceBeforeSaveRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    OrgDataPermissionService orgDataPermissionService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        //默认值，可以根据billnum修改对应字段名，接入其他单据；目前只接入定期余额维护
        String enterpriseBankAccount = "enterpriseBankAccount";
        String accentity = "accentity";
        if(IBillNumConstant.CMP_REGULARBALANCE.equals(billContext.getBillnum())|| IBillNumConstant.CMP_REGULARBALANCELIST.equals(billContext.getBillnum())){
             enterpriseBankAccount = "enterpriseBankAccount";
             accentity = "accentity";
        }
        //复制自bankReceiptBeforeSaveRule
        if (bills != null && bills.size() > 0) {
            for (BizObject bizObject : bills) {
                if (bizObject.get(enterpriseBankAccount) == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101368"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F4", "银行账户不能为空！") /* "银行账户不能为空！" */);
                }else {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get(enterpriseBankAccount));
                    if (enterpriseBankAcctVO == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101369"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080035", "数据中的银行账户[") /* "数据中的银行账户[" */ + bizObject.get("enterpriseBankAccount_account") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080034", "]未启用或不存在！") /* "]未启用或不存在！" */);
                    }
                    // 判断授权使用组织
                    /**
                     * 1，获取授权的组织
                     * 2，获取银行账户的适用范围
                     * 3，判断适用范围内的组织是否已授权，只要存在一个授权的，就可以导入
                     */
                    // 获取授权的组织
                    Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.CMP_REGULARBALANCELIST);
                    if(orgs != null && orgs.size() <1){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080037", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                    }
                    EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get(enterpriseBankAccount));
                    if(enterpriseBankAcctVoWithRange != null){
                        List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                        // 使用范围中的组织是否是授权的组织
                        Boolean containFlag = false;
                        for(OrgRangeVO orgRangeVO : orgRangeVOS){
                            if(orgs.contains(orgRangeVO.getRangeOrgId())){
                                containFlag = true;
                                break;
                            }
                        }
                        if(!containFlag){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080037", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                        }
                        // 判断使用组织
                        Object useOrg = bizObject.get(accentity);
                        if(useOrg !=null){
                            String useOrgId = useOrg.toString();
                            // 判断导入的使用组织是否在银行账户的使用组织范围内
                            List<String> orgRangeIds = orgRangeVOS.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
                            if(!orgRangeIds.contains(useOrgId)){
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CC34CA05A00005", "银行账户的适用范围未包含该使用组织，请检查【企业资金账户】节点！") /* "银行账户的适用范围未包含该使用组织，请检查【企业资金账户】节点！" */);
                            }
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
