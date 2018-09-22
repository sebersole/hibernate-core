/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.spi.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeMappingNode;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeInitializer extends AbstractFetchParentAccess implements CompositeInitializer {
	private final EmbeddedTypeDescriptor embeddedTypeDescriptor;
	private final FetchParentAccess fetchParentAccess;

	private final Map<StateArrayContributor, DomainResultAssembler> assemblerMap = new HashMap<>();

	// per-row state
	private Object compositeInstance;
	private Object[] resolvedValues;


	public AbstractCompositeInitializer(
			CompositeMappingNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationContext context,
			AssemblerCreationState creationState) {
		this.embeddedTypeDescriptor = resultDescriptor.getCompositeNavigableDescriptor().getEmbeddedDescriptor();
		this.fetchParentAccess = fetchParentAccess;

		embeddedTypeDescriptor.visitStateArrayContributors(
				stateArrayContributor -> {
					final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor.getNavigableName() );

					final DomainResultAssembler stateAssembler = fetch == null
							? new NullValueAssembler( stateArrayContributor.getJavaTypeDescriptor() )
							: fetch.createAssembler( this, initializerConsumer, context, creationState );

					assemblerMap.put( stateArrayContributor, stateAssembler );
				}
		);
	}

	@Override
	public EmbeddedTypeDescriptor getEmbeddedDescriptor() {
		return embeddedTypeDescriptor;
	}

	public FetchParentAccess getFetchParentAccess() {
		return fetchParentAccess;
	}

	@Override
	public Object getCompositeInstance() {
		return compositeInstance;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// todo (6.0) : register "parent resolution listener" if the composite is defined for `@Parent`
		//		something like:
//		final PersistentAttribute parentInjectionTarget = getEmbeddedDescriptor().getParentInjectionTarget();
//
//		if ( parentInjectionTarget != null ) {
//			fetchParentAccess.registerResolutionListener(
//					owner -> {
//						if ( compositeInstance == null ) {
//							return;
//						}
//						parentInjectionTarget.getPropertyAccess().getSetter().set(
//								compositeInstance,
//								owner,
//								rowProcessingState.getSession().getFactory()
//						);
//					}
//			);
//		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		compositeInstance = getEmbeddedDescriptor().instantiate( rowProcessingState.getSession() );
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		notifyParentResolutionListeners( fetchParentAccess.getFetchParentInstance() );

		resolvedValues = new Object[ assemblerMap.size() ];

		for ( Map.Entry<StateArrayContributor, DomainResultAssembler> entry : assemblerMap.entrySet() ) {
			final Object contributorValue = entry.getValue().assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			resolvedValues[ entry.getKey().getStateArrayPosition() ] = contributorValue;

		}

		getEmbeddedDescriptor().setPropertyValues( compositeInstance, resolvedValues );

		// todo (6.0) : handle `org.hibernate.annotations.Parent` injection as well
		// todo (6.0) : ? - add FetchParentAccess#findFirstEntity` for backwards-compatibility in regards to ^^ ?
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		resolvedValues = null;

		clearParentResolutionListeners();
	}
}
