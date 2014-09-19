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
 * Pluggable strategy contract for applying implicit naming rules in regards to
 * determining database names when an explicit names is not given.
 *
 * @author Steve Ebersole
 */
public interface ImplicitNamingStrategy {
	/**
	 * Determine the name of a entity's primary table given the source naming
	 * information, when a name is not explicitly given.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	public String determinePrimaryTableName(ImplicitPrimaryTableNameSource source);

	/**
	 * Determine the name of an association join table given the source naming
	 * information, when a name is not explicitly given.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	public String determineJoinTableName(ImplicitJoinTableNameSource source);

	/**
	 * Determine the name of an collection join table given the source naming
	 * information, when a name is not explicitly given.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	public String determineCollectionTableName(ImplicitCollectionTableNameSource source);

	/**
	 * Determine the name of an attribute's column given the source naming
	 * information, when a name is not explicitly given.
	 *
	 * @param source The source information
	 *
	 * @return The implicit column name.
	 */
	public String determineAttributeColumnName(ImplicitAttributeColumnNameSource source);

	public String determineCollectionJoinColumnName(ImplicitCollectionJoinColumnNameSource source);

}
