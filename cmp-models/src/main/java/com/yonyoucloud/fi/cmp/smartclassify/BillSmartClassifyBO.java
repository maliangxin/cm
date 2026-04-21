package com.yonyoucloud.fi.cmp.smartclassify;


import lombok.Data;


/**
 * @description: 单据智能分类BO
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/18 14:54
 */
@Data
public class BillSmartClassifyBO {

    /**
     * 对方类型 枚举类OppositeType
     */
    private Short oppositetype;
    /**
     * 对方单位ID，参照ID
     */
    private String oppositeobjectid;
    /**
     * 对方单位名称，参展展示名称
     */
    private String oppositeobjectname;
    /**
     * 对方银行账号id
     */
    private String oppositebankacctid;

    public BillSmartClassifyBO(Short oppositetype, String oppositeobjectid, String oppositeobjectname, String oppositebankacctid) {
        this(oppositetype, oppositeobjectid, oppositeobjectname);
        // 对方银行账号的id
        this.setOppositebankacctid(oppositebankacctid);
    }

    public BillSmartClassifyBO(Short oppositetype, String oppositeobjectid, String oppositeobjectname) {
        // 对方类型
        this.setOppositetype(oppositetype);
        // 对方单位id
        this.setOppositeobjectid(oppositeobjectid);
        // 对方单位名称
        this.setOppositeobjectname(oppositeobjectname);
    }
}
