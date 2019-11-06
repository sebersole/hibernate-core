/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.FetchableContainer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Commonality in regards to the mapping type system for all managed domain
 * types - entity types, mapped-superclass types, composite types, etc
 *
 * @author Steve Ebersole
 */
public interface ManagedMappingType extends MappingType, FetchableContainer {
	@Override
	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	/**
	 * Get the number of attributes defined on this class and any supers
	 */
	int getNumberOfAttributeMappings();

	default AttributeMapping findAttributeMapping(String name) {
		return null;
	}

	/**
	 * Get access to the attributes defined on this class and any supers
	 */
	Collection<AttributeMapping> getAttributeMappings();

	/**
	 * Visit attributes defined on this class and any supers
	 */
	void visitAttributeMappings(Consumer<AttributeMapping> action);

	/**
	 * @todo (6.0) : consider dropping this in favor of a form passing the ManagedMappingType
	 * 		which indicates the type to limit the attribute search to (the type and its super-type)
	 */
	default void visitStateArrayContributors(Consumer<StateArrayContributorMapping> mappingConsumer) {
		visitAttributeMappings(
				modelPart -> {
					if ( modelPart instanceof StateArrayContributorMapping ) {
						mappingConsumer.accept( ( (StateArrayContributorMapping) modelPart ) );
					}
				}
		);
	}
}
