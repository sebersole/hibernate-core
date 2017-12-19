/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.CLASS;
import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.LIST;
import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.MAP;

/**
 * Represents a dynamic instantiation ({@code select new XYZ(...) ...}) as part of the SQM.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiation
		implements SqmSelectableNode, SqmAliasedExpressionContainer<SqmDynamicInstantiationArgument> {

	private static final Logger log = Logger.getLogger( SqmDynamicInstantiation.class );

	public static SqmDynamicInstantiation forClassInstantiation(JavaTypeDescriptor targetJavaType) {
		return new SqmDynamicInstantiation(
				new DynamicInstantiationTargetImpl( CLASS, targetJavaType )
		);
	}

	public static SqmDynamicInstantiation forMapInstantiation(JavaTypeDescriptor<Map> mapJavaTypeDescriptor) {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( MAP, mapJavaTypeDescriptor ) );
	}

	public static SqmDynamicInstantiation forListInstantiation(JavaTypeDescriptor<List> listJavaTypeDescriptor) {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( LIST, listJavaTypeDescriptor ) );
	}

	private final SqmDynamicInstantiationTarget instantiationTarget;
	private List<SqmDynamicInstantiationArgument> arguments;

	private SqmDynamicInstantiation(SqmDynamicInstantiationTarget instantiationTarget) {
		this.instantiationTarget = instantiationTarget;
	}

	public SqmDynamicInstantiationTarget getInstantiationTarget() {
		return instantiationTarget;
	}

	public List<SqmDynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getInstantiationTarget().getTargetTypeDescriptor();
	}

	@Override
	public String asLoggableText() {
		return "<new " + instantiationTarget.getJavaType().getName() + ">";
	}

	public void addArgument(SqmDynamicInstantiationArgument argument) {
		if ( instantiationTarget.getNature() == LIST ) {
			// really should not have an alias...
			if ( argument.getAlias() != null ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
									"but such aliases are ignored for dynamic List instantiations",
							argument.getSelectableNode().asLoggableText(),
							argument.getAlias()
					);
				}
			}
		}
		else if ( instantiationTarget.getNature() == MAP ) {
			// must(?) have an alias...
			log.warnf(
					"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
							"but such aliases are needed for dynamic Map instantiations; " +
							"will likely cause problems later translating sqm",
					argument.getSelectableNode().asLoggableText(),
					argument.getAlias()
			);
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( argument );
	}

	@Override
	public SqmDynamicInstantiationArgument add(SqmExpression expression, String alias) {
		SqmDynamicInstantiationArgument argument = new SqmDynamicInstantiationArgument( expression, alias );
		addArgument( argument );
		return argument;
	}

	@Override
	public void add(SqmDynamicInstantiationArgument aliasExpression) {
		addArgument( aliasExpression );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		final DynamicInstantiationNature instantiationNature = getInstantiationTarget().getNature();
		final JavaTypeDescriptor<Object> targetTypeDescriptor = interpretInstantiationTarget(
				getInstantiationTarget(),
				walker.getSessionFactory()
		);

		final DynamicInstantiation dynamicInstantiation = new DynamicInstantiation(
				instantiationNature,
				targetTypeDescriptor
		);

		for ( SqmDynamicInstantiationArgument sqmArgument : getArguments() ) {
			dynamicInstantiation.addArgument(
					sqmArgument.getAlias(),
					(QueryResultProducer) sqmArgument.getSelectableNode().accept( walker )
			);
		}

		dynamicInstantiation.complete();

		return (T) dynamicInstantiation;
	}

	@SuppressWarnings("unchecked")
	private <T> JavaTypeDescriptor<T> interpretInstantiationTarget(
			SqmDynamicInstantiationTarget instantiationTarget,
			SessionFactoryImplementor sessionFactory) {
		final Class<T> targetJavaType;

		if ( instantiationTarget.getNature() == DynamicInstantiationNature.LIST ) {
			targetJavaType = (Class<T>) List.class;
		}
		else if ( instantiationTarget.getNature() == DynamicInstantiationNature.MAP ) {
			targetJavaType = (Class<T>) Map.class;
		}
		else {
			targetJavaType = instantiationTarget.getJavaType();
		}

		return sessionFactory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( targetJavaType );
	}

	public SqmDynamicInstantiation makeShallowCopy() {
		return new SqmDynamicInstantiation( getInstantiationTarget() );
	}

	private static class DynamicInstantiationTargetImpl implements SqmDynamicInstantiationTarget {
		private final DynamicInstantiationNature nature;
		private final JavaTypeDescriptor javaTypeDescriptor;


		public DynamicInstantiationTargetImpl(DynamicInstantiationNature nature, JavaTypeDescriptor javaTypeDescriptor) {
			this.nature = nature;
			this.javaTypeDescriptor = javaTypeDescriptor;
		}

		@Override
		public DynamicInstantiationNature getNature() {
			return nature;
		}

		@Override
		public JavaTypeDescriptor getTargetTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}
}
