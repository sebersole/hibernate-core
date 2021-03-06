/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.FetchableContainer;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeMapping
		extends AttributeMapping, StateArrayContributorMapping, TableGroupJoinProducer, FetchableContainer {

	CollectionPersister getCollectionDescriptor();

	ForeignKeyDescriptor getKeyDescriptor();

	CollectionPart getIndexDescriptor();

	CollectionPart getElementDescriptor();

	CollectionIdentifierDescriptor getIdentifierDescriptor();

	@Override
	default void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( indexDescriptor );
		}
	}

	@Override
	default void visitFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getElementDescriptor() );
	}

	String getSeparateCollectionTable();
}
