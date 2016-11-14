/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.result.Outputs;
import org.hibernate.sql.sqm.ast.SelectQuery;
import org.hibernate.sql.sqm.convert.spi.Callback;
import org.hibernate.sql.sqm.convert.spi.NotYetImplementedException;
import org.hibernate.sql.spi.ParameterBinder;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.sql.sqm.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.sqm.exec.spi.PreparedStatementExecutor;
import org.hibernate.query.QueryOptions;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;
import org.hibernate.sql.sqm.exec.spi.SqlTreeExecutor;

/**
 * Standard SqlTreeExecutor implementation
 *
 * @author Steve Ebersole
 */
public class SqlTreeExecutorImpl implements SqlTreeExecutor {
	@Override
	public <R, T> R executeSelect(
			SelectQuery sqlTree,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor<R, T> preparedStatementExecutor,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor session) {
		// Walk the SQL AST.  This produces:
		//		* SQL string
		//		* ParameterBinders
		//		* Returns

		// todo : should also pass in QueryOptions
		// 		as the rendered SQL would depend on first/max results, comment, db-hints, lock-options, entity-graph

		// todo : also need to account for multi-valued param bindings in terms of the generated SQL...

		final SqlTreeWalker sqlTreeWalker = new SqlTreeWalker( session.getFactory(), queryParameterBindings );
		sqlTreeWalker.visitSelectQuery( sqlTree );

		// Now start the execution
		final LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
		final Connection connection = logicalConnection.getPhysicalConnection();

		final JdbcServices jdbcServices = session.getFactory().getServiceRegistry().getService( JdbcServices.class );

		final String sql = sqlTreeWalker.getSql();
		try {
			jdbcServices.getSqlStatementLogger().logStatement( sql );

			// prepare the query
			final PreparedStatement ps = statementCreator.create( connection, sql );
			logicalConnection.getResourceRegistry().register( ps, true );

			// set options
			if ( queryOptions.getFetchSize() != null ) {
				ps.setFetchSize( queryOptions.getFetchSize() );
			}
			if ( queryOptions.getTimeout() != null ) {
				ps.setQueryTimeout( queryOptions.getTimeout() );
			}

			// bind parameters
			// 		todo : validate that all query parameters were bound?
			int position = 1;
			for ( ParameterBinder parameterBinder : sqlTreeWalker.getParameterBinders() ) {
				position += parameterBinder.bindParameterValue(
						ps,
						position,
						queryParameterBindings,
						session
				);
			}

			return preparedStatementExecutor.execute(
					ps,
					queryOptions,
					sqlTreeWalker.getReturns(),
					rowTransformer,
					session
			);
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + sql + "]"
			);
		}
		finally {
			logicalConnection.afterStatement();
		}
	}

	@Override
	public Object[] executeInsert(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public int executeUpdate(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public int executeDelete(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public <T> Outputs executeCall(
			String callableName,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedException( "Procedure/function call execution is not yet implemented" );
	}
}
