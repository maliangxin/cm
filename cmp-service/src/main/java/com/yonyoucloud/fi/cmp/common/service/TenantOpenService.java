package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.diwork.model.ControlPointStatus;
import com.yonyou.diwork.model.dto.ControlPointDTO;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.diwork.service.ILicenseQueryService;
import com.yonyou.diwork.service.IMenuBarService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.MenuBarVO;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.rule.ICtmOpenInitService;
import com.yonyoucloud.fi.basecom.rule.domain.OpenInitDTO;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 租户开通从ctm-base-core移植服务内
 *
 * @author maliangn
 */
@Service
@Slf4j
public class TenantOpenService implements ICtmOpenInitService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Value("${isPremises}")
    boolean isPremises;

    @Override
    public OpenInitDTO init(BillContext billContext, Map<String, Object> map) throws Exception {
        try {
            Tenant tenant = (Tenant) map.get("tenant");
            log.info("进入fiOpenInit，租户：{}", tenant);
            log.info("ficmOpenInit 财资领域现金管理初始化业务");
            //租户开通取消初始化货币面额，平台数据量过大，会导致查询超时，后期客户使用，手工维护
//            initDenominationsetting(tenant);
            //去掉开通租户初始化自动关联规则，改成通过节点首次打开时初始化
//            initAutoCrooSetting(tenant);

            Object productLineCode =tenant.get("productLineCode");
            // 当前为旗舰版就进行更新
            if ("diwork".equalsIgnoreCase((String)productLineCode)) {
                updateMenubarName(tenant);
            }
        } catch (Exception e) {
            log.error("cmpOpenInit 执行失败", e);
            throw e;
        }
        return new OpenInitDTO() {{
            setHasWf(Boolean.TRUE);
            setDomain("CM");
        }};
    }

    /**
     * 自动关联规则
     *
     * @param tenant
     * @throws Exception
     */
    private void initAutoCrooSetting(Tenant tenant) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantid", tenant.getId());
        map.put("ytenantid", tenant.getYTenantId());
        long idDebit = ymsOidGenerator.nextId();
        long idCredit = ymsOidGenerator.nextId();
        map.put("idDebit", idDebit);
        map.put("idCredit", idCredit);
        map.put("pubts", new Date());
        Object obj = SqlHelper.selectFirst("com.yonyoucloud.fi.ficm.mapper.initficmMapper.getAutoCorrSetting", map);
        if (obj == null) {
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.initficmMapper.initAutoCorrSetting", map);
        }
    }

    /**
     * YB、专属化客户，侧边栏调整名称
     *
     * @throws Exception
     */
    private void updateMenubarName(Tenant tenant) throws Exception {
//        boolean flag = false;
//        List<String> ppms = new ArrayList<>();
//        ppms.add("ppm0000107693");
//        ppms.add("ppm0000107860");
//        ppms.add("ppm0000125100");
//        ppms.add("ppm0000107987");
//        ppms.add("ppm0000108255");
//        ppms.add("ppm0000104836");
//        ppms.add("ppm0000104850");
//        ppms.add("ppm0000105177");
//        ppms.add("ppm0000104849");
//        ppms.add("ppm0000072579");
//        ppms.add("ppm0000072505");
//
//        List<ControlPointDTO> controlPointDTOs = RemoteDubbo.get(ILicenseQueryService.class, IDomainConstant.MDD_WORKBENCH_SERVICE).getCurrentByCodes(InvocationInfoProxy.getTenantid(), ppms);
//        if (controlPointDTOs.size() > 0) {
//            for (ControlPointDTO controlPoint : controlPointDTOs) {
//                if (controlPoint.getStatus() == ControlPointStatus.VALID) {
//                    flag = true;
//                    break;
//                }
//            }
//        }
//        if (flag) {
        MenuBarVO menuBarVO = new MenuBarVO();
        menuBarVO.setTenantId(tenant.getYTenantId());
        menuBarVO.setMenuBarCode("CTM");
        menuBarVO.setMenuBarName(IMsgConstant.MENU_BAR_NAME);
        menuBarVO.setMenuBarNameExt2(IMsgConstant.MENU_BAR_NAMEEXT2);
        menuBarVO.setMenuBarNameExt3(IMsgConstant.MENU_BAR_NAMEEXT3);
        AppContext.getBean(IMenuBarService.class).updateNameByMenubarCode(menuBarVO);
        log.error("ficmOpenInit 财资领域侧边栏修改为全球司库");
//        }
    }

}
