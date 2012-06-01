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
package org.hibernate.engine2.transaction.spi;

import org.hibernate.engine.transaction.spi.SynchronizationRegistry;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;

/**
 * Models the bridge between the Hibernate engine and physical transactions in relation to a database session.
 *
 * @author Steve Ebersole
 */
public interface TransactionContext {

	/**
	 * Get the Hibernate transaction facade object currently associated with this coordinator.
	 *
	 * @return The current Hibernate transaction.
	 */
	public TransactionImplementor getTransaction();

	/**
	 * Obtain the {@link javax.transaction.Synchronization} registry associated with this coordinator.
	 *
	 * @return The registry
	 */
	public SynchronizationRegistry getSynchronizationRegistry();

	/**
	 * Adds an observer to the coordinator.
	 * <p/>
	 * Unlike synchronizations added to the {@link #getSynchronizationRegistry() registry}, observers are not to be
	 * cleared on transaction completion.
	 *
	 * @param observer The observer to add.
	 */
	public void addObserver(TransactionObserver observer);

	/**
	 * Can we join to the underlying transaction?
	 *
	 * @return {@literal true} if the underlying transaction can be joined or is already joined; {@literal false}
	 * otherwise.
	 *
	 * @see org.hibernate.engine.transaction.spi.TransactionFactory#isJoinableJtaTransaction
	 */
	public boolean isTransactionJoinable();

	/**
	 * Is the underlying transaction already joined?
	 *
	 * @return {@literal true} if the underlying transaction is already joined; {@literal false} otherwise.
	 */
	public boolean isTransactionJoined();

	/**
	 * Reset the transaction's join status.
	 */
	public void resetJoinStatus();

	/**
	 * Are we "in" an active and joined transaction
	 *
	 * @return {@literal true} if there is currently a transaction in progress; {@literal false} otherwise.
	 */
	public boolean isTransactionInProgress();

	/**
	 * Attempts to register JTA synchronization if possible and needed.
	 */
	public void pulse();
}
