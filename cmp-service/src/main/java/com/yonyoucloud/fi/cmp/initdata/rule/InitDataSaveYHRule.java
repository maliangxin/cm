package com.yonyoucloud.fi.cmp.initdata.rule;

import org.imeta.orm.base.BizObject;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
public class InitDataSaveYHRule extends InitDataSaveRule {

    @Override
    public void initData(BizObject bizObject) {
        bizObject.set("moneyform",1);//银行存款
//        bizObject.set("direction",2);//贷
//        bizObject.set("cobookoribalance", bizObject.get("coinitloribalance"));
//        bizObject.set("coinitlocalbalance", bizObject.get("coinitloribalance"));
//        bizObject.set("cobooklocalbalance", bizObject.get("coinitloribalance"));
    }

}
