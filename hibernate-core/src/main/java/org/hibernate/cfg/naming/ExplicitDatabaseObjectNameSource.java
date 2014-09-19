/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.naming;

/**
 * Database objects are generally defined within a schema and/or catalog (depending on the type and
 * RDBMS).  This is a common contract for sources of these database object names to consistently
 * give access to the explicitly specified schema/catalog parts.
 *
 * @author Steve Ebersole
 */
public interface ExplicitDatabaseObjectNameSource {
	/**
	 * Access to the explicitly specified schema name.
	 *
	 * @return The explicitly specified schema name, or {@code null} if not specified.
	 */
	public String getExplicitSchemaName();

	/**
	 * Access to the explicitly specified catalog name.
	 *
	 * @return The explicitly specified catalog name, or {@code null} if not specified.
	 */
	public String getExplicitCatalogName();
}
