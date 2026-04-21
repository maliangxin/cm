package com.yonyoucloud.fi.cmp.auth;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.workbench.model.OrgPermVO;
import com.yonyou.workbench.param.OrgEntryParam;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 组织权限查询的工具类
 *
 * @author maliangn
 * @since 2024-01-25
 *
 */
@Service
public class OrgDataPermissionService {

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    /**
     * 根据服务编码查询授权的业务组织
     *
     * @return
     */
    public Set<String> queryAuthorizedOrgByServiceCode(String serviceCode) throws Exception {
        Set<String> orgs = new HashSet<>();
        OrgEntryParam orgEntryParam = new OrgEntryParam();
        orgEntryParam.setToken(InvocationInfoProxy.getToken());
        orgEntryParam.setTenantId(InvocationInfoProxy.getTenantid());
        orgEntryParam.setUserId(InvocationInfoProxy.getUserid());
        orgEntryParam.setConvertGloble(false);// 是否包含转化
        orgEntryParam.setIncludeDisable(false); // 是否包含禁用
        orgEntryParam.setServiceCode(serviceCode); // 单据页面所属服务编码
        OrgPermVO orgPermVO = AuthSdkFacadeUtils.getUserMasterOrgPermission(orgEntryParam);
        if (orgPermVO != null) {
            orgs = orgPermVO.getOrgIds();
        }
        return orgs;
    }

    /**
     * 根据服务编码和银行账号查询有权限的组织
     *
     * @param serviceCode
     * @param bankaccount
     * @return
     * @throws Exception
     */
    public Set<String> queryAuthorizedOrgByServiceCodeAndBankAccount(String serviceCode, String bankaccount) throws Exception {
        Set<String> intersectionSet = new HashSet<>();
        if(StringUtils.isNotEmpty(serviceCode) && StringUtils.isNotEmpty(bankaccount)){
            EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankaccount);
            if (enterpriseBankAcctVoWithRange != null && enterpriseBankAcctVoWithRange.getAccountApplyRange() != null) {
                List<String> orgids = new ArrayList<>();
                List<OrgRangeVO> orgRangeVOList = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                for (OrgRangeVO orgRangeVO : orgRangeVOList) {
                    orgids.add(orgRangeVO.getRangeOrgId());
                }
                if (orgids.size() > 0) {
                    Set<String> orgown = this.queryAuthorizedOrgByServiceCode(serviceCode);
                    intersectionSet.addAll(orgids); // 将 List 转换为 Set
                    intersectionSet.retainAll(orgown);
                }
            }
        }
        return intersectionSet;
    }


}
