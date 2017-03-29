/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.JpaCollectionJoinImplementor;
import org.hibernate.query.criteria.JpaAttributeJoinImplementor;
import org.hibernate.query.criteria.JpaListJoinImplementor;
import org.hibernate.query.criteria.JpaMapJoinImplementor;
import org.hibernate.query.criteria.JpaSetJoinImplementor;
import org.hibernate.query.criteria.internal.expression.DelegatedExpressionImpl;
import org.hibernate.query.criteria.internal.expression.AbstractExpression;
import org.hibernate.query.criteria.internal.path.RootImpl;

/**
 * The Hibernate implementation of the JPA {@link Subquery} contract.  Mostlty a set of delegation to its internal
 * {@link QueryStructure}.
 *
 * @author Steve Ebersole
 */
public class CriteriaSubqueryImpl<T> extends AbstractExpression<T> implements Subquery<T>, Serializable {
	private final CommonAbstractCriteria parent;
	private final QueryStructure<T> queryStructure;

	public CriteriaSubqueryImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			CommonAbstractCriteria parent) {
		super( criteriaBuilder, javaType);
		this.parent = parent;
		this.queryStructure = new QueryStructure<T>( this, criteriaBuilder );
	}

	@Override
	public AbstractQuery<?> getParent() {
		if ( ! AbstractQuery.class.isInstance( parent ) ) {
			throw new IllegalStateException( "Cannot call getParent on update/delete criterias" );
		}
		return (AbstractQuery<?>) parent;
	}

	@Override
	public CommonAbstractCriteria getContainingQuery() {
		return parent;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		for ( ParameterExpression param : queryStructure.getParameters() ) {
			registry.registerParameter( param );
		}
	}

	@Override
	public Class<T> getResultType() {
		return getJavaType();
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Set<Root<?>> getRoots() {
		return queryStructure.getRoots();
	}

	@Override
	public <X> Root<X> from(EntityType<X> entityType) {
		return queryStructure.from( entityType );
	}

	@Override
	public <X> Root<X> from(Class<X> entityClass) {
		return queryStructure.from( entityClass );
	}


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Subquery<T> distinct(boolean applyDistinction) {
		queryStructure.setDistinct( applyDistinction );
		return this;
	}

	@Override
	public boolean isDistinct() {
		return queryStructure.isDistinct();
	}

	private Expression<T> wrappedSelection;

	@Override
	public Expression<T> getSelection() {
		if ( wrappedSelection == null ) {
			if ( queryStructure.getSelection() == null ) {
				return null;
			}
			wrappedSelection = new SubquerySelection<T>( (AbstractExpression<T>) queryStructure.getSelection(), this );
		}
		return wrappedSelection;
	}

	@Override
	public Subquery<T> select(Expression<T> expression) {
		queryStructure.setSelection( expression );
		return this;
	}


	public static class SubquerySelection<S> extends DelegatedExpressionImpl<S> {
		private final CriteriaSubqueryImpl subQuery;

		public SubquerySelection(AbstractExpression<S> wrapped, CriteriaSubqueryImpl subQuery) {
			super( wrapped );
			this.subQuery = subQuery;
		}
	}


	// RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Predicate getRestriction() {
		return queryStructure.getRestriction();
	}

	@Override
	public Subquery<T> where(Expression<Boolean> expression) {
		queryStructure.setRestriction( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public Subquery<T> where(Predicate... predicates) {
		// TODO : assuming this should be a conjuntion, but the spec does not say specifically...
		queryStructure.setRestriction( criteriaBuilder().and( predicates ) );
		return this;
	}



	// GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public List<Expression<?>> getGroupList() {
		return queryStructure.getGroupings();
	}

	@Override
	public Subquery<T> groupBy(Expression<?>... groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public Subquery<T> groupBy(List<Expression<?>> groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public Predicate getGroupRestriction() {
		return queryStructure.getHaving();
	}

	@Override
	public Subquery<T> having(Expression<Boolean> expression) {
		queryStructure.setHaving( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public Subquery<T> having(Predicate... predicates) {
		queryStructure.setHaving( criteriaBuilder().and( predicates ) );
		return this;
	}


	// CORRELATIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Set<Join<?, ?>> getCorrelatedJoins() {
		return queryStructure.collectCorrelatedJoins();
	}

	@Override
	public <Y> Root<Y> correlate(Root<Y> source) {
		final RootImpl<Y> correlation = ( ( RootImpl<Y> ) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <X, Y> Join<X, Y> correlate(Join<X, Y> source) {
		final JpaAttributeJoinImplementor<X,Y> correlation = ( (JpaAttributeJoinImplementor<X,Y>) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <X, Y> CollectionJoin<X, Y> correlate(CollectionJoin<X, Y> source) {
		final JpaCollectionJoinImplementor<X,Y> correlation = ( (JpaCollectionJoinImplementor<X,Y>) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <X, Y> SetJoin<X, Y> correlate(SetJoin<X, Y> source) {
		final JpaSetJoinImplementor<X,Y> correlation = ( (JpaSetJoinImplementor<X,Y>) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <X, Y> ListJoin<X, Y> correlate(ListJoin<X, Y> source) {
		final JpaListJoinImplementor<X,Y> correlation = ( (JpaListJoinImplementor<X,Y>) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <X, K, V> MapJoin<X, K, V> correlate(MapJoin<X, K, V> source) {
		final JpaMapJoinImplementor<X, K, V> correlation = ( (JpaMapJoinImplementor<X, K, V>) source ).correlateTo( this );
		queryStructure.addCorrelationRoot( correlation );
		return correlation;
	}

	@Override
	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		return queryStructure.subquery( subqueryType );
	}

}
