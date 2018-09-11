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
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeMappingNode;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeInitializer implements CompositeInitializer {
	private final EmbeddedTypeDescriptor embeddedTypeDescriptor;

	private final Map<StateArrayContributor, DomainResultAssembler> assemblerMap = new HashMap<>();

	private Object compositeInstance;


	public AbstractCompositeInitializer(
			CompositeMappingNode resultDescriptor,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationContext context,
			AssemblerCreationState creationState) {
		this.embeddedTypeDescriptor = resultDescriptor.getCompositeNavigableDescriptor().getEmbeddedDescriptor();

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

	@Override
	public Object getCompositeInstance() {
		return compositeInstance;
	}

	@Override
	public void hydrate(RowProcessingState rowProcessingState) {
		compositeInstance = getEmbeddedDescriptor().instantiate( rowProcessingState.getSession() );
	}

	@Override
	public void resolve(RowProcessingState rowProcessingState) {
		for ( Map.Entry<StateArrayContributor, DomainResultAssembler> entry : assemblerMap.entrySet() ) {
			final Object contributorValue = entry.getValue().assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			entry.getKey().getPropertyAccess().getSetter().set(
					compositeInstance,
					contributorValue,
					rowProcessingState.getSession().getSessionFactory()
			);

			// todo (6.0) : handle `org.hibernate.annotations.Parent` injection as well
			// todo (6.0) : ? - add FetchParentAccess#findFirstEntity` for backwards-compatibility in regards to ^^ ?
		}
	}
}