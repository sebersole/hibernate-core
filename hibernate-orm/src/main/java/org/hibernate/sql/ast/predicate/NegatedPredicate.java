/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.predicate;

import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * @author Steve Ebersole
 */
public class NegatedPredicate implements Predicate {
	private final Predicate predicate;

	public NegatedPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public boolean isEmpty() {
		return predicate.isEmpty();
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter sqlTreeWalker) {
		sqlTreeWalker.visitNegatedPredicate( this );
	}
}
