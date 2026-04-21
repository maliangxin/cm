package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.google.common.collect.Lists;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoCheckConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankBillSmartCheckService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.reconciliate.vo.ReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.egl.voucher.api.v1.IVoucherBankRpcService;
import com.yonyoucloud.fi.egl.voucher.dto.cash.CashVoucherCheckInfoDTO;
import com.yonyoucloud.fi.egl.voucher.dto.cash.CheckInfoDTO;
import com.yonyoucloud.fi.egl.voucher.dto.cash.ResultDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.ConditionExpression;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @description: 智能对账，自动对账任务具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2022/9/29 15:58
 */

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankBillSmartCheckServiceImpl implements BankBillSmartCheckService {

    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Resource
    private BankAutoCheckConfigService bankAutoCheckConfigService;

    @Autowired
    private CmpCheckService cmpCheckService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    /**
     * 自动对账调度任务，具体实现入口
     * @param param 调度任务入参
     * @return
     */
    @Override
    public boolean smartCheck(CtmJSONObject param) throws Exception{
        //查询已启用的对账方案
        List<BankReconciliationSetting> settingList = new ArrayList<>();
        try {
            //查询符合要求的对账方案设置
            settingList = this.getEnableSettings(param);

            //20260115新增多要素匹配模式，调度任务参数解析提前
            BankAutoCheckConfig bankAutoCheckConfig = new BankAutoCheckConfig();
            //1=按财资统一对账码相同勾兑,2=按关键要素勾兑,不够选默认按财资统一对账码相同勾兑;多选时，优先按照财资统一对账码相同勾兑，其次按照关键要素勾兑；
            String[] automaticblendingrules = !StringUtils.isEmpty(param.getString("automaticblendingrules"))?  param.getString("automaticblendingrules").split(","): null;
            //包含2，代表需要多要素匹配
            if(automaticblendingrules!=null && Arrays.asList(automaticblendingrules).contains("2")) {
                bankAutoCheckConfig.setKeyElementMatchFlag((short)1);
            }else {
                bankAutoCheckConfig.setKeyElementMatchFlag((short)0);
            }
            //关键要素-日期浮动天数 以银行对账单交易日期为基准，查询前后浮动日期范围的数据进行匹配
            String floatingdays =StringUtils.isNotEmpty(param.getString("floatingdays"))?param.getString("floatingdays") : null;
            if (floatingdays !=null){
                bankAutoCheckConfig.setChangedays(Integer.valueOf(floatingdays));
            }
            //1=相同,2=模糊匹配;当自动勾选规则选中按关键要素勾兑时，此参数生效;相同：银行交易流水的“摘要”与银行日记账或凭证的“摘要”字段完全相等；
            String[] checkabstract = !StringUtils.isEmpty(param.getString("checkabstract"))?  param.getString("checkabstract").split(","): null;
            if (checkabstract!=null && checkabstract.length>0){
                bankAutoCheckConfig.setRemarkmatch(Short.valueOf(checkabstract[0]));
            }
            //票据号相同
            if (StringUtils.isNotEmpty(param.getString("notenoMatchMethod"))){
                String notenoMatchMethod = BOOLEAN_TYPE_MAP.getOrDefault(param.getString("notenoMatchMethod"),"0");
                bankAutoCheckConfig.setNotenoMatchMethod(Short.valueOf(notenoMatchMethod));
            }
            //对方名称相同
            if (StringUtils.isNotEmpty(param.getString("othernameMatchMethod"))){
                String othernameMatchMethod = BOOLEAN_TYPE_MAP.getOrDefault(param.getString("othernameMatchMethod"),"0");
                bankAutoCheckConfig.setOthernameMatchMethod(Short.valueOf(othernameMatchMethod));
            }
            //相同数据匹配方式
            if (StringUtils.isNotEmpty(param.getString("samedataMatchMethod"))){
                String samedataMatchMethod = SAME_DATA_MATCH_METHOD_MAP.getOrDefault(param.getString("samedataMatchMethod"),"3");
                bankAutoCheckConfig.setSamedataMatchMethod(Short.valueOf(samedataMatchMethod));
            }
            //票据号相同时，调度任务查询出来的银行日记账数，对应的是noteno
            bankAutoCheckConfig.put("isTaskParam",true);
            //增加多维度匹配
            this.executeSmartCheckAutomaticRules(settingList,param,bankAutoCheckConfig);
        } catch (Exception e) {
            log.error("自动对账异常" + e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102469"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080105", "自动对账异常") /* "自动对账异常" */ + e.getMessage());
        }

        return true;
    }

    // 定义静态常量 Map；调度任务展示中文，数据库需要对应的数字
    private static final Map<String, String> BOOLEAN_TYPE_MAP = new HashMap<String, String>() {{
        put("是", "1");//@notranslate
        put("否", "0");//@notranslate
    }};
    //相同数据匹配方式，调度任务中文参数转枚举
    private static final Map<String, String> SAME_DATA_MATCH_METHOD_MAP = new HashMap<String, String>() {{
        put("不匹配", "0");//@notranslate
        put("最近日期匹配", "1");//@notranslate
        put("最远日期匹配", "2");//@notranslate
        put("随机匹配", "3");//@notranslate
    }};


    /**
     * 执行自动对账流程(调度任务增加匹配规则)
     */
    private void executeSmartCheckAutomaticRules(List<BankReconciliationSetting> settingList,CtmJSONObject param,BankAutoCheckConfig bankAutoCheckConfig) throws Exception{
        String startDateStr = !StringUtils.isEmpty(param.getString("startdate"))? param.getString("startdate") : null;
        String endDateStr = !StringUtils.isEmpty(param.getString("enddate")) ? param.getString("enddate") : DateUtils.formatDate(new Date());
        //CZFW-524408 客户环境只能传递Date类型
        Date startDate = !StringUtils.isEmpty(param.getString("startdate"))? param.getDate("startdate") : null;
        Date endDate = !StringUtils.isEmpty(param.getString("enddate")) ? param.getDate("enddate") : new Date();

        for (BankReconciliationSetting setting : settingList) {
            //根据对账方案去查询未勾兑的银行对账单
            List<BankReconciliation> bankReconciliations = getBankReconciliationList(setting,startDate,endDate);
            BankreconciliationUtils.checkAndFilterData(bankReconciliations, BankreconciliationScheduleEnum.BANKRECONCILIATIONAUTOMATICTASK);

            //存放日记账或者凭证
            List<Journal> journals = new ArrayList<>();
            //银行日记账对账
            if (setting.getReconciliationdatasource().getValue() == ReconciliationDataSource.BankJournal.getValue()) {
                //根据对账方案查询未勾兑的日记账
                journals = getJournalList(setting,startDate,endDate);
            }
            //凭证日记账数据
            if (setting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()) {
                List<LinkedHashMap>  journalMaps = getVocherJournalList(setting,startDateStr,endDateStr);
                for (LinkedHashMap j:journalMaps){
                    Journal journal = new Journal();
                    journal.init(j);
                    if (j.get("bankverifycode") != null){
                        journal.setBankcheckno(j.get("bankverifycode").toString());
                    }
                    journals.add(journal);
                }
            }

            //处理自动勾兑
            try {
                CtmJSONObject checkResult = handleAutoCheckAutomaticRules(bankReconciliations, journals,setting.getId(),Integer.valueOf(setting.getReconciliationdatasource().getValue() + ""),bankAutoCheckConfig);
                CtmJSONObject logParams = new CtmJSONObject();
                logParams.put("bankReconciliationList",bankReconciliations);
                logParams.put("journalList",journals);
                logParams.put("checkResult",checkResult);
                ctmcmpBusinessLogService.saveBusinessLog(logParams, "", "银企对账后台自动对账", IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, "银企对账自动对账调度任务执行");
            } catch (Exception e) {
                log.error("勾兑处理异常" + e);
            }
        }
    }
    /**
     * 处理勾兑逻辑(调度任务增加规则)
     */
    @Override
    public CtmJSONObject handleAutoCheckAutomaticRules(List<BankReconciliation> bankReconciliationList, List<Journal> journalList,Long settingId,Integer reconciliationdatasource,BankAutoCheckConfig bankAutoCheckConfig) throws Exception {
        CtmJSONObject checkResult = new CtmJSONObject();
        //存放勾兑完的数据
        List<Journal> journals = new ArrayList<>();
        List<BankReconciliation> banks = new ArrayList<>();

        if (CollectionUtils.isEmpty(bankReconciliationList) || CollectionUtils.isEmpty(journalList)){
            //凭证勾对笔数
            checkResult.put("vouchCheckedNum",new BigDecimal(journals.size()));
            //银行流水勾对笔数
            checkResult.put("bankCheckedNum",new BigDecimal(banks.size()));
            //存在未勾对的数据，则状态是未对符
            if (journalList.size() >0 || bankReconciliationList.size() >0){
                checkResult.put("reconciliationStatus",0);
            }else {
                //reconciliationStatus
                checkResult.put("reconciliationStatus",1);
            }
            log.error("自动对账存在为空的对账单或者日记账数据，跳过执行");
            return checkResult;
        }

        //20260130 增加勾对关系记录
        List<ReconciliationInfoVO> reconciliationInfoVOList = new ArrayList<>();
       // 对账码对账,财资统一对账码一定优先匹配
        //step1: 202408 优先根据财资统一对账码对账(sourceType=1)
        List<ReconciliationInfoVO> step1List = handleBySmartCheckNo(1,journals,banks,bankReconciliationList,journalList,settingId,reconciliationdatasource);
        reconciliationInfoVOList.addAll(step1List);
        //step2: 202405 再根据银行对账编码进行对账(sourceType=2)
        List<ReconciliationInfoVO> step2List = handleBySmartCheckNo(2,journals,banks,bankReconciliationList,journalList,settingId,reconciliationdatasource);
        reconciliationInfoVOList.addAll(step2List);
        //step3: 20260115 适配自动对账设置按关键要素匹配
        if(bankAutoCheckConfig.getKeyElementMatchFlag() == (short)1) {
            List<ReconciliationInfoVO> step3List = handleByAutomaticRulesCheckFactor(journals, banks, bankReconciliationList, journalList, settingId, reconciliationdatasource, bankAutoCheckConfig);
            reconciliationInfoVOList.addAll(step3List);
        }

        // 没有可以勾兑的数据
        if (journals.size() == 0){
            //凭证勾对笔数
            checkResult.put("vouchCheckedNum",new BigDecimal(0));
            //银行流水勾对笔数
            checkResult.put("bankCheckedNum",new BigDecimal(banks.size()));
            //存在未勾对的数据，则状态是未对符
            if (bankReconciliationList.size() >0){
                checkResult.put("reconciliationStatus",0);
            }else {
                //reconciliationStatus
                checkResult.put("reconciliationStatus",1);
            }
            return checkResult;
        }
        //处理数据勾对
        this.handleJournalAndBankCheck(journals,banks,reconciliationdatasource);
        //20260130需求：记录勾对关系
        this.saveReconciliationMathRecord(reconciliationInfoVOList);
        //凭证勾对时，处理银行回单和凭证关联关系发送事件
        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
            this.handleBankReceiptCorrEvent(journals,banks);
        }

        //统计勾对笔数
        //凭证勾对笔数
        checkResult.put("vouchCheckedNum",new BigDecimal(journals.size()));
        //银行流水勾对笔数
        checkResult.put("bankCheckedNum",new BigDecimal(banks.size()));
        //存在未勾对的数据，则状态是未对符
        if (journalList.size() >0 || bankReconciliationList.size() >0){
            checkResult.put("reconciliationStatus",0);
        }else {
            checkResult.put("reconciliationStatus",1);
        }

        return checkResult;
    }

    /**
     * 根据财资统一对账码（凭证/日记账为银行对账编码字段；银行对账单为smartcheckno字段）
     *
     * @param journals 存储勾兑成功的日记账
     * @param banks 存储勾兑成功的银行对账单
     * @param bankReconciliationList 未勾兑的银行对账单集合
     * @param journalList 未勾兑的日记账集合
     * @param settingId 银行对账设置ID
     * @param reconciliationdatasource 数据来源 1凭证；2日记账
     * @param sourceType 勾对类型：1财资统一对账码；2银行对账编码
     */
    @Override
    public List<ReconciliationInfoVO> handleBySmartCheckNo(Integer sourceType, List<Journal> journals, List<BankReconciliation> banks, List<BankReconciliation> bankReconciliationList,
                                      List<Journal> journalList,Long settingId,Integer reconciliationdatasource){
        //202408 财资统一对账码适配，同一个对账码，存在多条凭证/日记账能和多条对账单数据进行勾对
        //凭证和日记账的财资统一对账码为之前的银行对账编码
        Map<String, List<Journal>> journalMap = new HashMap<>();
        //银行对账单的财资统一对账码为smartcheckno
        Map<String, List<BankReconciliation>> bankReconciliationMap = new HashMap<>();

        //将有财资统一对账码（银行对账单编码）的银行日记账数据单独拿出来
        Iterator<Journal> journalIterator = journalList.iterator();
        while(journalIterator.hasNext()){
            Journal journal = journalIterator.next();
            if(journal.getCheckflag()){ //已勾兑数据跳过
                journalIterator.remove();
                continue;
            }
            if (!StringUtils.isEmpty(journal.getBankcheckno()) ){
                if (journalMap.containsKey(journal.getBankcheckno())){
                    journalMap.get(journal.getBankcheckno()).add(journal);
                }else {
                    List<Journal> jList = new ArrayList<>();
                    jList.add(journal);
                    journalMap.put(journal.getBankcheckno(),jList);
                }
                journalIterator.remove();
            }
        }
        //将有财资统一对账码（smartcheckno）的银行对账单数据单独拿出来
        Iterator<BankReconciliation> bankIterator = bankReconciliationList.iterator();
        while (bankIterator.hasNext()){
            BankReconciliation bank = bankIterator.next();
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                if(bank.getOther_checkflag()){//已勾兑的数据跳过
                    bankIterator.remove();
                    continue;
                }
            }else{
                if(bank.getCheckflag()){ //已勾兑的数据跳过
                    bankIterator.remove();
                    continue;
                }
            }
            String smartcheckno;
            if(sourceType == 1){ //根据财资统一对账码勾对
                smartcheckno = bank.getSmartcheckno();
            }else { //根据银行对账编码勾对
                smartcheckno = bank.getBankcheckno();
            }
            if (!StringUtils.isEmpty(smartcheckno)){
                if (bankReconciliationMap.containsKey(smartcheckno)){
                    bankReconciliationMap.get(smartcheckno).add(bank);
                }else {
                    List<BankReconciliation> bList = new ArrayList<>();
                    bList.add(bank);
                    bankReconciliationMap.put(smartcheckno,bList);
                }
                bankIterator.remove();
            }
        }

        //用来存放哪些对账码没有进行遍历，需要将对应的银行对账单放到未匹配集合中
        Set<String> bankCheckNoSet = new HashSet<>();
        //统计勾对成功的关系记录
        List<ReconciliationInfoVO> reconciliationInfoVOList = new ArrayList<>();

        //202408 财资统一对账码适配，同一个对账码，存在多条凭证/日记账能和多条对账单数据进行勾对
        for (Map.Entry<String, List<Journal>> entry : journalMap.entrySet()) {
            String bankCheckNo = entry.getKey();
            bankCheckNoSet.add(bankCheckNo);
            List<Journal> journalCheckList = entry.getValue();
            if (bankReconciliationMap.containsKey(bankCheckNo)){
                List<BankReconciliation> bankReconciliationCheckList = bankReconciliationMap.get(bankCheckNo);
                //日记账净额
                BigDecimal journalAmount = BigDecimal.ZERO;
                //对账单净额
                BigDecimal bankAmount = BigDecimal.ZERO;
                //统计银行日记账/凭证净额
                for (Journal j : journalCheckList){
                    if (j.getDirection().equals(Direction.Debit)){ //借
                        journalAmount = journalAmount.add(j.getDebitoriSum());
                    } else {
                        journalAmount = journalAmount.subtract(j.getCreditoriSum());
                    }
                }
                //统计银行对账单净额
                for (BankReconciliation b : bankReconciliationCheckList){
                    if (b.getDc_flag().equals(Direction.Debit)){ //借
                        if (b.getDebitamount() != null) {
                            bankAmount = bankAmount.subtract(b.getDebitamount());
                        }
                    } else {
                        if (b.getCreditamount() != null) {
                            bankAmount = bankAmount.add(b.getCreditamount());
                        }
                    }
                }
                //在同一组财资对账码下判断净额是否一致，一致则满足可以勾对
                //需要净额统计相同
                if (journalAmount.compareTo(bankAmount) != 0) {
                    //将有对账编号，但是没有匹配上的数据，再放回原列表中，继续匹配
                    journalList.addAll(journalCheckList);
                    bankReconciliationList.addAll(bankReconciliationCheckList);
                    continue;
                }
                //勾对日期
                Date date = BillInfoUtils.getBusinessDate();//业务日期
                Date checkdate = date == null? new Date() : date;
                //勾对号;20260130需求调整为 AR + 勾对业务日期 + 19位OID
                String checkno = "AR" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();
                for (Journal checkedJournal : journalCheckList){
                    Journal journalNew = new Journal();
                    journalNew.setId(checkedJournal.getId());
                    journalNew.setSrcbillitemid(checkedJournal.getSrcbillitemid());
                    journalNew.setTradetype(checkedJournal.getTradetype());
                    journalNew.setBankreconciliationsettingid(settingId.toString());
                    journalNew.setDefine1(checkedJournal.get("ts"));
                    packBill(null, journalNew, checkno,checkdate,true, queryOperator(), SettleStatus.alreadySettled, reconciliationdatasource,settingId.toString());
                    journals.add(journalNew);
                }
                for (BankReconciliation checkedBank : bankReconciliationCheckList){
                    packBill(checkedBank, null, checkno,checkdate,true, queryOperator(), SettleStatus.alreadySettled, reconciliationdatasource,settingId.toString());
                    banks.add(checkedBank);
                }
                //组装勾对关系记录表信息
                ReconciliationInfoVO reconciliationInfoVO = this.handleReconciliationInfoVO(journalCheckList,bankReconciliationCheckList,
                        ReconciliationBasisType.SmartCheckNoMatching.getValue(),null,settingId.toString(),reconciliationdatasource,checkno,checkdate);
                reconciliationInfoVOList.add(reconciliationInfoVO);
            }else {
                journalList.addAll(journalCheckList);
            }
        }

        //对账单未处理的数据放到未匹配集合中
        for(Map.Entry<String, List<BankReconciliation>> entry : bankReconciliationMap.entrySet()){
            String bankCheckNo = entry.getKey();
            if (!bankCheckNoSet.contains(bankCheckNo)) {
                bankReconciliationList.addAll(entry.getValue());
            }
        }

        return reconciliationInfoVOList;
    }

    @Override
    public ReconciliationInfoVO handleReconciliationInfoVO(List<Journal> journalList,List<BankReconciliation> bankList,Short reconciliationBasisType, BankAutoCheckConfig bankAutoCheckConfig, String bankreconciliationscheme, Integer reconciliationdatasource, String checkno, Date checkDate) {
        ReconciliationInfoVO reconciliationInfoVO = new ReconciliationInfoVO();
        if (bankList != null && bankList.size() > 0){
            reconciliationInfoVO.setAccentity(bankList.get(0).getAccentity());
            reconciliationInfoVO.setBankaccount(bankList.get(0).getBankaccount());
            reconciliationInfoVO.setCurrency(bankList.get(0).getCurrency());
        }else if (journalList != null && journalList.size() > 0){
            reconciliationInfoVO.setAccentity(journalList.get(0).getAccentity());
            reconciliationInfoVO.setBankaccount(journalList.get(0).getBankaccount());
            reconciliationInfoVO.setCurrency(journalList.get(0).getCurrency());
    }
        reconciliationInfoVO.setReconciliationScheme(bankreconciliationscheme);
        reconciliationInfoVO.setReconciliationDataSource(Short.valueOf(String.valueOf(reconciliationdatasource)));
        reconciliationInfoVO.setCheckno(checkno);
        reconciliationInfoVO.setCheckOperator(queryOperator());
        reconciliationInfoVO.setCheckDate(checkDate);
        //勾对时间赋值
        Date checkTime = null;
        try {
            checkTime = DateUtils.parseDate(DateUtils.dateFormat(checkDate,"yyyy-MM-dd")+ DateUtils.dateFormat(new Date()," HH:mm:ss"),"yyyy-MM-dd HH:mm:ss");
        }catch (Exception e){
            log.error("银企对账勾对赋值 packBill 日期转换异常");
        }
        reconciliationInfoVO.setCheckTime(checkTime);
        reconciliationInfoVO.setReconciliationBasis(reconciliationBasisType);
        reconciliationInfoVO.setBankAutoCheckConfig(bankAutoCheckConfig);
        if (journalList != null && journalList.size() > 0){
            reconciliationInfoVO.setJournalList(journalList);
        }
        if (bankList != null && bankList.size() > 0){
            reconciliationInfoVO.setBankReconciliationList(bankList);
        }
        return reconciliationInfoVO;
    }


    //多维度匹配
    @Override
    public List<ReconciliationInfoVO> handleByAutomaticRulesCheckFactor(List<Journal> journals,List<BankReconciliation> banks,List<BankReconciliation> bankReconciliationList,
                                              List<Journal> journalList,Long settingId,Integer reconciliationdatasource,BankAutoCheckConfig bankAutoCheckConfig) throws  Exception{
        //存储全部的记录勾对关系
        List<ReconciliationInfoVO> reconciliationInfoVOList = new ArrayList<>();
        Iterator<Journal> journalIterator = journalList.iterator();
        while (journalIterator.hasNext()){
            Journal journal = journalIterator.next();
            if(journal.getCheckflag() != null){
                //已勾兑数据跳过
                if(journal.getCheckflag()){
                    journalIterator.remove();
                    continue;
                }
            }
            Direction direction = journal.getDirection();
            BigDecimal jourAmount = BigDecimal.ZERO;
            //CZFW-114883 借方金额和贷方金额都为空的数据 过滤掉
            if (journal.getDebitoriSum() == null && journal.getCreditoriSum() == null){
                    journalIterator.remove();
                    continue;
            }
            if (Direction.Debit.equals(direction)) {
                jourAmount = journal.getDebitoriSum();
            }else{
                jourAmount = journal.getCreditoriSum();
            }
            Iterator<BankReconciliation> bankIterator = bankReconciliationList.iterator();
            //20260130 增加相同数据匹配方式，用来记录匹配到的银行流水数据
            List<BankReconciliation> matchedBankList = new ArrayList<>();
            while(bankIterator.hasNext()){
                    BankReconciliation bank = bankIterator.next();
                    if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                        if(bank.getOther_checkflag()){
                            bankIterator.remove();
                            continue;
                             }
                    }else{
                        if(bank.getCheckflag()){
                            bankIterator.remove();
                            continue;
                        }
                    }

                    //币种
                    if (!String.valueOf(journal.getCurrency()).equals(String.valueOf(bank.getCurrency()))){
                        continue;
                    }
                    //银行账户
                    if (!String.valueOf(journal.getBankaccount()).equals(String.valueOf(bank.getBankaccount()))){
                        continue;
                    }
                    //20250530 日记账对账增加授权使用组织相同的校验
                    if  (ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource){
                        if (journal.getAccentity() == null || journal.getAccentity() == null || !journal.getAccentity().equals(bank.getAccentity()) ){
                            continue;
                        }
                    }
                    //方向需要相反
                    Direction bankDirection = bank.getDc_flag();
                    if (bankDirection!=null&&bankDirection.equals(direction)){
                        continue;
                    }
                    //金额
                    BigDecimal bankAmount = BigDecimal.ZERO;
                    if (bankDirection!=null&&bankDirection.equals(Direction.Debit)){
                        bankAmount = bank.getDebitamount();
                    }else{
                        bankAmount = bank.getCreditamount();
                    }
                    if(jourAmount == null || bankAmount == null || jourAmount.compareTo(bankAmount) != 0){
                        continue;
                    }

                    //关键要素-日期浮动天数 以银行对账单交易日期为基准，查询前后浮动日期范围的数据进行匹配
                    Integer floatingdays = bankAutoCheckConfig != null? bankAutoCheckConfig.getChangedays() : null;
                    //自动对账添加浮动日期
                    if (floatingdays !=null){
                        //CZFW-402712 银企对账浮动日期比较条件时银行日记账调整为按登账日期（dzdate）比较
                        int dateBetween = 0;
                        SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
                        if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                            Date journalDate = dateFormatWithoutTime.parse(DateUtils.dateFormatWithoutTime(journal.get("vouchdate").toString()));
                            dateBetween = DateUtils.dateBetween(bank.getDzdate(),journalDate);
                        }else {
                            dateBetween = DateUtils.dateBetween(bank.getDzdate(),journal.getDzdate());
                        }
                        //日期不在浮动日期内，则匹配不成功
                        if (!(Math.abs(dateBetween) < floatingdays || Math.abs(dateBetween) == floatingdays)){
                            continue;
                        }
                    }

                    // 银行日记账凭证的“摘要”字段包含在交易流水的“摘要”字段里
                    //1=相同,2=模糊匹配;当自动勾选规则选中按关键要素勾兑时，此参数生效;相同：银行交易流水的“摘要”与银行日记账或凭证的“摘要”字段完全相等；
                    // 模糊匹配：银行交易流水的“摘要”为基准，银行日记账凭证的“摘要”字段包含在交易流水的“摘要”字段里
                    Short remarkmatch = bankAutoCheckConfig != null? bankAutoCheckConfig.getRemarkmatch(): null;
                    if (remarkmatch != null){
                        //全匹配
                         if(MatchType.Same.getValue() == remarkmatch.intValue()){
                            if((bank.getRemark() == null && journal.getDescription() != null) ||
                                    (bank.getRemark() != null && journal.getDescription() == null) ){
                                continue;
                            }
                            if (bank.getRemark() != null && !bank.getRemark().equals(journal.getDescription())){
                                continue;
                            }
                        }

                        //模糊匹配
                    if(MatchType.PartMatch.getValue() == remarkmatch.intValue()){
                            if(bank.getRemark() == null || journal.getDescription() == null ){
                                continue;
                            }
                            if (bank.getRemark() != null && journal.getDescription() != null) {
                                if (!bank.getRemark().contains(journal.getDescription()) && !journal.getDescription().contains(bank.getRemark())) {
                                continue;
                            }
                        }
                    }
                }

                //票据号相同校验
                Short notenoMatchMethod = bankAutoCheckConfig != null? bankAutoCheckConfig.getNotenoMatchMethod(): null;
                if (notenoMatchMethod !=null && notenoMatchMethod.intValue() == 1){
                    //勾选[票据号相同]条件后，自动对账时， 在原对账的逻辑基础上，增加票据号相同的逻辑，
                    //流水中的票据号(noteno)字段与凭证中的票据号（billno)/日记账中的票据号（noteno）必须相同，才勾对成功，不同则不勾对
                    //票据号全都为空时，当做不满足条件
                    String bankNoteno = bank.getNoteno();
                    //UI模板中，已经将银行日记账和凭证的票据号字段都统一为billno，不需要再做区分(调度任务需要做处理)。
                    String journalNoteno = journal.getBillno();
                    if (bankAutoCheckConfig.get("isTaskParam") != null && ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource){
                        journalNoteno = journal.getNoteno();
                    }
                    // 票据号必须都非空且相等才能继续匹配
                    if (StringUtils.isEmpty(bankNoteno) || StringUtils.isEmpty(journalNoteno) || !bankNoteno.equals(journalNoteno)) {
                        continue;
                    }
                }

                //在银行日记账数据源下，判断对方名称是否相同。银行日记账对方类型=客户，供应商，员工，内部单位时，判断和流水中的对方单位id是否一致
                //对方类型=其他或者资金组织时，判断对方名称和银行流水的对方户名是否一致
                Short othernameMatchMethod = bankAutoCheckConfig != null? bankAutoCheckConfig.getOthernameMatchMethod(): null;
                if (ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource && othernameMatchMethod !=null && othernameMatchMethod.intValue() == 1){
                    CaObject caObject = journal.getCaobject();
                    if (caObject == null){
                        continue;
                    }
                    String journalOthername =null;
                    if (CaObject.Customer.getValue() == caObject.getValue() && journal.getCustomer() != null){
                        journalOthername = journal.getCustomer().toString();
                    }
                    if (CaObject.Supplier.getValue() == caObject.getValue() && journal.getSupplier() != null){
                        journalOthername = journal.getSupplier().toString();
                    }
                    if (CaObject.Employee.getValue() == caObject.getValue()){
                        journalOthername = journal.getEmployee();
                    }
                    if (CaObject.InnerUnit.getValue() == caObject.getValue()){
                        journalOthername = journal.getInnerunit();
                    }
                    if (CaObject.Other.getValue() == caObject.getValue() || CaObject.CapBizObj.getValue() == caObject.getValue()){
                        journalOthername = journal.getOthertitle();
                    }
                    String bankOthername;
                    if (CaObject.Customer.getValue() == caObject.getValue() || CaObject.Supplier.getValue() == caObject.getValue() || CaObject.Employee.getValue() == caObject.getValue() || CaObject.InnerUnit.getValue() == caObject.getValue()){
                        bankOthername = bank.getOppositeobjectid();
                    }else {
                        bankOthername = bank.getTo_acct_name();
                    }
                    if (StringUtils.isEmpty(journalOthername) || !journalOthername.equals(bankOthername)){
                        continue;
                    }
                }
                //20260130 增加相同数据匹配方式
                matchedBankList.add(bank);
            }
            if (matchedBankList.size() > 0) {
                BankReconciliation bankReconciliation = new BankReconciliation();//记录具体匹配到哪个数据
                if (matchedBankList.size() == 1){ //只匹配到一个，则直接设置勾对状态
                    bankReconciliation = matchedBankList.get(0);
                }else { //匹配到多个，则根据相同数据匹配方式进行匹配
                    Short samedataMatchMethod = bankAutoCheckConfig != null? bankAutoCheckConfig.getSamedataMatchMethod(): null;
                    //未设置相同数据匹配方式，或者设置随机匹配，则默认取第一个
                    if (samedataMatchMethod == null || samedataMatchMethod == SamedataMatchMethodType.RandomMatching.getValue()){
                        bankReconciliation = matchedBankList.get(0);
                    }
                    //不匹配直接返回
                    if (samedataMatchMethod != null && SamedataMatchMethodType.NoMatching.getValue() == samedataMatchMethod){
                        continue;
                    }
                    Date journalDate;
                    SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
                    if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                        journalDate = dateFormatWithoutTime.parse(DateUtils.dateFormatWithoutTime(journal.get("vouchdate").toString()));
                    }else {
                        journalDate = journal.getDzdate();
                    }
                    // 计算每个 BankReconciliation 与 journalDate 的日期差
                    Map<BankReconciliation, Integer> dateDifferences = new HashMap<>();
                    for (BankReconciliation bank : matchedBankList) {
                        int dateDiff = DateUtils.dateBetween(bank.getDzdate(), journalDate);
                        dateDifferences.put(bank, Math.abs(dateDiff));
                    }
                    // 找出最大和最小的日期差
                    Optional<Map.Entry<BankReconciliation, Integer>> minEntry = dateDifferences.entrySet().stream()
                            .min(Map.Entry.comparingByValue());
                    Optional<Map.Entry<BankReconciliation, Integer>> maxEntry = dateDifferences.entrySet().stream()
                            .max(Map.Entry.comparingByValue());
                    // 如果最大值和最小值相同，说明所有数据日期差都一样
                    // 只有在 FarthestDateMatching 或 NearestDateMatching 时才执行以下逻辑
                    if (samedataMatchMethod != null &&
                            (SamedataMatchMethodType.FarthestDateMatching.getValue() == samedataMatchMethod ||
                                    SamedataMatchMethodType.NearestDateMatching.getValue() == samedataMatchMethod)) {
                        if (minEntry.isPresent() && maxEntry.isPresent() &&
                                minEntry.get().getValue().equals(maxEntry.get().getValue())) {
                            log.info("所有匹配的银行流水数据与日记账日期差相同，均为{}天", minEntry.get().getValue());
                            continue;
                        }
                    }
                    if (samedataMatchMethod!=null && SamedataMatchMethodType.NearestDateMatching.getValue() == samedataMatchMethod){
                        if (minEntry.isPresent()){
                            // 检查是否存在多个相同的最小日期差
                            long minCount = dateDifferences.values().stream()
                                    .filter(diff -> diff.equals(minEntry.get().getValue()))
                                    .count();
                            if (minCount > 1) {
                                // 存在多个相同的最小日期差，中断循环
                                log.info("存在多个相同最小日期差的银行流水数据，中断匹配");
                                continue;
                            }
                            bankReconciliation =  minEntry.get().getKey();
                        }
                    }
                    if (samedataMatchMethod!=null && SamedataMatchMethodType.FarthestDateMatching.getValue() == samedataMatchMethod){
                        if (maxEntry.isPresent()){
                            // 检查是否存在多个相同的最大日期差
                            long maxCount = dateDifferences.values().stream()
                                    .filter(diff -> diff.equals(maxEntry.get().getValue()))
                                    .count();
                            if (maxCount > 1) {
                                // 存在多个相同的最大日期差，中断循环
                                log.info("存在多个相同最大日期差的银行流水数据，中断匹配");
                                continue;
                            }
                            bankReconciliation = maxEntry.get().getKey();
                        }
                    }
                }
                Iterator<BankReconciliation> newBankIterator = bankReconciliationList.iterator();
                while(newBankIterator.hasNext()){
                    BankReconciliation bank = newBankIterator.next();
                    if (bankReconciliation.getId() == null || !bank.getId().toString().equals(bankReconciliation.getId().toString())){
                        continue;
                    }
                    //勾对日期
                    Date date = BillInfoUtils.getBusinessDate();//业务日期
                    Date checkdate = date == null? new Date() : date;
                    //勾对号;20260130需求调整为 AR + 勾对业务日期 + 19位OID
                    String checkno = "AR" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();
                    Journal journalNew = new Journal();
                    journalNew.setId(journal.getId());
                    journalNew.setSrcbillitemid(journal.getSrcbillitemid());
                    journalNew.setTradetype(journal.getTradetype());
                    journalNew.setBankreconciliationsettingid(settingId.toString());
                    journalNew.setDefine1(journal.get("ts"));
                    packBill(bank, journalNew, checkno,checkdate,true,queryOperator(), SettleStatus.alreadySettled,reconciliationdatasource,settingId.toString());
                    banks.add(bank);
                    newBankIterator.remove();
                    journals.add(journalNew);
                    journalIterator.remove();
                    //组装勾对关系记录表信息
                    ReconciliationInfoVO reconciliationInfoVO = this.handleReconciliationInfoVO(Collections.singletonList(journalNew),Collections.singletonList(bank),
                            ReconciliationBasisType.KeyElementMatching.getValue(),bankAutoCheckConfig,settingId.toString(),reconciliationdatasource,checkno,checkdate);
                    reconciliationInfoVOList.add(reconciliationInfoVO);
                    break;
                }
            }

        }
        return reconciliationInfoVOList;
    }

    /**
     * 调用总账接口更新勾兑状态
     * @param journals
     */
    @Override
    public void batchUpdateCheckFlag(List<Journal> journals,String seqNo){
        if(journals==null||journals.size()==0){
            return;
        }
        //总账勾对/取消勾对接口
        CashVoucherCheckInfoDTO cashVoucherCheckInfoDTO = new CashVoucherCheckInfoDTO();
        cashVoucherCheckInfoDTO.setCheckflag(journals.get(0).getCheckflag());
        List<CheckInfoDTO> checkInfoDTOList = new ArrayList<>();
        for(Journal journal:journals){
            CheckInfoDTO checkInfoDTO = new CheckInfoDTO();
            checkInfoDTO.setVoucherbid(journal.getSrcbillitemid());
            checkInfoDTO.setTradetype(journal.getTradetype());
            checkInfoDTO.setCheckno(journal.getCheckno());
            checkInfoDTO.setBankreconciliationsettingid(journal.getBankreconciliationsettingid());
            checkInfoDTO.setTs(journal.getDefine1());
            checkInfoDTOList.add(checkInfoDTO);
            //记录业务日志
//            CtmJSONObject logparam = new CtmJSONObject();
//            logparam.put("vouchinfo",journal);
//            logparam.put("checkno",journal.getCheckno());
//            logparam.put("checkSeqNo",seqNo);
//            ctmcmpBusinessLogService.saveBusinessLog(logparam, journal.getCheckno(), "银企对账勾对-凭证数据勾对准备", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "对账勾对的的凭证数据");
        }
        cashVoucherCheckInfoDTO.setCheckinfo(checkInfoDTOList);
        ResultDataDTO result = RemoteDubbo.get(IVoucherBankRpcService.class, "yonbip-fi-gl").updateCheckFlagTry(cashVoucherCheckInfoDTO);
        //记录业务日志
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("requestVo",cashVoucherCheckInfoDTO);
        logparam.put("resultVo",result);
        ctmcmpBusinessLogService.saveBusinessLog(logparam,seqNo, "银企对账勾对-凭证对账接口调用", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "现金调用总账凭证勾对接口");
        if(!"200".equals(result.getCode())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A5","更新总账凭证勾兑状态失败，总账报错：") /* "更新总账凭证勾兑状态失败，总账报错：" */ + result.getMessage());
        }
    }

    /**
     * 处理凭证和银行流水或者一行日记账勾对/取消勾对
     * @param journals
     * @param banks
     * @param reconciliationdatasource
     * @throws Exception
     */
    @Override
    public void handleJournalAndBankCheck( List<Journal> journals,List<BankReconciliation> banks,Integer reconciliationdatasource) throws Exception{
        String seqNo = UUID.randomUUID().toString();
        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
            //调用总账接口更新勾兑状态、勾兑号
            batchUpdateCheckFlag(journals,seqNo);
        }else{
            for (Journal journal : journals){
                //记录业务日志
                CtmJSONObject logparam = new CtmJSONObject();
                logparam.put("journalinfo",journal);
                logparam.put("checkno",journal.getCheckno());
                logparam.put("checkSeqNo",seqNo);
                ctmcmpBusinessLogService.saveBusinessLog(logparam, journal.getCheckno(), "银企对账勾对-银行日记账", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "对账勾对的的银行日记账数据");
            }
            MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
        }
        //记录银行流水的日志
        for(BankReconciliation bankReconciliation:banks){
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("bankreconciliationinfo",bankReconciliation);
            //凭证
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                logparam.put("checkno",bankReconciliation.getOther_checkno());
            }else {
                logparam.put("checkno",bankReconciliation.getCheckno());
            }
            logparam.put("checkSeqNo",seqNo);
            ctmcmpBusinessLogService.saveBusinessLog(logparam, logparam.getString("checkno"), "银企对账勾对-银行流水数据", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "对账勾对的的银行流水数据");
        }
        CommonSaveUtils.updateBankReconciliation4Check(banks,reconciliationdatasource);
    }

    /**
     * 保存勾对关系记录
     * 将 ReconciliationInfoVO 转换成数据 ReconciliationMatchRecord 存库
     */
    @Override
    public void saveReconciliationMathRecord(List<ReconciliationInfoVO> reconciliationInfoVOList) throws Exception {
        List<ReconciliationMatchRecord> reconciliationMatchRecordList = new ArrayList<>();
        for (ReconciliationInfoVO reconciliationInfoVO : reconciliationInfoVOList) {
            //处理银行流水
            for (BankReconciliation bank : reconciliationInfoVO.getBankReconciliationList()) {
                ReconciliationMatchRecord bankRecord = initMatchRecordByReconciliationInfo(reconciliationInfoVO);
                bankRecord.setBankreconciliationId(bank.getId().toString());
                bankRecord.setDataSource(ReconciliationMatchDataSource.BankReconciliation.getValue());
                bankRecord.setTranDate(bank.getDzdate());
                reconciliationMatchRecordList.add(bankRecord);
            }
            //处理凭证或者日记账
            for (Journal journal : reconciliationInfoVO.getJournalList()){
                ReconciliationMatchRecord journalRecord = initMatchRecordByReconciliationInfo(reconciliationInfoVO);
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationInfoVO.getReconciliationDataSource()){
                    journalRecord.setDataSource(ReconciliationMatchDataSource.Voucher.getValue());
                    //银账对账工作台返回的是String
                    Object vouchDateObj = journal.get("vouchdate");
                    Date vouchDate;
                    if (vouchDateObj instanceof String) {
                        // 如果返回的是String类型，需要进行转换
                        String dateString = (String) vouchDateObj;
                        try {
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            vouchDate = formatter.parse(dateString);
                        } catch (ParseException e) {
                            log.error("日期转换异常: " + dateString, e);
                            // 处理转换异常，可以返回默认值或抛出异常
                            vouchDate = new Date(); // 或其他默认处理
                        }
                    } else if (vouchDateObj instanceof Date) {
                        // 如果返回的是Date类型，直接使用
                        vouchDate = (Date) vouchDateObj;
                    }else {
                        vouchDate = new Date();
                    }
                    journalRecord.setAccountingDate(vouchDate);
                    journalRecord.setVoucherId(journal.getSrcbillitemid());
                    journalRecord.setOthernameMatchMethod(null);
                }else {
                    journalRecord.setDataSource(ReconciliationMatchDataSource.Journal.getValue());
                    journalRecord.setAccountingDate(journal.getDzdate());
                    journalRecord.setJournalId(journal.getId().toString());
                }
                reconciliationMatchRecordList.add(journalRecord);
            }
        }
        MetaDaoHelper.insert(ReconciliationMatchRecord.ENTITY_NAME, reconciliationMatchRecordList);
    }

    @Override
    public List<ReconciliationMatchRecord> queryReconciliationMatchRecord(List<String> checknoList) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        group.addCondition(QueryCondition.name("checkno").in(checknoList));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryObject(ReconciliationMatchRecord.ENTITY_NAME, querySchema,null);
    }

    @Override
    public void deleteReconciliationMatchRecord(List<ReconciliationMatchRecord> recordList) throws Exception {
        MetaDaoHelper.delete(ReconciliationMatchRecord.ENTITY_NAME, recordList);
    }

    @Override
    public void sealReconciliationMatchRecord(List<ReconciliationMatchRecord> recordList, boolean sealFlag) throws Exception {
        for (ReconciliationMatchRecord record : recordList){
            record.setSealFlag(sealFlag);
            record.setEntityStatus(EntityStatus.Update);
        }
        MetaDaoHelper.update(ReconciliationMatchRecord.ENTITY_NAME, recordList);
    }

    @Override
    public List<Map<String, Object>> queryReconciliationRecordInfo(CtmJSONObject param) throws Exception {
        if (!param.containsKey("dataSource") || !param.containsKey("idList")){
            throw new CtmException("缺少必要入参dataSource或idList"); //@notranslate
        }
        List<String> idList = param.getJSONArray("idList").toJavaList(String.class);
        if (idList.isEmpty()){
            throw new CtmException("idList集合不可为空"); //@notranslate
        }
        //判断dataSource是否在1,2,3之内
        String dsStr = param.getString("dataSource");
        if (!("1".equals(dsStr) || "2".equals(dsStr) || "3".equals(dsStr))) {
            throw new CtmException("dataSource只可为1、2或3"); //@notranslate
        }
        QuerySchema querySchema;
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("dataSource").eq(param.getShort("dataSource")));
        if (ReconciliationMatchDataSource.Voucher.getValue() == param.getShort("dataSource")){
            querySchema = QuerySchema.create().addSelect("voucherId as id,checkTime,checkOperator as checkman,checkOperator.name as checkman_name ");
            group.appendCondition(QueryCondition.name("voucherId").in(idList));
        }else if (ReconciliationMatchDataSource.Journal.getValue() == param.getShort("dataSource")){
            querySchema = QuerySchema.create().addSelect("journalId as id,checkTime,checkOperator as checkman,checkOperator.name as checkman_name");
            group.appendCondition(QueryCondition.name("journalId").in(idList));
        }else {
            querySchema = QuerySchema.create().addSelect("bankreconciliationId as id,checkTime,checkOperator as checkman,checkOperator.name as checkman_name");
            group.appendCondition(QueryCondition.name("bankreconciliationId").in(idList));
        }
        querySchema.addCondition(group);
        return MetaDaoHelper.query(ReconciliationMatchRecord.ENTITY_NAME, querySchema);
    }

    /**
     * 根据对账关系初始化匹配关系记录
     */
    private ReconciliationMatchRecord initMatchRecordByReconciliationInfo(ReconciliationInfoVO reconciliationInfoVO){
        ReconciliationMatchRecord record = new ReconciliationMatchRecord();
        record.setAccentity(reconciliationInfoVO.getAccentity());
        record.setBankaccount(reconciliationInfoVO.getBankaccount());
        record.setCurrency(reconciliationInfoVO.getCurrency());
        record.setCheckno(reconciliationInfoVO.getCheckno());
        record.setCheckDate(reconciliationInfoVO.getCheckDate());
        record.setCheckTime(reconciliationInfoVO.getCheckTime());
        record.setCheckOperator(reconciliationInfoVO.getCheckOperator());
        record.setReconciliationBasis(reconciliationInfoVO.getReconciliationBasis());
        record.setReconciliationScheme(reconciliationInfoVO.getReconciliationScheme());
        //只有在关键要素匹配时，记录各要素
        if (reconciliationInfoVO.getBankAutoCheckConfig() != null && record.getReconciliationBasis() == ReconciliationBasisType.KeyElementMatching.getValue() ){
            BankAutoCheckConfig bankAutoCheckConfig = reconciliationInfoVO.getBankAutoCheckConfig();
            record.setDateFloat(bankAutoCheckConfig.getChangedays());
            record.setRemarkMatchMethod(bankAutoCheckConfig.getRemarkmatch());
            record.setNotenoMatchMethod(bankAutoCheckConfig.getNotenoMatchMethod());
            record.setOthernameMatchMethod(bankAutoCheckConfig.getOthernameMatchMethod());
            record.setSamedataMatchMethod(bankAutoCheckConfig.getSamedataMatchMethod());
        }
        record.setId(ymsOidGenerator.nextId());
        record.setEntityStatus(EntityStatus.Insert);
        return record;
    }

    /**
     * 组装勾兑数据
     */
    private void packBill(BankReconciliation bank, Journal journal, String checkno,
                          Date date, Boolean flag, Long checkman, SettleStatus settleStatus,
                          Integer reconciliationdatasource, String reconciliationdatasourceid) {
        //勾对时间赋值
        Date checkTime = null;
        try {
            checkTime = DateUtils.parseDate(DateUtils.dateFormat(date,"yyyy-MM-dd")+ DateUtils.dateFormat(new Date()," HH:mm:ss"),"yyyy-MM-dd HH:mm:ss");
        }catch (Exception e){
            log.error("银企对账勾对赋值 packBill 日期转换异常");
        }
        if (bank != null) {
            if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                bank.setOther_checkflag(flag);
                bank.setOther_checkno(checkno);
                bank.setOther_checkdate(date);
                bank.setOther_checktime(checkTime);
                bank.setGl_bankreconciliationsettingid(reconciliationdatasourceid);
            } else {
                bank.setCheckno(checkno);
                bank.setCheckflag(flag);
                bank.setCheckdate(date);
                bank.setChecktime(checkTime);
                bank.setCheckman(checkman);
                bank.setBankreconciliationsettingid(reconciliationdatasourceid);
            }
            bank.setEntityStatus(EntityStatus.Update);
        }
        if (journal != null) {
            journal.setCheckflag(flag);
            journal.setCheckno(checkno);
            journal.setCheckdate(date);
            journal.setChecktime(checkTime);
            journal.setCheckman(checkman);
            journal.setBankreconciliationsettingid(reconciliationdatasourceid);
            if (null != journal.getSrcbillitemid()) {
                journal.setSettlestatus(settleStatus);
            }
            journal.setEntityStatus(EntityStatus.Update);
        }
    }

    /**
     * 获取启用的对账方案
     *
     * @return
     */
    private List<BankReconciliationSetting> getEnableSettings(CtmJSONObject param) throws Exception {
        QuerySchema mainSchema = QuerySchema.create().addSelect("*");
        //启用状态
        List<Object> statusList = new ArrayList<>();
        statusList.add(EnableStatus.Enabled.getValue());
        QueryConditionGroup mainGroup = QueryConditionGroup.and(QueryCondition.name("enableStatus").in(statusList));
        //会计主体
        if (StringUtils.isNotEmpty(param.getString("accentity"))){
            String[] accentitys = param.getString("accentity").split(";");
            List<String> accentitysList = Lists.newArrayList(accentitys);
            mainGroup.appendCondition(QueryCondition.name("accentity").in(accentitysList));
        }

        //数据源 1=凭证；2=银行日记账
        if (StringUtils.isNotEmpty(param.getString("resourcetype"))){
            String[] resourcetypes = param.getString("resourcetype").split(",");
            List<String> resourcetypeList = Lists.newArrayList(resourcetypes);
            mainGroup.appendCondition(QueryCondition.name("reconciliationdatasource").in(resourcetypeList));
        }


        mainSchema.addCondition(mainGroup);
        List<Map<String, Object>> bankReconciliationSettings = MetaDaoHelper.query(BankReconciliationSetting.ENTITY_NAME, mainSchema);
        List<BankReconciliationSetting> list = new ArrayList<>();

        if (bankReconciliationSettings != null) {
            for (Map<String, Object> map : bankReconciliationSettings) {
                BankReconciliationSetting bankReconciliationSetting = new BankReconciliationSetting();
                bankReconciliationSetting.init(map);
                bankReconciliationSetting.setBankReconciliationSetting_b(getBank_b(bankReconciliationSetting));
                list.add(bankReconciliationSetting);
            }
        }
        return list;
    }

    /**
     * 获取子表数据
     *
     * @param bankReconciliationSetting
     * @return
     * @throws Exception
     */
    private List<BankReconciliationSetting_b> getBank_b(BankReconciliationSetting bankReconciliationSetting) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bankReconciliationSetting.getId()));
        QuerySchema schema = QuerySchema.create().addSelect("*");
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        List<BankReconciliationSetting_b> bank_bs = new ArrayList<>();
        for (Map<String, Object> map : bankReconciliationSetting_bs) {
            BankReconciliationSetting_b bankReconciliationSetting_b = new BankReconciliationSetting_b();
            bankReconciliationSetting_b.init(map);
            bank_bs.add(bankReconciliationSetting_b);
        }
        return bank_bs;
    }

    /**
     * 根据条件查询银行对账单
     *
     * @return
     */
    private List<BankReconciliation> getBankReconciliationList(BankReconciliationSetting setting,Date startDate, Date endDate) {
        List<QueryConditionGroup> groupList = new ArrayList<>();
        for (int i=0 ; i < setting.BankReconciliationSetting_b().size() ; i++) {
            BankReconciliationSetting_b b = setting.BankReconciliationSetting_b().get(i);
            if(b.getEnableStatus_b() != EnableStatus.Enabled.getValue()){
                continue;
            }
            QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                    QueryCondition.name("bankaccount").eq(b.getBankaccount()),
                    QueryCondition.name("currency").eq(b.getCurrency())
            );
            groupList.add(bankAndCurrency);
        }
        if (groupList.size() == 0) {
            QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                    QueryCondition.name("bankaccount").eq("0"),
                    QueryCondition.name("currency").eq("0")
            );
            groupList.add(bankAndCurrency);
        }
        QueryConditionGroup bankGroup = QueryConditionGroup.or(groupList.toArray(new ConditionExpression[0]));

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(bankGroup);
        //日记账和凭证分别设置勾对状态
        if (setting.getReconciliationdatasource().getValue() == ReconciliationDataSource.Voucher.getValue()){
            group.appendCondition(QueryCondition.name("other_checkflag").eq(0)); //总账未勾兑
        }else {
            group.appendCondition(QueryCondition.name("checkflag").eq(0)); //日记账未勾对
        }

        //非期初数据，限定日期大于启用日期
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("dzdate").egt(setting.getEnableDate()),
                QueryCondition.name("dzdate").elt(endDate)
        );
        if (startDate != null ){
            group1.appendCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(startDate)));
        }
        //期初数据
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(1),
                QueryCondition.name("bankreconciliationscheme").eq(setting.getId())
        );
        //银行流水对账银行账户数据权限适配
        try {
            String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
            if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                group.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
            }
        }catch (Exception e){
            log.error("获取数据权限时错误" + e);
        }

        group.appendCondition(QueryConditionGroup.or(group1,group2));
        querySchema.addCondition(group);
        try {
            return MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据对账方案查询未勾兑的银行日记账
     *
     * @param setting
     * @return
     */
    private List<Journal> getJournalList(BankReconciliationSetting setting,Date startDate, Date endDate) {
        List<QueryConditionGroup> groupList = new ArrayList<>();
        for (int i=0 ; i < setting.BankReconciliationSetting_b().size() ; i++) {
            BankReconciliationSetting_b b = setting.BankReconciliationSetting_b().get(i);
            if(b.getEnableStatus_b() != EnableStatus.Enabled.getValue()){
                continue;
            }
            QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                    QueryCondition.name("bankaccount").eq(b.getBankaccount()),
                    QueryCondition.name("currency").eq(b.getCurrency())
            );
            groupList.add(bankAndCurrency);
        }
        if (groupList.size() == 0) {
            QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                    QueryCondition.name("bankaccount").eq("0"),
                    QueryCondition.name("currency").eq("0")
            );
            groupList.add(bankAndCurrency);
        }
        QueryConditionGroup bankGroup = QueryConditionGroup.or(groupList.toArray(new ConditionExpression[0]));

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("checkflag").eq(0)//勾兑标识，未勾兑
        );
        group.appendCondition(bankGroup);
        //非期初数据，限定日期大于启用日期
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("dzdate").egt(setting.getEnableDate()),
                QueryCondition.name("dzdate").elt(endDate)
        );
        if (startDate != null ){
            group1.appendCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(startDate)));
        }
        //期初数据
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(1),
                QueryCondition.name("bankreconciliationscheme").eq(setting.getId())
        );
        //银行流水对账银行账户数据权限适配
        try {
            String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
            if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                group.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
            }
        }catch (Exception e){
            log.error("获取数据权限时错误" + e);
        }

        group.appendCondition(QueryConditionGroup.or(group1,group2));
        querySchema.addCondition(group);
        try {
            return MetaDaoHelper.queryObject(Journal.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询凭证日记账
     * @param setting
     * @return
     */
    private List<LinkedHashMap> getVocherJournalList(BankReconciliationSetting setting,String startDate, String endDate) throws Exception{
        List<LinkedHashMap> journals = new ArrayList<>();
        CtmJSONObject argsJson = new CtmJSONObject();
        //分页信息
        CtmJSONObject pageInfo = new CtmJSONObject();
        pageInfo.put("pageIndex",0);
        pageInfo.put("pageSize",1000);
        argsJson.put("page",pageInfo);

        //过滤条件
        CtmJSONObject conditionJson = new CtmJSONObject();
        CtmJSONArray ctmJSONArray = new CtmJSONArray();
        //业务日期
        LinkedHashMap<String,String> makeTimeMap = new LinkedHashMap<>();
        makeTimeMap.put("itemName","makeTime");
        if (startDate != null){
            makeTimeMap.put("value1",startDate);
        }
        makeTimeMap.put("value2",endDate);
        ctmJSONArray.add(makeTimeMap);

        //会计主体
        LinkedHashMap<String,String> accentityMap = new LinkedHashMap<>();
        accentityMap.put("itemName","accentity");
        accentityMap.put("value1",setting.getAccentity());
        ctmJSONArray.add(accentityMap);

        //是否勾对
        LinkedHashMap<String,String> checkflagMap = new LinkedHashMap<>();
        checkflagMap.put("itemName","checkflag");
        checkflagMap.put("value1","false");
        ctmJSONArray.add(checkflagMap);

        //是否封存
        LinkedHashMap<String,String> sealflagMap = new LinkedHashMap<>();
        sealflagMap.put("itemName","sealflag");
        sealflagMap.put("value1","false");
        ctmJSONArray.add(sealflagMap);

        //对账方案id
        LinkedHashMap<String,String> bankreconciliationschemeMap = new LinkedHashMap<>();
        bankreconciliationschemeMap.put("itemName","bankreconciliationscheme");
        bankreconciliationschemeMap.put("value1",setting.getId().toString());
        ctmJSONArray.add(bankreconciliationschemeMap);
        conditionJson.put("commonVOs",ctmJSONArray);
        conditionJson.put("reconciliationdatasourceid",setting.getId().toString());

        argsJson.put("condition",conditionJson);
        argsJson.put("billnum","cmp_check");

        //查询对账方案下使用组织的账簿
        PlanParam planParam = new PlanParam(null,null,setting.getId().toString());
        List<BankReconciliationSettingVO> infoList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);

        //按照账簿下查询
        Set<String> bookids = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            //需要对账方案启用
            if (settingVO.getEnableStatus() != EnableStatus.Enabled.getValue()){
                continue;
            }
            bookids.add(settingVO.getAccBook());
        }
        for(String accbookid : bookids){
            conditionJson.put("accbookId",accbookid);
            argsJson.put("condition",conditionJson);
            Pager page = reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
            if(page!=null){
                Integer pageCount = page.getPageCount();
                journals.addAll(page.getRecordList());
                if(pageCount!=1){
                    for (int i=2;i<=pageCount;i++){
                        pageInfo.put("pageIndex",i);
                        argsJson.put("page",pageInfo);
                        page = reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
                        if(page!=null){
                            journals.addAll(page.getRecordList());
                        }
                    }
                }
            }
        }

        // 【新增】防御性去重：防止因分页漂移或底层数据异常导致的重复凭证
        if (journals.size() > 0) {
            // 使用 LinkedHashMap 保持原有顺序，Key 为 srcbillitemid
            Map<Object, LinkedHashMap> uniqueMap = new LinkedHashMap<>();
            for (LinkedHashMap journal : journals) {
                Object id = journal.get("srcbillitemid");
                // 确保 ID 不为空且未出现过，才放入集合
                if (id != null && !uniqueMap.containsKey(id)) {
                    uniqueMap.put(id, journal);
                } else if (id != null) {
                    log.error("检测到重复的凭证ID: {}, 已自动去重", id);
                }
            }
            // 将去重后的 values 转回 List
            journals = new ArrayList<>(uniqueMap.values());
        }

        return journals;
    }

    /**
     * 获取操作人
     * @return
     * @throws Exception
     */
    private Long queryOperator() {
        return  AppContext.getCurrentUser().getId();
    }

    /**
     * 调用总账list2接口，获取凭证列表
     * @param filterArgs
     * @return
     */
    @Override
    public Pager reqVoucheList2(String filterArgs) throws Exception{
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/list2";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, filterArgs, header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        String code = result.getString("code");
        String message = result.getString("message");
        if(!"1".equals(code)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102471"),MessageUtils.getMessage("P_YS_FI_CM_0001252241") /* "查询总账失败，总账接口返回错误信息：" */+message);
        }
        Pager page = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(result.get("data")),Pager.class);
        if(page!=null) {
            return page;
        }
        return null;
    }

    /**
     * 处理跟凭证勾对时，已关联银行回单的银行流水向总账发送消息
     * @param journals
     * @param banks
     */
    private void handleBankReceiptCorrEvent(List<Journal> journals,List<BankReconciliation> banks) {
        //凭证勾兑回单文件需求
        //key：勾兑号 value:勾兑的对账单集合
        Map<String,List<BankReconciliation>> bankReceiptMap = new HashMap<>();
        //key：勾兑号 value:勾兑的凭证集合
        Map<String,List<Journal>> journalReceiptMap = new HashMap<>();

        //组装需要发送凭证回单关联的数据
        for (Journal journal : journals){
            List<Journal> journalList;
            if (journalReceiptMap.containsKey(journal.getCheckno())){
                journalList = journalReceiptMap.get(journal.getCheckno());
            }else {
                journalList = new ArrayList<>();
            }
            journalList.add(journal);
            journalReceiptMap.put(journal.getCheckno(),journalList);
        }
        for (BankReconciliation bank : banks){
            List<BankReconciliation> bankList;
            if (bankReceiptMap.containsKey(bank.getOther_checkno())){
                bankList = bankReceiptMap.get(bank.getOther_checkno());
            }else {
                bankList = new ArrayList<>();
            }
            bankList.add(bank);
            bankReceiptMap.put(bank.getOther_checkno(),bankList);
        }

        ExecutorService taskExecutor = null;
        try {
            taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"handleBankReceiptEvent-threadpool");
            taskExecutor.submit(() -> {
                try {
                    //凭证关联回单，事件发送
                    cmpCheckService.handleBankReceiptEvent(bankReceiptMap,journalReceiptMap);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally {
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
    }
}
