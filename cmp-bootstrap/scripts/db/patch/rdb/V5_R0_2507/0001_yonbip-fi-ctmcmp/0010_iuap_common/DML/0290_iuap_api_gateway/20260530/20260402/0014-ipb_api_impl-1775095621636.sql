-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ipb_api_impl
-- ----------------------------
DELETE FROM ipb_api_impl WHERE api_id IN ('2401384874613145608');

-- beginBatch
REPLACE INTO  `ipb_api_impl`(`Id`,`name`,`description`,`api_id`,`impl_type`,`time_out`,`content_type`,`request_protocol`,`service_http_method`,`rest_impl_url`,`rpc_app_name`,`rpc_service_name`,`rpc_method_name`,`rpc_service_url`,`fun_id`,`program_id`,`flow_id`,`mock_return`,`ytenant_id`,`creator`,`create_time`,`modifier`,`pubts`,`modify_time`,`dr`) VALUES 
('2401384874613145630','货币兑换结果回传接口','货币兑换结果回传接口','2401384874613145608','REST',30,'application/json','HTTP','POST','commonapi/updateCurrDataAndGeneratorVoucher','','','','','',NULL,NULL,'{\n	"code": "200",\n	"data": "",\n	"message": "操作成功"\n}','0','ce9f7fd2-0f45-42cb-bfa2-8598566a1afe','2025-11-10T14:59:54','ce9f7fd2-0f45-42cb-bfa2-8598566a1afe','2026-04-01T14:14:48','2026-04-01T14:14:48',0) ;
-- endBatch

update ipb_api_impl set time_out = 30 where time_out > 30 and api_id IN ('2401384874613145608');

