package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.dao.meta.crud.MetaDaoSupport;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.List;
import java.util.Map;

public class CommonSqlExecutor extends MetaDaoSupport {

	public CommonSqlExecutor(SqlSessionTemplate sqlSession) {
		super(sqlSession);
	}

	public void executeSql(String sql) throws Exception {
		 new NativeSqlSession(getSqlSession()).update(sql);
	}

	public void executeSql(String sql, Object value) throws Exception {
		new NativeSqlSession(getSqlSession()).update(sql, value);
	}

	public List<Map<String,Object>> executeSelectSql(String sql) throws Exception {
		return new NativeSqlSession(getSqlSession()).selectList(sql);
	}

	public List<Map<String, Object>> executeSelectSql(String sql, Object value) throws Exception {
		return new NativeSqlSession(getSqlSession()).selectList(sql, value);
	}

	public void executeInsertSql(String sql) throws Exception {
		new NativeSqlSession(getSqlSession()).insert(sql);
	}
}
