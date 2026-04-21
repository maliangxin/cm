package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.autocorrsettings.BussDocumentType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.INSERT;

/**
 * 银行对账单生成同名账户划转银行转账,银行对账单生成同名账户划转缴存现金,银行对账单生成同名账户划转提取现金,银行对账单生成同名账户划转第三方转账
 * 保存后，需要依据关联的银行对账单\认领单，回写关联关系；
 */
@Component
@Slf4j
public class TaReWriteSaveAfterRule extends AbstractCommonRule {

    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;
    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        if (bizObject.getEntityStatus().name().equals(INSERT)) {
            //数据转换可关联实体
            List<CorrDataEntityParam> corrDataEntityParamList = this.convertCorrDataEntity(bizObject);
            log.info("关联的银行对账单:回写", CtmJSONObject.toJSONString(corrDataEntityParamList));
            for (CorrDataEntityParam corrDataEntityParam : corrDataEntityParamList) {
                CorrDataEntity corrDataEntity = new CorrDataEntity();
                corrDataEntity.setMainid(corrDataEntityParam.getMainid());
                corrDataEntity.setDcFlag(corrDataEntityParam.getDcFlag());
                corrDataEntity.setSmartcheckno(corrDataEntityParam.getSmartcheckno());
                reWriteBusCorrDataService.reWriteTransferAccountDataInfo(corrDataEntity);
            }
            //回写银行对账单
            ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntityParamList);
        }
        return new RuleExecuteResult();
    }

    /**
     * 数据转换可关联实体
     *
     * @param bizObject
     * @return
     */
    private List<CorrDataEntityParam> convertCorrDataEntity(BizObject bizObject) throws Exception {
        List<CorrDataEntityParam> corrDataList = new ArrayList<>();
        //事项类型:通过对账单生成的默认为"银行对账单"=16；认领单生成的默认为”认领单“=80
        //关联【付款】对账单
        if (StringUtils.isNotEmpty(bizObject.getString("paybankbill"))) {
            BankReconciliation bankReconciliation = this.findBankReconciliationById(bizObject.get("paybankbill"));
            CorrDataEntityParam entity = this.getBankCorrDataEntity(bizObject, bankReconciliation);
            corrDataList.add(entity);
        }
        //关联【收款】对账单
        if (StringUtils.isNotEmpty(bizObject.getString("collectbankbill"))) {
            BankReconciliation bankReconciliation = this.findBankReconciliationById(bizObject.get("collectbankbill"));
            CorrDataEntityParam entity = this.getBankCorrDataEntity(bizObject, bankReconciliation);
            corrDataList.add(entity);
        }

        //关联【付款】对账单
        if (StringUtils.isNotEmpty(bizObject.getString("paybillclaim"))) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bizObject.getString("paybillclaim"), 2);
            CorrDataEntityParam entity = this.getClaimCorrDataEntity(bizObject, billClaim);
            corrDataList.add(entity);
        }
        //关联【收款】对账单
        if (StringUtils.isNotEmpty(bizObject.getString("collectbillclaim"))) {
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bizObject.getString("collectbillclaim"), 2);
            CorrDataEntityParam entity = this.getClaimCorrDataEntity(bizObject, billClaim);
            corrDataList.add(entity);
        }

        return corrDataList;
    }

    /**
     * 银行对账单
     * 初始化-银行对账单关联实体关联实体数据
     *
     * @param bizObject          同名账号划转实体
     * @param bankReconciliation 银行对账单是
     */
    private CorrDataEntityParam getBankCorrDataEntity(BizObject bizObject, BankReconciliation bankReconciliation) throws Exception {
        CorrDataEntityParam entity = new CorrDataEntityParam();
        //财资统一对账码；默认为银行流水上存在的
        String paysmartcheckno = bankReconciliation.getSmartcheckno() == null
                ? RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate()
                : bankReconciliation.getSmartcheckno();

        //设置生单关联
        entity.setGenerate(true);
        //定时任务是true 未确认，手动生单是false 已确认
        entity.setAuto(false);
        entity.setApi(false);
        entity.setBusid(bizObject.getId());
        entity.setBillType(String.valueOf(BussDocumentType.transferaccount.getValue()));
        entity.setCode(bizObject.getString(ICmpConstant.CODE));
        entity.setBillNum(IBillNumConstant.TRANSFERACCOUNT);
        entity.setMainid(bizObject.getId());
        entity.setProject(bizObject.getString(ICmpConstant.PROJECT));
        entity.setVouchdate(bizObject.getDate(ICmpConstant.VOUCHDATE));
        entity.setAccentity(bizObject.getString(ICmpConstant.ACCENTITY));
        entity.setOriSum(bizObject.getBigDecimal(ICmpConstant.ORISUM));
        //借贷方向
        entity.setDcFlag(bankReconciliation.getDc_flag().getValue());
        entity.setBankReconciliationId(bankReconciliation.getId());
        //银行流水处理的pubts
        entity.setBankReconciliationPubts(DateUtils.dateFormat(bankReconciliation.getPubts(), DateUtils.DATE_TIME_PATTERN));
        //智能对账
        entity.setSmartcheckno(paysmartcheckno);
        //是否提前入账
        entity.setIsadvanceaccounts(Boolean.FALSE);
        return entity;
    }

    /**
     * 我的认领单
     * 初始化-银行对账单关联实体关联实体数据
     *
     * @param bizObject 认领单
     * @param billClaim 认领单
     */
    private CorrDataEntityParam getClaimCorrDataEntity(BizObject bizObject, BillClaim billClaim) throws Exception {
        CorrDataEntityParam entity = new CorrDataEntityParam();
        //设置生单关联
        entity.setGenerate(true);
        //定时任务是true 未确认，手动生单是false 已确认
        entity.setAuto(false);
        entity.setApi(false);
        entity.setBusid(bizObject.getId());
        entity.setBillType(String.valueOf(BussDocumentType.transferaccount.getValue()));
        entity.setCode(bizObject.getString(ICmpConstant.CODE));
        entity.setBillNum(IBillNumConstant.TRANSFERACCOUNT);
        entity.setMainid(bizObject.getId());
        entity.setProject(bizObject.getString(ICmpConstant.PROJECT));
        entity.setVouchdate(bizObject.getDate(ICmpConstant.VOUCHDATE));
        entity.setAccentity(bizObject.getString(ICmpConstant.ACCENTITY));
        entity.setOriSum(bizObject.getBigDecimal(ICmpConstant.ORISUM));

        entity.setDept(billClaim.getDept());
        //借贷方向
        entity.setDcFlag(billClaim.getDirection());
        entity.setBankReconciliationId(billClaim.items().get(0).getBankbill());
        entity.setBillClaimItemId(billClaim.getId());
        //智能对账
        BankReconciliation bankReconciliation = findBankReconciliationById(billClaim.items().get(0).getBankbill());
        if (StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())) {
            entity.setSmartcheckno(bankReconciliation.getSmartcheckno());
        } else {
            String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
            entity.setSmartcheckno(smartcheckno);
        }
        //银行流水处理的pubts
        entity.setBankReconciliationPubts(DateUtils.dateFormat(billClaim.getPubts(), DateUtils.DATE_TIME_PATTERN));
        //是否提前入账
        entity.setIsadvanceaccounts(Boolean.FALSE);
        return entity;
    }

    /**
     * 查询银行对账单，根据主键
     *
     * @param id
     * @return
     */
    private BankReconciliation findBankReconciliationById(Object id) throws Exception {
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id);
        if (null == bankReconciliation) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101502"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080053", "同名账户划转保存，未查询到银行流水单据") /* "同名账户划转保存，未查询到银行流水单据" */ /*  */);
        }
        return bankReconciliation;
    }
}
