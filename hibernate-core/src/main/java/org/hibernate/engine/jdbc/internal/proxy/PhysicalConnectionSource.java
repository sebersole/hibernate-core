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
package org.hibernate.engine.jdbc.internal.proxy;

import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.NonDurableConnectionObserver;

/**
 * @author Steve Ebersole
 */
public interface PhysicalConnectionSource {
	/**
	 * Obtains access to the physical JDBC connection
	 *
	 * @return The physical JDBC connection
	 */
	public Connection getPhysicalConnection();

	/**
	 * Obtains the JDBC services associated with this source
	 *
	 * @return JDBC services
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Obtains the JDBC resource registry associated with this source
	 *
	 * @return The JDBC resource registry.
	 */
	public JdbcResourceRegistry getJdbcResourceRegistry();

	/**
	 * Add an observer interested in notification of connection events.  Specifically, only the non-durable
	 * variety are allowed to be registered via this contract
	 *
	 * @param observer The observer.
	 */
	public void addObserver(NonDurableConnectionObserver observer);

	/**
	 * Callback (inflow) notifying the source that a JDBC statement has been prepared.
	 */
	public void statementPrepared();

	/**
	 * Callback (inflow) notifying the source that a JDBC statement has been executed.
	 */
	public void statementExecuted();
}
