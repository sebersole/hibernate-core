/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.predicate;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * @author Steve Ebersole
 */
public class NullnessPredicate implements Predicate {
	private final Expression expression;
	private final boolean negated;

	public NullnessPredicate(Expression expression, boolean negated) {
		this.expression = expression;
		this.negated = negated;
	}

	public Expression getExpression() {
		return expression;
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
		sqlTreeWalker.visitNullnessPredicate( this );
	}
}
