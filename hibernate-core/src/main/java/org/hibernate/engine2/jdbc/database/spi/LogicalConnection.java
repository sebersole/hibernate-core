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
package org.hibernate.engine2.jdbc.database.spi;

import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.ConnectionObserver;

/**
 * Models the logical notion of a JDBC {@link java.sql.Connection}.  Basically responsible for managing the
 * Connection currently being used given the notion of {@link org.hibernate.ConnectionReleaseMode}.
 *
 * @author Steve Ebersole
 */
public interface LogicalConnection extends ConnectionReleaseInflow {

	// LogicalConnection ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Is this logical connection open?  Another phraseology sometimes used is: "are we
	 * logically connected"?
	 *
	 * @return True if logically connected; false otherwise.
	 */
	public boolean isOpen();

	/**
	 * Is this logical connection instance "physically" connected.  Meaning
	 * do we currently internally have a cached connection.
	 *
	 * @return True if physically connected; false otherwise.
	 */
	public boolean isPhysicallyConnected();

	/**
	 * Retrieves the shareable connection proxy.
	 *
	 * @return The shareable connection proxy.
	 */
	public Connection getShareableConnectionProxy();

	/**
	 * Retrieves a distinct connection proxy.  It is distinct in that it is not shared with others unless the caller
	 * explicitly shares it.
	 *
	 * @return The distinct connection proxy.
	 */
	public Connection getDistinctConnectionProxy();

	/**
	 * Release the underlying connection and clean up any other resources associated
	 * with this logical connection.
	 * <p/>
	 * This leaves the logical connection in a "no longer usable" state.
	 *
	 * @return The application-supplied connection, or {@code null} if Hibernate was managing connection.
	 */
	public Connection close();

	/**
	 * Add an observer interested in notification of connection events.
	 *
	 * Observers are released when the LogicalConnection is {@link #close() closed}.  A specialization of the observer
	 * contract, {@link org.hibernate.engine.jdbc.spi.NonDurableConnectionObserver}, indicates that the observer should
	 * be released whenever the physical connection is released.
	 *
	 * @param observer The observer.
	 */
	public void addObserver(ConnectionObserver observer);


	// LogicalConnectionImplementor~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection maintained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	public Connection manualDisconnect();

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at some point after manualDisconnect().
	 *
	 * @param suppliedConnection For user supplied connection strategy the user needs to hand us the connection
	 * with which to reconnect.  It is an error to pass a connection in the other strategies.
	 */
	public void manualReconnect(Connection suppliedConnection);

	public boolean isAutoCommit();

	public boolean isReadyForSerialization();
}
