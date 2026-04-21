package com.yonyoucloud.fi.cmp.journal;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
public interface JournalService {

    /**
     * 生成日记账
     *
     * @param journalList
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject generateJournal(List<Journal> journalList) throws Exception;

    /**
     * 更新日记账
     * @param bizObject
     * @throws Exception
     */
    void updateJournal(BizObject bizObject) throws Exception;

    /**
     * 第三方转账单更新日记账
     * @param bizObject
     * @throws Exception
     */
    void updateJournalThird(BizObject bizObject,boolean isAuthit) throws Exception;

    /**
     * 外币兑换更新日记账
     * @param bizObject
     * @throws Exception
     */
    void updateJournalForExchangeCurrency(BizObject bizObject) throws Exception;

    /**
     * 批量更新日记账
     * @param bizObjectList
     * @throws Exception
     */
    void updateJournal(List<BizObject> bizObjectList) throws Exception;


    /*
    根据单据查询并修改日记账登账日期
     */
    List<Journal>  updateJournalByBill(BizObject bizObject) throws Exception ;




    /*
     * @Author tongyd
     * @Description 检查日记账
     * @Date 2019/9/18
     * @Param [id]
     * @return java.lang.Boolean
     **/
    Boolean checkJournal(Long id) throws Exception;


    /**
     * 登账
     *
     * @throws Exception
     */
    void compute4Save(BizObject bizObject, Journal journal, Direction direction) throws Exception;

    /**
     * 是否匹配
     *
     * @throws Exception
     */

    Boolean matchJournal(Long id) throws Exception;

    /**
     * 校验日记账是否已勾兑或者回单匹配
     * @param id
     * @return
     * @throws Exception
     */
    Map<String, Boolean> isJournalCheckOrMatch(Long id) throws Exception;


    /**
     * 根据不同的对方类型给相关字段赋值登账 (对方类型、对方名称等)
     * @param journal
     * @throws Exception
     */
    void addOthertitle(Journal journal)throws Exception;

    /**
     * 对日记账vo的所属组织进行赋值
     * @param journal
     * @throws Exception
     */
    void setParentAccentityForJournal(Journal journal) throws Exception;


    /**
     * 更新日记账审批状态
     * @param srcbillitemid 单据明细id
     * @param auditStatus 审批状态
     * @throws Exception
     */
    void updateAuditStatusOfJournal(Object srcbillitemid, AuditStatus auditStatus) throws Exception;
}
