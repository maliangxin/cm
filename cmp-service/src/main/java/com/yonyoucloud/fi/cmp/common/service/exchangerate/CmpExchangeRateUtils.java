package com.yonyoucloud.fi.cmp.common.service.exchangerate;

import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.ExchangeRateMode;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateWithMode;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateWithModeService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.basedoc.ICtmExchangeRateService;
import com.yonyou.yonbip.ctm.basedoc.ICtmExchangeRateTypeService;
import com.yonyou.yonbip.ctm.basedoc.vo.ExchangeRateDTO;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public class CmpExchangeRateUtils {

//    /**
//     * 新汇率统一处理入口
//     *
//     * @param cmpExchangeRateDto 新汇率数据传输实体
//     * @return
//     * @throws Exception
//     */
//    @Deprecated
//    public static void handleNewExchangeRate(CmpExchangeRateDto cmpExchangeRateDto) throws Exception {
//        if (StringUtils.isEmpty(cmpExchangeRateDto.getCurrencyKey()) || StringUtils.isEmpty(cmpExchangeRateDto.getNatCurrencyKey()) || StringUtils.isEmpty(cmpExchangeRateDto.getExchangeRateTypeKey()) || cmpExchangeRateDto.getQuotationDate() == null || cmpExchangeRateDto.getCurrencyAmountKey() == null || cmpExchangeRateDto.getBizObject() == null) {
//            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071F", "以上字段传参不能为空") /* "以上字段传参不能为空" */);
//        }
//        ICtmExchangeRateTypeService ctmExchangeRateTypeService = AppContext.getBean(ICtmExchangeRateTypeService.class);
//        ICtmExchangeRateService ctmExchangeRateService = AppContext.getBean(ICtmExchangeRateService.class);
//        //获取汇率类型
//        ExchangeRateTypeVO exchangeRateTypeVO;
//        if (cmpExchangeRateDto.getAccountingExchangeRateType()) {
//            exchangeRateTypeVO = ctmExchangeRateTypeService.getAccountingExchangeRateType(cmpExchangeRateDto.getBizObject().get(cmpExchangeRateDto.getOrgKey()).toString());
//        } else {
//            exchangeRateTypeVO = ctmExchangeRateTypeService.getTransactionalExchangeRateType(cmpExchangeRateDto.getBizObject().get(cmpExchangeRateDto.getAccentityKey()).toString());
//        }
//        //获取汇率
//        ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
//        exchangeRateDTO.setExchangeType(exchangeRateTypeVO.getId());
//        exchangeRateDTO.setQuoteDate(cmpExchangeRateDto.getQuotationDate());
//        exchangeRateDTO.setSourceCurrencyId(cmpExchangeRateDto.getBizObject().get(cmpExchangeRateDto.getCurrencyKey()).toString());
//        exchangeRateDTO.setTargetCurrencyId(cmpExchangeRateDto.getBizObject().get(cmpExchangeRateDto.getNatCurrencyKey()).toString());
//        ExchangeRateWithMode exchangeRateWithMode = ctmExchangeRateService.getExchangeRateWithMode(exchangeRateDTO);
//        //计算主表目标金额
//        BigDecimal targetCurrencyAmount = BigDecimal.ZERO;
//        if (exchangeRateWithMode != null) {
//            if (exchangeRateWithMode.getExchRateOps().getValue() == 1) {
//                //乘法
//                targetCurrencyAmount = cmpExchangeRateDto.getBizObject().getBigDecimal(cmpExchangeRateDto.getCurrencyAmountKey()).multiply(exchangeRateWithMode.getExchRate());
//            } else {
//                //除法
//                targetCurrencyAmount = cmpExchangeRateDto.getBizObject().getBigDecimal(cmpExchangeRateDto.getCurrencyAmountKey()).divide(exchangeRateWithMode.getExchRate());
//            }
//        }
//        //给业务实体各字段赋值
//        cmpExchangeRateDto.getBizObject().set(cmpExchangeRateDto.getExchangeRateTypeKey(), exchangeRateTypeVO.getId());
//        cmpExchangeRateDto.getBizObject().set(cmpExchangeRateDto.getTargetCurrencyKey(), targetCurrencyAmount);
//        for (BizObject subBizObject : cmpExchangeRateDto.getSubBizObjectList()) {
//            //计算子表目标金额
//            BigDecimal subTargetCurrencyAmount;
//            if (exchangeRateWithMode != null) {
//                if (exchangeRateWithMode.getExchRateOps().getValue() == 1) {
//                    //乘法
//                    subTargetCurrencyAmount = subBizObject.getBigDecimal(cmpExchangeRateDto.getCurrencyAmountKey()).multiply(exchangeRateWithMode.getExchRate());
//                } else {
//                    //除法
//                    subTargetCurrencyAmount = subBizObject.getBigDecimal(cmpExchangeRateDto.getCurrencyAmountKey()).divide(exchangeRateWithMode.getExchRate());
//                }
//                subBizObject.set(cmpExchangeRateDto.getTargetCurrencyKey(), subTargetCurrencyAmount);
//            }
//        }
//    }

    public static CmpExchangeRateVO getNewExchangeRateWithMode(String currencyId, String natCurrencyId, Date vouchDate, String exchangeRateType, Integer scale) throws Exception {
        String version = DiffVersionUtils.getVersion();
        CmpExchangeRateVO exchangeRateByRate = new CmpExchangeRateVO();
        switch (version) {
            case ICmpConstant.V5_VERSION:
                ICtmExchangeRateService ctmExchangeRateService = AppContext.getBean(ICtmExchangeRateService.class);
                //获取汇率
                ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
                exchangeRateDTO.setExchangeType(exchangeRateType);
                exchangeRateDTO.setQuoteDate(vouchDate);
                exchangeRateDTO.setSourceCurrencyId(currencyId);
                exchangeRateDTO.setTargetCurrencyId(natCurrencyId);
                if(currencyId.equals(natCurrencyId)){
                    exchangeRateByRate.setExchangeRateOps((short) 1);
                    exchangeRateByRate.setExchangeRate(new BigDecimal(1));
                }else{
                    ExchangeRateWithMode exchangeRateWithMode = ctmExchangeRateService.getExchangeRateWithMode(exchangeRateDTO);
                    if(exchangeRateWithMode == null) {
                        String exchangeRateTypeName = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateTypeById(exchangeRateType).getName();
                        String vouchDateStr = DateUtils.dateToStr(vouchDate);
                        String currencyName = AppContext.getBean(CurrencyQueryService.class).findById(currencyId).getName();
                        String natCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(natCurrencyId).getName();
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400720", "根据汇率类型[%s]业务日期[%s]原币种[%s]目标币种[%s]，获取到的汇率为空，请检查汇率设置!") /* "根据汇率类型[%s]业务日期[%s]原币种[%s]目标币种[%s]，获取到的汇率为空，请检查汇率设置!" */,exchangeRateTypeName,vouchDateStr,currencyName,natCurrencyName));
                    }
                    exchangeRateByRate.setExchangeRateOps(exchangeRateWithMode.getExchRateOps().getShortValue());
                    exchangeRateByRate.setExchangeRate(exchangeRateWithMode.getExchRate());
                }
                break;
            case ICmpConstant.R6_VERSION:
            default:
                Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(exchangeRateType, currencyId, natCurrencyId, vouchDate, scale);
                exchangeRateByRate.setExchangeRate(BigDecimal.valueOf(currencyRateNew));
                exchangeRateByRate.setExchangeRateOps((short) 1);
                break;
        }
        return exchangeRateByRate;
    }

    public static CmpExchangeRateVO getNewExchangeRateWithMode(String currencyId, String natCurrencyId, Date vouchDate, String exchangeRateType) throws Exception {
        return getNewExchangeRateWithMode(currencyId, natCurrencyId, vouchDate, exchangeRateType, null);
    }

    /**
     * 查询汇率，根据汇率折算方式和是否方向计算
     *
     * @param currencyId 原币
     * @param natCurrencyId 目的币种
     * @param vouchDate 汇率日期
     * @param exchangeRateType 汇率类型
     * @param exchRateOps 汇率折算方式 1：乘法，2：除法
     * @param isReverse 是否反向查询最新汇率
     * @return 汇率，汇率折算方式
     * @throws Exception
     */
    public static CmpExchangeRateVO queryExchangeRateWithModeAndIsReverse(String currencyId, String natCurrencyId, Date vouchDate, String exchangeRateType,String exchRateOps, Boolean isReverse) throws Exception {
        String version = DiffVersionUtils.getVersion();
        CmpExchangeRateVO exchangeRateByRate = new CmpExchangeRateVO();
        switch (version) {
            case ICmpConstant.V5_VERSION:
                IExchangeRateWithModeService iExchangeRateWithModeService = AppContext.getBean(IExchangeRateWithModeService.class);
                short opsV5 = Short.parseShort(exchRateOps);
                //获取汇率
                ExchangeRateWithMode exchangeRateWithMode = iExchangeRateWithModeService.queryExchangeRateWithModeAndIsReverse(exchangeRateType, currencyId, natCurrencyId, vouchDate, ExchangeRateMode.fromValue(opsV5), isReverse);
                if (exchangeRateWithMode == null){
                    exchangeRateByRate.setExchangeRateOps(null);
                    exchangeRateByRate.setExchangeRate(null);
                }else {
                    exchangeRateByRate.setExchangeRateOps(exchangeRateWithMode.getExchRateOps().getShortValue());
                    exchangeRateByRate.setExchangeRate(exchangeRateWithMode.getExchRate());
                }
                break;
            case ICmpConstant.R6_VERSION:
            default:
                BaseRefRpcService baseRefRpcService = AppContext.getBean(BaseRefRpcService.class);
                ExchangeRate exchangeRate = baseRefRpcService.queryRateByExchangeType(currencyId, natCurrencyId, vouchDate, exchangeRateType);
                if (exchangeRate == null) {
                    exchangeRateByRate.setExchangeRate(null);
                    exchangeRateByRate.setExchangeRateOps(Short.parseShort(exchRateOps));
                }else {
                    short ops = Short.parseShort(exchRateOps);
                    if (ExchangeRateMode.FORWARD.getShortValue() == ops){ //直接汇率
                        exchangeRateByRate.setExchangeRate(BigDecimal.valueOf(exchangeRate.getExchangerate()));
                    }else { //间接汇率
                        exchangeRateByRate.setExchangeRate(BigDecimal.valueOf(exchangeRate.getIndirectExchangeRate()));
                    }
                    exchangeRateByRate.setExchangeRateOps(ops);
                }
                break;
        }

        return exchangeRateByRate;
    }

    public static ExchangeRateTypeVO getNewExchangeRateType(String orgId, boolean accountingExchangeRateType) throws Exception {
        ExchangeRateTypeVO exchangeRateTypeVO;
        String version = DiffVersionUtils.getVersion();
        switch (version) {
            case ICmpConstant.V5_VERSION:
                ICtmExchangeRateTypeService ctmExchangeRateTypeService = AppContext.getBean(ICtmExchangeRateTypeService.class);
                //获取汇率
                if (accountingExchangeRateType) {
                    exchangeRateTypeVO = ctmExchangeRateTypeService.getAccountingExchangeRateType(orgId);
                } else {
                    exchangeRateTypeVO = ctmExchangeRateTypeService.getTransactionalExchangeRateType(orgId);
                }
                break;
            case ICmpConstant.R6_VERSION:
            default:
                Map<String,Object> exchangeRateTypeMap = AppContext.getBean(CmCommonService.class).getDefaultExchangeRateType(orgId);
                exchangeRateTypeVO = new ExchangeRateTypeVO();
                exchangeRateTypeVO.setCode(exchangeRateTypeMap.get("code").toString());
                exchangeRateTypeVO.setId(exchangeRateTypeMap.get("id").toString());
                exchangeRateTypeVO.setDigit(Integer.valueOf(exchangeRateTypeMap.get("digit").toString()));
                exchangeRateTypeVO.setName(exchangeRateTypeMap.get("name").toString());
                break;
        }
        return exchangeRateTypeVO;
    }

    public static ExchangeRateTypeVO getUserDefineExchangeRateType() throws Exception {
        ExchangeRateTypeVO exchangeRateTypeVO;
        String version = DiffVersionUtils.getVersion();
        switch (version) {
            case ICmpConstant.V5_VERSION:
                ICtmExchangeRateTypeService ctmExchangeRateTypeService = AppContext.getBean(ICtmExchangeRateTypeService.class);
                exchangeRateTypeVO = ctmExchangeRateTypeService.getUserDefineExchangeRateType();
                break;
            case ICmpConstant.R6_VERSION:
            default:
                exchangeRateTypeVO = AppContext.getBean(BaseRefRpcService.class).queryExchangeRateRateTypeByCode("02");
                break;
        }
        return exchangeRateTypeVO;
    }

    public static BigDecimal getExchangeRateAndAmountCalResult(Short exchRateOps, BigDecimal exchRate, BigDecimal amount, Integer scale) {
        if (exchRate == null || amount == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005E", "参数错误！") /* "参数错误！" */);
        }
        if (exchRateOps == null) {
            // 如果折算方式为null，表示是老汇率方式，默认赋值1，采用乘法
            exchRateOps = 1;
        }
        if (scale != null) {
            return exchRateOps == 1 ? BigDecimalUtils.safeMultiply(exchRate, amount, scale) : BigDecimalUtils.safeDivide(amount, exchRate, scale);
        }
        return exchRateOps == 1 ? BigDecimalUtils.safeMultiply(exchRate, amount) : BigDecimalUtils.safeDivide(amount, exchRate, ICmpConstant.CONSTANT_TEN);
    }
}
