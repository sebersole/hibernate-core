/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final ColumnReferenceSource columnReferenceSource;
	private final NavigablePath navigablePath;

	private final PersistentCollectionDescriptor collectionPersister;


	public PluralAttributeIndexReference(
			NavigableContainerReference containerReference,
			PersistentCollectionDescriptor collectionPersister,
			ColumnReferenceSource columnReferenceSource,
			NavigablePath navigablePath) {
		this.containerReference = containerReference;
		this.collectionPersister = collectionPersister;
		this.columnReferenceSource = columnReferenceSource;
		this.navigablePath = navigablePath;
	}


	@Override
	public CollectionIndex getNavigable() {
		return collectionPersister.getIndexDescriptor();
	}

	@Override
	public SqlExpressionQualifier getSqlExpressionQualifier() {
		return columnReferenceSource;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}
}