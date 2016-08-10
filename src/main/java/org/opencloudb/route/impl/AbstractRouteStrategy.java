package org.opencloudb.route.impl;

import org.apache.log4j.Logger;
import org.opencloudb.MycatServer;
import org.opencloudb.cache.LayerCachePool;
import org.opencloudb.config.model.SchemaConfig;
import org.opencloudb.config.model.SystemConfig;
import org.opencloudb.mpp.LoadData;
import org.opencloudb.route.RouteResultset;
import org.opencloudb.route.RouteStrategy;
import org.opencloudb.route.util.RouterUtil;
import org.opencloudb.server.ServerConnection;
import org.opencloudb.server.parser.ServerParse;

import java.sql.SQLNonTransientException;
import java.sql.SQLSyntaxErrorException;

public abstract class AbstractRouteStrategy implements RouteStrategy {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractRouteStrategy.class);

	@Override
	public RouteResultset route(SystemConfig sysConfig, SchemaConfig schema, int sqlType, String origSQL,
			String charset, ServerConnection sc, LayerCachePool cachePool) throws SQLNonTransientException {

		/**
		 * 处理一些路由之前的逻辑
         * 全局序列号，父子表插入
		 */
		if ( beforeRouteProcess(schema, sqlType, origSQL, sc) )
			return null;

		/**
		 * SQL 语句拦截
		 */
		String stmt = MycatServer.getInstance().getSqlInterceptor().interceptSQL(origSQL, sqlType);
		if (origSQL != stmt && LOGGER.isDebugEnabled()) {
			LOGGER.debug("sql intercepted to " + stmt + " from " + origSQL);
		}

        // 对应schema标签checkSQLschema属性，把表示schema的字符去掉
		if (schema.isCheckSQLSchema()) {
			stmt = RouterUtil.removeSchema(stmt, schema.getName());
		}

		RouteResultset rrs = new RouteResultset(stmt, sqlType);

		/**
		 * 优化debug loaddata输出cache的日志会极大降低性能
		 */
		if (LOGGER.isDebugEnabled() && origSQL.startsWith(LoadData.loadDataHint)) {
			rrs.setCacheAble(false);
		}

        /**
         * rrs携带ServerConnection的autocommit状态用于在sql解析的时候遇到
         * select ... for update的时候动态设定RouteResultsetNode的canRunInReadDB属性
         */
		if (sc != null ) {
			rrs.setAutocommit(sc.isAutocommit());
		}

		/**
		 * DDL 语句的路由
		 */
		if (ServerParse.DDL == sqlType) {
			return RouterUtil.routeToDDLNode(rrs, sqlType, stmt, schema);
		}
		
		/**
		 * 检查是否有分片
		 */
		if (schema.isNoSharding() && ServerParse.SHOW != sqlType) {
			rrs = RouterUtil.routeToSingleNode(rrs, schema.getDataNode(), stmt);
		} else {
			RouteResultset returnedSet = routeSystemInfo(schema, sqlType, stmt, rrs);
			if (returnedSet == null) {
				rrs = routeNormalSqlWithAST(schema, stmt, rrs, charset, cachePool);
			}
		}

		return rrs;
	}

	/**
	 * 路由之前必要的处理
	 */
	private boolean beforeRouteProcess(SchemaConfig schema, int sqlType, String origSQL, ServerConnection sc)
			throws SQLNonTransientException {
        /**
         * 这里利用了Java的一个特性，||表达式，前半部分如果为真，则后半部分不会被执行。
         * 首先执行RouterUtil.processWithMycatSeq(schema, sqlType, origSQL, sc)，这个方法是判断是否是显示使用全局序列号的sql语句，
         * 比如像：insert into table1(id,name) values(next value for MYCATSEQ_GLOBAL,‘test’);
         * 对于这样的语句处理是先将改写next value for MYCATSEQ_GLOBAL 为调用全局ID生成的ID，之后进入AST语句解析路由。
         *
         * 如果不是，则执行(sqlType == ServerParse.INSERT && RouterUtil.processERChildTable(schema, origSQL, sc))，这个方法判断是否是子表插入
         */
		return RouterUtil.processWithMycatSeq(schema, sqlType, origSQL, sc)
				|| (sqlType == ServerParse.INSERT && RouterUtil.processERChildTable(schema, origSQL, sc))
				|| (sqlType == ServerParse.INSERT && RouterUtil.processInsert(schema, sqlType, origSQL, sc));
	}

	/**
	 * 通过解析AST语法树类来寻找路由
	 */
	public abstract RouteResultset routeNormalSqlWithAST(SchemaConfig schema, String stmt, RouteResultset rrs,
			String charset, LayerCachePool cachePool) throws SQLNonTransientException;

	/**
	 * 路由信息指令, 如 SHOW、SELECT@@、DESCRIBE
	 */
	public abstract RouteResultset routeSystemInfo(SchemaConfig schema, int sqlType, String stmt, RouteResultset rrs)
			throws SQLSyntaxErrorException;

	/**
	 * 解析 Show 之类的语句
	 */
	public abstract RouteResultset analyseShowSQL(SchemaConfig schema, RouteResultset rrs, String stmt)
			throws SQLNonTransientException;

}
