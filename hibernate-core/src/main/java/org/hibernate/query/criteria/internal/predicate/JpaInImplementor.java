/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaIn;
import org.hibernate.query.sqm.tree.predicate.InSqmPredicate;

/**
 * Implementor of JpaIn.
 *
 * @author Christian Beikov
 */
public interface JpaInImplementor<T> extends JpaIn<T>, JpaPredicateImplementor, InSqmPredicate {

	@Override
	@SuppressWarnings("unchecked")
	default JpaExpression<T> getExpression() {
		return (JpaExpression<T>) getTestExpression();
	}
}
