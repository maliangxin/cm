package com.yonyoucloud.fi.cmp.checkinventory;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.workbench.util.Lists;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkinventory.service.CheckInventoryService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckDir;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * <h1>支票盘点实现类</h1>
 *
 * @author 赵瑞
 * @version 1.0
 * @since 2021-05-26
 */
@Slf4j
@Component
public class CheckInventoryServiceImpl implements CheckInventoryService {

    @Autowired
    CurrencyQueryService currencyQueryService;
    @Autowired
    CheckStatusService checkStatusService;

    @Override
    public List<Map<String, Object>> getCheckInventoryInfo(CtmJSONObject params) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        String inventoryrange = params.getString("inventoryrange");
        String accentity = params.getString("accentity");
        String[] pks = inventoryrange.split(",");
        /*QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("checkBillDir").in(((Object) pks)));
        // if(accentity!=null) {
        group.addCondition(QueryCondition.name("accentity").eq(accentity));
        // }
        group.addCondition(QueryCondition.name("checkBillStatus").eq(CmpCheckStatus.InStock.getValue()));
        querySchema.addCondition(group);
        //   总数
        List<CheckStock> list = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);*/
        //子表盘点支票簿，显示在盘点日期，支票状态=已入库的支票，而不是显示当前支票状态=已入库的支票。
        Date inventorydate = new Date();
        if (params.get("inventorydate") != null) {
            inventorydate = DateUtils.convertToDate(params.get("inventorydate").toString(), DateUtils.DATE_TIME_PATTERN);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("checkBillDir").in(((Object) pks)));//支票方向
        group.addCondition(QueryCondition.name("accentity").eq(accentity));//会计主体
        group.addCondition(QueryCondition.name("operatorDate").elt(inventorydate));//操作日期
        querySchema.addCondition(group);
        List<CheckStatus> checkStatuses = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, querySchema, null);
        Map<Long, List<CheckStatus>> checkGroupList = checkStatuses.stream().collect(Collectors.groupingBy(CheckStatus::getCheckId));
        List<Long> checkIds = new ArrayList<>();
        if (!checkGroupList.isEmpty()) {
            for (Long checkId : checkGroupList.keySet()) {
                List<CheckStatus> checkStatusList = checkGroupList.get(checkId);
                //降序排列，取最近的一次状态,如果支票的更新后支票状态=已入库的支票，则符合要求
                checkStatusList.sort(Comparator.comparing(CheckStatus::getPubts).reversed());
                CheckStatus checkStatus = checkStatusList.get(0);
                if (CmpCheckStatus.InStock.getValue().equals(checkStatus.getAfterCheckBillStatus())) {
                    checkIds.add(checkStatus.getCheckId());
                }
            }
        }
        List<CheckStock> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(checkIds)) {
            QuerySchema checkStockQuerySchema = QuerySchema.create().addSelect("*,currency.name,payBank.name,mainid.accentity,mainid.org,mainid.checkBookNo,mainid.billType,mainid.tradetype,mainid.chequeType,mainid.account,mainid.account.code,mainid.account.name,mainid.account.bankNumber,mainid.account.bankNumber.name,mainid.currency,mainid.currency.name");
            QueryConditionGroup checkStockGroup = QueryConditionGroup.and(QueryCondition.name("id").in(checkIds));
            checkStockQuerySchema.addCondition(checkStockGroup);
            list = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, checkStockQuerySchema, null);
        }

        //上次盘点日期
        Long lastInventoryId = null;
        Date lastInventoryDate = null;
        //上次盘点信息
        QuerySchema lastQuerySchema = QuerySchema.create().addSelect("id,inventorydate").appendQueryCondition(
                QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("status").eq(AuditStatus.Complete.getValue()),
                QueryCondition.name("inventorydate").lt(inventorydate)
        ).addOrderBy("inventorydate desc");

        List<CheckInventory> checkInventories = MetaDaoHelper.queryObject(CheckInventory.ENTITY_NAME, lastQuerySchema, null);
        if (CollectionUtils.isNotEmpty(checkInventories)) {
            lastInventoryId = checkInventories.get(0).getId();
            lastInventoryDate = checkInventories.get(0).getInventorydate();
        }
        //   支票簿编号不为空
        List<CheckStock> filterList = list.stream().filter(item -> null != item.get("checkBookNo")).collect(Collectors.toList());
        //   根据支票簿编号分组
        Map<String, List<CheckStock>> checkBookNoGroupList = filterList.stream().collect(Collectors.groupingBy(CheckStock::getCheckBookNo));
        if (!checkBookNoGroupList.isEmpty()) {
            for (String checkBankNo : checkBookNoGroupList.keySet()) {
                //     循环根据分组key查分组下的所有数据
                List<CheckStock> checkBookNoGroup = checkBookNoGroupList.get(checkBankNo);
                //空白支票 带出入库单的银行网点名称数据；(cmp_check_stock_apply.account)
                Map<String, List<CheckStock>> tmp1Map = checkBookNoGroup.stream().filter(bill -> bill.get("mainid_chequeType") != null && 0 == (bill.getShort("mainid_chequeType"))).collect(Collectors.groupingBy(bill -> bill.get("mainid_account_code")));
                //支票簿编号不为空的 只有空白发票
                if (!tmp1Map.isEmpty()) {
                    for (String accountNo : tmp1Map.keySet()) {
                        Map<String, Object> map = this.buildData(checkBankNo, accountNo, tmp1Map.get(accountNo));
                        if (map != null && !map.isEmpty()) {
                            Map<String, Object> dataMap = this.calcLastData(lastInventoryId, lastInventoryDate, inventorydate, accentity, accountNo, checkBankNo, 0);
                            map.putAll(dataMap);
                            result.add(map);
                        }

                    }
                }
            }
        }
        //   支票簿编号为空
        List<CheckStock> filterisNullList = list.stream().filter(item -> StringUtils.isEmpty(item.getCheckBookNo())).collect(Collectors.toList());
        if (!filterisNullList.isEmpty()) {
            //     根据支票方向分组
            Map<String, List<CheckStock>> checkBillDirGroupList = filterisNullList.stream().collect(Collectors.groupingBy(CheckStock::getCheckBillDir));
            if (!checkBillDirGroupList.isEmpty()) {

                for (String checkBillDir : checkBillDirGroupList.keySet()) {
                    List<CheckStock> checkBillDirGroup = checkBillDirGroupList.get(checkBillDir);
                    List<CheckStock> blist = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(checkBillDirGroup)) {
                        checkBillDirGroup.forEach(sub -> {
                            CheckStock checkStock = new CheckStock();
                            checkStock.init(sub);
                            //设置标记
                            EntityTool.setUpdateStatus(checkStock);
                            checkStock.setIsInventory(CheckDir.Collect.getValue());
                            blist.add(checkStock);
                        });
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
                    }

                    //空白支票 带出入库单的银行网点名称数据；(cmp_check_stock_apply.account)
//                    Map<String,List<CheckStock>> tmp1Map = checkBillDirGroup.stream().filter(bill -> bill.get("mainid_chequeType") != null && 0 == (bill.getShort("mainid_chequeType"))).collect(Collectors.groupingBy(bill -> bill.get("mainid_account")));
                    //收入支票 按照会计主体(cmp_check_stock_apply.accentity)+出票人账号(cmp_check_stock.drawerAcct)+付款银行网点名称(cmp_check_stock.payBank)为维度显示收票入库的付款银行网点名称数据
                    //支票簿编号为空的 只有收入支票
                    Map<String, List<CheckStock>> tmp2Map = checkBillDirGroup.stream().filter(bill -> bill.get("mainid_chequeType") != null && 1 == (bill.getShort("mainid_chequeType"))).collect(Collectors.groupingBy(bill -> bill.get("mainid_accentity") + "," + bill.get("drawerAcctNo") + "," + bill.get("payBank")));

                    if (!tmp2Map.isEmpty()) {
                        for (String key : tmp2Map.keySet()) {
                            String[] keys = key.split(",");
                            Map<String, Object> map = this.buildDataByDir(checkBillDir, key, tmp2Map.get(key));
                            if (map != null && !map.isEmpty()) {
                                Map<String, Object> dataMap = this.calcLastData(lastInventoryId, lastInventoryDate, inventorydate, accentity, keys[1], keys[2], 1);
                                map.putAll(dataMap);
                                result.add(map);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void afterSaveBillToCmp(List<CheckInventory_b> billbs, String accentity) throws Exception {
        for (CheckInventory_b billb : billbs) {
            if (Short.parseShort(billb.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                if (null != billb.get("checkid")) {
                    String[] checkids = billb.get("checkid").toString().split(",");
                    List<String> checkIdList = Lists.newArrayList(checkids);
                    handleStatusUpdate(MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList));
                }
            }
        }
    }

    private void handleStatusUpdate(List<Map<String, Object>> checkStockList) throws Exception {
        if (checkStockList.isEmpty()) {
            return;
        }
        List<CheckStock> blist = new ArrayList<>();
        List<Map<String, Object>> oldBlist = new ArrayList<>();
        //盘点单审批完成后，支票状态为“盘点中”的支票状态恢复为已入库
        checkStockList.forEach(sub -> {
            CheckStock checkStock = new CheckStock();
            checkStock.init(sub);
            if (CmpCheckStatus.Inventory.getValue().equals(checkStock.getCheckBillStatus())) {
                EntityTool.setUpdateStatus(checkStock);
                checkStock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
//            checkStock.setIsInventory(null);
                blist.add(checkStock);
                oldBlist.add(sub);
            }
        });
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
        checkStatusService.recordCheckStatusByMap(oldBlist, CmpCheckStatus.InStock.getValue());
    }

    /**
     * 上期盘点数据
     * 本期已使用数量
     * 本期已处置数量
     * 支票状态 cmp_checkStatus
     *
     * @param lastInventoryId 上次盘点信息
     * @param accentity       会计主体
     * @param acctNo          银行账号
     * @param checkBookNo     空白支票下该字段为支票簿，收入支票为付款银行
     * @param chequeType      0 空白支票/ 1 收入支票
     * @return
     */
    private Map<String, Object> calcLastData(Long lastInventoryId, Date lastInventoryDate, Date inventorydate, String accentity, String acctNo, String checkBookNo, int chequeType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("prevPeriodCount", null);
        result.put("curUseCount", null);
        result.put("curHandleCount", null);
        if (lastInventoryId == null) {
            return result;
        }
        //盘点单子表信息
        QuerySchema querySchema = QuerySchema.create().addSelect("accountNo,checkBillNo,materialquantity,checkid")
                .appendQueryCondition(
                        QueryCondition.name("mainid").eq(lastInventoryId),
//                        QueryCondition.name("checkBillNo").eq(checkBookNo),
                        QueryCondition.name("accountNo").eq(acctNo)
                );
        if(chequeType == 0){
            querySchema.appendQueryCondition(QueryCondition.name("checkBillNo").eq(checkBookNo));
        }else{
            querySchema.appendQueryCondition(QueryCondition.name("bank").eq(checkBookNo));
        }
        List<CheckInventory_b> list = MetaDaoHelper.queryObject(CheckInventory_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(list)) {
            Long materialQuantity = list.get(0).getMaterialquantity();
            List<Long> checkIds = Arrays.stream(list.get(0).getCheckid().split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
            //第一次盘点时为空 以会计主体+银行账号+支票簿维度显示上一次盘点单的“实物数量（张）”
            result.put("prevPeriodCount", materialQuantity);

            //第一次盘点时为空 以会计主体+银行账号+支票簿维度显示，上期盘点时支票状态=已入库，本期盘点时支票状态=已领用、在开票、已开票、兑付中、已兑付的支票数量
            QuerySchema currentUseSchema = QuerySchema.create().addSelect("checkId,afterCheckBillStatus,operatorDate,pubts").appendQueryCondition(
                    QueryCondition.name("operatorDate").between(lastInventoryDate, inventorydate),
                    QueryCondition.name("checkId").in(checkIds)
            );
            List<CheckStatus> currentUseStatusList = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, currentUseSchema, null);
            if (CollectionUtils.isNotEmpty(currentUseStatusList)) {
                Map<Long, CheckStatus> collect = currentUseStatusList.stream().collect(Collectors.groupingBy(o -> o.getCheckId(),
                        Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(CheckStatus::getPubts)), Optional::get)));
                int currentUseNum = 0;
                for (Map.Entry<Long, CheckStatus> entry : collect.entrySet()) {
                    CheckStatus checkStatus = entry.getValue();
                    if (chequeType == 0) {
                        if (Arrays.asList("13", "2", "3", "6", "7").contains(checkStatus.getAfterCheckBillStatus())) {
                            currentUseNum++;
                        }
                    } else {
                        if (Arrays.asList("6", "7").contains(checkStatus.getAfterCheckBillStatus())) {
                            currentUseNum++;
                        }
                    }
                }
                result.put("curUseCount", currentUseNum == 0 ? null : currentUseNum);
            }

            //第一次盘点时为空 以会计主体+银行账号+支票簿维度显示，上期盘点时支票状态=已入库，本期盘点时支票状态=处置中、已退回、已挂失、已作废的支票数量
            QuerySchema currentHandleSchema = QuerySchema.create().addSelect("checkId,afterCheckBillStatus,operatorDate,pubts")
                    .distinct()
                    .appendQueryCondition(
                            QueryCondition.name("operatorDate").between(lastInventoryDate, inventorydate),
                            QueryCondition.name("checkId").in(checkIds)
                    );
            List<CheckStatus> currentHandleStatusList = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, currentHandleSchema, null);
            if (CollectionUtils.isNotEmpty(currentHandleStatusList)) {
                Map<Long, CheckStatus> collect = currentHandleStatusList.stream().collect(Collectors.groupingBy(o -> o.getCheckId(),
                        Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(CheckStatus::getPubts)), Optional::get)));
                int currentHandleNum = 0;
                for (Map.Entry<Long, CheckStatus> entry : collect.entrySet()) {
                    CheckStatus checkStatus = entry.getValue();
                    if (Arrays.asList("11", "12", "8", "9").contains(checkStatus.getAfterCheckBillStatus())) {
                        currentHandleNum++;
                    }
                }
                result.put("curHandleCount", currentHandleNum == 0 ? null : currentHandleNum);
            }
        }

        return result;
    }

    private Map<String, Object> buildData(String checkBookNo, String accoutNo, List<CheckStock> checkBookNoGroup) throws Exception {
        String startNo = null;
        String endNo = null;
        Long checkquantity;
        Short checkBillType = null;
        String currency = null;
        Date busiDate;
        List<Long> checkids = checkBookNoGroup.stream().map(e -> Long.parseLong(e.get("id").toString())).collect(Collectors.toList());
        String checkid = StringUtils.join(checkids, ",");
        List<CheckStock> blist = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(checkBookNoGroup)) {
//                    checkBookNoGroup.forEach(sub -> {
//                        CheckStock checkStock = new CheckStock();
//                        checkStock.init(sub);
//                        //设置标记
//                        EntityTool.setUpdateStatus(checkStock);
//                        checkStock.setIsInventory(CheckDir.Pay.getValue());
//                        blist.add(checkStock);
//                    });
//                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
            //     查出所有数据中支票编号最小的作为起始编号
            List<String> collect = checkBookNoGroup.stream().map(item -> item.getCheckBillNo()).collect(Collectors.toList());

            int r = collect.get(0).replaceAll("\\D", ",").lastIndexOf(",");
            String letter = collect.get(0).substring(0, r + 1);

            String checkBillNos = "";
            List<String> checkBookNoList = checkBookNoGroup.stream().map(CheckStock::getCheckBillNo).distinct().collect(Collectors.toList());
            List<Long> billNos = collect.stream().map(a -> {
                String str = a.replaceAll("\\D", ",");
                int i = str.lastIndexOf(",");
                String substring = str.substring(i + 1);
                return Long.valueOf(substring);
            }).collect(Collectors.toList());
            Long max = billNos.stream().max(Comparator.comparing(x -> x)).orElse(null);
            Long min = billNos.stream().min(Comparator.comparing(x -> x)).orElse(null);
            // 数字首位有0的特殊情况处理
            if (collect.get(0).length() == (letter + min).length()) {
                startNo = letter + min;
                endNo = letter + max;

                checkBillNos = LongStream.rangeClosed(min, max).mapToObj(i -> letter + i)
                        .filter(checkBookNoList::contains)
                        .collect(Collectors.joining(","));
            } else {
                int zeroNum = collect.get(0).length() - (letter + min).length();
                StringBuffer zeroStr = new StringBuffer();
                for (int i = 0; i < zeroNum; i++) {
                    zeroStr.append("0");
                }
                startNo = letter + zeroStr + min;
                endNo = letter + zeroStr + max;

                checkBillNos = LongStream.rangeClosed(min, max).mapToObj(i -> letter +zeroStr+i)
                        .filter(checkBookNoList::contains)
                        .collect(Collectors.joining(","));
            }
            List<Short> checkBillTypeList = checkBookNoGroup.stream().map(CheckStock::getCheckBillType).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(checkBillTypeList)) {
                checkBillType = checkBillTypeList.get(0);
            } else {
                checkBillType = null;
            }
            //     查出币种
            currency = checkBookNoGroup.stream().filter(item -> null != item.getCurrency()).collect(Collectors.toList()).get(0).getCurrency();
            CurrencyTenantDTO currencyTenantDTO = null;
            currencyTenantDTO = currencyQueryService.findById(currency);
            //     台账数量(张)
            checkquantity = Long.valueOf(checkBookNoGroup.size());
            //     入库日期
            busiDate = checkBookNoGroup.stream().filter(item -> null != item.getBusiDate()).collect(Collectors.toList()).get(0).getBusiDate();
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("checkBillNo", checkBookNo);
            paramMap.put("checkresult", CheckResultStatus.NoInventory.getValue());
            paramMap.put("inventoryrange", CheckDir.Pay.getValue());
            paramMap.put("startNo", startNo);
            paramMap.put("endNo", endNo);
            paramMap.put("checkBillType", checkBillType);
            paramMap.put("currency", currency);
            paramMap.put("currency_name", currencyTenantDTO.getName());
            paramMap.put("checkquantity", checkquantity);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            paramMap.put("busiDate", sdf.format(busiDate));
            paramMap.put("checkid", checkid);

            //空白支票：带出入库单的银行网点名称数据；收到支票：按照会计主体+出票人账号+付款银行网点名称为维度显示收票入库的付款银行网点名称数据
            paramMap.put("bank", checkBookNoGroup.get(0).get("mainid_account_bankNumber"));
            paramMap.put("bank_name", checkBookNoGroup.get(0).get("mainid_account_bankNumber_name"));
            //空白支票 带出入库单的银行账号数据;收到支票：按照会计主体+出票人账号+付款银行网点名称为维度显示收票入库的出票人账号数据
            paramMap.put("accountNo", checkBookNoGroup.get(0).get("mainid_account_code"));
            //空白支票：显示本次待盘点的支票编号，逗号相隔；收到支票：以会计主体+出票人账号+付款银行网点名称为维度显示所有的待盘点的支票编号，逗号相隔
            paramMap.put("checkNumber", checkBillNos);

            return paramMap;
        }
        return null;
    }

    private Map<String, Object> buildDataByDir(String checkBillDir, String accoutNo, List<CheckStock> checkBillDirGroup) throws Exception {
        List<Long> checkids = checkBillDirGroup.stream().map(e -> Long.parseLong(e.get("id").toString())).collect(Collectors.toList());
        String checkid = StringUtils.join(checkids, ",");
        if (CollectionUtils.isNotEmpty(checkBillDirGroup)) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("checkresult", CheckResultStatus.NoInventory.getValue());
            paramMap.put("inventoryrange", checkBillDir);
            paramMap.put("checkquantity", (long) checkBillDirGroup.size());
            paramMap.put("checkid", checkid);

            paramMap.put("bank", checkBillDirGroup.get(0).get("payBank"));
            paramMap.put("bank_name", checkBillDirGroup.get(0).get("payBank_name"));
            paramMap.put("accountNo", checkBillDirGroup.get(0).get("drawerAcctNo"));
            paramMap.put("checkNumber", checkBillDirGroup.stream().map(CheckStock::getCheckBillNo).sorted().collect(Collectors.joining(",")));
//            paramMap.put("prevPeriodCount", null);
//            paramMap.put("curUseCount", null);
//            paramMap.put("curHandleCount", null);
            CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(checkBillDirGroup.get(0).getCurrency());
            paramMap.put("checkBillNo", checkBillDirGroup.get(0).getCheckBillNo());
            paramMap.put("checkBillType", checkBillDirGroup.get(0).getCheckBillType());
            paramMap.put("currency", checkBillDirGroup.get(0).getCurrency());
            paramMap.put("currency_name", currencyTenantDTO.getName());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            paramMap.put("busiDate", sdf.format(checkBillDirGroup.get(0).getBusiDate()));
            return paramMap;
        }
        return null;
    }
}