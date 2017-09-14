/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents the ENTRY() function for obtaining the map entries from a {@code Map}-typed association.
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryBinding implements SqmExpression {
	private final SqmPluralAttributeReference attributeBinding;

	public SqmMapEntryBinding(SqmPluralAttributeReference attributeBinding) {
		this.attributeBinding = attributeBinding;
	}

	public SqmPluralAttributeReference getAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public ExpressableType getExpressableType() {
		return null;
	}

	@Override
	public ExpressableType getInferableType() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMapEntryFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "ENTRY(" + attributeBinding.asLoggableText() + ")";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return attributeBinding.getJavaTypeDescriptor();
	}

//	@Override
//	public QueryResult createQueryResult(
//			Expression expression,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		throw new UnsupportedOperationException( "At the moment, selection of Map.Entry for persistent plural attribute is not supported" );
//		// todo (6.0) : could be supported though - consider adding support
//	}
}
