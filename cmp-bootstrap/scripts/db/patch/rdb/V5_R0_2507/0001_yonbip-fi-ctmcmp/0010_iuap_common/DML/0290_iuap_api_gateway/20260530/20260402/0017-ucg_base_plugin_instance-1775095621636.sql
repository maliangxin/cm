-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ucg_base_plugin_instance
-- ----------------------------
DELETE FROM ucg_base_plugin_instance WHERE superior_id IN ('2401384874613145608') and strategy_id not in ("2077039046279823362", "2077039295387926534", "2077039501546356741", "2077039767834329096");

-- beginBatch
REPLACE INTO  `ucg_base_plugin_instance`(`id`,`open_status`,`plugin_id`,`required`,`seq`,`strategy_id`,`strategy_name`,`superior_id`,`superior_level`,`gmt_create`,`gmt_update`,`dr`,`ytenant_id`,`micro_service_code`,`applicationCode`) VALUES 
('2506749828369219592',1,'09ecc1b0-9d7f-41d1-803a-e78ea2f4e88b',1,NULL,NULL,NULL,'2401384874613145608','api','2026-04-01 14:14:48.000','2026-04-01 14:14:48.000',0,'0',NULL,NULL) ;
-- endBatch

