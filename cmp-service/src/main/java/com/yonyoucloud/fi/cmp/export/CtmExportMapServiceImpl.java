package com.yonyoucloud.fi.cmp.export;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-25-19:18
 */
@Service
@Slf4j
public class CtmExportMapServiceImpl implements ICtmExportMapService {

    @Override
    public List<CmpExportMap> queryCtmExportMapList(String billnum, int type, boolean isOpen) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("billno", billnum);
        params.put("type", type);
        params.put("isOpen", isOpen);
        params.put("tenantId", AppContext.getTenantId());// 用于获取租户级别的自定义项对象翻译配置
        List<CmpExportMap> fields = null;
        try {
            fields = SqlHelper
                    .use("uimeta")
                    .selectList(
                            "com.yonyoucloud.fi.cmp.export.ExportMapper.getCmpexportMap",
                            params);
        } catch (Exception e) {
            log.error("获取CmpExportMap失败",e.getMessage());
        }

        if (fields != null && fields.size() > 0) {
            return fields;
        }
        return null;
    }
}
