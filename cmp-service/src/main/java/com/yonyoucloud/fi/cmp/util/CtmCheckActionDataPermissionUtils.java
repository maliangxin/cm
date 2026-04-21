package com.yonyoucloud.fi.cmp.util;

import com.yonyou.permission.entity.AuthActionDataAuthDto;
import com.yonyou.permission.util.AuthSdkFacadeUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * 数据权限校验工具类
 */
public class CtmCheckActionDataPermissionUtils {

    public static void querySettingDetailInfo(Map<String, String> parammap, CtmJSONObject param)throws Exception {
        AuthActionDataAuthDto authActionDataAuthDto = initAuthActionDataAuthDto(parammap, param);
        Long id = param.getLong("id");
        authActionDataAuthDto.setId(id);
        if (!AuthSdkFacadeUtils.isActionHasDataAuthByBillId(authActionDataAuthDto)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102472"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_1B08281405B00009", "当前数据无操作权限") /* "当前数据无操作权限" */);
        }
    }


    private static AuthActionDataAuthDto initAuthActionDataAuthDto (Map<String, String> parammap,  CtmJSONObject param) {
        String scene = "bill_edit";
        if (null != param.get("scene")){
            scene = param.getString("scene");
        }
        AuthActionDataAuthDto authActionDataAuthDto = new AuthActionDataAuthDto();
        authActionDataAuthDto.setActionCode(parammap.get("action"));
        authActionDataAuthDto.setBizObjCode(parammap.get("bizObjCode"));
        authActionDataAuthDto.setFullName(parammap.get("fullName"));
        authActionDataAuthDto.setServiceCode(parammap.get("serviceCode"));
        authActionDataAuthDto.setSceneCode(scene);
        return authActionDataAuthDto;
    }

}
