package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.balanceadjust.CtmCmpBalanceAdjustRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BalanceAdjustRetVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.balanceadjust.BalanceAdjustQueryVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.balanceadjust.BalanceAdjustResultVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.BalanceAdjustVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.RpcResponseCode;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 银企联账户提供查询银行账户银企联启用状态的接口*
 *
 * @author Administrator
 * @date 2024/4/28 10:25
 */
@Slf4j
@Service
public class CtmCmpBalanceAdjustRpcServiceImpl implements CtmCmpBalanceAdjustRpcService {

    private static String BALANCEADJUSTRESULT = "com.yonyoucloud.fi.cmp.mapper.BalanceAdjustResultMapper.";

    @Override
    public BalanceAdjustRetVo filterBalanceAdjustPage(BalanceAdjustVo balanceAdjustVo) {
        BalanceAdjustRetVo balanceAdjustRetVo = checkParam(Collections.singletonList(balanceAdjustVo));
        if (RpcResponseCode.PARAM_ERROR.equals(balanceAdjustRetVo.getCode())) {
            return balanceAdjustRetVo;
        }
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(balanceAdjustVo.getBankaccount()));
        conditionGroup.addCondition(QueryCondition.name("currency").eq(balanceAdjustVo.getCurrency()));
        String beginDateStr = balanceAdjustVo.getBegindate() + " 00:00:00";
        String endDateStr = balanceAdjustVo.getEnddate() + " 23:59:59";
        conditionGroup.addCondition(QueryCondition.name("dzdate").between(beginDateStr, endDateStr));//对账截至日期
        if (!StringUtils.isEmpty(balanceAdjustVo.getAuditstatus())) {
            conditionGroup.addCondition(QueryCondition.name("auditstatus").eq(balanceAdjustVo.getAuditstatus()));
        }
        schema.addCondition(conditionGroup);
        try {
            Pager pager = MetaDaoHelper.queryByPage(BalanceAdjustResult.ENTITY_NAME, schema, null);
            balanceAdjustRetVo.setCode(RpcResponseCode.SUCCESS);
            balanceAdjustRetVo.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400683", "调用成功!!") /* "调用成功!!" */);
            balanceAdjustRetVo.setData(pager.getRecordList());
            balanceAdjustRetVo.setPageNum(pager.getPageCount());
            balanceAdjustRetVo.setTotalCount(pager.getRecordCount());
        } catch (Exception e) {
            log.error("com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBalanceAdjustRpcServiceImpl.filterBalanceAdjustPage," + e.getMessage());
            balanceAdjustRetVo.setMessage(e.getMessage());
            balanceAdjustRetVo.setCode(RpcResponseCode.PARAM_ERROR);
        }

