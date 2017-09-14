/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Expression representing the type of an entity-valued expression.
 * E.g. a {@code TYPE(path)} expression
 *
 * @author Steve Ebersole
 */
public class SqmEntityTypeSqmExpression implements SqmExpression {
	private final SqmNavigableReference binding;

	public SqmEntityTypeSqmExpression(SqmNavigableReference binding) {
		this.binding = binding;
	}

	public SqmNavigableReference getBinding() {
		return binding;
	}

	@Override
	public ExpressableType getExpressableType() {
		return binding.getReferencedNavigable();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityTypeExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + binding.asLoggableText() + ")";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return binding.getJavaTypeDescriptor();
	}

//	@Override
//	public QueryResult createQueryResult(
//			Expression expression,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		throw new UnsupportedOperationException( "At the moment, selection of an entity's type as a QUeryResult is not supported" );
//		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
//	}
}
