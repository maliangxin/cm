package com.yonyoucloud.fi.cmp.openjournal;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Date 2021/6/10 14:42
 * @Author shangxd  超时问题
 * @Description 登日记账公用工具类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JournalOpenCommonServiceImpl implements JournalOpenCommonService {

    private final SettlementService settlementService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;
    @Autowired
    JournalService journalService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JournalOpenCommonServiceImpl.class);

    /**
     * 1.校验数据 (a.校验必传参数；b.校验会计主体是否已经日结；c.单据日期是否大于登账日期
     * d.先删除后登账)2.更新期初余额数据 3.保存日记账数据
     * 支持批量
     * @param journalVo
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor=Exception.class)
    public void journalRegister(JournalVo journalVo) throws Exception {
        log.error("JournalOpenCommonServiceImpl.journalRegister param:{}", CtmJSONObject.toJSONString(journalVo));
        CtmJSONObject logData = new CtmJSONObject();
        logData.put("journalVo", CtmJSONObject.toJSONString(journalVo));
        ctmcmpBusinessLogService.saveBusinessLog(logData, "",IServicecodeConstant.BANKJOURNAL, IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, IMsgConstant.ADD);
        // 来源单据唯一标识
        String uniqueIdentification = journalVo.getUniqueIdentification();
        if (StringUtils.isEmpty(uniqueIdentification)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180746","来源单据唯一标识不能为空!") /* "来源单据唯一标识不能为空!" */);
        }
        // 请求参数校验
        List<Journal> journalList = journalVo.getJournalList();
        if (CollectionUtils.isEmpty(journalList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180744","请求参数不能为空!") /* "请求参数不能为空!" */);
        }
        String registerKey = journalVo.getUniqueIdentification() + AppContext.getTenantId();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(registerKey);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747","该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        try {
            // 校验数据---设计不考虑多个会计主体情况
            JournalVo journalVoQuery = checkJournalVo(journalList,"insert");
            List<Journal> journalOldList = getJournalsByItemBodyIdList(journalVoQuery);
            if (!CollectionUtils.isEmpty(journalOldList)) {
                if (!journalVo.isReinsert()) {
                    // 备份rollback使用
                    List<Journal> journalOldListBack = journalOldListBack(journalOldList);
                    YtsContext.setYtsContext(ICmpConstant.DELETE_JOURNAL_INSERT_DATA+uniqueIdentification, journalOldListBack);
                    // 删除日记账 更新期初
                    for (Journal journalOld : journalOldList) {
                        CmpWriteBankaccUtils.delAccountBookByJournal(journalOld);
                    }
                }
            }
            // insert日记账与更新期初余额数据
            updateBalance(journalList,"insert");
        } catch (Exception e) {
            log.error("journalRegister error:{}",e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100653"),e.getMessage(), e);
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * 银行账号获取处理
     * @param journalList
     * @param bankAccountNoMap
     * @param cashAccountNoMap
     */
    public void getBankAccountNoMap(List<Journal> journalList,Map<String,Map<String,String>> bankAccountNoMap,Map<String, String> cashAccountNoMap) throws Exception {
        List<String> bankAccountList = new ArrayList<>();
        List<String> cashAccountList = new ArrayList<>();
        for (Journal journaltemp:journalList){
            String bankaccount = StringUtils.isEmpty(journaltemp.getBankaccount()) ? "" : journaltemp.getBankaccount();
            String cashaccount = StringUtils.isEmpty(journaltemp.getCashaccount()) ? "" : journaltemp.getCashaccount();
            if(!StringUtils.isEmpty(bankaccount) && !bankAccountList.contains(bankaccount)){
                bankAccountList.add(bankaccount);
            }
            if(!StringUtils.isEmpty(cashaccount) && !bankAccountList.contains(cashaccount)){
                cashAccountList.add(cashaccount);
            }
        }
        if(CollectionUtils.isNotEmpty(bankAccountList)){
            bankAccountNoMap.putAll(getBankAccountNoById(bankAccountList));
        }
        if(CollectionUtils.isNotEmpty(cashAccountList)){
            cashAccountNoMap.putAll(getCashAccountNoById(cashAccountList));
        }
    }

    /**
     * 校验参数
     * @param journalList 入参日记账集合
     * @param type insert 插入；other 其他
     * @return JournalVo 日记账实体
     */
    private JournalVo checkJournalVo(List<Journal> journalList,String type) throws Exception {
        // 校验数据---设计不考虑多个会计主体情况
        List<String> srcbillItemnoList = new ArrayList<>();
        Map<String,Journal> journalMap = new HashMap<>();
        Map<String,Map<String,String>> bankAccountNoMap = new HashMap<>();
        Map<String, String> cashAccountNoMap = new HashMap<>();
        if("insert".equals(type)){
            getBankAccountNoMap( journalList, bankAccountNoMap, cashAccountNoMap);
        }
        JournalVo journalQuery = new JournalVo();
        // 会计主体，启用日期
        Map<String, Date> enabledBeginDataMap = new HashMap<>();
        for (Journal journal : journalList) {
            if ("insert".equals(type)) {
                checkJournal(journal, bankAccountNoMap, cashAccountNoMap);
                journal.setCreateDate(new Date());
                journal.setCreateTime(new Date());
                if (enabledBeginDataMap.isEmpty() || !enabledBeginDataMap.containsKey(journal.getAccentity())) {
                    enabledBeginDataMap.put(journal.getAccentity(), QueryBaseDocUtils.queryOrgPeriodBeginDate(journal.getAccentity()));
                }
                // 校验过模块启用之后，不再再次进行校验
                Date enabledBeginData = enabledBeginDataMap.get(journal.getAccentity());
                if (enabledBeginData == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101870"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4","该资金组织现金管理模块未启用，不能保存单据！"));
                }
                if (enabledBeginData.compareTo(journal.getVouchdate()) > 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100655"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180481","单据日期早于模块启用日期，不能保存单据！"));
                }

            } else if ("other".equals(type)) {
                checkJournalCommon(journal);
            }
            // 查看老逻辑使用这个字段存储的id
            srcbillItemnoList.add(journal.getSrcbillitemno());
            journalMap.put(journal.getSrcbillitemno(), journal);
            if (null == journalQuery.getTempAccentity()) {
                journalQuery.setTempAccentity(journal.getAccentity());
            }
            if (objectIsNull(journalQuery.getTempBilltype(), "tempBilltype")) {
                journalQuery.setTempBilltype(journal.getBilltype().getValue());
            }
        }
        journalQuery.setSrcbillitemnoList(srcbillItemnoList);
        journalQuery.setJournalMap(journalMap);
        return journalQuery;
    }


    /**
     * 插入日记账数据更新实时余额
     * @param journalList 日记账接口入参
     * @param type insert 新增，other 其他
     * @throws Exception
     */
    private void updateBalance(List<Journal> journalList, String type) throws Exception {
        Map<String, Boolean> dateCheck = new HashMap<>();
        for (Journal journal : journalList) {
            journal.setId(ymsOidGenerator.nextId());
            journal.setEntityStatus(EntityStatus.Insert);
            // 解决MDD框架报事项来源错误添加
            journal.setSrcitem(journal.getSrcitem());
            journal.setTopsrcitem(journal.getTopsrcitem());
            journal.setTopbilltype(journal.getTopbilltype());
            journal.setDztime(journal.getDztime());
            // 校验会计主体是否日结
            if ("insert".equals(type)) {
                // 已经结算成功
                if (journal.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                    if (null == journal.getDzdate()) {
                        throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180745","已结算单据登账日期不能为空！") /* "已结算单据登账日期不能为空！" */);
                    }
                    if (dateCheck.get(journal.getAccentity()+journal.getDzdate()) == null) {
                        settlementService.checkSettleForAcctParentAccentity(journal.getAccentity(),journal.getBankaccount(),journal.getDzdate(),EntityStatus.Update);
                        dateCheck.put(journal.getAccentity() + journal.getDzdate(), true);
                    }
                } else {
                    if (dateCheck.get(journal.getAccentity() + journal.getDzdate()) == null) {
                        // 登账日期为空时，根据单据日期校验是否日结
                        settlementService.checkSettleForAcctParentAccentity(journal.getAccentity(),journal.getBankaccount(),journal.getVouchdate(),EntityStatus.Update);
                        dateCheck.put(journal.getAccentity()+journal.getDzdate(),true);
                    }
                }
            }else { // 结算支付 或 取消支付 的时候走这里
                settlementService.checkSettleForAcctParentAccentity(journal.getAccentity(), journal.getBankaccount(), journal.getDzdate(), EntityStatus.Update);
            }
            // 设置对方名称
            journalService.addOthertitle(journal);
            // 给所属组织赋值
            journalService.setParentAccentityForJournal(journal);
        }
        // 插入日记账
        CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, journalList);
        // 处理期初数据
        for (Journal journal : journalList) {
            cmpWriteBankaccUtils.addAccountBookSTWB(journal);
        }
    }

    /**
     * 1.回滚新插入数据日记账与期初余额 2.有老数据恢复老数据
     * @param journalVo
     * @throws Exception
     */
    @Override
    public void rollbackJournalRegister(JournalVo journalVo) throws Exception {
        log.error("JournalOpenCommonServiceImpl.rollbackJournalRegister param:{}", CtmJSONObject.toJSONString(journalVo));
        List<Journal> journalList = journalVo.getJournalList();
        if (!CollectionUtils.isEmpty(journalList)) {
            JournalVo journalVoQuery = checkJournalVo(journalList, "other");
            // 查询新插入数据日记账
            List<Journal> journalNewList = getJournalsByItemBodyIdList(journalVoQuery);
            // 回滚删除日记账数据与期初余额
            if (!CollectionUtils.isEmpty(journalNewList)) {
                for (Journal journalNew : journalNewList) {
                    CmpWriteBankaccUtils.delAccountBookByJournal(journalNew);
                }
            }
        }
        // 有老数据恢复老数据
        recovery(journalVo, ICmpConstant.DELETE_JOURNAL_INSERT_DATA + journalVo.getUniqueIdentification());
    }

    /**
     * 回滚旧数据使用
     *
     * @param journalVo
     * @throws Exception
     */
    private void recovery(JournalVo journalVo, String ytsBackups) throws Exception {
        List<Journal> journalOldList = new ArrayList<>();
        if (!Objects.isNull(YtsContext.getYtsContext(ytsBackups))) {
            journalOldList = (List<Journal>) YtsContext.getYtsContext(ytsBackups);
        }
        if (!CollectionUtils.isEmpty(journalOldList)) {
            recoveryJournal(journalOldList);
            CtmJSONObject logData = new CtmJSONObject();
            logData.put("recovery", CtmJSONObject.toJSONString(journalOldList));
            ctmcmpBusinessLogService.saveBusinessLog(logData, ytsBackups,IServicecodeConstant.BANKJOURNAL, IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, IMsgConstant.BANK_JOURNAL);
        }
    }

    /**
     * 恢复删除的日记账数据
     * @param journalOldList yts上下文中存储的日记账数据
     * @throws Exception
     */
    private void recoveryJournal(List<Journal> journalOldList) throws Exception {
        for (Journal journal : journalOldList) {
            journal.setEntityStatus(EntityStatus.Insert);
            // 设置对方名称
            journalService.addOthertitle(journal);
            // 给所属组织赋值
            journalService.setParentAccentityForJournal(journal);
            // 登帐
            cmpWriteBankaccUtils.addAccountBookSTWB(journal);
            // 兼容类型
            journal.setTopsrcitem(journal.getTopsrcitem());
            journal.setTopbilltype(journal.getTopbilltype());
        }
        // 插入日记账
        CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, journalOldList);
    }


    /**
     * 根据结算单明细id 查询日记账信息
     * journalVo.srcbillitemnoList  外部信息id
     * journalVo.journal  日记账信息 --accentity 会计主体.ID  srcbillitemno 来源单据行号 billtype 事项类型  三项必传
     *
     * @return
     * @throws Exception
     */
    private List<Journal> getJournalsByItemBodyIdList(JournalVo journalVo) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
        QueryConditionGroup conditionGroup = new QueryConditionGroup();
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(journalVo.getTempAccentity())));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.SRC_BILL_ITEM_NO).in(journalVo.getSrcbillitemnoList())));
        conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.BILLTYPE).eq(journalVo.getTempBilltype())));
        querySchema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(Journal.ENTITY_NAME, querySchema, null);
    }

    /**
     * 校验入参数据格式是否正确
     *
     * @param journal
     */
    private void checkJournal(Journal journal, Map<String, Map<String, String>> bankAccountNoMap, Map<String, String> cashAccountNoMap) {
        stringIsNull(journal.getAccentity(), IBussinessConstant.ACCENTITY);//会计主体.ID
        stringIsNull(journal.getCurrency(), IBussinessConstant.CURRENCY);//币种.ID
        stringIsNull(journal.getSrcbillitemno(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180752","来源单据行号") /* "来源单据行号" */);//来源单据行号
        stringIsNull(journal.getSrcbillitemid(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180753","来源单据行id") /* "来源单据行id" */);//来源单据行id
        stringIsNull(journal.getBillno(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180754","来源单据billnum") /* "来源单据billnum" */);//来源单据billnum -- 报表使用
        stringIsNull(journal.getSrcbillno(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180755","来源单据号") /* "来源单据号" */);//来源单据billnum -- 报表使用
        stringIsNull(journal.getBillnum(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180756","单据编号") /* "单据编号" */);//来源单据billnum
        stringIsNull(journal.getServicecode(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180757","来源单据服务编码") /* "来源单据服务编码" */);//来源单据服务编码 -- 报表使用
        stringIsNull(journal.getTradetype(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180758","交易类型") /* "交易类型" */);//获取交易类型
        stringIsNull(journal.getNatCurrency(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418075A","本币币种") /* "本币币种" */);//本币币种
        objectIsNull(journal.getBilltype(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418075C","事项类型") /* "事项类型" */);//事项类型
        objectIsNull(journal.getSrcitem(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418075F","事项来源") /* "事项来源" */);
        objectIsNull(journal.getPaymentstatus(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180763","网银支付状态") /* "网银支付状态" */);
        objectIsNull(journal.getSettlestatus(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180764","结算状态") /* "结算状态" */);
        objectIsNull(journal.getAuditstatus(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180767","审批状态") /* "审批状态" */);
        objectIsNull(journal.getDirection(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180768","方向") /* "方向" */);
        String bankaccount = journal.getBankaccount();//银行账户.ID
        String cashaccount = journal.getCashaccount();//现金账户.ID
        boolean accountFlag = (StringUtils.isEmpty(bankaccount) && StringUtils.isEmpty(cashaccount));
        if (accountFlag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100656"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180743","银行账户或现金账户必须有一个不为空！") /* "银行账户或现金账户必须有一个不为空！" */);
        }
        Object exchangerate = journal.get("exchangerate");//汇率
        Object vouchdate = journal.get("vouchdate");//单据日期
        if (objectIsNull(exchangerate, "exchangerate")) {
            if (!(journal.get("exchangerate") instanceof BigDecimal)) {
                journal.setExchangerate(new BigDecimal(exchangerate.toString()));
            }
        }
        if (objectIsNull(vouchdate, "vouchdate")) {
            if (!(journal.get("vouchdate") instanceof Date)) {
                journal.setVouchdate(DateUtils.strToDate(vouchdate.toString()));
            }
        }
        Date dzdate = journal.getDzdate();
        if (null != dzdate && dzdate.compareTo(journal.getVouchdate()) < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100657"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074A","单据日期不能大于登账日期！") /* "单据日期不能大于登账日期！" */);
        }
        //有待校验--- 校验没问题后下面代码可以删除
        //解决MDD框架报事项来源错误添加
        if (objectIsNull(journal.getSrcitem(), "srcitem")) {
            if (!(journal.getSrcitem() instanceof EventSource)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100658"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074D","事项来源字段类型错误") /* "事项来源字段类型错误" */);
            } else {
                journal.setSrcitem(journal.getSrcitem());
            }
        }
//        if (objectIsNull(journal.getTopsrcitem(), "topsrcitem")) {
//            journal.setTopsrcitem(journal.getTopsrcitem());
//        }
        if (StringUtils.isEmpty(journal.getCreator())) {
            journal.set("creator", AppContext.getCurrentUser().getId());
        }
        if (null == journal.getBookkeeper()) {
            journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        }
        if (!StringUtils.isEmpty(bankaccount)) {
            Map accountMap = bankAccountNoMap.get(bankaccount);
            if (accountMap != null && !accountMap.isEmpty()) {
                if (StringUtils.isEmpty(journal.getBankaccountno())) {
                    journal.setBankaccountno(accountMap.get("account").toString());
                }
                //20241108-企业银行账户基础档案 新增开户类型其他金融机构 此类型没有银行类别
                if (StringUtils.isEmpty(journal.getBanktype())) {
                    journal.setBanktype(accountMap.get("bank")!=null?accountMap.get("bank").toString():null);
                }
            }
        }
        if (!StringUtils.isEmpty(cashaccount)) {
            String cashAccountNo = cashAccountNoMap.get(cashaccount);
            if (!StringUtils.isEmpty(cashAccountNo)) {
                journal.setCashaccountno(cashAccountNo);
            }
        }
    }

    private void stringIsNull(String value, String tips) {
        if (StringUtils.isEmpty(value)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100660"),tips + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180742",":字段不能为空!") /* ":字段不能为空!" */);
        }
    }

    private boolean objectIsNull(Object value, String tips) {
        if (Objects.isNull(value)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100660"),tips + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180742",":字段不能为空!") /* ":字段不能为空!" */);
        }
        return true;
    }

    private Map<String, String> getCashAccountNoById(List<String> cashAccountList) throws Exception {
        EnterpriseParams params = new EnterpriseParams();
        params.setIdList(cashAccountList);
        List<EnterpriseCashVO> enterpriseCashVOs = baseRefRpcService.queryEnterpriseCashAcctByCondition(params);
        if (CollectionUtils.isNotEmpty(enterpriseCashVOs)) {
            Map<String, String> accountNoMap = new HashMap<>();
            enterpriseCashVOs.stream().forEach(e -> {
                if (e.getCode() != null) {
                    accountNoMap.put(e.getId(), e.getCode());
                }
            });
            return accountNoMap;
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074B","根据传入的现金账号id，找不到对应的现金账号信息！") /* "根据传入的现金账号id，找不到对应的现金账号信息！" */);
        }
    }

    /**
     * 查询银行账号信息
     *
     * @author wangshbv
     * @date 10:37
     */
    private Map<String, Map<String, String>> getBankAccountNoById(List<String> bankaccountList) throws Exception {
        EnterpriseParams params = new EnterpriseParams();
        params.setIdList(bankaccountList);
        List<EnterpriseBankAcctVO> dataList = baseRefRpcService.queryEnterpriseBankAcctByCondition(params);
        if (CollectionUtils.isNotEmpty(dataList)) {
            Map<String, Map<String, String>> accountNoMap = new HashMap<>();
            dataList.stream().forEach(e -> {
                Map<String, String> accountMap = new HashMap<>();
                if (e.getAccount() != null) {
                    accountMap.put("account", e.getAccount().toString());
                }
                if (e.getBank() != null) {
                    accountMap.put("bank", e.getBank().toString());
                }
                if (accountMap != null && !accountMap.isEmpty()) {
                    accountNoMap.put(e.getId().toString(), accountMap);
                }
            });
            return accountNoMap;
        } else {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418075E","根据传入的银行账号id，找不到对应的银行账号信息！") /* "根据传入的银行账号id，找不到对应的银行账号信息！" */);
        }
    }

    /**
     * 1.审批状态（1.已审批 2.未审批） 结算状态 （1.未结算 2.已结算   为2已结算时候 dzdate有值   为1未结算时候dzdate清空）
     *
     * @param journalVo --accentity 会计主体.ID  List<String> srcbillitemnoList 来源单据行号 billtype 事项类型 settlestatus 结算状态 auditstatus 审批状态
     *                  五项必传 dzDate登账日期
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void journalUpdate(JournalVo journalVo) throws Exception {
        log.error("JournalOpenCommonServiceImpl.journalUpdate param:{}", CtmJSONObject.toJSONString(journalVo));
        CtmJSONObject logData = new CtmJSONObject();
        logData.put("journalVo", CtmJSONObject.toJSONString(journalVo));
        ctmcmpBusinessLogService.saveBusinessLog(logData, "",IServicecodeConstant.BANKJOURNAL, IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, IMsgConstant.UPDATE);
        String uniqueIdentification = journalVo.getUniqueIdentification();//来源单据唯一标识
        if (StringUtils.isEmpty(uniqueIdentification)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180746","来源单据唯一标识不能为空!") /* "来源单据唯一标识不能为空!" */);
        }
        String registerKey = journalVo.getUniqueIdentification() + AppContext.getTenantId();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(registerKey);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747","该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        try {
            List<Journal> journalList = journalVo.getJournalList();
            if (!CollectionUtils.isEmpty(journalList)) {
                JournalVo journalVoQuery = checkJournalVo(journalList, "other");
                //查询日记账表
                List<Journal> journalOldList = getJournalsByItemBodyIdList(journalVoQuery);
                if (!CollectionUtils.isEmpty(journalOldList)) {
                    //备份rollback使用
                    List<Journal> journalOldListBack = journalOldListBack(journalOldList);
                    YtsContext.setYtsContext(ICmpConstant.JOURNAL_UPDATE_OLD_DATA + uniqueIdentification, journalOldListBack);
                    updateJournal(journalOldList, journalVoQuery.getJournalMap());
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100661"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074E","未查询到登账记录!") /* "未查询到登账记录!" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180744","请求参数不能为空!") /* "请求参数不能为空!" */);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100662"),e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    private List<Journal> journalOldListBack(List<Journal> journalOldList) {
        List<Journal> journalOldListBack = new ArrayList<>();
        for (Journal journal : journalOldList) {
            Journal t = new Journal();
            BeanUtils.copyProperties(journal, t);
            journalOldListBack.add(t);
        }
        return journalOldListBack;
    }

    /**
     * 此方法与回滚逻辑公用
     *
     * @param journalOldList
     * @param journalMap
     * @throws Exception
     */
    private void updateJournal(List<Journal> journalOldList, Map<String, Journal> journalMap) throws Exception {
        if (!CollectionUtils.isEmpty(journalOldList)) {
            for (Journal journal : journalOldList) {
                //获取接口传参数据
                Journal journalNew = journalMap.get(journal.getSrcbillitemno());
                updateStatus(journal, journalNew.getAuditstatus(), journalNew.getSettlestatus(), journalNew);
            }
            EntityTool.setUpdateStatus(journalOldList);
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalOldList);
        }
    }

    /**
     * 更新日记账状态
     *
     * @param journalold   数据库中数据
     * @param auditstatus  接口参数
     * @param settlestatus 接口参数
     * @param journalNew   接口参数
     */
    private void updateStatus(Journal journalold, AuditStatus auditstatus, SettleStatus settlestatus, Journal journalNew) throws Exception {
        if (Objects.isNull(auditstatus)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100663"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180748","审批状态不能为空！") /* "审批状态不能为空！" */);
        }
        if (Objects.isNull(settlestatus)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100664"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180749","结算状态不能为空！") /* "结算状态不能为空！" */);
        }
        Date dzdate = null;
        Date dztime = null;
        if(journalNew.getDzdate() != null){
            dzdate = DateUtils.dateTimeToDate(journalNew.getDzdate());
            if (null != dzdate && dzdate.compareTo(DateUtils.dateTimeToDate(journalold.getVouchdate())) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100657"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074A","单据日期不能大于登账日期！") /* "单据日期不能大于登账日期！" */);
            }
            dztime = journalNew.getDztime();
        }
        //校验会计主体是否日结
        if (journalold.getSettlestatus().getValue() == 2) {
            settlementService.checkSettleForAcctParentAccentity(journalold.getAccentity(),journalold.getBankaccount(),journalold.getVouchdate(),EntityStatus.Update);
        }
        if (SettleStatus.alreadySettled == settlestatus) {
            if (!Objects.isNull(dzdate)) {
                journalold.setDzdate(dzdate);
                journalold.setDztime(dztime);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100665"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074F","登账日期不能为空！") /* "登账日期不能为空！" */);
            }
            settlementService.checkSettleForAcctParentAccentity(journalold.getAccentity(),journalold.getBankaccount(),journalold.getDzdate(),EntityStatus.Update);
            isBlend(journalold);
        } else if (SettleStatus.noSettlement == settlestatus) {
            if (SettleStatus.alreadySettled == journalold.getSettlestatus()) {
                isBlend(journalold);
            }
            journalold.setDzdate(null);
            journalold.setDztime(null);
        }
        journalold.setAuditstatus(auditstatus);
        journalold.setSettlestatus(settlestatus);
        //20241130 新增凭证唯一标识码
        journalold.setVoucheronlyno(journalNew.getVoucheronlyno());
        if(journalNew.getCreditoriSum() != null){
            journalold.setCreditoriSum(journalNew.getCreditoriSum());
        }
        if(journalNew.getCreditnatSum() != null){
            journalold.setCreditnatSum(journalNew.getCreditnatSum());
        }

        if (journalNew.getDebitoriSum() != null) {
            journalold.setDebitoriSum(journalNew.getDebitoriSum());
        }
        if (journalNew.getDebitnatSum() != null) {
            journalold.setDebitnatSum(journalNew.getDebitnatSum());
        }
    }

    /**
     * 公共校验
     *
     * @param journal
     */
    private void checkJournalCommon(Journal journal) {
        String accentity = journal.getAccentity();
        if (StringUtils.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100666"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418075B","会计主体不能为空！") /* "会计主体不能为空！" */);
        }
        EventType billtype = journal.getBilltype();
        if (Objects.isNull(billtype)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100667"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180761","事项类型不能为空！") /* "事项类型不能为空！" */);
        }
        stringIsNull(journal.getSrcbillitemno(), "srcbillitemno");//来源单据行号
//        objectIsNull(journal.getSrcitem());
    }

    /**
     * 更新日记账回滚
     *
     * @param journalVo
     * @throws Exception
     */
    @Override
    public void rollbackJournalUpdate(JournalVo journalVo) throws Exception {
        log.error("JournalOpenCommonServiceImpl.rollbackJournalUpdate param:{}", CtmJSONObject.toJSONString(journalVo));
        List<Journal> journalOldList = new ArrayList<>();
        if (!Objects.isNull(YtsContext.getYtsContext(ICmpConstant.JOURNAL_UPDATE_OLD_DATA + journalVo.getUniqueIdentification()))) {
            journalOldList = (List<Journal>) YtsContext.getYtsContext(ICmpConstant.JOURNAL_UPDATE_OLD_DATA + journalVo.getUniqueIdentification());
        }
        if (!CollectionUtils.isEmpty(journalOldList)) {
            JournalVo journalVoQuery = checkJournalVo(journalOldList, "other");
            List<Journal> journalNewList = getJournalsByItemBodyIdList(journalVoQuery);
            if (!CollectionUtils.isEmpty(journalNewList)) {
                Map<String, Journal> journalMap = journalVoQuery.getJournalMap();
                for (Journal journal : journalNewList) {
                    Journal journalNew = journalMap.get(journal.getSrcbillitemno());
                    journal.setAuditstatus(journalNew.getAuditstatus());
                    journal.setSettlestatus(journalNew.getSettlestatus());
                    journal.setDzdate(journalNew.getDzdate());
                    journal.setCreditoriSum(journalNew.getCreditoriSum());
                    journal.setCreditnatSum(journalNew.getCreditnatSum());
                }
                EntityTool.setUpdateStatus(journalNewList);
                MetaDaoHelper.update(Journal.ENTITY_NAME, journalNewList);
            }
            CtmJSONObject logData = new CtmJSONObject();
            logData.put("rollbackJournalUpdate", CtmJSONObject.toJSONString(journalOldList));
            ctmcmpBusinessLogService.saveBusinessLog(logData, ICmpConstant.JOURNAL_UPDATE_OLD_DATA + journalVo.getUniqueIdentification(),IServicecodeConstant.BANKJOURNAL, IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, IMsgConstant.ROLLBACK);
        }
    }

    /**
     * 删除日记账
     *
     * @param journalVo
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void journalDelete(JournalVo journalVo) throws CtmException {
        log.error("JournalOpenCommonServiceImpl.journalDelete true:", journalVo.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put("journalVo", CtmJSONObject.toJSONString(journalVo));
        ctmcmpBusinessLogService.saveBusinessLog(logData, "",IServicecodeConstant.BANKJOURNAL, IServicecodeConstant.BANKJOURNAL, IMsgConstant.BANK_JOURNAL, IMsgConstant.DELETE);
        String uniqueIdentification = journalVo.getUniqueIdentification();//来源单据唯一标识
        if (StringUtils.isEmpty(uniqueIdentification)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180746","来源单据唯一标识不能为空!") /* "来源单据唯一标识不能为空!" */);
        }
        String registerKey = journalVo.getUniqueIdentification() + AppContext.getTenantId();
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(registerKey);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100652"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747","该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        try {
            List<Journal> journalList = journalVo.getJournalList();
            journalVo.setOldJournalList(null);
            if (!CollectionUtils.isEmpty(journalList)) {
                JournalVo journalVoQuery = checkJournalVo(journalList, "other");
                //查询日记账表
                List<Journal> journalOldList = getJournalsByItemBodyIdList(journalVoQuery);
                if (!CollectionUtils.isEmpty(journalOldList)) {
                    //备份rollback使用
                    List<Journal> journalOldListBack = journalOldListBack(journalOldList);
                    YtsContext.setYtsContext(ICmpConstant.JOURNAL_DELETE_OLD_DATA + uniqueIdentification, journalOldListBack);
                    for (Journal journalNew : journalOldList) {
                        //校验会计主体是否日结
                        settlementService.checkSettleForAcctParentAccentity(journalNew.getAccentity(),journalNew.getBankaccount(),journalNew.getVouchdate(),EntityStatus.Delete);
                        //检测是否勾对
                        isBlend(journalNew);
                        //删除日记账 更新期初
                        CmpWriteBankaccUtils.delAccountBookByJournal(journalNew);
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100651"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180744","请求参数不能为空!") /* "请求参数不能为空!" */);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    /**
     * 删除日记账回滚
     *
     * @param journalVo
     * @throws Exception
     */
    @Override
    public void rollbackJournalDelete(JournalVo journalVo) throws Exception {
        log.error("JournalOpenCommonServiceImpl.rollbackJournalDelete param:{}", CtmJSONObject.toJSONString(journalVo));
        recovery(journalVo, ICmpConstant.JOURNAL_DELETE_OLD_DATA + journalVo.getUniqueIdentification());
    }

    //1.会计主体是否日结
    //2.回单是否勾兑 a).对账勾兑 b).回单勾兑
    //3.单据日期是否大于登账日期

    /**
     * 校验会计主体是否日结
     * 修改点：传入当前会计主体的最大日结日期即可 避免循环查询
     * @return
     */
    private Boolean isSettlement(Date maxSettleDate, Date dzDate) throws Exception {
        //最大日结日期
//        Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
        if (SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, dzDate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100668"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180766","会计主体已日结,当前日结日期：") /* "会计主体已日结,当前日结日期：" */ + DateUtils.dateFormat(dzDate, null) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180765",",不能进行相关操作！") /* ",不能进行相关操作！" */);
        }
        return true;
    }

    /**
     * 是否勾兑
     *
     * @return
     */
    private Boolean isBlend(Journal journalNew) throws Exception {
        String id = journalNew.getSrcbillitemno();
        if(journalNew.getSrcbillitemno()==null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100669"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074C","来源单据行号不能为空！") /* "来源单据行号不能为空！" */);
        }
        if(journalNew.getCheckflag()!=null && journalNew.getCheckflag()){
            throw new CtmException(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180750","该单据已勾对，不能取消结算！") /* "该单据已勾对，不能取消结算！" */);
        }
        if(journalNew.getCheckmatch()!=null && journalNew.getCheckmatch()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100670"),id + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180759","单据已经关联匹配银行交易回单，不能取消结算") /* "单据已经关联匹配银行交易回单，不能取消结算" */);
        }