        return balanceAdjustRetVo;
    }

    @Override
    public BalanceAdjustRetVo filterListBalanceAdjustPage(List<BalanceAdjustVo> balanceAdjustVos) {

        BalanceAdjustRetVo balanceAdjustRetVo = checkParam(balanceAdjustVos);
        if (RpcResponseCode.PARAM_ERROR.equals(balanceAdjustRetVo.getCode())) {
            return balanceAdjustRetVo;
        }
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup orConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        for (BalanceAdjustVo balanceAdjustVo : balanceAdjustVos) {
            QueryConditionGroup andConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            andConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(balanceAdjustVo.getBankaccount()));
            andConditionGroup.addCondition(QueryCondition.name("currency").eq(balanceAdjustVo.getCurrency()));
            String beginDateStr = balanceAdjustVo.getBegindate() + " 00:00:00";
            String endDateStr = balanceAdjustVo.getEnddate() + " 23:59:59";
            andConditionGroup.addCondition(QueryCondition.name("dzdate").between(beginDateStr, endDateStr));//对账截至日期
            if (!StringUtils.isEmpty(balanceAdjustVo.getAuditstatus())) {
                andConditionGroup.addCondition(QueryCondition.name("auditstatus").eq(balanceAdjustVo.getAuditstatus()));
            }
            orConditionGroup.addCondition(andConditionGroup);
        }
        schema.addCondition(orConditionGroup);
        try {
            Pager pager = MetaDaoHelper.queryByPage(BalanceAdjustResult.ENTITY_NAME, schema, null);
            balanceAdjustRetVo.setCode(RpcResponseCode.SUCCESS);
            balanceAdjustRetVo.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400683", "调用成功!!") /* "调用成功!!" */);
            balanceAdjustRetVo.setData(pager.getRecordList());
            balanceAdjustRetVo.setPageNum(pager.getPageCount());
            balanceAdjustRetVo.setTotalCount(pager.getRecordCount());
        } catch (Exception e) {
            log.error("com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBalanceAdjustRpcServiceImpl.filterBalanceAdjustPage," + e.getMessage());
            balanceAdjustRetVo.setMessage(e.getMessage());
            balanceAdjustRetVo.setCode(RpcResponseCode.PARAM_ERROR);
        }

        return balanceAdjustRetVo;
    }

    @Override
    public List<BalanceAdjustResultVO> queryBalanceAdjustResult(BalanceAdjustQueryVO balanceAdjustQueryVO) throws Exception {
        log.error("queryBalanceAdjustResult.BalanceAdjustQueryVO:" + balanceAdjustQueryVO.toString());
        List<BalanceAdjustResultVO> balanceAdjustResultVOList = new ArrayList<>();
        balanceAdjustQueryVO.setYtenant_id(AppContext.getYTenantId());
        //1 根据条件查询对应的对账方案
        List<Map<String, Object>> bankreconciliationscheme = queryBankreconciliationscheme(balanceAdjustQueryVO);
        if(CollectionUtils.isEmpty(bankreconciliationscheme)){
            return balanceAdjustResultVOList;
        }
        //2 根据对账方案 + 对账截止日 筛选符合条件的余额调节表数据并进行分组(由于存在不同对账方案下 方案子表账户币种账簿一致的情况 余额调节表 需要按对账方案分组)
        List<BalanceAdjustResult> balanceList = queryBalanceList(balanceAdjustQueryVO,bankreconciliationscheme);
        //按对账方案 + 账户 + 币种进行数据分组
        Map<String, List<BalanceAdjustResult>> balanceAdjustMap = balanceList.stream()
                .collect(Collectors.groupingBy(balanceAdjust ->
                        balanceAdjust.getBankreconciliationscheme() + balanceAdjust.getBankaccount() + balanceAdjust.getCurrency()));

        if(CollectionUtils.isEmpty(balanceList)){
            //如果仅有对账方案 但没有对应的余额表信息 需要拼接数据返回
            for(Map<String, Object> thisSchemeMap : bankreconciliationscheme){
                BalanceAdjustResultVO resultVO = new BalanceAdjustResultVO();
                buildBalanceAdjustResult(resultVO,null,thisSchemeMap);
                balanceAdjustResultVOList.add(resultVO);
            }
            return balanceAdjustResultVOList;
        }
        //3 组装返回数据 通过余额调节表上的币种+账户 可以对应找到对账方案子表的对应数据
        buildResult(bankreconciliationscheme,balanceAdjustMap,balanceAdjustResultVOList);
        //过滤重复数据
        List<BalanceAdjustResultVO> distinctList = balanceAdjustResultVOList.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new java.util.LinkedHashSet<>()),
                        java.util.ArrayList::new
                ));
        return distinctList;
    }

    private List<Map<String, Object>> queryBankreconciliationscheme(BalanceAdjustQueryVO balanceAdjustQueryVO){
        List<Map<String, Object>> bankreconciliationscheme = SqlHelper.selectList(BALANCEADJUSTRESULT + "queryBankreconciliationscheme", balanceAdjustQueryVO);
        return bankreconciliationscheme;
    }

    private List<BalanceAdjustResult> queryBalanceList(BalanceAdjustQueryVO balanceAdjustQueryVO,List<Map<String, Object>> bankreconciliationscheme) throws Exception {
        Set<String> schemeIdSet = new HashSet<>();
        for(Map<String, Object> map : bankreconciliationscheme){
            schemeIdSet.add(map.get(ICmpConstant.ID).toString());
        }
        QuerySchema balanceSchema = new QuerySchema().addSelect("*");
        QueryConditionGroup condition_balance = new QueryConditionGroup();
        condition_balance.addCondition(QueryConditionGroup.and(QueryCondition.name("bankreconciliationscheme").in(schemeIdSet)));
        if(balanceAdjustQueryVO.getDzdate()!=null){ //大于等于 对账截止日期
            condition_balance.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(balanceAdjustQueryVO.getDzdate())));
        }
        balanceSchema.addCondition(condition_balance);
        List<BalanceAdjustResult> balanceList = MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, balanceSchema,null);
        return balanceList;
    }

    private void buildResult(List<Map<String, Object>> bankreconciliationscheme,Map<String, List<BalanceAdjustResult>> balanceAdjustMap,List<BalanceAdjustResultVO> balanceAdjustResultVOList) throws Exception {
        for(Map<String, Object> thisSchemeMap : bankreconciliationscheme){
            String schemeKey = thisSchemeMap.get(ICmpConstant.ID).toString() + thisSchemeMap.get(ICmpConstant.BANK_ACCOUNT).toString() +  thisSchemeMap.get(ICmpConstant.CURRENCY).toString();
            List<BalanceAdjustResult> balanceAdjustList = balanceAdjustMap.get(schemeKey);
            if(balanceAdjustList!=null){
                Boolean thisSchemeFlag = false;
                for(BalanceAdjustResult balanceAdjust : balanceAdjustList){
                    BalanceAdjustResultVO resultVO = new BalanceAdjustResultVO();
                    if(balanceAdjust.getBankaccount().equals(thisSchemeMap.get(ICmpConstant.BANK_ACCOUNT)) && balanceAdjust.getCurrency().equals(thisSchemeMap.get(ICmpConstant.CURRENCY))){
                        buildBalanceAdjustResult(resultVO,balanceAdjust,thisSchemeMap);
                        balanceAdjustResultVOList.add(resultVO);
                        thisSchemeFlag = true;
                    }
                }
                //如果循环匹配的余额表数据后 没有匹配的数据 需要将此条对账方案组装返回
                if(!thisSchemeFlag){
                    //如果没有匹配的余额表数据 需要返回对应的对账方案信息
                    BalanceAdjustResultVO resultVO = new BalanceAdjustResultVO();
                    buildBalanceAdjustResult(resultVO,null,thisSchemeMap);
                    balanceAdjustResultVOList.add(resultVO);
                }
            }else{
                //如果没有匹配的余额表数据 需要返回对应的对账方案信息
                BalanceAdjustResultVO resultVO = new BalanceAdjustResultVO();
                buildBalanceAdjustResult(resultVO,null,thisSchemeMap);
                balanceAdjustResultVOList.add(resultVO);
            }
        }
    }

    private void buildBalanceAdjustResult(BalanceAdjustResultVO resultVO,BalanceAdjustResult balanceAdjust,Map<String, Object> thisSchemeMap) throws Exception {
        resultVO.setAccentity(thisSchemeMap.get("accentity").toString());
        resultVO.setReconciliationSettingId(thisSchemeMap.get("id").toString());
        resultVO.setBankreconciliationschemecode(thisSchemeMap.get("bankreconciliationschemecode").toString());
        resultVO.setReconciliationdatasource(Short.valueOf(thisSchemeMap.get("reconciliationdatasource").toString()));
        resultVO.setEnableDate(thisSchemeMap.get("enableDate")!=null?DateUtils.parseDate(thisSchemeMap.get("enableDate").toString()):null);
        resultVO.setStopDate(thisSchemeMap.get("stopDate")!=null?DateUtils.parseDate(thisSchemeMap.get("stopDate").toString()):null);
        resultVO.setEnableStatus_b(Short.valueOf(thisSchemeMap.get("enableStatus_b").toString()));
        resultVO.setBankaccount(thisSchemeMap.get("bankaccount").toString());
        resultVO.setCurrency(thisSchemeMap.get("currency").toString());
        resultVO.setUseorg(thisSchemeMap.get("useorg").toString());
        resultVO.setAccentityRaw(thisSchemeMap.get("accentityRaw").toString());
        resultVO.setAccbook_b(thisSchemeMap.get("accbook_b").toString());
        if(balanceAdjust!=null){
            resultVO.setBalanceadjustresultId(balanceAdjust.getId().toString());
            resultVO.setBankdate(balanceAdjust.getBankdate());
            resultVO.setAuditstatus(balanceAdjust.getAuditstatus());
            resultVO.setOperator(balanceAdjust.getOperator());
            resultVO.setCreator(balanceAdjust.getCreator());
            resultVO.setArchivingstatus(balanceAdjust.getArchivingstatus());
            resultVO.setBalenceState(balanceAdjust.getBalenceState());
            resultVO.setBankye(balanceAdjust.getBankye());
            resultVO.setBankqyyf(balanceAdjust.getBankqyyf());
            resultVO.setBankqyys(balanceAdjust.getBankqyys());
            resultVO.setBanktzye(balanceAdjust.getBanktzye());
            resultVO.setJournalye(balanceAdjust.getJournalye());
            resultVO.setJournalyhyf(balanceAdjust.getJournalyhyf());
            resultVO.setJournalyhys(balanceAdjust.getJournalyhys());
            resultVO.setJournaltzye(balanceAdjust.getJournaltzye());
            resultVO.setUncheckflag(balanceAdjust.getUncheckflag());
        }

    }

    /**
     * checkParam
     */
    private BalanceAdjustRetVo checkParam(List<BalanceAdjustVo> balanceAdjustVoList) {
        BalanceAdjustRetVo balanceAdjustRetVo = new BalanceAdjustRetVo();
        balanceAdjustRetVo.setData(null);
        try {
            if (CollectionUtils.isEmpty(balanceAdjustVoList)) {
                balanceAdjustRetVo.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400682", "参数不能为空") /* "参数不能为空" */);
                balanceAdjustRetVo.setCode(RpcResponseCode.PARAM_ERROR);
                return balanceAdjustRetVo;
            }
            for (BalanceAdjustVo balanceAdjustVo : balanceAdjustVoList) {
                if (DateUtils.dateCompare(DateUtils.parseDate(balanceAdjustVo.getBegindate()), DateUtils.parseDate(balanceAdjustVo.getEnddate())) >= 1) {
                    balanceAdjustRetVo.setMessage(balanceAdjustVo.getEnddate() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400684", "截止日期（结束），需晚于等于截止日期（开始）") /* "截止日期（结束），需晚于等于截止日期（开始）" */ + balanceAdjustVo.getBegindate());
                    balanceAdjustRetVo.setCode(RpcResponseCode.PARAM_ERROR);
                    return balanceAdjustRetVo;
                }
            }

        } catch (Exception e) {
            log.error("时间转换异常!!");
            balanceAdjustRetVo.setMessage(e.getMessage());
            balanceAdjustRetVo.setCode(RpcResponseCode.PARAM_ERROR);
            return balanceAdjustRetVo;
        }
        return balanceAdjustRetVo;
    }
}
