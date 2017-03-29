/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.persister.common.spi.ConvertibleNavigable;
import org.hibernate.sqm.domain.SqmPluralAttributeIndexBasic;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public interface CollectionIndexBasic<T> extends CollectionIndex<T,BasicType<T>>, ConvertibleNavigable<T>, SqmPluralAttributeIndexBasic {
	@Override
	BasicType<T> getOrmType();

	@Override
	BasicType<T> getExportedDomainType();
}
