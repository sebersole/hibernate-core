/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;
import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.metamodel.model.domain.Representation;
import org.hibernate.metamodel.model.domain.spi.Instantiator;

/**
 * Corollary to what JPA calls a "managed type" as part of Hibernate's boot-time
 * metamodel.  Essentially a base class describing the commonality between an entity,
 * a mapped-superclass and an embeddable.
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMapping {
	/**
	 * The name of this managed type.  Generally the class name.
	 */
	String getName();

	/**
	 * The mappings persistence type.
	 */
	PersistenceType getPersistenceType();

	Representation getExplicitRepresentation();

	Instantiator getExplicitInstantiator();

	/**
	 * The ordering here is defined by the alphabetical ordering of the
	 * attributes' names
	 *
	 * todo (6.0) : is this what we want?
	 */
	List<PersistentAttributeMapping> getDeclaredPersistentAttributes();

	/**
	 * Get the persistent attributes of this managed type and all super managed types.
	 */
	List<PersistentAttributeMapping> getPersistentAttributes();

	boolean hasDeclaredPersistentAttribute(String name);

	/**
	 * Check whether the managed type contains an attribute of the specified name or any
	 * super managed types.
	 *
	 * @param name the attribute name.
	 */
	boolean hasPersistentAttribute(String name);

	/**
	 * Get the super managed type associated with this managed type or {@code null}.
	 */
	ManagedTypeMapping getSuperManagedTypeMapping();

	/**
	 * Get all super managed type mappings associated with this managed type or empty list.
	 */
	List<ManagedTypeMapping> getSuperManagedTypeMappings();
}
