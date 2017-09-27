/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;

/**
 * @author Steve Ebersole
 */
public interface HqlQueryImplementor<R> extends QueryImplementor<R> {
	default NamedHqlQueryDescriptor toNamedDescriptor(String name) {
		throw new NotYetImplementedFor6Exception();
	}
}
