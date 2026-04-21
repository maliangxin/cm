package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.BaseOrgDTO;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.org.dto.OrgUnitDTO;
import com.yonyou.iuap.org.dto.tree.TreeContext;
import com.yonyou.iuap.org.service.itf.tree.IOrgTreeDefineQueryService;
import com.yonyou.iuap.org.service.itf.tree.IOrgTreeMemberQueryService;
import com.yonyou.iuap.org.service.itf.tree.IOrgTreeMemberWithOrgUnitQueryService;
import com.yonyou.iuap.org.service.itf.tree.IOrgTreeTenantConfService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FundOrgService {
    private static final String GLOBAL_ACCENTITY = "666666";

    public static FundsOrgQueryServiceComponent getFundQueryServiceComponent() {
        return AppContext.getBean(FundsOrgQueryServiceComponent.class);
    }

    /**
     * 获取资金组织的下级节点id
     * @param fundOrgId
     * @return
     */
    public static List<String> getChildFundOrg(String fundOrgId) {
        boolean enableOrgTree = AppContext.getBean(IOrgTreeTenantConfService.class).isEnableOrgTree(InvocationInfoProxy.getTenantid());
        if (enableOrgTree) {
            TreeContext treeContext = AppContext.getBean(IOrgTreeDefineQueryService.class).getDefaultTreeContextBySceneCode(InvocationInfoProxy.getTenantid(), "fundsorg", new Date());
            List<String> childrenIdById = AppContext.getBean(IOrgTreeMemberQueryService.class).getChildrenIdById(treeContext, fundOrgId, false);
            return childrenIdById;
        } else {
            FundsOrgQueryServiceComponent component = getFundQueryServiceComponent();
            return component.getChildrenIds(fundOrgId);
        }
    }

    public static List<BaseOrgDTO> getAllFundOrgsTree() {
        List<BaseOrgDTO> orgs = new ArrayList<>();
        //查询是否开启组织树
        boolean enableOrgTree = AppContext.getBean(IOrgTreeTenantConfService.class).isEnableOrgTree(InvocationInfoProxy.getTenantid());
        if (enableOrgTree) {
            TreeContext treeContext = AppContext.getBean(IOrgTreeDefineQueryService.class).getDefaultTreeContextBySceneCode(InvocationInfoProxy.getTenantid(), "fundsorg", new Date());
            List<OrgUnitDTO> orgUnitDTOS = AppContext.getBean(IOrgTreeMemberWithOrgUnitQueryService.class).getByCondition(treeContext, ConditionDTO.newOrgCondition());
            if (CollectionUtils.isNotEmpty(orgUnitDTOS)) {
                for (OrgUnitDTO orgUnitDTO : orgUnitDTOS) {
                    orgs.add(orgUnitDTO);
                }
            }
            //账户基础参数，如果开启了组织树，默认企业级账户显示
            List<OrgUnitDTO> globalOrg = orgUnitDTOS.stream().filter(vo -> GLOBAL_ACCENTITY.equals(vo.getId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(globalOrg)) {
                FundsOrgQueryServiceComponent component = getFundQueryServiceComponent();
                ConditionDTO conditionDTO = ConditionDTO.newOrgCondition();
                List<FundsOrgDTO> fundsOrgDTOs = component.getByCondition(conditionDTO);

                List<FundsOrgDTO> globalfinOrg = fundsOrgDTOs.stream().filter(vo -> GLOBAL_ACCENTITY.equals(vo.getId())).collect(Collectors.toList());
                if(CollectionUtils.isNotEmpty(globalfinOrg)){
                    orgs.addAll(globalfinOrg);
                }
            }
        } else {
            FundsOrgQueryServiceComponent component = getFundQueryServiceComponent();
            ConditionDTO conditionDTO = ConditionDTO.newOrgCondition();
            List<FundsOrgDTO> fundsOrgDTOs = component.getByCondition(conditionDTO);
            if (CollectionUtils.isNotEmpty(fundsOrgDTOs)) {
                orgs.addAll(fundsOrgDTOs);
            }
        }
        return orgs;
    }
}
