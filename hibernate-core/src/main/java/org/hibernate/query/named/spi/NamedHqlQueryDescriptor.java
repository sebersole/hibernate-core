/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * @author Steve Ebersole
 */
public interface NamedHqlQueryDescriptor extends NamedQueryDescriptor {
	String getHqlString();

	@Override
	default String getQueryString() {
		return getHqlString();
	}

	// todo (6.0) : args?
	QueryImplementor toQuery(SharedSessionContractImplementor session);

	NamedHqlQueryDescriptor makeCopy(String name);
}
