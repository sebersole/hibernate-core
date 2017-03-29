/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.sql.ast.expression.domain.NavigablePath;

/**
 * @author Steve Ebersole
 */
public interface FetchEntityAttribute extends EntityReference, FetchAttribute {
	@Override
	SingularPersistentAttributeEntity getFetchedAttributeDescriptor();

	default NavigablePath getNavigablePath() {
		return getFetchParent().getNavigablePath().append( getFetchedAttributeDescriptor().getNavigableName() );
	}
}
