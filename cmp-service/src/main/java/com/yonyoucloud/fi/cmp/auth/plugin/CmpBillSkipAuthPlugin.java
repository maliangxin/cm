package com.yonyoucloud.fi.cmp.auth.plugin;

import com.yonyou.ucf.mdd.plugins.auth.AuthBillExtendPlugin;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * https://gfwiki.yyrd.com/pages/viewpage.action?pageId=42394435
 * @description: 设置跳过权限校验的billnum
 * @author: wanxbo@yonyou.com
 * @date: 2024/6/21 16:44
 */
@Component
@Slf4j
public class CmpBillSkipAuthPlugin implements AuthBillExtendPlugin {
    @Autowired
    private TransTypeQueryService transTypeQueryService;
    /**
     * 根据单据编码跳过全校校验
     * @return
     */
    @Override
    public Set<String> getIgnoreAuthBillNoSet() {
        Set<String> skipBillNumSet = new HashSet<>();
        skipBillNumSet.add("collection");
        skipBillNumSet.add("collectionMobileArchive");
        return skipBillNumSet;
    }

    @Override
    public String getCustomTransTypeAuthId(String transType, String authId) {
        log.error("传入的交易类型为:{}",transType);
        try {
            BdTransType bdTransType = transTypeQueryService.findById(transType);
            log.error("查询到交易类型为:{}",bdTransType);
            if(bdTransType.getIsPublish() == 1 && !StringUtils.isEmpty(bdTransType.getServiceCode()) && (bdTransType.getId() + "_" + authId).equals(bdTransType.getServiceCode())){
                return bdTransType.getServiceCode();
            }
        } catch (Exception e) {
            log.error("根据{}查询交易类型错误{}:",transType,e.getMessage(),e);
        }
        return null;
    }

    @Override
    public Set<String> getCommonCommandSet() {
        return null;
    }

    @Override
    public int order() {
        return 0;
    }
}
