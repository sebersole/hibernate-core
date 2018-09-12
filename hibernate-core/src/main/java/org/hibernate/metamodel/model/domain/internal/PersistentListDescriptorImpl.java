/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.ListInitializerProducer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * Hibernate's standard PersistentCollectionDescriptor implementor
 * for Lists
 *
 * @author Steve Ebersole
 */
public class PersistentListDescriptorImpl<O,E> extends AbstractPersistentCollectionDescriptor<O,List<E>, E> {
	public PersistentListDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, context );
	}

	@Override
	protected CollectionJavaDescriptor resolveCollectionJtd(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		return findCollectionJtd( List.class, creationContext );
	}



	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			FetchParent fetchParent,
			boolean isJoinFetch,
			String resultVariable,
			LockMode lockMode,
			DomainResult keyResult,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		final NavigableReference navigableReference = creationState.getNavigableReferenceStack().getCurrent();

		return new ListInitializerProducer(
				this,
				isJoinFetch,
				getIndexDescriptor().createDomainResult(
						navigableReference,
						null,
						creationContext,
						creationState
				),
				getElementDescriptor().createDomainResult(
						navigableReference,
						null,
						creationContext,
						creationState
				)
		);
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		return ( (List) collection ).contains( childObject );
	}
}
