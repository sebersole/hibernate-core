/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.predicate;

import org.hibernate.sql.ast.QuerySpec;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * @author Steve Ebersole
 */
public class InSubQueryPredicate implements Predicate {
	private final Expression testExpression;
	private final QuerySpec subQuery;
	private final boolean negated;

	public InSubQueryPredicate(Expression testExpression, QuerySpec subQuery, boolean negated) {
		this.testExpression = testExpression;
		this.subQuery = subQuery;
		this.negated = negated;
	}

	public Expression getTestExpression() {
		return testExpression;
	}

	public QuerySpec getSubQuery() {
		return subQuery;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter sqlTreeWalker) {
		sqlTreeWalker.visitInSubQueryPredicate( this );
	}
}
