package com.yonyoucloud.fi.cmp.fcdsusesetting.service.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.utils.DateEventUtils;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting;
import com.yonyoucloud.fi.cmp.fcdsusesetting.FcDsUseSetting_b;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.FcdsUseSettingVO;
import com.yonyoucloud.fi.cmp.fcdsusesetting.dto.MessageResultVO;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingService;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author guoxh
 */
@Slf4j
@Service
public class FcdsUseSettingServiceImpl implements IFcdsUseSettingService {

    @Override
    public List<FcdsUseSettingVO> queryByCondition(String flowAction, String businessScenario, String dcFlag) {
        List<FcdsUseSettingVO> result = new ArrayList<>();
        if(StringUtils.isEmpty(flowAction)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102379"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD3FE05180009", "流程处理环节不能为空"));
        }
        if(StringUtils.isEmpty(businessScenario)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102378"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CD42804D80002", "适用对象不能为空"));
        }
        if(StringUtils.isEmpty(dcFlag)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102411"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800FC", "收付方向不能为空！") /* "收付方向不能为空！" */);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM + ",cdp.name")
                .appendQueryCondition(
                        QueryCondition.name("mainid.action").eq(flowAction),
                        QueryCondition.name("mainid.businessScenario").eq(businessScenario),
                        QueryCondition.name("dcFlag").eq(dcFlag),
                        QueryCondition.name("enable").eq("1")
                );
        try {
            List<FcDsUseSetting_b> list = MetaDaoHelper.query(FcDsUseSetting_b.ENTITY_NAME,querySchema,null);
            if(CollectionUtils.isNotEmpty(list)){
                result = convertEntity(list);
            }
        } catch (Exception e) {
            log.error("查询失败，失败原因：",e);
        }
        return result;
    }


    public List<FcdsUseSettingVO> convertEntity(List<FcDsUseSetting_b> list){
        return list.stream().map(item ->{
            FcdsUseSettingVO vo = new FcdsUseSettingVO();
            vo.setId(item.getId());
            vo.setBizObject(item.getBizObject());
            vo.setBizObjectCode(item.getBizObjectCode());
            vo.setBizObjectName(item.getBizObjectName());
            vo.setObject(item.getName());
            vo.setTransType(item.getTradeType());
            vo.setCdpId(item.getCdp());
            vo.setCdpName(item.get("cdp_name"));
            return vo;
        }).collect(Collectors.toList());
    }
}
