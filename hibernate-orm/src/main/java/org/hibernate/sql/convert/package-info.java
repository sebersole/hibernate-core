/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Represents the Hibernate ORM functionality for interpreting a semantic query tree (SQM).  The
 * interpretation is performed by {@link org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter}
 * and represented by {@link org.hibernate.sql.exec.spi.JdbcSelect}.
 * <p/>
 * For execution of these interpretations see {@link org.hibernate.sql.exec}.
 */
package org.hibernate.sql.convert;
