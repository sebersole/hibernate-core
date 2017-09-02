/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.ParameterExpression;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

/**
 * Hibernate ORM specialization of the JPA {@link ParameterExpression}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaParameterExpression<T>
		extends QueryParameter<T>, JpaExpressionImplementor<T>, ParameterExpression<T> {
}
