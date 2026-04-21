package com.yonyoucloud.fi.cmp.task.real;


import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import cn.hutool.core.thread.BlockPolicy;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class RTBalanceServiceImpl implements RTBalanceService {

	@Autowired
	private PaymentService paymentService;
	@Autowired
	YmsOidGenerator ymsOidGenerator;
	@Autowired
	private AccountRealtimeBalanceService accountRealtimeBalanceService;
	@Autowired
	private BankConnectionAdapterContext bankConnectionAdapterContext;
	@Autowired
	private CtmThreadPoolExecutor executorServicePool;

	@Autowired
	BaseRefRpcService baseRefRpcService;

	@Resource
	private IApplicationService appService;

	///调度任务线程池 不同的任务在大数据量下 需要不同的线程池参数 及配置
	private ExecutorService taskExecutor;
	static final String QUERY_ALL = "3";//所有银行
	static final String QUERY_INNER = "1";//内部银行
	static final String QUERY_OUTER = "2";//外部银行

	public static final String INNER_ACCOUNTS = "innerAccounts";
	public static final String NET_ACCOUNTS_TO_HTTP = "netAccountsToHttp";

	@Autowired
	private HttpsService httpsService;
	static ExecutorService executorService = null;
	static {
		int maxSize = Integer.parseInt(AppContext.getEnvConfig("cmp.httpBalanceTask.max.poolSize","10"));
		int queueLength = Integer.parseInt(AppContext.getEnvConfig("cmp.httpBalanceTask.queueCapacity","2000"));
		int coreSize = 10;
		String threadName = "cmp-httpBalanceTask-async-";
		executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
		.setDaemon(false)
		.setRejectHandler(new BlockPolicy())
		.builder(coreSize,maxSize,queueLength,threadName);
	}

	/**
	 * 查询银行账户实时余额调度任务
	 * @param paramMap
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String,Object> queryAccountBalanceTask(Map<String,Object> paramMap) throws Exception {
		//传一个tenant_id 进来 防止id变化
		String tenant_id = AppContext.getTenantId().toString();
		String logId = paramMap.get("logId").toString();
		//线程数默认为1
		Integer corePoolSize = paramMap.get("corepoolsize")!=null&&paramMap.get("corepoolsize")!=""?Integer.valueOf(paramMap.get("corepoolsize").toString()):1;
		ExecutorService executorService = initTaskExecutor(corePoolSize);
		String innerAccountFlag = paramMap.get("innerAccountFlag") == null ? "3" : paramMap.get("innerAccountFlag").toString();

		try {
			executorService.submit(() -> {
				ThreadResult threadResult = new ThreadResult();
				StringBuilder accountCountMsg = new StringBuilder();
				try {
					//查询直联账户、内部账户、客户号等信息
					Map<String, Object> groupAcct = queryBankAcctVOsGroup(paramMap);
					List<EnterpriseBankAcctVO> innerAccounts = (List<EnterpriseBankAcctVO>) groupAcct.get("innerAccounts");
					//直连账户
					String directmethod = (String) (Optional.ofNullable(paramMap.get("directmethod")).orElse(""));
					List<EnterpriseBankAcctVO> netAccountsToHttp = (List<EnterpriseBankAcctVO>) groupAcct.get("netAccountsToHttp");
					netAccountsToHttp = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(paramMap, netAccountsToHttp);

					//ApplicationVO appVo = appService.findByTenantIdAndApplicationCode(AppContext.getCurrentUser().getYhtTenantId(), "BAM");
					//if (appVo == null || !appVo.isEnable()) { // TODO,判断账户管理是否启用
					//	log.error("客户环境未安装财资账户管理");
					//} else {
					//	if (StringUtils.isNotEmpty(directmethod)) {
					//		netAccountsToHttp = queryAccountInfo(directmethod, netAccountsToHttp);
					//	}
					//}
					accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046F", "拉取账户总数：") /* "拉取账户总数：" */).append(netAccountsToHttp.size() + innerAccounts.size()).append(";");
					//查询银企联实时余额
					if (CollectionUtils.isNotEmpty(netAccountsToHttp) && (QUERY_ALL.equals(innerAccountFlag) || QUERY_OUTER.equals(innerAccountFlag))) {
						threadResult = getRealBalance((String) groupAcct.get("customNo"), corePoolSize, netAccountsToHttp,paramMap.get("logId").toString());
						accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400473", "拉取直联账户数：") /* "拉取直联账户数：" */).append(netAccountsToHttp.size()).append(";");
					}
					//查询内部账户实时余额
					if (CollectionUtils.isNotEmpty(innerAccounts) && (QUERY_ALL.equals(innerAccountFlag) || QUERY_INNER.equals(innerAccountFlag)) ) {
						getInnerBalance(paramMap,tenant_id, innerAccounts,paramMap.get("logId").toString());
						accountCountMsg.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400476", "拉取内部账户数：") /* "拉取内部账户数：" */).append(innerAccounts.size()).append(";");
					}
					//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */ + ":" + accountCountMsg.toString(), TaskUtils.UPDATE_TASK_LOG_URL);
				} catch (Exception e) {
					threadResult.getErrorReturnList().add(e.getMessage());
					//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
					log.error("queryAccountBalanceTask exception when batch process executorServicePool", e);
				}
				TaskUtils.updateTaskLogbyThreadResult((Map<String,String>)paramMap.get("ipaParams"),logId, accountCountMsg.toString() ,threadResult);
			});
		} finally {
			if (executorService != null) {
				executorService.shutdown();
			}
		}
		Map<String,Object> retMap = new HashMap<>();
		retMap.put("asynchronized",true);
		return retMap;
	}

	private List<EnterpriseBankAcctVO> queryAccountInfo(String directmethod, List<EnterpriseBankAcctVO> allAccountList) throws Exception {
		// 通过现有逻辑查询到的企业银行账号，再去查询账户管理模块对应账户信息查询模块中对应的直联方式进行过滤
		List<EnterpriseBankAcctVO> allAccountListNew = new ArrayList<>();
		if (allAccountList.size() < 1) {
			return allAccountListNew;
		}
		String directmethod1;
		if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400474", "银企直联") /* "银企直联" */.equals(directmethod)) {
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

	private Map<String , Object>  queryBankAcctVOsGroup(Map<String,Object> paramMap) throws Exception {
		String accentitys = (String) (Optional.ofNullable(paramMap.get("accentity")).orElse(""));
		String banktypes = (String) (Optional.ofNullable(paramMap.get("banktype")).orElse(""));
		String currencys = (String) (Optional.ofNullable(paramMap.get("currency")).orElse(""));
		String bankaccounts = (String) (Optional.ofNullable(paramMap.get("bankaccount")).orElse(""));
		CtmJSONObject queryParam = new CtmJSONObject();
		if (!StringUtils.isEmpty(accentitys)) {
			queryParam.put("accEntity",Arrays.asList(accentitys.split(";")));
		}
		if (!StringUtils.isEmpty(banktypes)) {
			queryParam.put("banktypeList",Arrays.asList(banktypes.split(";")));
		}
		if (!StringUtils.isEmpty(currencys)) {
			queryParam.put("currencyList",Arrays.asList(currencys.split(";")));
		}
		String[] bankaccountArr = null;
		if (!StringUtils.isEmpty(bankaccounts)) {
			bankaccountArr = bankaccounts.split(";");
		}
		queryParam.put("enterpriseBankAccount", bankaccountArr);

		List<EnterpriseBankAcctVO> bankAccounts = accountRealtimeBalanceService.queryEnterpriseBankAccountByCondition(queryParam);
		//做完整初始化，防止空指针报错
		Map<String, Object> bankAcctVOsGroupMap = new HashMap<>();
		bankAcctVOsGroupMap.put(INNER_ACCOUNTS,new ArrayList<>());
		bankAcctVOsGroupMap.put(NET_ACCOUNTS_TO_HTTP,new ArrayList<>());
		if (CollectionUtils.isNotEmpty(bankAccounts)) {
			bankAcctVOsGroupMap= accountRealtimeBalanceService.getBankAcctVOsGroupByTask(bankAccounts);
		}
		return bankAcctVOsGroupMap;
	}

	/**
	 * 获取直联账户余额
	 * @param customNo
	 * @param corePoolSize
	 * @param netAccountsToHttp
	 * @param logId
	 * @throws Exception
	 */
	private ThreadResult getRealBalance(String customNo,Integer corePoolSize,List<EnterpriseBankAcctVO> netAccountsToHttp, String logId) throws Exception {
		//构建params 用于组装请求参数
		String channel= bankConnectionAdapterContext.getChanPayCustomChanel();
		CtmJSONObject params = new CtmJSONObject();
		params.put("customNo", customNo);
		params.put("operator", null);
		params.put("signature", null);
		params.put("channel", channel);
		List<EnterpriseBankAcctVO> accountsOne2One = accountRealtimeBalanceService.getEnterpriseBankAcctVOS(null, netAccountsToHttp);
		List<List<EnterpriseBankAcctVO>> lists = accountRealtimeBalanceService.groupData(accountsOne2One, 10);
		//构建线程池
		ExecutorService executorService = initTaskExecutor(corePoolSize);
		//构建线程池
		ThreadResult threadResult  = ThreadPoolUtil.executeByBatchCollectResultsNoSemaphore(executorService,lists,10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400470", "直联账户余额调度任务") /* "直联账户余额调度任务" */,true,(int fromIndex, int toIndex)->{
			ThreadResult subThreadResult = new ThreadResult();
			for(int t = fromIndex ; t < toIndex; t++){
				List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = lists.get(t);
				// 加锁的账号信息
				String accountInfos = enterpriseBankAcctVOS.stream().map(EnterpriseBankAcctVO::getAccount).reduce((s1,s2) -> s1 + ", " + s2).orElse("");
				List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYREALBALANCE_AND_TASK_BALANCE_SUPPLEMENT_COMBINE_LOCK, enterpriseBankAcctVOS);
				try {
					//和余额弥补加一个锁
					CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
						if(lockstatus == LockStatus.GETLOCK_FAIL){
							//加锁失败添加报错信息
							//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId, String.format("[%s]:系统正在对此账户拉取中",accountInfos), TaskUtils.UPDATE_TASK_LOG_URL);
							subThreadResult.getSucessReturnList().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400471", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfos));
							return ;
						}
						String noDataMsg = queryAccountBalanceForHttp(enterpriseBankAcctVOS, params);
						if (noDataMsg != null) {
							String noDataSuccessMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400472", "[%s]:此账户返回[%s]") /* "[%s]:此账户返回[%s]" */,accountInfos, noDataMsg);
							subThreadResult.getSucessReturnList().add(noDataSuccessMessage);
						}
					});
				} catch (Exception e) {
					log.error("queryHttpHistoryBalanceTask",e);
					////加锁失败添加报错信息
					//TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId, String.format("[%s]:此账户操作发生异常",accountInfos) + "[Failure Reason]" + e.getMessage(),
					//		TaskUtils.UPDATE_TASK_LOG_URL);
					String errorMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400477", "账户[%s]") /* "账户[%s]" */,accountInfos) + "[Error]" + e.getMessage();
					subThreadResult.getErrorReturnList().add(errorMessage);
				}
			}
			return subThreadResult;
		});
		return threadResult;
	}

	private String queryAccountBalanceForHttp(List<EnterpriseBankAcctVO> enterpriseBankAcctList,CtmJSONObject params) throws Exception {
		CtmJSONObject queryBalanceMsg = new CtmJSONObject();
		CtmJSONObject result = new CtmJSONObject();
		//考虑线程安全，生成线程安全params
		CtmJSONObject paramsNew = new CtmJSONObject();
		paramsNew.putAll(params);
		try{
			//请求流水号 需要随机数 这里移动到内部执行
			paramsNew.put("requestseqno", DigitalSignatureUtils.buildRequestNum(paramsNew.getString("customNo")));
			queryBalanceMsg = buildQueryBalanceMsg(paramsNew, enterpriseBankAcctList);
			log.error("实时余额请求参数==========================》：" + queryBalanceMsg.toString());
			String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
			List<BasicNameValuePair> requestData = new ArrayList<>();
			requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
			requestData.add(new BasicNameValuePair("reqSignData", signMsg));
			result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
			log.error("实时余额响应参数==========================》：" + result.toString());
			//银企联返回结果解析
			if (result.getInteger("code") == 1) {
				CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
				String service_resp_code = responseHead.getString("service_resp_code");
				//无论是否成功 都把信息提示错出来
				if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,service_resp_code)) {
					CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
					accountRealtimeBalanceService.insertAccountBalanceData(enterpriseBankAcctList.get(0).getOrgid(), enterpriseBankAcctList, responseHead, responseBody, null, null, queryBalanceMsg);
				} else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
					String noDataMsg = YQLUtils.getYQLNoDataMsq(responseHead);
					return noDataMsg;
					//没有数据不报错
				} else {
					//调度任务时，抛出银企联异常给上层收集后展示
					throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead));
				}
			} else {
				//调度任务时，抛出银企联请求异常给上层收集后展示
				throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400475", "请求银企联报错：请求参数 = %s,响应参数 = %s") /* "请求银企联报错：请求参数 = %s,响应参数 = %s" */,queryBalanceMsg, result));
			}
		}catch(Exception e){
			log.error(String.format("queryAccountBalanceForHttpTask-error，请求参数 = %s,响应参数 = %s,报错信息 = %s", queryBalanceMsg, result, e.getMessage()), e);
			throw new Exception(e.getMessage(), e);
		}
		return null;
	}

	public CtmJSONObject buildQueryBalanceMsg(CtmJSONObject params, List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
		CtmJSONObject requestHead = buildRequestHeadNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,
				params.getString("operator"),
				params.getString("customNo"),
				params.getString("requestseqno"),
				params.getString("channel"));
		CtmJSONObject requestBody = new CtmJSONObject();
		CtmJSONArray record = new CtmJSONArray();
		for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
			List<BankAcctCurrencyVO> currencyList = bankAccount.getCurrencyList();
			//筛选启用币种
			List<BankAcctCurrencyVO> enableCurrencyVOList = currencyList.stream().filter(currencyVO -> currencyVO.getEnable() == 1).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(enableCurrencyVOList)) {
				throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400478", "账号【%s】无启用状态的”币种“，请检查【企业资金账户】设置！") /* "账号【%s】无启用状态的”币种“，请检查【企业资金账户】设置！" */, bankAccount.getAccount()));
			}else {
				HashMap<String,String> currencyMap = accountRealtimeBalanceService.queryCurrencyCode(enableCurrencyVOList);
				for (BankAcctCurrencyVO currencyVO : enableCurrencyVOList) {
					CtmJSONObject recordDate = new CtmJSONObject();
					recordDate.put("acct_no", bankAccount.getAccount());
					recordDate.put("acct_name", bankAccount.getAcctName());
					recordDate.put("curr_code", currencyMap.get(currencyVO.getCurrency()));// 这里取到的是币种编码
					record.add(recordDate);
				}
			}
		}
		requestBody.put("record", record);
		CtmJSONObject placeOrderMsg = new CtmJSONObject();
		placeOrderMsg.put("request_head", requestHead);
		placeOrderMsg.put("request_body", requestBody);
		return placeOrderMsg;
	}
	private CtmJSONObject buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String channel) {
		CtmJSONObject requestHead = new CtmJSONObject();
		requestHead.put("version", "1.0.0");
		requestHead.put("request_seq_no", requestseqno);
		requestHead.put("cust_no", customNo);
		requestHead.put("cust_chnl", channel);
		LocalDateTime dateTime = LocalDateTime.now();
		requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
		requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
		requestHead.put("oper", operator);
		requestHead.put("oper_sign", null);
		requestHead.put("tran_code", transCode);
		return requestHead;
	}

	/**
	 * 获取内部账户余额信息
	 * @param tenant_id
	 * @throws Exception
	 */
	private void getInnerBalance(Map<String,Object> paramMap,String tenant_id,List<EnterpriseBankAcctVO> innerBankAccounts,String logId) throws Exception {
		//有内部账户才查询 否则不查询
		if(innerBankAccounts!=null && innerBankAccounts.size()>0){
			List<String> sourceAccountIds = new ArrayList<>();
			CtmJSONObject param = new CtmJSONObject();
			param.put("tenant_id",tenant_id);
			innerBankAccounts.forEach(account -> { sourceAccountIds.add(account.getId());});
			// 加锁的账号信息
			String accountInfos = innerBankAccounts.stream().map(EnterpriseBankAcctVO::getAccount).reduce((s1, s2) -> s1 + ", " + s2).orElse("");;
			List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYREALBALANCEKEY, innerBankAccounts);
			try {
				CtmLockTool.executeInOneServiceExclusivelyBatchLock( accountInfoLocks,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
					if(lockstatus == LockStatus.GETLOCK_FAIL){
						//加锁失败添加报错信息
						TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams")
								,TaskUtils.TASK_BACK_SUCCESS, logId, String.format("[%s]:系统正在对此账户拉取中",accountInfos), TaskUtils.UPDATE_TASK_LOG_URL);
						return ;
					}
					accountRealtimeBalanceService.queryAccountBalance(param,sourceAccountIds,innerBankAccounts, null);
				});
			} catch (Exception e) {
				log.error("getInnerBalance",e);
				//加锁失败添加报错信息
				TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams")
						,TaskUtils.TASK_BACK_FAILURE, logId, String.format("账户[%s]",accountInfos) + "[Error]" + e.getMessage(),
						TaskUtils.UPDATE_TASK_LOG_URL);
			}
		}
	}

	/**
	 * 初始化线程池
	 * @param poolSize
	 * @return
	 */
	private ExecutorService initTaskExecutor(Integer poolSize) {
		int queueLength = Integer.parseInt(AppContext.getEnvConfig("cmp.httpBalanceTask.queueCapacity","2000"));
		String threadName = "cmp-httpBalanceTask-async-";
		return ThreadPoolBuilder.ioThreadPoolBuilder()
				.setDaemon(false)
				.setRejectHandler(new BlockPolicy())
				.builder(poolSize,poolSize,queueLength,threadName);

	}

}
