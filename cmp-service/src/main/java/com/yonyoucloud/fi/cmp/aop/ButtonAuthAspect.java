package com.yonyoucloud.fi.cmp.aop;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.permission.entity.AuthActionDataAuthDto;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.utils.BeanUtils;
import com.yonyoucloud.fi.cmp.aop.vo.CmpDataAuthVo;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: ButtonAuthAspect
 * @Description: 按钮权限控制切面
 * @Author: wujfeng
 * @Date: 2024/3/25 14:55
 */

@Component
@Order(0)
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ButtonAuthAspect {


    @Pointcut(value = "@annotation(annotation) && execution(* com.yonyoucloud.fi.cmp..*(..))", argNames = "annotation")
    public void Pointcut(ButtonAuth annotation) {
    }

    @Before(value = "Pointcut(annotation)")
    public void ButtonAuthAspect(JoinPoint jp, ButtonAuth annotation) throws Exception {
        //获取权限动作
        DataAuthActionEnum actionEnum = annotation.supportAction();
        //对应fullname
        String fullName = annotation.fullName();

        Object[] args = jp.getArgs();

        List<BizObject> bizObjectList = new ArrayList<>();
        CtmJSONObject params = new CtmJSONObject();
        BizObject bizObjectParams = new BizObject();

        if (args[0] instanceof ArrayList || args[0] instanceof List) {
            bizObjectList = (List<BizObject>) args[0];
        } else if (args[0] instanceof CtmJSONObject) {
            params = (CtmJSONObject) args[0];
        } else if (args[0] instanceof BizObject) {
            bizObjectParams = (BizObject) args[0];
        } else {
            // 撤回操作
            bizObjectList = (ArrayList<BizObject>) args[2];
        }

        if (CollectionUtils.isEmpty(bizObjectList) && Objects.isNull(params) && Objects.isNull(bizObjectParams)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101609"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180585", "单据不存在或已被删除!") /* "单据不存在或已被删除!" */));
        }
        CmpDataAuthVo authVo = CmpDataAuthVo.find(annotation.fullName(), actionEnum.getCode());

        if (CollectionUtils.isNotEmpty(bizObjectList)) {
            Set<Object> ids = bizObjectList.stream().map(bizObject -> bizObject.getId()).collect(Collectors.toSet());
            authVo.setIds(ids);
        }

        if (Objects.nonNull(params) && params.size() > 0) {
            if (IBillNumConstant.CMP_BILLCLAIM_CARD.equals(params.get("billnum")) || IBillNumConstant.CMP_MYBILLCLAIM_LIST.equals(params.get("billnum"))) {
                if (params.get("data") instanceof ArrayList) {
                    ArrayList<LinkedHashMap<String, Object>> data = (ArrayList<LinkedHashMap<String, Object>>) params.get("data");
                    if (CollectionUtils.isEmpty(data)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101609"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180585", "单据不存在或已被删除!") /* "单据不存在或已被删除!" */));
                    }
                    Set<Object> ids = data.stream().map(bizObject -> bizObject.get("id")).collect(Collectors.toSet());
                    authVo.setIds(ids);
                } else if (params.get("data") instanceof LinkedHashMap) {
                    Object id = ((LinkedHashMap<String, Object>) params.get("data")).get("id");
                    authVo.setIds(Collections.singleton(id));
                }
            }
        }

        if (Objects.nonNull(bizObjectParams) && bizObjectParams.size() > 0) {
            if (Objects.nonNull(bizObjectParams.get("id"))) {
                authVo.setIds(Collections.singleton(bizObjectParams.get("id")));
            }
        }

        if (!checkDBData(authVo.getIds(), fullName)) {
            return;
        }

        HashMap<Object, Boolean> result = AuthSdkFacadeUtils.isActionHasDataAuthByBillIds(handleAuthDo(authVo));
        if (Objects.nonNull(result)) {
            Set<Object> objects = result.keySet();
            for (Object key : objects) {
                Boolean isPass = result.get(key);
                if (!isPass.booleanValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102472"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_1B08281405B00009", "当前数据无操作权限") /* "当前数据无操作权限" */);
                }
            }
        }

    }

    public static AuthActionDataAuthDto handleAuthDo(CmpDataAuthVo stctDataAuthVo) {
        AuthActionDataAuthDto authDto = new AuthActionDataAuthDto();
        BeanUtils.copyProperties(stctDataAuthVo, authDto);
        return authDto;
    }

    private boolean checkDBData(Set<Object> ids, String fullName) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        querySchema.appendQueryCondition(QueryCondition.name("id").in(ids));
        List<BizObject> iBillDOS = MetaDaoHelper.queryObject(fullName, querySchema, null);

        return CollectionUtils.isEmpty(iBillDOS) ? Boolean.FALSE : Boolean.TRUE;
    }
}
