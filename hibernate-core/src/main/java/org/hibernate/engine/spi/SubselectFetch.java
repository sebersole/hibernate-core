/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.NotYetImplementedException;

import org.jboss.logging.Logger;

/**
 * @author Gavin King
 */
public class SubselectFetch {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SubselectFetch.class.getName()
	);

	private static final String FROM_STRING = " from ";

	private final Set resultingEntityKeys;
	private final String queryString;
	private final String alias;
//	private final Loadable loadable;
//	private final QueryParameters queryParameters;
	private final Map namedParameterLocMap;

	/**
	 * Construct a SubselectFetch instance. The subselect fetch query fragment is generated by
	 * {@link #createSubselectFetchQueryFragment}.
	 *
	 * If the same value for {@code queryParameters} is to be used when constructing multiple
	 * SubselectFetch objects, then it is preferable to generate the subselect fetch query
	 * fragment using {@link #createSubselectFetchQueryFragment}, and pass the result as an
	 * argument to constructor {@link #SubselectFetch(String, String, Loadable, QueryParameters, Set, Map)}.
	 *
	 * @param alias - the table alias used in the subselect fetch query fragment
	 * (to be generated by {@link #createSubselectFetchQueryFragment(QueryParameters)} that
	 * corresponds to {@code loadable};
	 * @param loadable - the {@link Loadable} for the associated entities to be subselect fetched;
	 * @param queryParameters - the query parameters;
	 * @param resultingEntityKeys - the {@link EntityKey} objects for the entities to be subselect fetched;
	 * @param namedParameterLocMap - mapping from named parameter to the parameter index located in the
	 * subselect fetch query fragment.
	 *
	 * @see #SubselectFetch(String, String, Loadable, QueryParameters, Set, Map)
	 */
	public SubselectFetch(
			final String alias,
//			final Loadable loadable,
//			final QueryParameters queryParameters,
			final Set resultingEntityKeys,
			final Map namedParameterLocMap) {
		throw new NotYetImplementedException(  );
//		this(
//				createSubselectFetchQueryFragment( queryParameters ),
//				alias,
//				loadable,
//				queryParameters,
//				resultingEntityKeys,
//				namedParameterLocMap
//		);
	}

	/**
	 * Construct a SubselectFetch instance using the provided subselect fetch query fragment,
	 * {@code subselectFetchQueryFragment}. It is assumed that {@code subselectFetchQueryFragment}
	 * is the result of calling {@link #createSubselectFetchQueryFragment} with the same value
	 * provided for {@code queryParameters}.
	 *
	 * @param subselectFetchQueryFragment - the subselect fetch query fragment;
	 * @param alias - the table alias used in {@code subselectFetchQueryFragment} that
	 * corresponds to {@code loadable};
	 * @param loadable - the {@link Loadable} for the associated entities to be subselect fetched;
	 * @param queryParameters - the query parameters;
	 * @param resultingEntityKeys - the {@link EntityKey} objects for the entities to be subselect fetched;
	 * @param namedParameterLocMap - mapping from named parameter to the parameter index located in the
	 * subselect fetch query fragment.
	 */
	public SubselectFetch(
			final String subselectFetchQueryFragment,
			final String alias,
//			final Loadable loadable,
//			final QueryParameters queryParameters,
			final Set resultingEntityKeys,
			final Map namedParameterLocMap) {
		throw new NotYetImplementedException(  );
//		this.resultingEntityKeys = resultingEntityKeys;
//		this.queryParameters = queryParameters;
//		this.namedParameterLocMap = namedParameterLocMap;
//		this.loadable = loadable;
//		this.alias = alias;
//
//		this.queryString = subselectFetchQueryFragment;
	}

	/**
	 * Create the subselect fetch query fragment for the provided {@link QueryParameters}
	 * with SELECT and ORDER BY clauses removed.
	 *
	 * @param queryParameters -the query parameters.
	 * @return the subselect fetch query fragment.
	 */
//	public static String createSubselectFetchQueryFragment(QueryParameters queryParameters) {
//		//TODO: ugly here:
//		final String queryString = queryParameters.getFilteredSQL();
//		final int fromIndex = getFromIndex( queryString );
//		final int orderByIndex = queryString.lastIndexOf( "order by" );
//		final String subselectQueryFragment =  orderByIndex > 0
//				? queryString.substring( fromIndex, orderByIndex )
//				: queryString.substring( fromIndex );
//		if ( LOG.isTraceEnabled() ) {
//			LOG.tracef( "SubselectFetch query fragment: %s", subselectQueryFragment );
//		}
//		return subselectQueryFragment;
//	}
//
//	private static int getFromIndex(String queryString) {
//		int index = queryString.indexOf( FROM_STRING );
//
//		if ( index < 0 ) {
//			return index;
//		}
//
//		while ( !parenthesesMatch( queryString.substring( 0, index ) ) ) {
//			String subString = queryString.substring( index + FROM_STRING.length() );
//
//			int subIndex = subString.indexOf( FROM_STRING );
//
//			if ( subIndex < 0 ) {
//				return subIndex;
//			}
//
//			index += FROM_STRING.length() + subIndex;
//		}
//
//		return index;
//	}
//
//	private static boolean parenthesesMatch(String string) {
//		int parenCount = 0;
//
//		for ( int i = 0; i < string.length(); i++ ) {
//			char character = string.charAt( i );
//
//			if ( character == '(' ) {
//				parenCount++;
//			}
//			else if ( character == ')' ) {
//				parenCount--;
//			}
//		}
//
//		return parenCount == 0;
//	}
//
//	public QueryParameters getQueryParameters() {
//		return queryParameters;
//	}

	/**
	 * Get the Set of EntityKeys
	 */
	public Set getResult() {
		return resultingEntityKeys;
	}

//	public String toSubselectString(String ukname) {
//		String[] joinColumns = ukname == null
//				? StringHelper.qualify( alias, loadable.getIdentifierColumnNames() )
//				: ( (PropertyMapping) loadable ).toColumns( alias, ukname );
//
//		return "select " + StringHelper.join( ", ", joinColumns ) + queryString;
//	}

	@Override
	public String toString() {
		return "SubselectFetch(" + queryString + ')';
	}

	public Map getNamedParameterLocMap() {
		return namedParameterLocMap;
	}

}
