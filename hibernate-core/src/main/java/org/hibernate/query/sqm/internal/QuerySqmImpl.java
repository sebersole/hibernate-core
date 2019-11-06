/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.Tuple;

import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.BasicType;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractQuery<R>
		implements HqlQueryImplementor<R>, ExecutionContext {

	private final String hqlString;
	private final SqmStatement sqmStatement;
	private final Class resultType;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	private final QueryParameterBindingsImpl parameterBindings;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	public QuerySqmImpl(
			NamedHqlQueryMemento memento,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = memento.getHqlString();

		final SessionFactoryImplementor factory = producer.getFactory();

		this.sqmStatement = factory.getQueryEngine().getHqlTranslator().interpret( hqlString );

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmDmlStatement ) {
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}
		this.resultType = resultType;

		if ( sqmStatement.getSqmParameters().isEmpty() ) {
			this.domainParameterXref = DomainParameterXref.empty();
			this.parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			this.domainParameterXref = DomainParameterXref.from( sqmStatement );
			this.parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );

		applyOptions( memento );
	}

	protected void applyOptions(NamedHqlQueryMemento memento) {
		super.applyOptions( memento );

		if ( memento.getFirstResult() != null ) {
			setFirstResult( memento.getFirstResult() );
		}

		if ( memento.getMaxResults() != null ) {
			setMaxResults( memento.getMaxResults() );
		}

		if ( memento.getLockOptions() != null ) {
			setLockOptions( memento.getLockOptions() );
		}

		if ( memento.getParameterTypes() != null ) {
			for ( Map.Entry<String, String> entry : memento.getParameterTypes().entrySet() ) {
				final QueryParameterImplementor<?> parameter = parameterMetadata.getQueryParameter( entry.getKey() );
				final BasicType type = getSessionFactory().getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( entry.getValue() );
				parameter.applyAnticipatedType( type );
			}
		}
	}

	/**
	 * Form used for HQL queries
	 */
	public QuerySqmImpl(
			String hqlString,
			HqlInterpretation hqlInterpretation,
			Class resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = hqlString;
		this.resultType = resultType;

		this.sqmStatement = hqlInterpretation.getSqmStatement();

		if ( resultType != null ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement );
			//noinspection unchecked
			checkQueryReturnType( (SqmSelectStatement<R>) sqmStatement, resultType, producer.getFactory() );
		}

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement sqmStatement,
			Class resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmSelectStatement ) {
				//noinspection unchecked
				checkQueryReturnType( (SqmSelectStatement<R>) sqmStatement, resultType, producer.getFactory() );
			}
			else {
				assert sqmStatement instanceof SqmDmlStatement;
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}

		this.hqlString = "<criteria>";
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;

		this.domainParameterXref = DomainParameterXref.from( sqmStatement );
		if ( ! domainParameterXref.hasParameters() ) {
			this.parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			this.parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
	}

	private static <T> void checkQueryReturnType(SqmSelectStatement<T> sqm, Class<T> resultClass, SessionFactoryImplementor sessionFactory) {
		if ( resultClass == null ) {
			// nothing to check
			return;
		}

		final List<SqmSelection> selections = sqm.getQuerySpec().getSelectClause().getSelections();

		if ( resultClass.isArray() ) {
			// todo (6.0) : implement
		}
		else if ( Tuple.class.isAssignableFrom( resultClass ) ) {
			// todo (6.0) : implement
		}
		else {
			if ( selections.size() != 1 ) {
				final String errorMessage = "Query result-type error - multiple selections: use Tuple or array";

				if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
					throw new IllegalArgumentException( errorMessage );
				}
				else {
					throw new QueryTypeMismatchException( errorMessage );
				}
			}

			final SqmSelection sqmSelection = selections.get( 0 );

			if ( sqmSelection.getSelectableNode() instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmSelection.getSelectableNode();

				// we may not yet know a selection type
				if ( sqmParameter.getNodeType() == null || sqmParameter.getNodeType().getExpressableJavaTypeDescriptor() == null ) {
					// we can't verify the result type up front
					return;
				}
			}

			verifyResultType( resultClass, sqmSelection.getNodeType(), sessionFactory );
		}
	}

	private static <T> void verifyResultType(
			Class<T> resultClass,
			SqmExpressable<?> sqmExpressable,
			SessionFactoryImplementor sessionFactory) {
		assert sqmExpressable != null;
		assert sqmExpressable.getExpressableJavaTypeDescriptor() != null;

		if ( ! resultClass.isAssignableFrom( sqmExpressable.getExpressableJavaTypeDescriptor().getJavaType() ) ) {
			final String errorMessage = String.format(
					"Specified result type [%s] did not match Query selection type [%s] - multiple selections: use Tuple or array",
					resultClass.getName(),
					sqmExpressable.getExpressableJavaTypeDescriptor().getJavaType().getName()
			);

			if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
				throw new IllegalArgumentException( errorMessage );
			}
			else {
				throw new QueryTypeMismatchException( errorMessage );
			}
		}
	}

	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	private boolean isSelect() {
		return sqmStatement instanceof SqmSelectStatement;
	}

	@Override
	public String getQueryString() {
		return hqlString;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public Query<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		queryOptions.applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph) {
		final GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );

		applyGraph( entityGraph, graphSemantic );
	}

	@Override
	public SqmStatement getSqmStatement() {
		return sqmStatement;
	}

	@SuppressWarnings("unchecked")
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		Set<Parameter<?>> parameters = new HashSet<>();
		parameterMetadata.collectAllParameters( parameters::add );
		return parameters;
	}

	@Override
	protected boolean canApplyAliasSpecificLockModes() {
		return isSelect();
	}

	@Override
	protected void verifySettingLockMode() {
		if ( !isSelect() ) {
			throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
		}
	}

	@Override
	protected void verifySettingAliasSpecificLockModes() {
		// todo : add a specific Dialect check as well? - if not supported, maybe that can trigger follow-on locks?
		verifySettingLockMode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( parameterBindings ) ) {
			return (T) parameterBindings;
		}

		if ( cls.isInstance( sqmStatement ) ) {
			return (T) sqmStatement;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}

		if ( cls.isInstance( queryOptions.getAppliedGraph() ) ) {
			return (T) queryOptions.getAppliedGraph();
		}

		if ( EntityGraphQueryHint.class.isAssignableFrom( cls ) ) {
			return (T) new EntityGraphQueryHint( queryOptions.getAppliedGraph() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
		);
	}


	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( queryOptions.getAppliedGraph() != null ) {
			hints.put(
					queryOptions.getAppliedGraph().getSemantic().getJpaHintName(),
					queryOptions.getAppliedGraph().getGraph()
			);
		}
	}

	@Override
	protected List<R> doList() {
		SqmUtil.verifyIsSelectStatement( getSqmStatement() );
		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );

		return resolveSelectQueryPlan().performList( this );
	}

	private boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		// resolve (or make) the QueryPlan.  This QueryPlan might be an aggregation of multiple plans.
		//
		// QueryPlans can be cached, except for in certain circumstances
		// 		- the determination of these circumstances occurs in SqmInterpretationsKey#generateFrom.
		//		If SqmInterpretationsKey#generateFrom returns null the query is not cacheable

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateFrom( this );
		if ( cacheKey != null ) {
			return getSession().getFactory().getQueryEngine().getInterpretationCache().resolveSelectQueryPlan(
					cacheKey,
					this::buildSelectQueryPlan
			);
		}
		else {
			return buildSelectQueryPlan();
		}
	}

	private SelectQueryPlan<R> buildSelectQueryPlan() {
		final SqmSelectStatement[] concreteSqmStatements = QuerySplitter.split(
				(SqmSelectStatement) getSqmStatement(),
				getSessionFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedSelectQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteSelectQueryPlan(
					concreteSqmStatements[0],
					getResultType(),
					getQueryOptions()
			);
		}
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> buildAggregatedSelectQueryPlan(SqmSelectStatement[] concreteSqmStatements) {
		final SelectQueryPlan[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteSelectQueryPlan(
					concreteSqmStatements[i],
					getResultType(),
					getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private SelectQueryPlan<R> buildConcreteSelectQueryPlan(
			SqmSelectStatement concreteSqmStatement,
			Class<R> resultType,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				domainParameterXref,
				resultType,
				queryOptions
		);
	}

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		SqmUtil.verifyIsSelectStatement( getSqmStatement() );
		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );

		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	protected int doExecuteUpdate() {
		SqmUtil.verifyIsNonSelectStatement( getSqmStatement() );
		getSession().prepareForQueryExecution( true );

		return resolveNonSelectQueryPlan().executeUpdate( this );
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		// resolve (or make) the QueryPlan.

		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateNonSelectKey( this );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getInterpretationCache().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = buildNonSelectQueryPlan();
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryEngine().getInterpretationCache().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private NonSelectQueryPlan buildNonSelectQueryPlan() {
		// to get here the SQM statement has already been validated to be
		// a non-select variety...
		if ( getSqmStatement() instanceof SqmDeleteStatement ) {
			return buildDeleteQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmUpdateStatement ) {
			return buildUpdateQueryPlan();
		}

		throw new NotYetImplementedException( "Query#executeUpdate not yet implemented" );
	}

	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final SqmDeleteStatement sqmDelete = (SqmDeleteStatement) getSqmStatement();

		final String entityNameToDelete = sqmDelete.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToDelete );

		final DeleteHandler deleteHandler = entityDescriptor.getSqmMultiTableMutationStrategy().buildDeleteHandler(
				sqmDelete,
				domainParameterXref,
				this::getSessionFactory
		);

		return new DeleteQueryPlanImpl( sqmDelete, deleteHandler, this );
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		final SqmUpdateStatement sqmStatement = (SqmUpdateStatement) getSqmStatement();

		final String entityNameToUpdate = sqmStatement.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToUpdate );

		final UpdateHandler updateHandler = entityDescriptor.getSqmMultiTableMutationStrategy().buildUpdateHandler(
				sqmStatement,
				domainParameterXref,
				this::getSessionFactory
		);

		return new UpdateQueryPlanImpl( sqmStatement, updateHandler, this );
	}

	@Override
	public Callback getCallback() {
		return afterLoadAction -> {};
	}


	@Override
	public NamedHqlQueryMemento toMemento(String name) {
		return new NamedHqlQueryMementoImpl(
				name,
				hqlString,
				getFirstResult(),
				getMaxResults(),
				isCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getHibernateFlushMode(),
				isReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				Collections.emptyMap(),
				getHints()
		);
	}
}
