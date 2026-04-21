package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.stct.api.openapi.IBusinessDelegationApiService;
import com.yonyoucloud.fi.stct.api.openapi.businessdelegation.BDAccountAuthApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.request.BDAccountAuthBatchReqVO;
import com.yonyoucloud.fi.stct.api.openapi.request.QueryDelegationReqVo;
import com.yonyoucloud.fi.stct.api.openapi.response.BDAccountAuthBatchRespVO;
import com.yonyoucloud.fi.stct.api.openapi.vo.businessDelegation.BusinessDelegationVo;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class BillCountUtil {
    @Resource
    IBusinessDelegationApiService iBusinessDelegationApiService;

    @Resource
    BDAccountAuthApiService bdAccountAuthApiService;

    @Resource
    IOpenApiService iOpenApiService;

    /**
     * 获取会计主体
     *
     * @param orgList
     * @param inoutFlag
     * @param agentFlag
     * @return
     * @throws Exception
     */
    public List<String> getAccentityList(List<String> orgList, Boolean inoutFlag, Boolean agentFlag) throws Exception {
        List<String> accentityList = new ArrayList<>();
        accentityList = orgList;
        if (inoutFlag) {
            // 查询统收统支关系：下级查上级
            IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
            incomeAndExpenditureReqVO.setControllId(orgList);
            List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryRecControllList(incomeAndExpenditureReqVO);
            List<String> expendIds = new ArrayList<>();
            if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0) {
                for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                    expendIds.add(incomeAndExpenditureResVO.getActualAccentity());
                }
            }
            accentityList.addAll(expendIds);
        }
        if (agentFlag) {
            //查询资金业务委托关系  会计主体查询结算中心
            QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
            queryDelegationReqVo.setAccentitys(orgList);
            queryDelegationReqVo.setEnableoutagestatus(0);
            Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
            Set<String> expendIds = new HashSet<>();
            if (result.getData() != null && result.getCode() != 404) {
                List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
                if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                    for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                        if (businessDelegationVo.getSettlementCenter() != null) {
                            expendIds.add(businessDelegationVo.getSettlementCenter());
                        }
                    }
                }
            }
            accentityList.addAll(expendIds);
        }
        return accentityList;
    }

    /**
     * 获取结算中心会计主体
     *
     * @param orgList
     * @return
     * @throws Exception
     */
    public Set<String> getAgentAccentityList(Boolean agentFlag, List<String> orgList) throws Exception {
        if (!agentFlag) {
            return new HashSet<>();
        }
        //查询资金业务委托关系  会计主体查询结算中心
        QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
        queryDelegationReqVo.setAccentitys(orgList);
        queryDelegationReqVo.setEnableoutagestatus(0);
        Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
        Set<String> expendIds = new HashSet<>();
        if (result.getData() != null && result.getCode() != 404) {
            List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
            if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                    if (businessDelegationVo.getSettlementCenter() != null) {
                        expendIds.add(businessDelegationVo.getSettlementCenter());
                    }
                }
            }
        }
        return expendIds;
    }

    /**
     * 获取统收统支会计主体
     *
     * @param orgList
     * @return
     * @throws Exception
     */
    public Set<String> getInoutAccentityList(Boolean inoutFlag, List<String> orgList) throws Exception {
        if (!inoutFlag) {
            return new HashSet<>();
        }
        // 查询统收统支关系：下级查上级
        IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
        incomeAndExpenditureReqVO.setControllId(orgList);
        List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryRecControllList(incomeAndExpenditureReqVO);
        Set<String> expendIds = new HashSet<>();
        if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0) {
            for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                expendIds.add(incomeAndExpenditureResVO.getActualAccentity());
            }
        }
        return expendIds;
    }

    /**
     * 根据实际结算会计主体查询受控的资金组织； 或者根据结算中心查询对应的业务资金组织
     *
     * @param orgList   上级资金组织
     * @param inoutFlag 是否开启统收统支
     * @param agentFlag 是否开启资金结算
     * @return
     * @throws Exception
     */
    public List<String> getControllAccentityList(List<String> orgList, Boolean inoutFlag, Boolean agentFlag) throws Exception {
        List<String> accentityList = new ArrayList<>();
        accentityList = orgList;
        if (inoutFlag) {
            // 查询统收统支关系：上级查询下级
            IncomeAndExpenditureReqVO incomeAndExpenditureReqVO = new IncomeAndExpenditureReqVO();
            incomeAndExpenditureReqVO.setActualAccentity(orgList);
            List<IncomeAndExpenditureResVO> incomeAndExpenditureResVOS = iOpenApiService.queryControllList(incomeAndExpenditureReqVO);
            List<String> expendIds = new ArrayList<>();
            if (incomeAndExpenditureResVOS != null && incomeAndExpenditureResVOS.size() > 0) {
                for (IncomeAndExpenditureResVO incomeAndExpenditureResVO : incomeAndExpenditureResVOS) {
                    expendIds.add(incomeAndExpenditureResVO.getControllId());
                }
            }
            accentityList.addAll(expendIds);
        }
        if (agentFlag) {
            //查询资金业务委托关系  结算中心查询业务资金组织
            QueryDelegationReqVo queryDelegationReqVo = new QueryDelegationReqVo();
            queryDelegationReqVo.setSettlementCenter(orgList.get(0));
            queryDelegationReqVo.setEnableoutagestatus(0);
            Result result = iBusinessDelegationApiService.queryBusinessDelegation(queryDelegationReqVo);
            Set<String> expendIds = new HashSet<>();
            if (result.getData() != null && result.getCode() != 404) {
                List<BusinessDelegationVo> businessDelegationVos = (List<BusinessDelegationVo>) result.getData();
                if (businessDelegationVos != null && businessDelegationVos.size() > 0) {
                    for (BusinessDelegationVo businessDelegationVo : businessDelegationVos) {
                        if (businessDelegationVo.getAccentity() != null) {
                            expendIds.add(businessDelegationVo.getAccentity());
                        }
                    }
                }
            }
            accentityList.addAll(expendIds);
        }

        return accentityList;
    }

    /**
     * RPT0341到账认领中心代理结算模式优化
     *
     * @param orgList
     * @param agentFlag
     * @return
     * @throws Exception
     */
    public Map<BDAccountAuthBatchRespVO.BDAccountAuthKey, BDAccountAuthBatchRespVO.BDAccountAuthRespVO> getAuthDelegationList(List<String> orgList, Boolean agentFlag) throws Exception {
        if (ObjectUtils.isNotEmpty(orgList) && agentFlag) {
            BDAccountAuthBatchReqVO bdAccountAuthBatchReqVO = new BDAccountAuthBatchReqVO();
            List<BDAccountAuthBatchReqVO.BDAccountAuthReqVO> reqList = new ArrayList<>();
            orgList.forEach(item -> {
                BDAccountAuthBatchReqVO.BDAccountAuthReqVO bdAccountAuthReqVO = new BDAccountAuthBatchReqVO.BDAccountAuthReqVO();
                bdAccountAuthReqVO.setAccentityId(item);
                reqList.add(bdAccountAuthReqVO);
            });
            bdAccountAuthBatchReqVO.setReqList(reqList);
            BDAccountAuthBatchRespVO bdAccountAuthBatchRespVO =
                    bdAccountAuthApiService.queryAuthorizedEnterpriseAccounts(bdAccountAuthBatchReqVO);
            if (ObjectUtils.isEmpty(bdAccountAuthBatchRespVO)) {
                return null;
            }
            Map<BDAccountAuthBatchRespVO.BDAccountAuthKey, BDAccountAuthBatchRespVO.BDAccountAuthRespVO> returnMap =
                    bdAccountAuthBatchRespVO.getAuthMap();
            if (ObjectUtils.isEmpty(returnMap)) {
                return null;
            }
            return returnMap;
        }
        return null;
    }
}
