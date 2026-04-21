package com.yonyoucloud.fi.cmp.util.basedoc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.basedoc.service.itf.ITenantCurrencyService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.util.ErrorMsgUtil;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 查询币种万能接口
 * 设置为静态类
 */
@Service
public class CurrencyQueryService {

    @Autowired
    ITenantCurrencyService iTenantCurrencyService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    private static final @NonNull Cache<String, String> currencyCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 缓存银行账户的对应币种（多币种时不适用）
     * 缓存规则：账户单币种时缓存单一币种；多币种时缓存默认币种；若多币种时无默认 则取人民币
     */
    private static final @NonNull Cache<String, String> accountCurrencyCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    private static final @NonNull Cache<String, CurrencyTenantDTO> currencyNatAmountCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 根据币种id查询币种
     * @param id
     * @return
     * @throws Exception
     */
    public CurrencyTenantDTO findById(String id) throws Exception {
        BdRequestParams bdRequestParams = new BdRequestParams();
        bdRequestParams.setId(id);
        return iTenantCurrencyService.findById(bdRequestParams);
    }


    /**
     * 根据币种id集合查询币种
     * @param id
     * @return
     * @throws Exception
     */
    public CurrencyTenantDTO findByIds(List<String> id) throws Exception {
        BdRequestParams bdRequestParams = new BdRequestParams();
        bdRequestParams.setIdList(id);
        return iTenantCurrencyService.findById(bdRequestParams);
    }


