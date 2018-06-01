/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeCollection;

/**
 * When a transient entity is passed to lock(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 * existing snapshot
 * 3. throw an exception for each "new" collection
 *
 * @author Gavin King
 */
public class OnLockVisitor extends ReattachVisitor {

	public OnLockVisitor(EventSource session, Object key, Object owner) {
		super( session, key, owner );
	}

	@Override
	public Object processCollection(Object collection, PluralAttributeCollection attributeCollection) throws HibernateException {
		if ( collection == null ) {
			return null;
		}

		final SessionImplementor session = getSession();

		if ( collection instanceof PersistentCollection ) {
			final PersistentCollection persistentCollection = (PersistentCollection) collection;
			if ( persistentCollection.setCurrentSession( session ) ) {
				final PersistentCollectionDescriptor descriptor = session.getFactory()
						.getMetamodel()
						.findCollectionDescriptor( attributeCollection.getNavigableName() );
				if ( isOwnerUnchanged( persistentCollection, descriptor, extractCollectionKeyFromOwner( descriptor ) ) ) {
					// a "detached" collection that originally belonged to the same entity
					if ( persistentCollection.isDirty() ) {
						throw new HibernateException( "reassociated object has dirty collection" );
					}
					reattachCollection( persistentCollection, attributeCollection.getNavigableRole() );
				}
				else {
					// a "detached" collection that belonged to a different entity
					throw new HibernateException( "reassociated object has dirty collection reference" );
				}
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				throw new HibernateException( "reassociated object has dirty collection reference" );
			}
		}
		else {
			// brand new collection
			//TODO: or an array!! we can't lock objects with arrays now??
			throw new HibernateException( "reassociated object has dirty collection reference (or an array)" );
		}

		return null;
	}

}
