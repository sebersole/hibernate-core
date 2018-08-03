/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.internal.PerQuerySpecSqlExpressionResolver;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.internal.FetchGraphBuilder;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

import org.jboss.logging.Logger;

/**
 * Interprets an SqmSelectStatement as a SQL-AST SelectQuery.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
@SuppressWarnings("unchecked")
public class SqmSelectToSqlAstConverter
		extends BaseSqmToSqlAstConverter
		implements QueryResultCreationContext {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );
	private final PerQuerySpecSqlExpressionResolver expressionResolver;

	// todo (6.0) : SqmSelectToSqlAstConverter needs to account for the EntityGraph hint
	private FetchGraphBuilder fetchGraphBuilder;

	private final Stack<Shallowness> shallownessStack = new StandardStack<>( Shallowness.NONE );
	private final Stack<NavigableReference> navigableReferenceStack = new StandardStack<>();
	private final Stack<Expression> currentSelectedExpression = new StandardStack<>();

	private final Map<Expression,SqlSelection> sqlSelectionByExpressionMap = new HashMap<>();

	private final List<QueryResult> queryResults = new ArrayList<>();

	private int counter;

	public String generateSqlAstNodeUid() {
		return "<uid(fetchgraph):" + counter++ + ">";
	}

	public SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			SqlAstProducerContext producerContext) {
		super( producerContext, queryOptions );
		this.fetchDepthLimit = producerContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		this.entityGraphQueryHintType = queryOptions.getEntityGraphQueryHint() == null
				? EntityGraphQueryHint.Type.NONE
				:  queryOptions.getEntityGraphQueryHint().getType();

		this.expressionResolver = new PerQuerySpecSqlExpressionResolver(
				producerContext.getSessionFactory(),
				() -> getQuerySpecStack().getCurrent(),
				this::normalizeSqlExpression,
				this::collectSelection
		);
	}

	public SqlAstSelectDescriptor interpret(SqmSelectStatement statement) {
		return new SqlAstSelectDescriptorImpl(
				visitSelectStatement( statement ),
				queryResults,
				affectedTableNames()
		);
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getProducerContext().getSessionFactory();
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		// todo (6.0) : we also need to vary this for ctor result based on ctor sigs + user option
		return shallownessStack.getCurrent() != Shallowness.NONE;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker


	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "Not expecting UpdateStatement" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Fetches

	private final EntityGraphQueryHint.Type entityGraphQueryHintType;

	private final int fetchDepthLimit;

	private Stack<FetchParent> fetchParentStack = new StandardStack<>();
	private Stack<NavigablePath> navigablePathStack = new StandardStack<>();
	private final Stack<TableGroup> tableGroupStack = new StandardStack<>();
	private Stack<SqmFrom> sqmFromStack = new StandardStack<>();
	private Stack<AttributeNodeContainer> entityGraphNodeStack = new StandardStack<>();

	@Override
	public Void visitSelection(SqmSelection sqmSelection) {
		// todo (6.0) : this should actually be able to generate multiple SqlSelections
		final QueryResultProducer resultProducer = (QueryResultProducer) sqmSelection.getSelectableNode().accept( this );

		if ( getQuerySpecStack().depth() > 1 && Expression.class.isInstance( resultProducer ) ) {
			// we only need the QueryResults if we are in the top-level select-clause.
			// but we do need to at least resolve the sql selections
			getSqlSelectionResolver().resolveSqlSelection(
					(Expression) resultProducer,
					(BasicJavaDescriptor) sqmSelection.getJavaTypeDescriptor(),
					getProducerContext().getSessionFactory().getTypeConfiguration()
			);
			return null;
		}

		final QueryResult queryResult = resultProducer.createQueryResult(
				sqmSelection.getAlias(),
				this
		);

		queryResults.add( queryResult );
		applyFetches( queryResult );

		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyFetches(QueryResult queryReturn) {
		if ( !FetchParent.class.isInstance( queryReturn ) ) {
			return;
		}

		applyFetches( (FetchParent) queryReturn );
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyFetches(FetchParent fetchParent) {
		new FetchGraphBuilder(
				getQuerySpecStack().getCurrent(),
				this,
				getQueryOptions().getEntityGraphQueryHint()
		).process( fetchParent );
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return expressionResolver;
	}

//	@Override
//	public SqlSelection resolveSqlSelection(Expression expression) {
//		return sqlSelectionByExpressionMap.get( expression );
//	}

	//	@Override
//	public DomainReferenceExpression visitAttributeReferenceExpression(AttributeBinding attributeBinding) {
//		if ( attributeBinding instanceof PluralAttributeBinding ) {
//			return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeExpression(
//					this,
//					(PluralAttributeBinding) attributeBinding
//			);
//		}
//		else {
//			return getCurrentDomainReferenceExpressionBuilder().buildSingularAttributeExpression(
//					this,
//					(SingularAttributeBinding) attributeBinding
//			);
//		}
//	}
}
