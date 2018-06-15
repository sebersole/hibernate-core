/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * If a collection is extra lazy and has queued ops, we still need to
 * process them.  Ex: OneToManyPersister needs to insert indexes for List
 * collections.  See HHH-8083.
 * 
 * @author Brett Meyer
 */
public final class QueuedOperationCollectionAction extends CollectionAction {
	
	/**
	 * Constructs a CollectionUpdateAction
	 *  @param collection The collection to update
	 * @param descriptor The collection descriptor
	 * @param id The collection key
	 * @param session The session
	 */
	public QueuedOperationCollectionAction(
			final PersistentCollection collection,
			final PersistentCollectionDescriptor descriptor,
			final Object id,
			final SharedSessionContractImplementor session) {
		super( descriptor, collection, id, session );
	}

	@Override
	public void execute() throws HibernateException {
		getPersistentCollectionDescriptor().processQueuedOps( getCollection(), getKey(), getSession() );
	}
}
