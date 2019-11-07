/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.spi.TypeConfiguration;

public class ListType extends CollectionType {

	public ListType(TypeConfiguration typeConfiguration, String role, String propertyRef) {
		super( typeConfiguration, role, propertyRef );
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentList( session );
	}

	@Override
	public Class getReturnedClass() {
		return List.class;
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentList( session, (List) collection );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

	@Override
	public Object indexOf(Object collection, Object element) {
		List list = (List) collection;
		for ( int i=0; i<list.size(); i++ ) {
			//TODO: proxies!
			if ( list.get(i)==element ) {
				return i;
			}
		}
		return null;
	}
}
