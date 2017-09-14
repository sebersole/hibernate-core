/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmMinIndexReferenceBasic
		extends AbstractSpecificSqmCollectionIndexReference
		implements SqmMinIndexReference {
	public SqmMinIndexReferenceBasic(SqmPluralAttributeReference attributeBinding) {
		super( attributeBinding );
	}

	@Override
	public ExpressableType getExpressableType() {
		return getPluralAttributeBinding().getReferencedNavigable().getPersistentCollectionMetadata().getIndexDescriptor();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMinIndexFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "MININDEX(" + getPluralAttributeBinding().asLoggableText() + ")";
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	public QueryResult createQueryResult(
//			Expression expression,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		return getReferencedNavigable().createQueryResult( expression, resultVariable, creationContext );
//	}
}
