/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine2.transaction.internal;

import java.util.List;

import org.hibernate.ResourceClosedException;
import org.hibernate.engine.transaction.internal.SynchronizationRegistryImpl;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.engine.transaction.synchronization.internal.SynchronizationCallbackCoordinatorImpl;
import org.hibernate.engine2.transaction.spi.TransactionContext;

/**
 * @author Steve Ebersole
 */
public class TransactionContextImpl implements TransactionContext {

	private final transient List<TransactionObserver> observers;
	private final transient SynchronizationRegistryImpl synchronizationRegistry;

	private transient TransactionImplementor currentHibernateTransaction;

	private transient SynchronizationCallbackCoordinatorImpl callbackCoordinator;

	private transient boolean open = true;
	private transient boolean synchronizationRegistered;
	private transient boolean ownershipTaken;


	@Override
	public TransactionImplementor getTransaction() {
		if ( ! open ) {
			throw new ResourceClosedException( "This TransactionCoordinator has been closed" );
		}
		pulse();
		return currentHibernateTransaction;
	}
}
