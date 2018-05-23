/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.internal.QueryParameterNamedImpl;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.spi.ParameterRecognizer;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class ParameterRecognizerImpl implements ParameterRecognizer {
	private enum PositionalParameterStyle {
		/**
		 * Ordinal
		 */
		JDBC,
		/**
		 * Positional
		 */
		JPA
	}

	private final int ordinalParameterBase;

	private boolean hadMainOutputParameter;

	private Map<String,QueryParameterImplementor<?>> namedQueryParameters;
	private Map<Integer,QueryParameterImplementor<?>> positionalQueryParameters;

	private PositionalParameterStyle positionalParameterStyle;
	private int ordinalParameterImplicitPosition;

	private List<JdbcParameterBinder> parameterBinders;

	public ParameterRecognizerImpl(SessionFactoryImplementor factory) {
		if ( factory.getSessionFactoryOptions().isJpaBootstrap() ) {
			ordinalParameterBase = 1;
		}
		else {
			final Integer configuredBase = factory.getSessionFactoryOptions().getNonJpaNativeQueryOrdinalParameterBase();
			ordinalParameterBase = configuredBase == null
					? 1
					: configuredBase;
		}
		assert ordinalParameterBase == 0 || ordinalParameterBase == 1;

		ordinalParameterImplicitPosition = ordinalParameterBase;
	}

	public void validate() {
		if ( hadMainOutputParameter ) {
			throw new QueryException(
					"Calling database procedures/functions is no longer supported through the NativeQuery API; " +
							"use Hibernate's " + ProcedureCall.class.getName() + " API or JPA's " +
							StoredProcedureQuery.class.getName() + " API"
			);
		}

		// validate the positions.  JPA says that these should start with 1 and
		// increment contiguously (no gaps)
		int[] positionsArray = positionalQueryParameters.keySet().stream().mapToInt( Integer::intValue ).toArray();
		Arrays.sort( positionsArray );

		int previous = 0;
		boolean first = true;
		for ( Integer position : positionsArray ) {
			if ( position != previous + 1 ) {
				if ( first ) {
					throw new QueryException( "Positional parameters did not start with base [" + ordinalParameterBase + "] : " + position );
				}
				else {
					throw new QueryException( "Gap in positional parameter positions; skipped " + (previous+1) );
				}
			}
			first = false;
			previous = position;
		}
	}

	public Map<String, QueryParameterImplementor<?>> getNamedQueryParameters() {
		return namedQueryParameters;
	}

	public Map<Integer, QueryParameterImplementor<?>> getPositionalQueryParameters() {
		return positionalQueryParameters;
	}

	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recognition code

	@Override
	public void outParameter(int position) {
		if ( hadMainOutputParameter ) {
			throw new IllegalStateException( "Recognized multiple `mainOutputParameter`s" );
		}

		hadMainOutputParameter = true;
	}

	@Override
	public void ordinalParameter(int sourcePosition) {
		if ( positionalParameterStyle == PositionalParameterStyle.JPA ) {
			throw new IllegalStateException( "Cannot mix JDBC-style (?) and JPA-style (?1) parameters in the same query" );
		}

		positionalParameterStyle = PositionalParameterStyle.JDBC;

		int implicitPosition = ordinalParameterImplicitPosition++;

		if ( positionalQueryParameters == null ) {
			positionalQueryParameters = new HashMap<>();
		}
		positionalQueryParameters.put( implicitPosition, QueryParameterPositionalImpl.fromNativeQuery( implicitPosition ) );

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new PositionalQueryParameterBinderImpl( implicitPosition ) );
	}

	@Override
	public void namedParameter(String name, int sourcePosition) {
		if ( !namedQueryParameters.containsKey( name ) ) {
			if ( namedQueryParameters == null ) {
				namedQueryParameters = new HashMap<>();
			}
			namedQueryParameters.put( name, QueryParameterNamedImpl.fromNativeQuery( name ) );
		}

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new NamedQueryParameterBinder( name ) );
	}

	@Override
	public void jpaPositionalParameter(int position, int sourcePosition) {
		if ( positionalParameterStyle == PositionalParameterStyle.JDBC ) {
			throw new IllegalStateException( "Cannot mix JDBC-style (?) and JPA-style (?1) parameters in the same query" );
		}

		if ( position < 1 ) {
			throw new QueryException( "Incoming parameter position [" + position + "] is less than base [1]" );
		}

		positionalParameterStyle = PositionalParameterStyle.JPA;

		if ( positionalQueryParameters == null || !positionalQueryParameters.containsKey( position ) ) {
			if ( positionalQueryParameters == null ) {
				positionalQueryParameters = new HashMap<>();
			}
			positionalQueryParameters.put( position, QueryParameterPositionalImpl.fromNativeQuery( position ) );
		}

		if ( parameterBinders == null ) {
			parameterBinders = new ArrayList<>();
		}
		parameterBinders.add( new PositionalQueryParameterBinderImpl( position ) );
	}

	@Override
	public void other(char character) {
		// don't care...
	}
}
