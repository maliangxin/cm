package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.common.service.ArapBillServiceImpl;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDef;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBillCharacterDefb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 收付款单远程推送保存规则
 *
 * @author maliang
 * @version V1.0
 * @date 2021/4/15 15:19
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpReceiveBillSaveRule extends FiCmpReceiveBillBaseRule implements ISagaRule {

    /**
     * 收款单单据转换code，详见单据转换表配置
     */
    private static final String RECEIVEMAKEBILLCODE = "arapToCmpReceivebill";
    @Autowired
    PushAndPullService pushAndPullService;
    @Autowired
    private ArapBillServiceImpl arapBillServiceImpl;

    /**
     * 正流程，先删除老单据，再将新单据保存
     *
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103029"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2618E04A0000D", "在财务新架构环境下，不允许保存收款单。") /* "在财务新架构环境下，不允许保存收款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("==============================   executing  FiCmpReceiveBillSaveRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        //获取上游表单内容，判断单据数据是否为空
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills.size() == 0) {// 收付推送过来的单据为空，直接结束流程
            return new RuleExecuteResult();
        }

        //校验收款工作台有没有开通，没开通则抛异常提示
//        arapBillServiceImpl.checkService(IBillNumConstant.RECEIVE_BILL);
        Long billId = bills.get(0).getId();
        // 结算方式标识
        Boolean serviceFlag = arapBillServiceImpl.checkSettleMode(bills.get(0).get("settlemode"));
        ReceiveBill bill = getReceiveBill(billId);
        // 修改操作，先将原单据删除，再保存新单据
        if (bill != null) {
            log.debug("============  executing  FiCmpReceiveBillSaveRule  delete  ========== ");
            YtsContext.setYtsContext("oldbill", bill);
            CmpWriteBankaccUtils.delAccountBook(bill.getId().toString());
            MetaDaoHelper.delete(ReceiveBill.ENTITY_NAME, bill);
        }
        //查询lisence是否有效 true-有效
        boolean lisenceValidFlag = arapBillServiceImpl.checkLisenceValid();
        // 应付的单据类型的结算方式不是现金可处理单据或者lisence过期，则结束流程
        if (!serviceFlag || !lisenceValidFlag) {
            return new RuleExecuteResult();
        }
        boolean newArch = true;//新架构标识
        BizObject bizObject = null;
        if(newArch){
            // 新增操作调用生单规则，直接将数据插入
            PushAndPullModel pushAndPullModel = new PushAndPullModel();
            pushAndPullModel.setNeedDivide(false);//是否需要分单
            pushAndPullModel.setCode(RECEIVEMAKEBILLCODE);
            bizObject = pushAndPullService.transformBillByMakeBillCode(bills, pushAndPullModel);
            // 转换之后的单据进行保存前的校验
            arapBillServiceImpl.beforeSaveBillToCmp(bizObject);
        }else{
            PushAndPullVO vo = new PushAndPullVO();
            vo.setCode(RECEIVEMAKEBILLCODE);
            vo.setIsMainSelect(0);
            vo.setSourceData(bills);
            BillDataDto billDataDto = getTargetDataDto(vo);
            billDataDto.setBillnum(IBillNumConstant.RECEIVE_BILL);
            // 转换之后的单据进行保存前的校验
            bizObject = arapBillServiceImpl.beforeSaveBillToCmp(billDataDto);
            bizObject.set("creator", bills.get(0).get("creator"));
        }

        // 新增数据直接插入
        List<ReceiveBill_b> receiveBill_bs = bizObject.getBizObjects("ReceiveBill_b", ReceiveBill_b.class);
        if (receiveBill_bs != null && receiveBill_bs.size() > 0) {
            for (ReceiveBill_b receiveBill_b : receiveBill_bs) {
                ReceiveBillCharacterDefb receiveBillCharacterDefb = receiveBill_b.getBizObject("receiveBillCharacterDefb", ReceiveBillCharacterDefb.class);
                if (receiveBillCharacterDefb != null) {
                    receiveBillCharacterDefb.setId(receiveBill_b.getId());
                    receiveBillCharacterDefb.setEntityStatus(EntityStatus.Insert);
                    receiveBill_b.set("receiveBillCharacterDefb", receiveBillCharacterDefb);
                }
            }
        }
        ReceiveBillCharacterDef receiveBillCharacterDef = bizObject.getBizObject("receiveBillCharacterDef", ReceiveBillCharacterDef.class);
        if (receiveBillCharacterDef != null) {
            receiveBillCharacterDef.setId(bizObject.getId());
            receiveBillCharacterDef.setEntityStatus(EntityStatus.Insert);
            bizObject.set("receiveBillCharacterDef", receiveBillCharacterDef);
        }
        bizObject.set("creator", bills.get(0).get("creator"));
        // 新增数据直接插入
        bizObject.setEntityStatus(EntityStatus.Insert);
        CmpMetaDaoHelper.insert(ReceiveBill.ENTITY_NAME, bizObject);
        YtsContext.setYtsContext("newbillId", bizObject.getId());

        //保存后的单据进行记账处理
        BillContext newBillContext = new BillContext();
        newBillContext.setFullname(ReceiveBill.ENTITY_NAME);
        newBillContext.setBillnum(IBillNumConstant.RECEIVE_BILL);
        arapBillServiceImpl.afterSaveBillToCmp(newBillContext, bizObject);
        return new RuleExecuteResult();
    }

    /**
     * 回滚操作，先删除新单据，再保存旧单据。
     *
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("==============================   FiCmpReceiveBillSaveRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        // 若单据已保存成功，则删除新增单据
        Long newbillId = (Long) YtsContext.getYtsContext("newbillId");
        if (newbillId != null) {
            //删除日记账，更新账面余额
            CmpWriteBankaccUtils.delAccountBook(newbillId.toString());
            MetaDaoHelper.delete(ReceiveBill.ENTITY_NAME, newbillId);
        }
        // 如果存在旧单据，则将删除的旧单据恢复，执行保存逻辑，并将原code保存回原单据
        ReceiveBill oldbill = (ReceiveBill) YtsContext.getYtsContext("oldbill");
        if (oldbill == null) {
            return new RuleExecuteResult();
        }
        oldbill.setEntityStatus(EntityStatus.Insert);
        CmpMetaDaoHelper.insert(ReceiveBill.ENTITY_NAME, oldbill);

        //保存后的单据进行记账处理
        BillContext newBillContext = new BillContext();
        newBillContext.setFullname(ReceiveBill.ENTITY_NAME);
        newBillContext.setBillnum(IBillNumConstant.RECEIVE_BILL);
        arapBillServiceImpl.afterSaveBillToCmp(newBillContext, oldbill);
        return new RuleExecuteResult();
    }


}
