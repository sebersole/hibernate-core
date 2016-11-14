/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import org.hibernate.sqm.domain.SingularAttributeReference;
import org.hibernate.type.mapper.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface SingularAttributeImplementor extends SingularAttributeReference, SqmTypeImplementor {
	Type getType();
	Column[] getColumns();
}
