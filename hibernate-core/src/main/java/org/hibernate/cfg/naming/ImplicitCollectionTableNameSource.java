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

import org.hibernate.metamodel.relational.ObjectName;

/**
 * Access to the source information used to determine the logical name for a collection table.
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.CollectionTable
 */
public interface ImplicitCollectionTableNameSource {
	/**
	 * Access to the physical name of the owning entity's primary table.
	 *
	 * @return Owning entity's primary table  name.
	 */
	public String getOwningPhysicalTableName();

	/**
	 * Access to entity naming information for the owning side.
	 *
	 * @return Owning entity naming information
	 */
	public EntityNamingSource getOwningEntityNamingSource();

	/**
	 * Access to the name of the attribute, from the owning side, that defines the association.
	 *
	 * @return The owning side's attribute name.
	 */
	public String getAssociationOwningAttributePath();
}
