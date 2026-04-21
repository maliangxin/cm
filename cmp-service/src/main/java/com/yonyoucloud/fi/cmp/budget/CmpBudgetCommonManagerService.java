package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.epmp.control.dto.ControlDetailVO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface CmpBudgetCommonManagerService {
    /**
     * 签名字段不传，且包含特殊字符可能被预算中台拦截
     */
    String SIGNATURE = "signature";
    /**
     * 服务编码
     */
    String SERVICE_CODE = "serviceCode";
    /**
     * 单据id(整单id)
     */
    String BILL_ID = "billId";
    /**
     * 子表唯一标志
     * 本参数为String类型，值为单据行id，为必传参数，当所传单据中无子单据仅有主单据时，值应与billId相同
     */
    String BILL_LINE_ID = "billLineId";
    /**
     * 单据编号(code)
     */
    String BILL_NO = "billNo";
    /**
     * 单据类型唯一标识
     */
    String BILL_CODE = "billCode";

    /**
     * 单据动作 not required
     */
    String ACTION = "action";

    /**
     * 交易类型唯一标识
     */
    String TRANSAC_CODE = "transacCode";
    /**
     * 1-删除,0-新增
     */
    String ADD_OR_REDUCE = "addOrReduce";
    /**
     * 单据id
     */
    String LINE_NO = "lineNo";
    /**
     * 是否冲抵（逆向）单据，true为是冲抵（逆向）单据，false为不是冲抵（逆向）单据，默认为false
     */
    String IS_OFFSET = "isOffset";
    /**
     * 预占--0,实占--1
     */
    String PREEMPTION_OR_EXECFLAG = "preemptionOrExecFlag";

    String RULE_ID = "ruleId";

    String RULECTRL_ID = "ruleCtrlId";

    /**
     * 规则类型(预占pre；执行implement)
     */
    String RULE_TYPE = "ruleType";
    /**
     * 单据编号
     */
    String BILL_BUS_CODE = "billBusCode";

    /**
     * 单据行号 not required
     */
    String LINE = "line";
    /**
     * 开始时间
     */
    String START_DATE = "startDate";
    /**
     * 结束时间
     */
    String END_DATE = "endDate";
    String VERIFYSTATE = "verifystate";
    String ISWFCONTROLLED = "isWfControlled";
    String SETTLESTATUS = "settlestatus";
    String CHARACTERDEF_ = "characterDef_";
    String CHARACTERDEF = "characterDef.";
    String CHARACTERDEFB_ = "characterDefb_";
    String CHARACTERDEFB = "characterDefb.";
    // 预占
    String PRE = "pre";
    // 实占
    String IMPLEMENT = "implement";

    /**
     * 报文拼接
     *
     * @param bizObject
     * @param addOrReduce
     * @param billCode
     */
    Map<String, Object> buildBudgetParam(BizObject bizObject, int addOrReduce, String preemptionOrExecFlag, String billAction, String billCode, String serviceCode) throws Exception;

    /**
     * * 联查控制明细查询
     *
     * @param
     * @return
     */
    String budgetCheckNew(List<BizObject> bizObjects, String billCode, String billAction);

    /**
     * 查询预算
     *
     * @param param
     * @return
     */
    String queryBudgetDetail(CtmJSONObject param);

    /**
     * 预占
     * cancel false
     * 释放预占
     * cancel true
     *
     * @param bizObject
     * @param billCode
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget budget(BizObject bizObject, String billAction, String billCode, String serviceCode, boolean cancel) throws Exception;


    /**
     * cancel false 删除预占，执行实占
     * cancel true 删除实占
     *
     * @param bizObject
     * @param billCode
     * @return
     */
    ResultBudget implement(BizObject bizObject, String billCode, String serviceCode, boolean cancel) throws Exception;


    /**
     *
     * 直接实占
     *
     * @param bizObject
     * @param billCode
     * @return
     */
    ResultBudget implementOnly(BizObject bizObject, String billCode, String serviceCode) throws Exception;


//    ControlDetailVO initControlDetailVO(Map[] bills, String action, int operateFlag) throws Exception;

    /**
     * 初始化预算控制明细
     * @param bill
     * @param action
     * @param operateFlag
     * @return
     * @throws Exception
     */
    ControlDetailVO initControlDetailVO(Map bill, String action, int operateFlag) throws Exception;

    /**
     * 批量初始化预算控制明细
     * @param bills
     * @param action
     * @param operateFlag
     * @return
     * @throws Exception
     */
    ControlDetailVO[] initControlDetailVOs(Map[] bills, String action, int operateFlag) throws Exception;


    String doCheckResult(CtmJSONObject resultBack, CtmJSONObject result);

    CtmJSONObject toJsonObj(Map<String, Object> map);

    void doMatchInfos(CtmJSONArray matchInfos);

    ResultBudget doResult(CtmJSONObject result);
    ResultBudget doResultSoft(CtmJSONObject result);
}
