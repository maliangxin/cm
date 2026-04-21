package com.yonyoucloud.fi.cmp.initdata.rule;

import org.imeta.orm.base.BizObject;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
public class InitDataSaveXJRule extends InitDataSaveRule {

    @Override
    public void initData(BizObject bizObject) {
//        if(1!=(short)bizObject.get("direction")){
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101201"),"余额方向不能为贷！");
//        }
        bizObject.set("moneyform",0);//现金存款
//        bizObject.set("direction",1);//借
//        bizObject.set("cobookoribalance", bizObject.get("coinitloribalance"));
//        bizObject.set("coinitlocalbalance", bizObject.get("coinitloribalance"));
//        bizObject.set("cobooklocalbalance", bizObject.get("coinitloribalance"));
    }

}
