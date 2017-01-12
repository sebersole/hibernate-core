/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor;

import javax.persistence.EnumType;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * More-or-less a parameter-object intended for use in determining the SQL/JDBC type recommended
 * by the JDBC spec (explicitly or implicitly) for a given Java type.
 *
 * @see JavaTypeDescriptor#getJdbcRecommendedSqlType
 *
 * @author Steve Ebersole
 */
public interface JdbcRecommendedSqlTypeMappingContext {
	/**
	 * Was nationalized character datatype requested for the given Java type?
	 *
	 * @return {@code true} if nationalized character datatype should be used; {@code false} otherwise.
	 */
	boolean isNationalized();

	/**
	 * Was LOB datatype requested for the given Java type?
	 *
	 * @return {@code true} if LOB datatype should be used; {@code false} otherwise.
	 */
	boolean isLob();

	/**
	 * For enum mappings, what style of storage was requested (name vs. ordinal)?
	 *
	 * @return The enum type.
	 */
	EnumType getEnumeratedType();

	/**
	 * Provides access to the TypeConfiguration for access to various type-system registries.
	 *
	 * @return The TypeDescriptorRegistryAccess to use
	 */
	TypeConfiguration getTypeConfiguration();
}
