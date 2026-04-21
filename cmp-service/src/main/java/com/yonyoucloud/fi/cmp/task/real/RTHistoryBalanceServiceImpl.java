package com.yonyoucloud.fi.cmp.task.real;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.StctBalanceHistoryApiService;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.dto.BalanceHistoryParamDTO;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.vo.BalanceHistoryVO;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class RTHistoryBalanceServiceImpl implements RTHistoryBalanceService {
	@Autowired
	YmsOidGenerator ymsOidGenerator;
	@Autowired
	CurrencyQueryService currencyQueryService;
	private static String YONSUITE_AUTOTASK = "Yonsuite_AutoTask";
	private static String AUTO_PAY_IDEN = "Y";
	private static final Cache<String, CurrencyTenantDTO> currencyCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(1))
			.concurrencyLevel(4)
			.maximumSize(1000)
			.softValues()
			.build();

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private AccountHistoryBalanceService accountHistoryBalanceService;

	@Autowired
	private CtmThreadPoolExecutor executorServicePool;

	@Autowired
	private BankConnectionAdapterContext bankConnectionAdapterContext;
	@Autowired
	BaseRefRpcService baseRefRpcService;
	@Autowired
	StctBalanceHistoryApiService stctBalanceHistoryApiService;
	@Autowired
	private HttpsService httpsService;

	@Resource
	private IApplicationService appService;
	@Autowired
	private CTMCMPBusinessLogService ctmcmpBusinessLogService;

	static final String QUERY_ALL = "3";//所有银行
	static final String QUERY_INNER = "1";//内部银行
	static final String QUERY_OUTER = "2";//外部银行


	/**
	 * 同步银行账户历史余额任务
	 * @param paramMap
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> queryAccountHistoryBalanceTask(CtmJSONObject paramMap) throws Exception {

		String logId = paramMap.get("logId").toString();
		//包装类，用于lambda表达式内存线程结果
		ThreadResult[] wrapperThreadResult = new ThreadResult[1];
		wrapperThreadResult[0] = new ThreadResult();
		StringBuilder accountCountMsg = new StringBuilder();
		executorServicePool.getThreadPoolExecutor().submit(() -> {
			try {
				//根据条件查询结算中心内部户历史余额
				Map<String,List<EnterpriseBankAcctVO>> bankAccountsGroup = accountHistoryBalanceService.getBankAccountsGroupForBalanceByTask(paramMap);

				String innerAccountFlag = paramMap.getString("innerAccountFlag") == null ? "3" : paramMap.getString("innerAccountFlag");

				//内部账户
				List<EnterpriseBankAcctVO> innerBankAccounts = bankAccountsGroup.get("innerAccounts");
				//直联账户
				List<EnterpriseBankAcctVO>  httpBankAccounts = bankAccountsGroup.get("checkSuccess");
				httpBankAccounts = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(paramMap, httpBankAccounts);
				CtmJSONObject logData = new CtmJSONObject();
				logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DC", "账户历史余额调度任务查询账户请求") /* "账户历史余额调度任务查询账户请求" */, paramMap);
				logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D3", "账户历史余额调度任务查询账户返回") /* "账户历史余额调度任务查询账户返回" */, bankAccountsGroup);
				ctmcmpBusinessLogService.saveBusinessLog(logData, "","", IServicecodeConstant.ACCHISBAL, IMsgConstant.QUERY_ACCHISBAL, IMsgConstant.QUERY_ACCHISBAL);
				//计算时间并进行分装
				CtmJSONObject queryDateObej = buidQueryDateForHisBalance(paramMap);
				//获取直连账户查询信息组装
				List<CashHttpBankEnterpriseLinkVo>  httpList = accountHistoryBalanceService.querHttpAccount(httpBankAccounts, queryDateObej, true);
				List<EnterpriseBankAcctVO> mergedAccounts = new ArrayList<>();
				if (innerBankAccounts != null) {mergedAccounts.addAll(innerBankAccounts);}
				if (httpBankAccounts != null) {mergedAccounts.addAll(httpBankAccounts);}
				List<String> httpBankAccountIds = httpBankAccounts.stream().map(httpAcct -> httpAcct.getId().toString()).collect(Collectors.toList());
				List<String> accountIdList = BatchLockGetKeysUtils.<EnterpriseBankAcctVO>batchLockCombineKeys(ICmpConstant.QUERYHISBALANCEKEY, mergedAccounts);
				BankAccountUtil.refreshEnableDate(httpBankAccountIds);
				// 改为批量锁
				CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountIdList,60 * 60L, TimeUnit.SECONDS,(int lockstatus)->{
//				CtmLockTool.executeInOneServiceLock(ICmpConstant.QUERYHISBALANCEKEY,60*60L, TimeUnit.SECONDS,(int lockstatus)->{
					if(lockstatus == LockStatus.GETLOCK_FAIL){
						//加锁失败
						//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS,logId,
						//		InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD","系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */, TaskUtils.UPDATE_TASK_LOG_URL);
						wrapperThreadResult[0].getSucessReturnList().add(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F0006E", "系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */) /* "系统正在对此账户拉取中" */);
						return ;
					}
					//查询银企联历史余额
					if(CollectionUtils.isNotEmpty(innerBankAccounts) && (QUERY_ALL.equals(innerAccountFlag) || QUERY_INNER.equals(innerAccountFlag))){
						//查询内部账户历史余额
						queryHistoryInnerBalanceTask(paramMap,queryDateObej,innerBankAccounts);
						accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DB", "拉取内部账户数：") /* "拉取内部账户数：" */).append(innerBankAccounts.size()).append(";");
					}
					if(CollectionUtils.isNotEmpty(httpList) && (QUERY_ALL.equals(innerAccountFlag) || QUERY_OUTER.equals(innerAccountFlag))){
						//查询直联账户历史余额
						wrapperThreadResult[0] = queryHttpHistoryBalanceTask(paramMap,httpList);
						accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D4", "拉取直联账户数：") /* "拉取直联账户数：" */).append(httpList.size()).append(";");
					}
					//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS,logId, MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */ + ":" + accountCountMsg.toString(), TaskUtils.UPDATE_TASK_LOG_URL);
				});
			} catch (Exception e) {
				wrapperThreadResult[0].getErrorReturnList().add(e.getMessage());
				//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
			}
			ThreadResult threadResult = wrapperThreadResult[0];
			TaskUtils.updateTaskLogbyThreadResult((Map<String,String>)paramMap.get("ipaParams"),logId, accountCountMsg.toString() ,threadResult);
		});
		Map<String,Object> retMap = new HashMap<>();
		retMap.put("asynchronized",true);
		return retMap;
	}

	//private void getHistoryBalance(CtmJSONObject paramMap) throws Exception {
	//	//根据条件查询结算中心内部户历史余额
	//	Map<String,List<EnterpriseBankAcctVO>> bankAccountsGroup = accountHistoryBalanceService.getBankAccountsGroupForBalanceByTask(paramMap);
	//
	//	String directmethod = (String) (Optional.ofNullable(paramMap.get("directmethod")).orElse(""));
	//	//内部账户
	//	List<EnterpriseBankAcctVO> innerBankAccounts = bankAccountsGroup.get("innerAccounts");
	//
	//	//直联账户
	//	List<EnterpriseBankAcctVO>  httpBankAccounts = bankAccountsGroup.get("checkSuccess");
	//
	//	ApplicationVO appVo = appService.findByTenantIdAndApplicationCode(AppContext.getCurrentUser().getYhtTenantId(), "BAM");
	//	if (appVo == null || !appVo.isEnable()) { // TODO,判断账户管理是否启用
	//		log.error("客户环境未安装财资账户管理");
	//	} else {
	//		if (StringUtils.isNotEmpty(directmethod)) {
	//			httpBankAccounts = queryAccountInfo(directmethod,httpBankAccounts);
	//		}
	//	}
	//
	//	//计算时间并进行分装
	//	CtmJSONObject queryDateObej = buidQueryDateForHisBalance(paramMap);
	//	//获取直连账户查询信息组装
	//	List<CashHttpBankEnterpriseLinkVo>  httpList = accountHistoryBalanceService.querHttpAccount(httpBankAccounts, queryDateObej);
	//	if(!innerBankAccounts.isEmpty()){
	//		//查询内部账户历史余额
	//		queryHistoryInnerBalanceTask(paramMap,queryDateObej,innerBankAccounts);
	//	}
	//	if(!httpList.isEmpty()){
	//		//查询直联账户历史余额
	//		queryHttpHistoryBalanceTask(paramMap,httpList);
	//	}
	//}

	private List<EnterpriseBankAcctVO> queryAccountInfo(String directmethod, List<EnterpriseBankAcctVO> allAccountList) throws Exception {
		// 通过现有逻辑查询到的企业银行账号，再去查询账户管理模块对应账户信息查询模块中对应的直联方式进行过滤
		List<EnterpriseBankAcctVO> allAccountListNew = new ArrayList<>();
		String directmethod1;
		if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D7", "银企直联") /* "银企直联" */.equals(directmethod)) {
			directmethod1 = "1";
		} else { // SWIFT直联
			directmethod1 = "3";
		}
		List<String> accountList = allAccountList.stream().map(item -> item.getId()).distinct().collect(Collectors.toList());
		QuerySchema schema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
		schema.appendQueryCondition(QueryCondition.name("directChannel").eq(directmethod1));
		schema.appendQueryCondition(QueryCondition.name("accountId").in(accountList));
		List<Map<String, Object>> accountInfoVOs = MetaDaoHelper.query("yonbip-fi-ctmbam.accountInfo.accinfo", schema, "yonbip-fi-ctmbam");
		if (CollectionUtils.isNotEmpty(accountInfoVOs)) {
			for (Map<String, Object> accountInfoVO : accountInfoVOs) {
				String accountId = (String) accountInfoVO.get("accountId");
				for (EnterpriseBankAcctVO stringObjectMap : allAccountList) {
					if (stringObjectMap.getId().equals(accountId)) {
						allAccountListNew.add(stringObjectMap);
					}
				}
			}
		}

		return allAccountListNew;
	}

	private void queryHistoryInnerBalanceTask(CtmJSONObject paramMap,CtmJSONObject queryDateObej,List<EnterpriseBankAcctVO> innerBankAccounts) throws Exception {
		ExecutorService executorService = null;
		try{
			//构建线程池
			Integer corePoolSize = 0;
			if(paramMap.get("corepoolsize") != null){
				corePoolSize = StringUtils.isNotEmpty(paramMap.get("corepoolsize").toString())?Integer.valueOf(paramMap.get("corepoolsize").toString()):0;
			}
			executorService = buildThreadPoolForHisBalance("innerHisBalanceTask",corePoolSize);
			//时间参数
			List betweendates = (List) queryDateObej.get("betweendate");
			//校验余额日期
			if (betweendates == null) {
				return;
			}
			Date startDate = DateUtils.dateParse(betweendates.get(0).toString(), "yyyyMMdd");
			Date endDate = DateUtils.dateParse(betweendates.get(1).toString(), "yyyyMMdd");

			List<Object> results = ThreadPoolUtil.executeByBatch(executorService,innerBankAccounts,10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D5", "内部账户历史余额调度任务") /* "内部账户历史余额调度任务" */,(int fromIndex, int toIndex)->{
				for(int t = fromIndex ; t < toIndex; t++){
					EnterpriseBankAcctVO  innerAccount= innerBankAccounts.get(t);
					String logId = paramMap.get("logId").toString();
					// 加锁的账号信息
					String accountInfo = innerAccount.getAccount() +"|"+  startDate;
					// 加锁信息：账号+行为
					String lockKey = accountInfo + ICmpConstant.QUERYHISBALANCEKEY;
					try {
						CtmLockTool.executeInOneServiceLock( lockKey,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
							if(lockstatus == LockStatus.GETLOCK_FAIL){
								//加锁失败添加报错信息
								TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D6", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
								return ;
							}
							excuteInnerHisBalanceTask(innerAccount, startDate,endDate);
						});
					} catch (Exception e) {
						log.error("queryHistoryInnerBalanceTask",e);
						//加锁失败添加报错信息
						TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, YQLUtils.getErrorMsqWithAccount(e, accountInfo),
								TaskUtils.UPDATE_TASK_LOG_URL);
					}
				}
				return null;
			});
		}finally {
			if(executorService != null){
				executorService.shutdown();
			}
		}
	}

	private void excuteInnerHisBalanceTask(EnterpriseBankAcctVO innerAccount,Date startDate,Date endDate) throws Exception{
		BalanceHistoryParamDTO dto = new BalanceHistoryParamDTO();
		dto.setSourceAccountId(innerAccount.getId());
		dto.setStartDate(startDate);
		dto.setEndDate(endDate);
		List<String> deleteInnerAccountIds = new ArrayList<>();
		List<String> deleteInnerCurrencyIds = new ArrayList<>();
		List<BalanceHistoryVO> list = stctBalanceHistoryApiService.queryBalanceHistoryList(dto);
		if(list.isEmpty()){
			return;
		}
		List<AccountRealtimeBalance> insertList = new ArrayList();
		List<AccountRealtimeBalance> deleteList = new ArrayList();
		for (BalanceHistoryVO balanceHistoryVO : list) {
			// 根据余额四要素，删除重复历史余额数据
			deleteInnerAccountIds.add(innerAccount.getId());
			deleteInnerCurrencyIds.add(balanceHistoryVO.getCurrency());

			AccountRealtimeBalance balance = new AccountRealtimeBalance();
			balance.setAccentity(innerAccount.getOrgid());
			balance.setBalancedate(balanceHistoryVO.getSettlementDate());
			balance.setTenant(AppContext.getTenantId());
			// 账户余额
			balance.setAcctbal(balanceHistoryVO.getCurrentBalance() == null ? BigDecimal.ZERO : balanceHistoryVO.getCurrentBalance());
			if (balanceHistoryVO.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
				balance.setDepositbalance(balanceHistoryVO.getAvailableBalance().abs());
				balance.setOverdraftbalance(BigDecimal.ZERO);
				balance.setAvlbal(balanceHistoryVO.getAvailableBalance().abs());
			} else if (balanceHistoryVO.getAvailableBalance().compareTo(BigDecimal.ZERO) < 0) {
				balance.setDepositbalance(BigDecimal.ZERO);
				balance.setOverdraftbalance(balanceHistoryVO.getAvailableBalance().abs());
				balance.setAvlbal(balanceHistoryVO.getAvailableBalance().abs());
			} else {
				balance.setDepositbalance(BigDecimal.ZERO);
				balance.setOverdraftbalance(BigDecimal.ZERO);
				balance.setAvlbal(BigDecimal.ZERO);
			}
			// 冻结金额&可用余额
			balance.setFrzbal(BigDecimal.ZERO);
//			balance.setAvlbal(balance.getAcctbal());
			balance.setFlag(BalanceFlag.AutoPull.getCode());
			//加入合计金额
			balance.setTotal_amt(BigDecimalUtils.safeAdd(balance.getAcctbal(),balance.getRegular_amt()));
			balance.setEntityStatus(EntityStatus.Insert);
			balance.setCreateTime(new Date());
			balance.setCreateDate(DateUtils.getNowDateShort2());
			balance.setCreator(AppContext.getCurrentUser().getName());//新增人名称
			balance.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
			balance.setId(ymsOidGenerator.nextId());
			balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
			balance.setEnterpriseBankAccount(innerAccount.getId());
			balance.setBanktype(innerAccount.getBank());
			balance.setCurrency(balanceHistoryVO.getCurrency());
			insertList.add(balance);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		deleteList.addAll(accountHistoryBalanceService.deleteAccountBalanceList(innerAccount.getOrgid(), deleteInnerCurrencyIds, deleteInnerAccountIds, sdf.format(startDate),sdf.format(endDate)));
		if(deleteList != null && deleteList.size() > 0 ){
			MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, deleteList);
		}
		if (insertList != null && insertList.size() > 0) {
			CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, insertList);
		}
	}

	private ThreadResult queryHttpHistoryBalanceTask(CtmJSONObject paramMap,List<CashHttpBankEnterpriseLinkVo> httpBankAccounts)throws Exception {
		//构建线程池
		Integer corePoolSize = paramMap.get("corepoolsize")!=null&&paramMap.get("corepoolsize")!=""?Integer.valueOf(paramMap.get("corepoolsize").toString()):0;
		ExecutorService executorService = buildThreadPoolForHisBalance("httpHisBalanceTask",corePoolSize);
		ThreadResult threadResult = ThreadPoolUtil.executeByBatchCollectResultsNoSemaphore(executorService,httpBankAccounts,10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D8", "直联账户历史余额调度任务") /* "直联账户历史余额调度任务" */, true, (int fromIndex, int toIndex)->{
			ThreadResult subThreadResult = new ThreadResult();
			for(int t = fromIndex ; t < toIndex; t++){
				CashHttpBankEnterpriseLinkVo httpAcct = httpBankAccounts.get(t);
				String logId = paramMap.get("logId").toString();
				// 加锁的账号信息
				String accountInfo = httpAcct.getAcct_no() + "|" + httpAcct.getBeg_date() + "|"+ httpAcct.getCurr_code();;
				// 加锁信息：账号+行为
				String lockKey = accountInfo + ICmpConstant.QUERYHISBALANCEKEY;
				try {
					CtmLockTool.executeInOneServiceLock( lockKey,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
						if(lockstatus == LockStatus.GETLOCK_FAIL){
							//加锁失败添加报错信息
							TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D6", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
							return ;
						}
						String noDataMsg = excuteHttpHisBalanceTask(httpAcct);
						if (noDataMsg != null) {
							String noDataSuccessMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005DA", "[%s]:此账户返回[%s]") /* "[%s]:此账户返回[%s]" */,accountInfo, noDataMsg);
							subThreadResult.getSucessReturnList().add(noDataSuccessMessage);
						}
					});
				} catch (Exception e) {
					log.error("queryHttpHistoryBalanceTask",e);
					////加锁失败添加报错信息
					//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId, YQLUtils.getErrorMsqWithAccount(e, accountInfo),
					//		TaskUtils.UPDATE_TASK_LOG_URL);
					String errorMessage = YQLUtils.getErrorMsqWithAccount(e, accountInfo);
					subThreadResult.getErrorReturnList().add(errorMessage);
				}
			}
			return subThreadResult;
		});
		return threadResult;
	}

	private String excuteHttpHisBalanceTask(CashHttpBankEnterpriseLinkVo httpAcct) throws Exception{
		CtmJSONObject queryBalanceMsg = new CtmJSONObject();
		CtmJSONObject result = new CtmJSONObject();
		try{
			queryBalanceMsg = accountHistoryBalanceService.buildQueryHistoryBalanceMsg(httpAcct);//组装查询参数
			log.error("历史余额task请求参数==========================》：" + queryBalanceMsg);
			String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
			List<BasicNameValuePair> requestData = new ArrayList<>();
			requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
			requestData.add(new BasicNameValuePair("reqSignData", signMsg));
			result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
			log.error("历史余额task响应参数==========================》：" + result);
			if (result.getInteger("code") == 1) {
				CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
				String service_resp_code = responseHead.getString("service_resp_code");
				if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE,service_resp_code)) {
					CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
					//保存账户历史余额
					accountHistoryBalanceService.insertAccountHistoryBalanceData(httpAcct, responseBody, null);
				} else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
					String noDataMsg = YQLUtils.getYQLNoDataMsq(responseHead);
					return noDataMsg;
					//没有数据不报错
				} else {
					throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead));
				}
			} else {
				//调度任务时，抛出银企联请求异常给上层收集后展示
				throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005D9", "请求银企联报错：请求参数 = %s,响应参数 = %s") /* "请求银企联报错：请求参数 = %s,响应参数 = %s" */,queryBalanceMsg, result));
			}
		}catch (Exception e){
			log.error("excuteHttpHisBalanceTask错误，请求参数 = {},响应参数 = {},报错信息={}", queryBalanceMsg.toString(),result.toString(),e);
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105069"), e.getMessage());
		}
		return null;
	}

	/**
	 * 封装相关时间参数
	 * @param paramMap
	 * @return
	 * @throws Exception
	 */
	private CtmJSONObject  buidQueryDateForHisBalance(CtmJSONObject paramMap) throws Exception {
		String beg_date = null;
		String end_date = null;
		HashMap<String, String> queryData = TaskUtils.queryDateProcess(paramMap, null);
		Date tody = DateUtils.getNowDateShort2();
		String yesTody = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1), DateUtils.YYYYMMDD);
		if (queryData.isEmpty()) {
			beg_date = yesTody;
			end_date = yesTody;
		//开始日期大于当前日期，则获取数据为空 此时为了不让程序报错 默认返回前一天
		} else if (queryData.containsKey(TaskUtils.TASK_NO_DATA)) {
			beg_date = yesTody;
			end_date = yesTody;
		} else {
			beg_date = queryData.get(TaskUtils.TASK_START_DATE);
			end_date = queryData.get(TaskUtils.TASK_END_DATE);
			//开始日期晚于等于今天，设置开始日期为昨天
			if (DateUtils.dateCompare(DateUtils.convertToDate(beg_date, DateUtils.YYYYMMDD), tody) >= 0) {
				beg_date = yesTody;
			}
			//结束日期晚于等于今天，设置结束日期为昨天
			if (DateUtils.dateCompare(DateUtils.convertToDate(end_date, DateUtils.YYYYMMDD), tody) >= 0) {
				end_date = yesTody;
			}
		}
		List betweendates = new ArrayList();
		betweendates.add(beg_date);
		betweendates.add(end_date);
		CtmJSONObject result = new CtmJSONObject();
		result.put("betweendate",betweendates);
		return result;
	}

	/**
	 * 构建历史余额查询线程池
	 * @param threadPollName
	 * @param corePoolSize
	 * @return
	 */
	private ExecutorService buildThreadPoolForHisBalance(String threadPollName,Integer corePoolSize){
		String threadParam = "";
		if (threadPollName.equals("innerHisBalanceTask")) {
			// 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
			threadParam = AppContext.getEnvConfig("cmp.innerHisBalanceTask.thread.param","8,32,1000,cmp-innerHisBalanceTask-async-");
		} else if (threadPollName.equals("httpHisBalanceTask")) {
			// 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
			threadParam = AppContext.getEnvConfig("cmp.httpHisBalanceTask.thread.param","8,32,1000,cmp-httpHisBalanceTask-async-");
		}
		String[] threadParamArray = threadParam.split(",");
		if (corePoolSize == null) {
			corePoolSize = Integer.parseInt(threadParamArray[0]);;
		}
		int maxPoolSize = Integer.parseInt(threadParamArray[1]);
		int queueSize = Integer.parseInt(threadParamArray[2]);
		String threadNamePrefix = threadParamArray[3];
		ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
				.setDaemon(false)
				.setRejectHandler(new BlockPolicy())
				.builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
		return executorService;
	}



}
