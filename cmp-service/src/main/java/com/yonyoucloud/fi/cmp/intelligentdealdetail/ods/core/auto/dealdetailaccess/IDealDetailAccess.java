package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSLifeCycle;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 流水接入接口
 * */
public interface IDealDetailAccess<T> extends ODSLifeCycle {

    /**
     * 生成流水接入模型,基于此模型流水后续入库
     * @param t
     * @return BankDealDetailModel 流水接入模型
     * */
    BankDealDetailModel getBankDealDetailModelList(T t) throws BankDealDetailException;
    /**
     * 实现ods去重逻辑
     * @param  bankDealDetailModel 流水接入模型
     * @return Map <p>
     *     key <p>
     *     com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst#NORMALSTREAM,<p>
     *     com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst#REPEATSTREAM<p/>
     * @see  contentSign(String content)
     * */
    Map<String,List<BankDealDetailODSModel>> checkRepeatFromODS(BankDealDetailModel bankDealDetailModel);
    /**
     * 基于流水内容生成流水签名
     * @param content 流水内容
     * @return String 流水签名
     * */
    String contentSign(String content);
    /**
     * 流水ods处理完成，通知消费者来消费流水，完成后续的关联、生单、发布认领中心等业务
     * @param bankDealDetailModel 流水接入模型
     * @param mapByRepeatStatus 流水分类<p>
     * 事务传播行为是not_support，不能抛出异常 否则影响ods入库
     * */
   //@Transactional(propagation = Propagation.NOT_SUPPORTED,rollbackFor = RuntimeException.class)
    void notifyConsumer( BankDealDetailModel bankDealDetailModel,Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus);
    /**
     * 流水模型入库
     * @param bankDealDetailModel 银企联流水入库
     * @param mapByRepeatStatus   手工流水入库
     * */
    void processDealDetailTOODS(BankDealDetailModel bankDealDetailModel,Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus);
}