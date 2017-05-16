/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexBasicImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexBasic<J> {

	private final BasicType<J> basicType;
	private final Column column;
	private final AttributeConverterDefinition attributeConverter;

	public CollectionIndexBasicImpl(
			CollectionPersister persister,
			IndexedCollection mappingBinding,
			List<Column> columns) {
		super( persister, columns );

		final SimpleValue simpleValueMapping = (SimpleValue) mappingBinding.getIndex();
		this.attributeConverter = simpleValueMapping.getAttributeConverterDescriptor();

		// todo (6.0) : SimpleValue -> BasicType
		this.basicType = null;

		assert columns != null && columns.size() == 1;
		this.column = columns.get( 0 );
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.BASIC;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionIndexBasic( this );
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultScalarImpl(
				selectedExpression,
				sqlSelectionResolver.resolveSqlSelection(
						columnReferenceSource.resolveColumnReference( column )
				),
				resultVariable,
				this
	  	);
	}
}
