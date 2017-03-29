/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import javax.persistence.ParameterMode;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class JdbcCallParameterRegistrationImpl implements JdbcCallParameterRegistration {
	private final String name;
	private final int jdbcParameterPositionStart;
	private final ParameterMode parameterMode;
	private final int jdbcTypeCode;
	private final Type ormType;
	private final JdbcParameterBinder parameterBinder;
	private final JdbcCallParameterExtractorImpl parameterExtractor;
	private final JdbcCallRefCursorExtractorImpl refCursorExtractor;

	public JdbcCallParameterRegistrationImpl(
			String name,
			int jdbcParameterPositionStart,
			ParameterMode parameterMode,
			int jdbcTypeCode,
			Type ormType,
			JdbcParameterBinder parameterBinder,
			JdbcCallParameterExtractorImpl parameterExtractor,
			JdbcCallRefCursorExtractorImpl refCursorExtractor) {
		this.name = name;
		this.jdbcParameterPositionStart = jdbcParameterPositionStart;
		this.parameterMode = parameterMode;
		this.jdbcTypeCode = jdbcTypeCode;
		this.ormType = ormType;
		this.parameterBinder = parameterBinder;
		this.parameterExtractor = parameterExtractor;
		this.refCursorExtractor = refCursorExtractor;
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return parameterBinder;
	}

	@Override
	public JdbcCallParameterExtractor getParameterExtractor() {
		return parameterExtractor;
	}

	@Override
	public JdbcCallRefCursorExtractorImpl getRefCursorExtractor() {
		return refCursorExtractor;
	}

	@Override
	public ParameterMode getParameterMode() {
		return parameterMode;
	}

	@Override
	public void registerParameter(
			CallableStatement callableStatement, SharedSessionContractImplementor session) {
		switch ( parameterMode ) {
			case REF_CURSOR: {
				registerRefCursorParameter( callableStatement, session );
				break;
			}
			case IN: {
				// nothing to prepare
				break;
			}
			default: {
				// OUT and INOUT
				registerOutputParameter( callableStatement, session );
				break;
			}
		}
	}

	private void registerRefCursorParameter(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		if ( name != null ) {
			session.getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.registerRefCursorParameter( callableStatement, name );
		}
		else {
			session.getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.registerRefCursorParameter( callableStatement, jdbcParameterPositionStart );
		}

	}

	private void registerOutputParameter(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		if ( ormType.getColumnSpan() > 1 ) {
			assert name == null;

			// there is more than one column involved; see if the Hibernate Type can handle
			// multi-param extraction...
			if ( ProcedureParameterExtractionAware.class.isInstance( ormType ) ) {
				final ProcedureParameterExtractionAware extractionAware = (ProcedureParameterExtractionAware) ormType;
				if ( ! extractionAware.canDoExtraction() ) {
					// it cannot...
					throw new UnsupportedOperationException(
							"Type [" + ormType + "] does support multi-parameter value extraction"
					);
				}
			}

			// todo : this needs CompositeType etal to be finished

			try {
				for ( int i = 0; i < ormType.getColumnSpan(); i++ ) {
					callableStatement.registerOutParameter( jdbcParameterPositionStart + i, ormType.sqlTypes()[i] );
				}
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to register CallableStatement out parameter" );
			}
		}
		else {
			// again, needs Type to get finished (access to JDBC type code
			try {
				if ( name != null ) {
					callableStatement.registerOutParameter( name, ormType.sqlTypes()[0] );
				}
				else {
					callableStatement.registerOutParameter( jdbcParameterPositionStart, ormType.sqlTypes()[0] );
				}
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to register CallableStatement out parameter" );
			}
		}
	}
}
