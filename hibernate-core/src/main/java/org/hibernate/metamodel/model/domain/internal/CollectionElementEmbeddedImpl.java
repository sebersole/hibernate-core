/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.internal.CompositeQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEmbeddedImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEmbedded<J> {

	private final EmbeddedTypeDescriptor<J> embeddedPersister;
	private final List<Column> columnList;

	public CollectionElementEmbeddedImpl(
			PersistentCollectionDescriptor persister,
			Collection mapping,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		this.embeddedPersister = creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
				(EmbeddedValueMapping) mapping.getElement(),
				persister,
				NAVIGABLE_NAME,
				creationContext
		);
		this.columnList = embeddedPersister.collectColumns();
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedPersister;
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEmbeddedDescriptor().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEmbeddedDescriptor().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return embeddedPersister.getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		// visit our sub-navigables
		getEmbeddedDescriptor().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		// visit our declared sub-navigables
		getEmbeddedDescriptor().visitDeclaredNavigables( visitor );
	}

	@Override
	public List<Column> getColumns() {
		return columnList;
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression, String resultVariable, QueryResultCreationContext creationContext) {
		return new CompositeQueryResultImpl( resultVariable, embeddedPersister );
	}

	@Override
	public QueryResult createQueryResult(
			TableReference tableReference,
			SqmNavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new CompositeQueryResultImpl(
				resultVariable,
				(EmbeddedValuedNavigable) navigableReference.getReferencedNavigable()
		);
	}
}
