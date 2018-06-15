/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.EntityQueryResultImpl;
import org.hibernate.sql.results.internal.EntitySqlSelectionGroupImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEntity<J> {
	private final EntityDescriptor<J> entityDescriptor;
	private final NavigableRole navigableRole;

	public CollectionIndexEntityImpl(
			PersistentCollectionDescriptor descriptor,
			IndexedCollection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor );

		this.entityDescriptor = null;
		this.navigableRole = descriptor.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityDescriptor().getJpaEntityName();
	}

	@Override
	public IndexClassification getClassification() {
		// todo : distinguish between OneToMany and ManyToMany
		return IndexClassification.ONE_TO_MANY;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitNavigables( visitor );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionIndexReferenceEntity( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		assert navigableReference instanceof EntityValuedNavigableReference;
		final EntityValuedNavigableReference entityReference = (EntityValuedNavigableReference) navigableReference;

		return new EntityQueryResultImpl(
				entityReference.getNavigable(),
				resultVariable,
				EntitySqlSelectionGroupImpl.buildSqlSelectionGroup(
						getEntityDescriptor(),
						entityReference.getSqlExpressionQualifier(),
						creationContext
				),
				entityReference.getLockMode(),
				entityReference.getNavigablePath(),
				creationContext
		);
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			org.hibernate.sql.ast.JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector, tableGroupContext );
	}
}
