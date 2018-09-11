/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.function.Consumer;

import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeMappingNode;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class CompositeFetchInitializerImpl
		extends AbstractCompositeInitializer
		implements CompositeInitializer {
	private final FetchParentAccess fetchParentAccess;

	public CompositeFetchInitializerImpl(
			FetchParentAccess fetchParentAccess,
			CompositeMappingNode resultDescriptor,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState) {
		super( resultDescriptor, initializerConsumer, creationContext, creationState );
		this.fetchParentAccess = fetchParentAccess;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {

	}

	@Override
	public Object getFetchParentInstance() {
		return getCompositeInstance();
	}
}