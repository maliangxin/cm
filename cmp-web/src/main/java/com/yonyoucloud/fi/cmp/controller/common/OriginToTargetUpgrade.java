package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.meta.biz.FillFkDao;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Author guoyangy
 * @Date 2023/6/6 15:22
 * @Description guoyang
 * @Version 1.0
 */
@RestController
@Slf4j
public class OriginToTargetUpgrade extends BaseController {
    @PostMapping(value = "/etl/upgrade")
    public void upgradeUserDefineToTarget(HttpServletResponse response, @RequestBody CtmJSONObject param) throws Exception {
        /**
         {
         "characterinstance":"characterDef",
         "characteruri":"cm.transferaccount.TransferAccountCharacterDef",
         "cyzdomain":"cmp",
         "define":[ {"PK_ORG":"0001A11000000000I4B9","ZJYT":"1001A110000000001EKE","ytenant":"pg67ctbp","DXJE":"壹角整"}],
         "fullname":"cm.transferaccount.TransferAccount",
         "pkfield":"PK_ORG"
         }
         */
        //InvocationInfoProxy.getNewArch();
        log.error("请求入参:" + CtmJSONObject.toJSONString(param));
        //单据主实体uri
        String fullname = (String) param.get("fullname");
        //domain
        String czyDomain = (String) param.get("cyzdomain");
        //特征instanceid
        String nameinmainentity = (String) param.get("characterinstance");
        //特征实体uri
        String characterdefuri = (String) param.get("characteruri");
        //高级版业务主键名称
        String pkfield = (String) param.get("pkfield");

        List<Map<String, Object>> defineList = (List<Map<String, Object>>) param.get("define");
        //特征值map,需要去除主键kv
        //List<Map<String,Object>> defineList = JSON.parseObject(definestrObj.toString(),new TypeReference<List<Map<String,Object>>>() {});
        if (CollectionUtils.isEmpty(defineList)) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80075", "特征集合为空") /* "特征集合为空" */));
            return;
        }

        for (Map<String, Object> characterMap : defineList) {
            Object pkValue = characterMap.get(pkfield);
            if (pkValue == null) {
                continue;
            }
            characterMap.remove(pkfield);
            this.writeCharacterToDB(response, fullname, pkValue, czyDomain, characterMap, characterdefuri, nameinmainentity);
        }
        renderJson(response, ResultMessage.data("succ"));
    }

    /**
     * @param fullname
     * @param pkValue
     * @param czyDomain
     * @param characterMap
     * @param characterdefuri
     * @param nameinmainentity
     */
    private void writeCharacterToDB(HttpServletResponse response, String fullname, Object pkValue, String czyDomain,
                                    Map<String, Object> characterMap, String characterdefuri, String nameinmainentity) throws Exception {
        //单据实体
        BizObject mainEntity = MetaDaoHelper.findById(fullname, Long.parseLong(pkValue.toString()), czyDomain);
        if (null == mainEntity) {
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80074", "单据主实体查询空,主键") /* "单据主实体查询空,主键" */ + pkValue + " fullname=" + fullname + " czyDomain=" + czyDomain));
            return;
        }
        //兼容资金付款单数据库类型与元数据类型不一致
        mainEntity.remove("rejecttype");

        //特征实体
        BizObject characterDef = Objectlizer.convert(characterMap, characterdefuri);
        characterDef.setEntityStatus(EntityStatus.Insert);
        //将特征赋值给单据实体类
        mainEntity.put(nameinmainentity, characterDef);
        mainEntity.setEntityStatus(EntityStatus.Update);
        //主键关联
        FillFkDao.execute(fullname, mainEntity);
        //更新单据主实体+特征值
        MetaDaoHelper.update(fullname, mainEntity, czyDomain);
    }
}
