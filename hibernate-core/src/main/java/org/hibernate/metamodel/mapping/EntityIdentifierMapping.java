/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public interface EntityIdentifierMapping extends ValueMapping, ModelPart {
	String ROLE_LOCAL_NAME = "{id}";

	PropertyAccess getPropertyAccess();

	@Override
	default String getPartName() {
		return ROLE_LOCAL_NAME;
	}
}
