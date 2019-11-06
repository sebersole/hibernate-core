/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * todo (6.0) : most of this is SQM -> SQL specific.  Should move to that package.
 * 		As-is, this complicates SQL AST creation from model walking e.g.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationState {
	SqlAstCreationContext getCreationContext();

	SqlAstProcessingState getCurrentProcessingState();

	SqlExpressionResolver getSqlExpressionResolver();

	FromClauseAccess getFromClauseAccess();

	SqlAliasBaseGenerator getSqlAliasBaseGenerator();

	LockMode determineLockMode(String identificationVariable);

	/**
	 * Visit fetches for the given parent.
	 *
	 * We walk fetches via the SqlAstCreationContext because each "context"
	 * will define differently what should be fetched (HQL versus load)
	 */
	List<Fetch> visitFetches(FetchParent fetchParent);
}