    /**
     * 根据币种查询币种
     * @param currencyList
     * @return
     * @throws Exception
     */
    public HashMap<String, String> queryByCurrencyList(List<BankAcctCurrencyVO> currencyList) throws Exception {
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        List<String> ids = new ArrayList<>();
        currencyList.stream().forEach(e -> {
            ids.add(e.getCurrency());
        });
        currencyBdParams.setIdList(ids);
        HashMap<String, String> currencyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(ids)) {
            return currencyMap;
        }
        List<CurrencyTenantDTO> currencyTenantDTOList = iTenantCurrencyService.queryListByCondition(currencyBdParams);
        for(CurrencyTenantDTO currencyTenantDTO : currencyTenantDTOList){
            currencyMap.put(currencyTenantDTO.getId(),currencyTenantDTO.getCode());
        }
        return currencyMap;
    }


    /**
     * 根据币种编码查询币种
     * @param code
     * @return
     * @throws Exception
     */
    public CurrencyTenantDTO findByCode(String code) throws Exception {
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        currencyBdParams.setCode(code);
        List<CurrencyTenantDTO> list = iTenantCurrencyService.queryListByCondition(currencyBdParams);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * 根据币种名称查询币种*
     * @param name
     * @return
     * @throws Exception
     */
    public CurrencyTenantDTO findByName(String name) throws Exception {
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        currencyBdParams.setName(name);
        List<CurrencyTenantDTO> list = iTenantCurrencyService.queryListByCondition(currencyBdParams);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * 根据币种编码查询币种
     * @param currencyCode
     * @return
     * @throws Exception
     */
    public static String getCurrencyByCode(String currencyCode) throws Exception {
        ITenantCurrencyService iTenantCurrencyService= AppContext.getBean(ITenantCurrencyService.class);
        String currency;
        Object tenantId = AppContext.getTenantId();
        String cacheKey = currencyCode + ":" + tenantId;
        currency = currencyCache.getIfPresent(cacheKey);
        if (currency != null) {
            return currency;
        }
        CurrencyBdParams params = new CurrencyBdParams();
        params.setCode(currencyCode);
        List<CurrencyTenantDTO> currencyTenantDTOs = iTenantCurrencyService.queryListByCondition(params);
        if (currencyTenantDTOs != null && currencyTenantDTOs.size() == 1 && currencyTenantDTOs.get(0).getDr() == 0) {
            currencyCache.put(cacheKey, currencyTenantDTOs.get(0).getId());
            return  currencyTenantDTOs.get(0).getId();
        } else {
            //若查不出数据 传入人民币code
            params.setCode("CNY");
            List<CurrencyTenantDTO> currencyTenantDTOCNs = iTenantCurrencyService.queryListByCondition(params);
            if (currencyTenantDTOCNs != null && currencyTenantDTOCNs.size() == 1 && currencyTenantDTOCNs.get(0).getDr() == 0) {
                currencyCache.put(cacheKey, currencyTenantDTOCNs.get(0).getId());
                return  currencyTenantDTOCNs.get(0).getId();
            } else {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101615"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001562882") /* "根据币种编码未获取到币种！" */);
                throw new CtmException(ErrorMsgUtil.getCurrencyCodeWrongMsg(currencyCode));
            }
        }
    }
    /**
     * 根据银行账户获取币种
     *
     * @param bankAcc
     * @return
     * @throws Exception
     */
    public String getCurrencyByAccount(String bankAcc) throws Exception {
        BaseRefRpcService baseRefRpcService= AppContext.getBean(BaseRefRpcService.class);
        Object tenantId = AppContext.getTenantId();
        String cacheKey = bankAcc + ":" + tenantId + "accountCurrency";
        String currency = accountCurrencyCache.getIfPresent(cacheKey);
        if (currency != null) {
            return currency;
        }
        //账户单币种时取单一币种；多币种时取默认币种；若多币种时无默认 则取人民币
        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankAcc);
        if(enterpriseBankAcctVO != null){
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            if (currencyList != null && currencyList.size()>0) {
                if(currencyList.size() == 1){//但单币种
                    currency = currencyList.get(0).getCurrency();
                }else {
                    for(BankAcctCurrencyVO bankAcctCurrencyVO : currencyList){
                        if(bankAcctCurrencyVO.getIsdefault() == 1){
                            currency = bankAcctCurrencyVO.getCurrency();
                        }
                    }
                }
            }
        }
        if (currency == null) {
            currency = getCurrencyByCode("CNY");
        }
        if (currency != null) {
            accountCurrencyCache.put(cacheKey, currency);
        }
        return currency;
    }


    /**
     * 根据银行账户获取币种
     *
     * @param bankAcc
     * @return
     * @throws Exception
     */
    public List<String> getCurrencyListByAccount(String bankAcc) throws Exception {
        BaseRefRpcService baseRefRpcService= AppContext.getBean(BaseRefRpcService.class);
        Object tenantId = AppContext.getTenantId();
        String cacheKey = bankAcc + ":" + tenantId + "accountCurrency";
        List<String> currencys =  new ArrayList<>();
        //账户单币种时取单一币种；多币种时取默认币种；若多币种时无默认 则取人民币
        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bankAcc.trim());
        if(enterpriseBankAcctVO != null){
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
            if (CollectionUtils.isNotEmpty(currencyList)) {
                for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                    currencys.add(bankAcctCurrencyVO.getCurrency());
                }
            }
        }
        return currencys;
    }

    /**
     * 对金额进行币种精度处理
     * @param currency 币种
     * @param amount 金额
     * @return
     * @throws Exception
     */
    public BigDecimal getAmountOfCurrencyPrecision(String currency, BigDecimal amount) throws Exception {
        if (StringUtils.isEmpty(currency) || amount == null) {
            throw new CtmException("param error");
        }
        CurrencyTenantDTO currencyDTO;
        if (currencyNatAmountCache.getIfPresent(currency) != null) {
            currencyDTO = currencyNatAmountCache.getIfPresent(currency);
        } else {
            currencyDTO = baseRefRpcService.queryCurrencyById(currency);
            if (currencyDTO == null) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400387", "根据币种id未获取到币种！") /* "根据币种id未获取到币种！" */ + "currencyId  :" + currency);
            }
            currencyNatAmountCache.put(currency, currencyDTO);
        }
        assert currencyDTO != null;
        return amount.setScale(currencyDTO.getMoneydigit(), currencyDTO.getMoneyrount());
    }


}
