package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.yonyou.iuap.ruleengine.service.pub.RuleCacheService;
import com.yonyou.ucf.mdd.ext.core.AppContext;

/**
 * @Description: 相关性规则缓存实现类
 * @Author: gengrong
 * @createTime: 2022/9/30
 * @version: 1.0
 */
public class CtmCmpRuleCacheServiceImpl implements RuleCacheService {
    @Override
    public boolean set(String key, String val) {
        return AppContext.cache().set(key,val);
    }

    @Override
    public String get(String key) {
        return AppContext.cache().get(key);
    }

    @Override
    public boolean expire(String key, int seconds) {
        AppContext.cache().expire(key,seconds);
        return true;
    }

    @Override
    public boolean delete(String key) {
        Long del = AppContext.cache().del(key);
        return true;
    }

    @Override
    public long delete(String... keys) {
        return AppContext.cache().del(keys);
    }
}
