/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
public interface SetPersistentAttribute<O,E> extends PluralPersistentAttribute<O,Set<E>,E>,
		javax.persistence.metamodel.SetAttribute<O,E> {
}
