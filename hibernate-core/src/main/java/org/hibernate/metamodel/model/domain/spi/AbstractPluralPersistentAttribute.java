/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.PluralAttributeFetchImpl;
import org.hibernate.sql.results.internal.PluralAttributeQueryResultImpl;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class AbstractPluralPersistentAttribute<O,C,E>
		extends AbstractPersistentAttribute<O,C>
		implements PluralPersistentAttribute<O,C,E> {

	private final PersistentCollectionDescriptor<O,C,E> collectionMetadata;

	public AbstractPluralPersistentAttribute(
			ManagedTypeDescriptor<O> container,
			String name,
			JavaTypeDescriptor<C> javaTypeDescriptor,
			PropertyAccess access,
			PersistentCollectionDescriptor<O, C, E> collectionMetadata) {
		super( container, name, javaTypeDescriptor, access );
		this.collectionMetadata = collectionMetadata;
	}

	@Override
	public PersistentCollectionDescriptor<O,C,E> getPersistentCollectionMetadata() {
		return collectionMetadata;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public Type<E> getElementType() {
		// NOTE : this is the JPA Type
		return getPersistentCollectionMetadata().getElementDescriptor();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public Class<E> getBindableJavaType() {
		return getPersistentCollectionMetadata().getElementDescriptor().getJavaType();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		switch ( getPersistentCollectionMetadata().getElementDescriptor().getClassification() ) {
			case EMBEDDABLE:
			case BASIC: {
				return PersistentAttributeType.ELEMENT_COLLECTION;
			}
			case ONE_TO_MANY: {
				return PersistentAttributeType.ONE_TO_MANY;
			}
			case MANY_TO_MANY: {
				return PersistentAttributeType.MANY_TO_MANY;
			}
			case ANY: {
				throw new NotYetImplementedException(  );
			}
			default: {
				throw new UnsupportedOperationException(
						"Could not interpret JPA PersistentAttributeType for plural attribute : " +
								getNavigableRole().getFullPath()
				);
			}
		}
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY
				|| getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY;
	}

	@Override
	public boolean isCollection() {
		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Navigable

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getPersistentCollectionMetadata().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getPersistentCollectionMetadata().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getPersistentCollectionMetadata().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getPersistentCollectionMetadata().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionMetadata().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionMetadata().visitDeclaredNavigables( visitor );
	}

	@Override
	public String asLoggableText() {
		return getPersistentCollectionMetadata().asLoggableText();
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference, String resultVariable, QueryResultCreationContext creationContext) {
		return new PluralAttributeQueryResultImpl(
				this,
				resultVariable,
				creationContext
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Fetchable


	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			SqlExpressionQualifier qualifier,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		// todo (6.0) : use qualifier to create the SqlSelection mappings
		return new PluralAttributeFetchImpl(
				fetchParent,
				qualifier,
				this,
				fetchStrategy,
				resultVariable,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public ManagedTypeDescriptor<C> getFetchedManagedType() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public ForeignKey.ColumnMappings getJoinColumnMappings() {
		return getPersistentCollectionMetadata().getForeignKeyDescriptor().getJoinColumnMappings();
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			SqlExpressionQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		throw new NotYetImplementedException(  );
	}
}
