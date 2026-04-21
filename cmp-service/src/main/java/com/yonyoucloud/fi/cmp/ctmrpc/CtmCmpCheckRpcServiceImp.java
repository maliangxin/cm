package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.*;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTOForRpc;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 支票对外操作接口
 * 目前供：结算单调用
 */
@Slf4j
@Service
public class CtmCmpCheckRpcServiceImp implements CtmCmpCheckRpcService {

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private CheckStatusService checkStatusService;
    private static final String CHECKMAPPER;
    private static final String UPDATECHECKPAYTICKET;
    private static final String CHECKCASHINGANDENDORSEMENT;
    private static final String UPDATECHECKCASHDATE;

    static {
        //sql路径名称
        CHECKMAPPER = "com.yonyoucloud.fi.cmp.mapper.CheckMapper";
        UPDATECHECKPAYTICKET = ".updateCheckPayTicket";
        CHECKCASHINGANDENDORSEMENT = ".checkCashingAndEndorsement";
        UPDATECHECKCASHDATE = ".updateCheckCashDate";
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String checkOperation(List<CheckDTO> list) throws Exception {
        List<CheckStock> cancelList = new ArrayList<>();
        CheckOperationType operationType = null;
        for (CheckDTO checkDTO : list) {
            //保存修改前数据
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkDTO.getCheckBillNo());//批量查询
            cancelList.add(checkStock);
            operationType = checkDTO.getOperationType();
            CtmJSONObject logData = new CtmJSONObject();
            logData.put("checkDTO", checkDTO);
            logData.put("checkStock", checkStock);
            logData.put("operationType", operationType.getIndex());
            ctmcmpBusinessLogService.saveBusinessLog(logData, checkStock.getCheckBillNo(), "checkOperation",
                    IServicecodeConstant.CHECKSTOCKAPPLY, IMsgConstant.CHECKSTOCK_OPERATION, IMsgConstant.CHECKSTOCK_OPERATION);
            switch (operationType.getIndex()) {// TODO 策略模式
                case 1:
                    //锁定
                    checkLock(checkDTO, CmpLock.YES);
                    break;
                case 2:
                    //解锁
                    checkLock(checkDTO, CmpLock.NO);
                    break;
                case 3:
                    //付票
                    checkPayTicket(checkDTO, true);
                    break;
                case 4:
                    //取消付票
                    checkPayTicket(checkDTO, false);
                    break;
                case 5:
                    //兑付、背书
                    checkCashingAndendorsement(checkDTO);
                    break;
                case 6:
                    //作废
                    checkCancel(checkDTO);
                    break;
                case 7:
                    //付票（无锁定）
                    checkLock(checkDTO, CmpLock.YES);
                    checkPayTicket(checkDTO, true);
                    break;
                case 8:
                    //兑付、背书（无锁定）
                    checkLock(checkDTO, CmpLock.YES);
                    if (CmpInputBillDir.Pay == checkDTO.getInputBillDir()) {
                        checkPayTicket(checkDTO, true);
                    }
                    checkCashingAndendorsement(checkDTO);
                    break;
                default:
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101120"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180501","支票操作类型有误，支票编号：") /* "支票操作类型有误，支票编号：" */ + checkDTO.getCheckBillNo());
            }
        }
        rollBackOperation(cancelList, operationType);
        YtsContext.setYtsContext("CHECK_OPERATION_CANCEL_DATA", cancelList);
        return ResultMessage.success();
    }

    @Override
    public String checkOperationNew(List<CheckDTOForRpc> list) throws Exception {
        List<CheckStock> cancelList = new ArrayList<>();
        CheckOperationType operationType = null;
        for (CheckDTOForRpc checkDTORpc : list) {
            //将api工程下的rpcvo转为 modle下的普通vo
            CheckDTO checkDTO = new CheckDTO();
            BeanUtils.copyProperties(checkDTORpc,checkDTO);
            //保存修改前数据
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkDTO.getCheckBillNo());//批量查询
            cancelList.add(checkStock);
            operationType = checkDTO.getOperationType();
            switch (operationType.getIndex()) {// TODO 策略模式
                case 1:
                    //锁定
                    checkLock(checkDTO, CmpLock.YES);
                    break;
                case 2:
                    //解锁
                    checkLock(checkDTO, CmpLock.NO);
                    break;
                case 3:
                    //付票
                    checkPayTicket(checkDTO, true);
                    break;
                case 4:
                    //取消付票
                    checkPayTicket(checkDTO, false);
                    break;
                case 5:
                    //兑付、背书
                    checkCashingAndendorsement(checkDTO);
                    break;
                case 6:
                    //作废
                    checkCancel(checkDTO);
                    break;
                case 7:
                    //付票（无锁定）
                    checkLock(checkDTO, CmpLock.YES);
                    checkPayTicket(checkDTO, true);
                    break;
                case 8:
                    //兑付、背书（无锁定）
                    checkLock(checkDTO, CmpLock.YES);
                    if (CmpInputBillDir.Pay == checkDTO.getInputBillDir()) {
                        checkPayTicket(checkDTO, true);
                    }
                    checkCashingAndendorsement(checkDTO);
                    break;
                default:
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101120"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180501","支票操作类型有误，支票编号：") /* "支票操作类型有误，支票编号：" */ + checkDTO.getCheckBillNo());
            }
        }
        rollBackOperation(cancelList, operationType);
        YtsContext.setYtsContext("CHECK_OPERATION_CANCEL_DATA", cancelList);
        return ResultMessage.success();
    }

    /**
     * 回滚操作特殊处理
     *
     * @param list
     * @param operationType
     */
    private void rollBackOperation(List<CheckStock> list, CheckOperationType operationType) {
        // 线下结算成功，调用兑付接口（CheckOperationType.Cash），需清空“兑付日期（cashDate）”
        if (operationType == CheckOperationType.Cash) {
            list.stream().forEach(checkStock -> {
                checkStock.setCashDate(null);
                checkStock.setCashType(null);//兑付方式
                checkStock.setCashPerson(null);//兑付人
            });
        }
    }

    /**
     * 支票锁定/解锁接口
     *
     * @param check
     * @return
     * @throws Exception
     */
    private String checkLock(CheckDTO check, CmpLock cmpLock) throws Exception {
        check.setLock(cmpLock);
        CmpLock lock = check.getLock();           //锁定状态 1锁定 0未锁定
        String checkBillNo = check.getCheckBillNo();    //支票编号ID
        CmpInputBillDir inputBillDir = check.getInputBillDir();   //单据方向 2：付款 1：收款
        if (checkBillNo == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
        }
        if ( check.getSystem() == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
        }
        if (check.getBillType() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
        }
        if (check.getInputBillNo() == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
        }
        if (inputBillDir == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","单据收支方向不可为空"));
        }
        if (lock == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","锁定状态不可为空"));
        }
        //根据支票编号ID查询支票
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
        if (checkOne == null || checkOne.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101122"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180505","支票状态异常，请重新选择") /* "支票状态异常，请重新选择" */));
        }

        if (CmpCheckStatus.Cancle.getValue().equals(checkOne.getCheckBillStatus())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101123"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F2", "关联的支票已作废，请确认！") /* "关联的支票已作废，请确认！" */);
        }


        String checkBillDir = checkOne.getCheckBillDir();// '2':付票   '1':收票
        String checkBillStatus = checkOne.getCheckBillStatus();//支票状态
        //支票锁定时，校验：支票状态是否为‘已入库’
        if (CmpLock.YES == lock && check.getBillType().getValue() == BillType.TransferAccount.getValue()) {//转账单-提取现金支票业务结算
            boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
            if(checkFundTransfer){//传结算
                //不处理
            }else{//不传结算
                boolean checkStockCanUse = autoConfigService.getCheckStockCanUse();
                if(checkStockCanUse && !CmpCheckStatus.Use.getValue().equals(checkBillStatus) && (!BillType.TransferAccount.equals(check.getBillType()))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101124"),checkOne.getCheckBillNo() + MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_1BDBFFC405580007","结算单使用支票且启用领用时，支票状态须为已领用！") /* "结算单使用支票且启用领用时，支票状态须为已领用！" */));
                }else if(!checkStockCanUse && !CmpCheckStatus.InStock.getValue().equals(checkBillStatus)){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101125"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F3", "转账单使用支票且未启用领用时，支票状态必须为已入库！") /* "转账单使用支票且未启用领用时，支票状态必须为已入库！" */);
                }
            }
        }
        if (CmpLock.YES == lock && check.getBillType().getValue() == BillType.StwbSettlebench.getValue() && ICmpConstant.TWO.equals(check.getInputBillDir().getValue())) {//结算使用支票
            boolean checkStockCanUse = autoConfigService.getCheckStockCanUse();
            log.error("支票号{}是否启用{}状态{}",checkOne.getCheckBillNo(),checkStockCanUse,checkBillStatus);
            if(checkStockCanUse && !CmpCheckStatus.Use.getValue().equals(checkBillStatus)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101124"),checkOne.getCheckBillNo() + MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-FE_1BDBFFC405580007","结算单使用支票且启用领用时，支票状态须为已领用！") /* "结算单使用支票且启用领用时，支票状态须为已领用！" */));
            }else if(!checkStockCanUse && !CmpCheckStatus.InStock.getValue().equals(checkBillStatus)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101126"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800F4", "结算单使用支票且未启用领用时，支票状态必须为已入库！") /* "结算单使用支票且未启用领用时，支票状态必须为已入库！" */);
            }
        }
