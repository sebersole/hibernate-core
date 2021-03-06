/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * Specialized CASE statement for resolving the first non-null value in a list of values
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqmCoalesce<T> extends AbstractSqmExpression<T> implements JpaCoalesce<T>, DomainResultProducer<T> {

	private List<SqmExpression<? extends T>> arguments = new ArrayList<>();
	private SqmFunctionTemplate coalesceFunction;

	public SqmCoalesce(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCoalesce(AllowableFunctionReturnType<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		coalesceFunction = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().findFunctionTemplate("coalesce");
	}

	public void value(SqmExpression<? extends T> expression) {
		arguments.add( expression );

		//TODO: improve this
//		if ( getNodeType() == null ) {
			setExpressableType( expression.getNodeType() );
//		}
	}

	public List<SqmExpression<? extends T>> getArguments() {
		return arguments;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFunction(
				coalesceFunction.makeSqmFunctionExpression(
						new ArrayList<>(arguments),
						(AllowableFunctionReturnType<?>) getNodeType(),
						nodeBuilder().getQueryEngine()
				)
		);
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCoalesce<T> value(T value) {
		value( nodeBuilder().literal( value ) );
		return this;
	}

	@Override
	public SqmCoalesce<T> value(Expression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	public SqmCoalesce<T> value(JpaExpression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmCoalesce<T> values(T... values) {
		for ( T value : values ) {
			value( nodeBuilder().literal( value ) );
		}
		return this;
	}

}
