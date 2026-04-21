-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ucg_baseapi_param_info
-- ----------------------------
DELETE FROM ucg_baseapi_param_info WHERE api_id IN ('2401384874613145608');

-- beginBatch
REPLACE INTO  `ucg_baseapi_param_info`(`id`,`gmt_update`,`name`,`param_order`,`param_desc`,`param_type`,`request_param_type`,`api_id`,`arr`,`gmt_create`,`default_value`,`required`,`visible`,`dr`,`base_type`,`example`,`max_length`,`parent_id`,`regular_rule`,`ref_type`,`ref_type_context`,`ytenant_id`,`full_name`,`define_hidden`,`micro_service_code`,`applicationCode`,`integrate_object_id`,`def_param_id`,`format`,`decimals`,`row_limit`,`enable_multi`) VALUES 
('2402745150900535300','2026-04-01 14:14:48.000','data',0,'货币兑换数据','object','BodyParam','2401384874613145608',1,'2025-11-12 10:59:11.000','',1,1,0,1,NULL,'0',NULL,'','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),
('2402745150900535301','2026-04-01 14:14:48.000','id',0,'货币兑换id','string','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',1,1,0,1,'1905863330859843588','0','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),
('2402745150900535302','2026-04-01 14:14:48.000','contractNo',1,'外汇交易合约编号','string','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',0,1,0,1,'30859843588','0','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),
('2402745150900535303','2026-04-01 14:14:48.000','settlestatus',2,'交割状态；3：处理中，5：已交割，6：逾期，7：交割失败（已经交割完成的单据，不允许更新结果信息）','number','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',1,1,0,1,'3','1','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,0,NULL,0),
('2402745150900535304','2026-04-01 14:14:48.000','purchaseamount',3,'买入金额（必须大于0）','number','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',0,1,0,1,'10.01','28','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,8,NULL,0),
('2402745150900535305','2026-04-01 14:14:48.000','sellamount',4,'卖出金额（必须大于0）','number','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',0,1,0,1,'10.01','28','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,8,NULL,0),
('2402745150900535306','2026-04-01 14:14:48.000','exchangerate',5,'成交汇率','number','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',0,1,0,1,'1.3','28','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,NULL,8,NULL,0),
('2402745150900535307','2026-04-01 14:14:48.000','settledate',6,'交割日期','date','BodyParam','2401384874613145608',0,'2025-11-12 10:59:11.000','',0,1,0,1,'2023-01-24 12:12:12','0','2402745150900535300','','0',NULL,'0',NULL,0,NULL,NULL,NULL,NULL,'yyyy-MM-dd HH:mm:ss',NULL,NULL,0),
('2418522893653114899','2026-04-01 14:14:48.000','characterDef',7,'特征组','characteristic','BodyParam','2401384874613145608',0,'2025-12-03 17:12:02.000','',0,1,0,1,NULL,'0','2402745150900535300','','0',NULL,'0','cmp.currencyexchange.CurrencyExchange',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0) ;
-- endBatch