//        if (!checkJournal(id)) {
//        }
//        if (!matchJournal(id)) {
//        }

        return false;
    }


    /**
     * 校验日记账与对账单是否已勾兑
     *
     * @param id
     * @return
     * @throws Exception
     */
    public Boolean checkJournal(Long id) throws Exception {
        if (null == id) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100669"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074C","来源单据行号不能为空！") /* "来源单据行号不能为空！" */);
        }
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("1");
        querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemno").eq(id.toString()), QueryCondition.name("checkflag").eq(1)));
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJ);
        if (journalList.size() > 0) {
            throw new CtmException(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180750","该单据已勾对，不能取消结算！") /* "该单据已勾对，不能取消结算！" */);
        }
        return false;
    }

    /**
     * 校验日记账与回单是否已匹配
     *
     * @param id
     * @return
     * @throws Exception
     */
    public Boolean matchJournal(Long id) throws Exception {
        if (null == id) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100669"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418074C","来源单据行号不能为空！") /* "来源单据行号不能为空！" */);
        }
        QuerySchema querySchemaJ = QuerySchema.create().addSelect("1");
        querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("srcbillitemno").eq(id.toString()), QueryCondition.name("checkmatch").eq(1)));
        List<Journal> journalList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJ);
        if (journalList.size() > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100670"),id + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180759","单据已经关联匹配银行交易回单，不能取消结算") /* "单据已经关联匹配银行交易回单，不能取消结算" */);
        }
        return false;
    }


}
