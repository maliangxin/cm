package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.ConditionDTO;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FinOrgQueryServiceComponent;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: liaojbo
 * @Date: 2024年08月27日 18:58
 * @Description:资金组织改造工具类
 */
@Resource
@Slf4j
public class AccentityUtil {
    static FundsOrgQueryServiceComponent fundsOrgQueryService = AppContext.getBean(FundsOrgQueryServiceComponent.class);
    static FinOrgQueryServiceComponent finOrgQueryService = AppContext.getBean(FinOrgQueryServiceComponent.class);
    static BaseRefRpcService baseRefRpcService = AppContext.getBean(BaseRefRpcService.class);;
    static OrgRpcService orgRpcService = AppContext.getBean(OrgRpcService.class);;
    public static void setAccentityRawToDtofromCtmJSONObject(CtmJSONObject param, AccentityRawInterface accentityRawInterface) {
        String accentityRaw = param.getString("accentityRaw");
        if (accentityRaw != null) {
            accentityRawInterface.setAccentityRaw(accentityRaw);
        }else {
            String actual_accentityRaw = CmpMetaDaoHelper.getAccentityRaw(accentityRawInterface.getAccentity());
            accentityRawInterface.setAccentityRaw(actual_accentityRaw);
        }
    }

    public static void setAccentityRawToDtofromField(String accentity, AccentityRawInterface accentityRawInterface) {
        if (accentity != null) {
            String actual_accentityRaw = CmpMetaDaoHelper.getAccentityRaw(accentityRawInterface.getAccentity());
            accentityRawInterface.setAccentityRaw(actual_accentityRaw);
        }
    }


    public static void addQueryConditionToGroupFromCtmJSONObject(CtmJSONObject param, QueryConditionGroup group) {
        if (param.getString("accentityRaw") != null) {
            group.addCondition(QueryCondition.name("accentityRaw").eq(param.getString("accentityRaw")));
        }
    }

    /**
     * 根据资金组织查询会计主体
     * @param fundorgid
     * @return
     */
    public static  Map<String, Object> getFinEntityMapByAccentityId(String fundorgid) {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getByIdWithFinOrg(InvocationInfoProxy.getTenantid(), fundorgid);
        FinOrgDTO finOrgDTO = finOrgQueryService.getById(fundsOrgDTO.getFinorgid());
        if (finOrgDTO == null) {
            return null;
        }
        ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
        Map<String, Object> accEntityMap = objectMapper.convertValue(finOrgDTO, Map.class);
        return accEntityMap;
    }

    public static  List<Map<String, Object>> getFinEntityMapListByAccentityIds(List<String> ids) {
        List<FinOrgDTO> finOrgDTOByAccentityIds = getFinOrgDTOByAccentityIds(ids);
        List<Map<String, Object>> accEntityMapList = new ArrayList<>();
        for (FinOrgDTO finOrgDTO :
                finOrgDTOByAccentityIds) {
            ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
            Map<String, Object> accEntityMap = objectMapper.convertValue(finOrgDTO, Map.class);
            accEntityMapList.add(accEntityMap);
        }
        return accEntityMapList;
    }

    //根据资金组织查询会计主体
    public static  FinOrgDTO getFinOrgDTOByAccentityId(String accEntityId) {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getByIdWithFinOrg(accEntityId);
        if (fundsOrgDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050010", "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！") /* "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！" */);
        }
        FinOrgDTO finOrgDTO = finOrgQueryService.getById(fundsOrgDTO.getFinorgid());
        if (finOrgDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101382"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080039", "根据 ID 查询会计组织接口未查询到数据！") /* "根据 ID 查询会计组织接口未查询到数据！" */);
        }
        return finOrgDTO;
    }

    public static List<FinOrgDTO> getFinOrgDTOByAccentityIds(List<String> idListnow) {
        Map<String, FinOrgDTO> stringFinOrgDTOMap = fundsOrgQueryService.queryAccEntityByFundOrgIds(idListnow);
        if (stringFinOrgDTOMap.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050010", "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！") /* "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！" */);
        }
        List<FinOrgDTO> finOrgDTOList = stringFinOrgDTOMap.values().stream().collect(Collectors.toList());
        return finOrgDTOList;
    }

    public static  List<FinOrgDTO> getFinOrgDTOsByAccentityId(List<String> accEntityIds) {
        Map<String, FinOrgDTO> idFinOrgDTOMap = fundsOrgQueryService.queryAccEntityByFundOrgIds(accEntityIds);
        List<FinOrgDTO> finOrgDTOList = idFinOrgDTOMap.values().stream().collect(Collectors.toList());
        return finOrgDTOList;
    }

    //根据资金组织查询会计主体，再查询本位币
    public static String getNatCurrencyIdByAccentityId(String accentityId) throws Exception {
        List<Map<String, Object>> finEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentityId); /* 暂不修改 已登记*/
        if (finEntity.size() == 0) {
            return ResultMessage.success();
        }
        log.error("accEntity.get(0).get(currency) ========== " + finEntity.get(0).get("currency"));
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(String.valueOf(finEntity.get(0).get("currency")));
        if (currencyTenantDTO == null) {
            return null;
        }
        return currencyTenantDTO.getId();
    }

    public static List<String> getFundsIdsByEnabledCondition() throws Exception {
       return fundsOrgQueryService.getIdsByCondition(ConditionDTO.newCondition().withEnabled());

    }

    public static List<Map<String, Object>> getFundMapLIstById(String fundsorgid) throws Exception {
        Map<String, Object> fundMap = getFundMapById(fundsorgid);
        List<Map<String, Object>> fundMapList = new ArrayList<>();
        if (fundMap == null) {
            return fundMapList;
        } else {
            fundMapList.add(fundMap);
            return fundMapList;
        }
    }

    public static Map<String, Object> getFundMapById(String fundsorgid) throws Exception {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getById(fundsorgid);
        if (fundsOrgDTO == null) {
            return null;
        }
        ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
        Map<String, Object> fundMap = objectMapper.convertValue(fundsOrgDTO, Map.class);
        return fundMap;
    }

    public static FundsOrgDTO getFundsOrgDTOById(String fundsorgid) throws Exception {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getById(fundsorgid);
        if (fundsOrgDTO == null) {
            return null;
        }
        return fundsOrgDTO;
    }

    public static void fillAccentityRawFiledsToBizObjectbyAccentityId(BizObject bizObject, String accEntity) {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getByIdWithFinOrg(accEntity);
        if (fundsOrgDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101383"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050009", "资金组织不存在") /* "资金组织不存在" */);
        }
        String finorgid = fundsOrgDTO.getFinorgid();
        if (StringUtils.isEmpty(finorgid)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101384"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050011", "资金组织对应的会计主体不存在") /* "资金组织对应的会计主体不存在" */);
        }
        bizObject.set(IFieldConstant.ACCENTITYRAW, finorgid);
        FinOrgDTO finOrgDTO = finOrgQueryService.getById(finorgid);
        bizObject.set(IFieldConstant.ACCENTITYRAW_NAME, finOrgDTO.getName());
        bizObject.set(IFieldConstant.ACCENTITYRAW_CODE, finOrgDTO.getCode());
    }

    //根据资金组织id 查询当前资金组织本身的信息
    public static  FundsOrgDTO getFundsOrgDTOByAccentityId(String accEntityId) {
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getByIdWithFinOrg(accEntityId);
        if (fundsOrgDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050010", "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！") /* "根据 ID 查询资金组织并返回关联的会计主体名称接口未查询到数据！" */);
        }
        return fundsOrgDTO;
    }


}
