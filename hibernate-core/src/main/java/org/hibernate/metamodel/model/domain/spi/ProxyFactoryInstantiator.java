/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.proxy.ProxyFactory;

/**
 * @author Chris Cranford
 */
public interface ProxyFactoryInstantiator<J> {
	ProxyFactory instantiate(
			AbstractEntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext);
}