//        //支票锁定时，校验：支票状态是否为‘已入库’
//        if (CmpLock.YES == lock && !CmpCheckStatus.InStock.getValue().equals(checkBillStatus)) {
//            log.error("支票锁定时必须已入库！");
//            //因为提取现金支票业务结算转账单支票状态保存时改为在开票，因此此方法仅为该转账单撤回时单独处理
//            boolean isTQXJAndCheck = this.isTQXJAndCheck(check);
//            if (isTQXJAndCheck) {
//                //如果是提取现金支票业务结算转账单则校验是否为已开票
//                log.error("提取现金支票业务时不为‘在开票’");
//                if (!CmpCheckStatus.Billing.getValue().equals(checkBillStatus)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
//                }
//            } else {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
//            }
//        }
        if (CmpLock.NO == lock && !(checkBillStatus.equals(CmpCheckStatus.Billing.getValue()) || checkBillStatus.equals(CmpCheckStatus.Endorsing.getValue()) || checkBillStatus.equals(CmpCheckStatus.Cashing.getValue()))) {
            log.error("支票解锁时为‘在开票’或‘在背书’或‘兑付中’");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        //CM202400696 根据支票锁定接口的入参，当传参的处理方式为锁定，且支票编号为收到方向的支票时，校验其票面金额与单据金额是否一致，不一致时，提示：操作失败,支票编号[SRZP0002]、[SRZP0003]票面金额与单据金额不符
        if (!Objects.isNull(checkOne.getAmount()) && CmpLock.YES == lock && CmpInputBillDir.Receive == inputBillDir && checkOne.getAmount().compareTo(check.getAmount()) != 0) {
            log.error( "操作失败,支票编号[%s]票面金额与单据金额不符！");
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_212E98E604680006", "操作失败,支票编号[%s]票面金额与单据金额不符！") /* "操作失败,支票编号[%s]票面金额与单据金额不符！" */,checkOne.getCheckBillNo()));
        }
        //根据锁定状态跟新支票状态
        if (CmpLock.YES == lock && CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Cash.getValue().equals(checkBillDir)) {
            checkOne.setCheckBillStatus(CmpCheckStatus.Billing.getValue());//支票状态   在开票
        } else if (CmpLock.YES == lock && CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Com.getValue().equals(checkBillDir)) {
            checkOne.setCheckBillStatus(CmpCheckStatus.Endorsing.getValue());//支票状态   在背书
        } else if (CmpLock.YES == lock && CmpInputBillDir.Receive == inputBillDir && CmpCheckDir.Com.getValue().equals(checkBillDir)) {
            checkOne.setCheckBillStatus(CmpCheckStatus.Cashing.getValue());//支票状态   兑付中
        } else if (CmpLock.NO == lock) {
            boolean checkStockCanUse = autoConfigService.getCheckStockCanUse();
            if(checkStockCanUse && CmpCheckDir.Cash.getValue().equals(checkBillDir)){
                //支票取消锁定时，回滚支票状态至‘已领用’，清空业务单据信息；
                checkOne.setCheckBillStatus(CmpCheckStatus.Use.getValue());//支票状态   已领用
                checkOne.setInputBillNo(null);
                checkOne.setSysNo(null);
                checkOne.setBillType(null);
            }else if(!checkStockCanUse || CmpCheckDir.Com.getValue().equals(checkBillDir)){
                //支票取消锁定时，回滚支票状态至‘已入库’，清空业务单据信息；
                checkOne.setCheckBillStatus(CmpCheckStatus.InStock.getValue());//支票状态   已入库
                checkOne.setInputBillNo(null);
                checkOne.setSysNo(null);
                checkOne.setBillType(null);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        checkOne.setIsLock(cmpLock.getValue());
        checkOne.setSysNo(String.valueOf(check.getSystem().getIndex()));
        checkOne.setBillType(String.valueOf(check.getBillType().getValue()));
        checkOne.setInputBillNo(check.getInputBillNo());
        checkOne.setEntityStatus(EntityStatus.Update);
        checkStatusService.recordCheckStatusByCheckId(checkOne.getId(),checkOne.getCheckBillStatus());
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkOne);
        return ResultMessage.success();
    }

    /**
     * 支票付票接口
     *
     * @param check
     * @return
     * @throws Exception
     */
    private String checkPayTicket(CheckDTO check, boolean flag) throws Exception {
        String checkBillNo = check.getCheckBillNo();    //支票编号ID
        BigDecimal amount = check.getAmount();          //金额
        String payeeName = check.getPayeeName();        //收款人名称
        Date drawerDate = check.getDrawerDate();        //出票日期
        String drawerName = check.getDrawerName();      //出票人员
        SystemType system = check.getSystem();          //业务系统
        BillType billType = check.getBillType();        //业务单据类型
        String inputBillNo = check.getInputBillNo();    //业务单据明细ID
        if (checkBillNo == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
        }
        if (amount == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","金额不可为空"));
        }
        if ( system == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
        }
        if (billType == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
        }
        if (payeeName == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","收款人名称不可为空"));
        }
        if (inputBillNo == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
        }
        if (drawerDate == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","出票日期不可为空"));
        }
        if (drawerName == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","出票人员不可为空"));
        }
        //根据支票编号ID查询支票
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
        String checkOneCheckBillStatus = checkOne.getCheckBillStatus();//支票状态 '02':在开票   '04':在背书
        String checkOneSystem = checkOne.getSysNo();//业务系统
        String checkOneBillType = checkOne.getBillType();//业务单据类型
        String checkOneinputBillNo = checkOne.getInputBillNo();//业务单据明细
        String checkOnecheckBillDir = checkOne.getCheckBillDir();// '2':付票   '1':收票
        String lock = checkOne.getIsLock();//锁定状态
        if (CmpLock.NO.getValue().equals(lock)) {
            log.error("支票状态异常");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        if (!checkOneSystem.equals(String.valueOf(system.getIndex())) || !checkOneBillType.equals(String.valueOf(billType.getValue())) || !checkOneinputBillNo.equals(inputBillNo)) {
            log.error("单据编号支票记录信息不一致");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101128"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180506","单据编号支票记录信息不一致") /* "单据编号支票记录信息不一致" */);
        }
        //支票方向
        if (flag) {//支票付票
            if (!checkOneCheckBillStatus.matches(CmpCheckStatus.Billing.getValue() + "|" + CmpCheckStatus.Endorsing.getValue())) {// 2 在开票 4 在背书
                log.error(checkOne.getCheckBillNo() + "支票状态异常");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
            }
            check.setCheckBillStatus(CmpCheckStatus.BillOver.getValue());//已开票
            check.setAmount(amount);
            check.setDrawerDate(drawerDate);//出票日期
            check.setDrawerName(drawerName);//出票人名称
            check.setPayeeName(payeeName);//收款人名称
            check.setEndorsee(payeeName);//被背书人
        } else {//取消付票
            if (!checkOneCheckBillStatus.matches(CmpCheckStatus.BillOver.getValue() + "|" + CmpCheckStatus.Endorsing.getValue())) {
                log.error(checkOne.getCheckBillNo() + "支票状态异常");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
            }
            check.setCheckBillStatus(CmpCheckStatus.Billing.getValue());//在开票
            check.setAmount(null);
            check.setDrawerDate(null);//出票日期
            check.setDrawerName(null);//出票人名称
            check.setPayeeName(null);//收款人名称
            check.setEndorsee(null);//被背书人
        }
        check.setCheckBillNo(checkBillNo);
        check.setCheckBillDir(checkOnecheckBillDir);//支票方向
        if (CheckDirection.Pay.getIndex().equals(checkOnecheckBillDir)) {//只有付票才会更新stock状态所以才记录status
            checkStatusService.recordCheckStatusByCheckDTO(checkOne.getId(), check);
        }
        SqlHelper.update(CHECKMAPPER + UPDATECHECKPAYTICKET, check);
        return ResultMessage.success();
    }

    /**
     * 支票兑付/背书接口
     *
     * @param check
     * @return
     * @throws Exception
     */
    private String checkCashingAndendorsement(CheckDTO check) throws Exception {
        String checkBillNo = check.getCheckBillNo();     //支票编号ID
        Date settlementDate = check.getSettlementDate();  //结算日期
        SystemType system = check.getSystem();          //业务系统
        BillType billType = check.getBillType();        //业务单据类型
        String inputBillNo = check.getInputBillNo();     //业务单据明细ID
        Short checkPurpose = check.getCheckPurpose();   //支票用途
        CmpInputBillDir inputBillDir = check.getInputBillDir();    //单据方向 2：付款 1：收款
        //根据支票编号ID查询支票
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
        String checkOneCheckBillStatus = checkOne.getCheckBillStatus();//支票状态 '02':在开票   '04':在背书
        String checkOneSystem = checkOne.getSysNo();//支票业务系统
        String checkOneBillType = checkOne.getBillType();//单据类型
        String checkOneinputBillNo = checkOne.getInputBillNo();//业务单据明细
        String checkOneCheckBillDir = checkOne.getCheckBillDir();// '2':付票   '1':收票
        if (checkBillNo == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
        }
        if (settlementDate == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","结算日期不可为空"));
        }
        if ( system == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
        }
        if (billType == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
        }
        if (inputBillNo == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
        }
        if (inputBillDir == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","单据收付方向不可为空"));
        }
        if (checkPurpose == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票用途不可为空"));
        }
        if (CmpInputBillDir.Receive == inputBillDir && CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
            log.error(checkOne.getCheckBillNo() + "支票方向异常");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101129"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180504","支票方向异常") /* "支票方向异常" */);
        }
        // 3已开票 4在背书  6兑付中
        if ((CmpInputBillDir.Pay == inputBillDir && !checkOneCheckBillStatus.matches(CmpCheckStatus.BillOver.getValue() + "|" + CmpCheckStatus.Endorsing.getValue())) || (CmpInputBillDir.Receive == inputBillDir && !CmpCheckStatus.Cashing.getValue().equals(checkOneCheckBillStatus))) {
            log.error(checkOne.getCheckBillNo() + "支票状态异常");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        if (!checkOneSystem.equals(String.valueOf(system.getIndex())) || !checkOneBillType.equals(String.valueOf(billType.getValue())) || !checkOneinputBillNo.equals(inputBillNo)) {
            log.error(checkOne.getCheckBillNo() + "单据编号支票记录信息不一致");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101128"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180506","单据编号支票记录信息不一致") /* "单据编号支票记录信息不一致" */);
        }
        check.setCheckBillNo(checkBillNo);
        check.setInputBillDir(inputBillDir);
        check.setInputBillDirString(inputBillDir.getValue());
        check.setCashDate(settlementDate);
        check.setCheckPurpose(checkPurpose);
        if (CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
            check.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());//已兑付
            check.setCashType(CashType.Business.getIndex());//兑付方式
            check.setCashPerson(AppContext.getCurrentUser().getName());//兑付人
        } else if (CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Com.getValue().equals(checkOneCheckBillDir)) {
            check.setCheckBillStatus(CmpCheckStatus.Endorsed.getValue());//已背书
        } else if (CmpInputBillDir.Receive == inputBillDir && CmpCheckDir.Com.getValue().equals(checkOneCheckBillDir)) {
            check.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());//已兑付
            check.setCashType(CashType.Business.getIndex());//兑付方式
            check.setCashPerson(AppContext.getCurrentUser().getName());//兑付人
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        checkStatusService.recordCheckStatusByCheckDTO(checkOne.getId(), check);
        SqlHelper.update(CHECKMAPPER + CHECKCASHINGANDENDORSEMENT, check);
        return ResultMessage.success();
    }

    /**
     * 支票作废接口
     *
     * @param check
     * @return
     * @throws Exception
     */
    public String checkCancel(CheckDTO check) throws Exception {
        String checkBillNo = check.getCheckBillNo();     //支票编号ID
        SystemType system = check.getSystem();          //业务系统
        BillType billType = check.getBillType();        //业务单据类型
        String inputBillNo = check.getInputBillNo();     //业务单据明细ID
        CmpInputBillDir inputBillDir = check.getInputBillDir();    //单据方向 2：付款 1：收款
        if (checkBillNo == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
        }
        if ( system == null ) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
        }
        if (billType == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
        }
        if (inputBillNo == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
        }
        if (inputBillDir == null) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","单据收付方向不可为空"));
        }
        //根据支票编号ID查询支票
        CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
        String checkOneSystem = checkStock.getSysNo();//支票业务系统
        String checkOneBillType = checkStock.getBillType();//单据类型
        String checkOneinputBillNo = checkStock.getInputBillNo();//业务单据明细
        String checkOneCheckBillDir = checkStock.getCheckBillDir();// '2':付票   '1':收票
        String checkOneCheckBillStatus = checkStock.getCheckBillStatus();//支票状态 '02':在开票   '04':在背书
        if (CmpInputBillDir.Receive == inputBillDir && CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
            log.error("支票方向异常");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101129"),checkStock.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180504","支票方向异常") /* "支票方向异常" */);
        }
        //03已开票 04在背书  06兑付中
        if ((CmpInputBillDir.Pay == inputBillDir && !checkOneCheckBillStatus.matches(CmpCheckStatus.BillOver.getValue() + "|" + CmpCheckStatus.Endorsing.getValue())) || (CmpInputBillDir.Receive == inputBillDir && !CmpCheckStatus.Cashing.getValue().equals(checkOneCheckBillStatus))) {
            log.error(checkStock.getCheckBillNo() + "支票状态异常");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkStock.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
        }
        if (!checkOneSystem.equals(String.valueOf(system.getIndex())) || !checkOneBillType.equals(String.valueOf(billType.getValue())) || !checkOneinputBillNo.equals(inputBillNo)) {
            log.error(checkStock.getCheckBillNo() + "单据编号支票记录信息不一致");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101128"),checkStock.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180506","单据编号支票记录信息不一致") /* "单据编号支票记录信息不一致" */);
        }
        if (CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
            checkStock.setCheckBillStatus(CmpCheckStatus.Cancle.getValue());//已作废
            checkStock.setCancelPerson(AppContext.getCurrentUser().getName());// 作废人
            checkStock.setDisposer(AppContext.getCurrentUser().getName());// 处置人
            checkStock.setCancelDate(BillInfoUtils.getBusinessDate());// 作废日期
        } else if (CmpInputBillDir.Pay == inputBillDir && CmpCheckDir.Com.getValue().equals(checkOneCheckBillDir)) {
            checkStock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());//已入库
        } else if (CmpInputBillDir.Receive == inputBillDir && CmpCheckDir.Com.getValue().equals(checkOneCheckBillDir)) {
            checkStock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());//已入库
        }
        checkStock.setEntityStatus(EntityStatus.Update);
        checkStatusService.recordCheckStatusByCheckId(checkStock.getId(),checkStock.getCheckBillStatus());
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
        return ResultMessage.success();
    }

    /**
     * 分布式事物回滚方法
     *
     * @param checkDTO
     * @return
     * @throws Exception
     */
    @Override
    public void checkOperationCancel(List<CheckDTO> checkDTO) throws Exception {
        List<CheckStock> list = (List<CheckStock>) YtsContext.getYtsContext("CHECK_OPERATION_CANCEL_DATA");
        if (list != null && !list.isEmpty()) {
            for (CheckStock checkStock : list) {
                // 获取最新的pubts，然后再进行回滚操作
                CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkStock.getId());
                checkStock.setPubts(newCheckStock.getPubts());
                checkStock.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
            }
        }
    }

    @Override
    public void checkOperationCancelNew() throws Exception {
        List<CheckStock> list = (List<CheckStock>) YtsContext.getYtsContext("CHECK_OPERATION_CANCEL_DATA");
        if (list != null && !list.isEmpty()) {
            for (CheckStock checkStock : list) {
                // 获取最新的pubts，然后再进行回滚操作
                CheckStock newCheckStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkStock.getId());
                checkStock.setPubts(newCheckStock.getPubts());
                checkStock.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
            }
        }
    }

    /**
     * 兑付日期变更接口实现
     * @param list
     * @return
     * @throws Exception
     */
    @Override
    public String checkCashDateAlteration(List<CheckDTO> list) throws Exception {
        for (CheckDTO checkDTO : list) {
            String checkBillNo = checkDTO.getCheckBillNo();     //支票编号ID
            Date settlementDate = checkDTO.getSettlementDate();  //结算日期
            SystemType system = checkDTO.getSystem();          //业务系统
            BillType billType = checkDTO.getBillType();        //业务单据类型
            String inputBillNo = checkDTO.getInputBillNo();     //业务单据明细ID
            CmpInputBillDir inputBillDir = checkDTO.getInputBillDir();    //单据方向 2：付款 1：收款

            //根据支票编号ID查询支票
            CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
            String checkOneCheckBillStatus = checkOne.getCheckBillStatus();//支票状态 '02':在开票   '04':在背书
            String checkOneCheckBillDir = checkOne.getCheckBillDir();// '2':付票   '1':收票

            if (checkBillNo == null ) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
            }
            if (settlementDate == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","结算日期不可为空"));
            }
            if ( system == null ) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
            }
            if (billType == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
            }
            if (inputBillNo == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
            }
            if (inputBillDir == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","单据收付方向不可为空"));
            }
            // 单据方向为收款
            if (CmpInputBillDir.Receive.getValue().equals(inputBillDir.getValue())) {
                if (CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
                    log.error(checkOne.getCheckBillNo() + "支票方向异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101129"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180504","支票方向异常") /* "支票方向异常" */);
                }
                if (!CmpCheckStatus.Cashed.getValue().equals(checkOneCheckBillStatus)) {
                    log.error(checkOne.getCheckBillNo() + "支票状态异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
                }
            } else { // 单据方向为付款
                if (!CmpCheckStatus.Cashed.getValue().equals(checkOneCheckBillStatus)) {
                    log.error(checkOne.getCheckBillNo() + "支票状态异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
                }
//                checkDTO.setCashDate(settlementDate);
            }
//            SqlHelper.update(CHECKMAPPER + UPDATECHECKCASHDATE, checkDTO);
            checkOne.setCashDate(settlementDate);
            checkOne.setEntityStatus(EntityStatus.Update);
            checkDTO.setCashDate(settlementDate);
            checkDTO.setCheckBillStatus(checkOneCheckBillStatus);
            checkStatusService.recordCheckStatusByCheckDTO(checkOne.getId(), checkDTO);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkOne);
        }

        return ResultMessage.success();
    }

    @Override
    public String checkCashDateAlterationNew(List<CheckDTOForRpc> list) throws Exception {
        for (CheckDTOForRpc checkDTORpc : list) {
            CheckDTO checkDTO = new CheckDTO();
            BeanUtils.copyProperties(checkDTORpc,checkDTO);
            String checkBillNo = checkDTO.getCheckBillNo();     //支票编号ID
            Date settlementDate = checkDTO.getSettlementDate();  //结算日期
            SystemType system = checkDTO.getSystem();          //业务系统
            BillType billType = checkDTO.getBillType();        //业务单据类型
            String inputBillNo = checkDTO.getInputBillNo();     //业务单据明细ID
            CmpInputBillDir inputBillDir = checkDTO.getInputBillDir();    //单据方向 2：付款 1：收款

            //根据支票编号ID查询支票
            CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkBillNo);
            String checkOneCheckBillStatus = checkOne.getCheckBillStatus();//支票状态 '02':在开票   '04':在背书
            String checkOneCheckBillDir = checkOne.getCheckBillDir();// '2':付票   '1':收票
            if (checkBillNo == null ) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","支票编号ID不可为空"));
            }
            if (settlementDate == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","结算日期不可为空"));
            }
            if ( system == null ) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","来源业务系统不可为空"));
            }
            if (billType == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据类型不可为空"));
            }
            if (inputBillNo == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","业务单据明细ID不可为空"));
            }
            if (inputBillDir == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("","单据收付方向不可为空"));
            }
            // 单据方向为收款
            if (CmpInputBillDir.Receive.getValue().equals(inputBillDir.getValue())) {
                if (CmpCheckDir.Cash.getValue().equals(checkOneCheckBillDir)) {
                    log.error(checkOne.getCheckBillNo() + "支票方向异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101129"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180504","支票方向异常") /* "支票方向异常" */);
                }
                if (!CmpCheckStatus.Cashed.getValue().equals(checkOneCheckBillStatus)) {
                    log.error(checkOne.getCheckBillNo() + "支票状态异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
                }
            } else { // 单据方向为付款
                if (!CmpCheckStatus.Cashed.getValue().equals(checkOneCheckBillStatus)) {
                    log.error(checkOne.getCheckBillNo() + "支票状态异常");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101127"),checkOne.getCheckBillNo() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180503","支票状态异常") /* "支票状态异常" */);
                }
                checkDTO.setCashDate(settlementDate);
            }
            checkDTO.setCheckBillStatus(checkOneCheckBillStatus);
            checkStatusService.recordCheckStatusByCheckDTO(checkOne.getId(), checkDTO);
            SqlHelper.update(CHECKMAPPER + UPDATECHECKCASHDATE, checkDTO);
        }

        return ResultMessage.success();
    }

    private boolean isTQXJAndCheck(CheckDTO check) throws Exception {
        String transferCode = check.getInputBillNo();

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("code").eq(transferCode));
        querySchema.addCondition(group);
        List<TransferAccount> transferAccountList = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
        if (transferAccountList != null && transferAccountList.size() > 0) {
            TransferAccount transferAccount = transferAccountList.get(0);

            BizObject tradeTypeObj = MetaDaoHelper.findById("bd.bill.TransType", Long.parseLong(transferAccount.getTradetype()), ISchemaConstant.MDD_SCHEMA_TRANSTYPE);
            String tradeType = null;
            if (tradeTypeObj != null || tradeTypeObj.get("extend_attrs_json") != null) {
                tradeType = (String) CtmJSONObject.parseObject(tradeTypeObj.get("extend_attrs_json")).get("transferType_zz");
            }
            //判断是否为支票业务
            int settel = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
            boolean isCheck = false;
            if (settel == 8 ) {
                isCheck = true;
            } else if (settel == 0) {
                isCheck = true;
            }

            //是否为提取现金支票业务结算
            boolean isTQXJAndCheck = ("ec".equals(transferAccount.getType()) || "ec".equals(transferAccount.getType())) && "tqxj".equals(tradeType) && isCheck;
            return isTQXJAndCheck;
        }
        return false;
    }
}



