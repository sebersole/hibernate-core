/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.spi;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.hibernate.AssertionFailure;
import org.hibernate.persister.collection.spi.ImprovedCollectionPersister;
import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.persister.common.spi.SqmTypeImplementor;
import org.hibernate.sql.sqm.ast.from.EntityTableGroup;
import org.hibernate.sql.sqm.ast.from.TableGroup;
import org.hibernate.sql.sqm.ast.from.TableGroupJoin;
import org.hibernate.sql.sqm.ast.from.TableSpace;
import org.hibernate.sql.sqm.ast.predicate.Junction;
import org.hibernate.sql.sqm.ast.predicate.Predicate;
import org.hibernate.sql.sqm.internal.FromClauseIndex;
import org.hibernate.sql.sqm.internal.SqlAliasBaseManager;
import org.hibernate.sqm.BaseSemanticQueryWalker;
import org.hibernate.sqm.domain.PluralAttribute;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.sqm.query.DeleteStatement;
import org.hibernate.sqm.query.InsertSelectStatement;
import org.hibernate.sqm.query.QuerySpec;
import org.hibernate.sqm.query.SelectStatement;
import org.hibernate.sqm.query.UpdateStatement;
import org.hibernate.sqm.query.expression.AttributeReferenceSqmExpression;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.sqm.query.expression.CaseSearchedSqmExpression;
import org.hibernate.sqm.query.expression.CaseSimpleSqmExpression;
import org.hibernate.sqm.query.expression.CoalesceSqmExpression;
import org.hibernate.sqm.query.expression.ConcatSqmExpression;
import org.hibernate.sqm.query.expression.ConstantEnumSqmExpression;
import org.hibernate.sqm.query.expression.ConstantFieldSqmExpression;
import org.hibernate.sqm.query.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.sqm.query.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.sqm.query.expression.LiteralCharacterSqmExpression;
import org.hibernate.sqm.query.expression.LiteralDoubleSqmExpression;
import org.hibernate.sqm.query.expression.LiteralFalseSqmExpression;
import org.hibernate.sqm.query.expression.LiteralFloatSqmExpression;
import org.hibernate.sqm.query.expression.LiteralIntegerSqmExpression;
import org.hibernate.sqm.query.expression.LiteralLongSqmExpression;
import org.hibernate.sqm.query.expression.LiteralNullSqmExpression;
import org.hibernate.sqm.query.expression.LiteralStringSqmExpression;
import org.hibernate.sqm.query.expression.LiteralTrueSqmExpression;
import org.hibernate.sqm.query.expression.NamedParameterSqmExpression;
import org.hibernate.sqm.query.expression.NullifSqmExpression;
import org.hibernate.sqm.query.expression.PositionalParameterSqmExpression;
import org.hibernate.sqm.query.expression.SqmExpression;
import org.hibernate.sqm.query.expression.UnaryOperationSqmExpression;
import org.hibernate.sqm.query.expression.function.AvgFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.CountFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.MaxFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.MinFunctionSqmExpression;
import org.hibernate.sqm.query.expression.function.SumFunctionSqmExpression;
import org.hibernate.sqm.query.from.CrossJoinedFromElement;
import org.hibernate.sqm.query.from.FromClause;
import org.hibernate.sqm.query.from.FromElementSpace;
import org.hibernate.sqm.query.from.JoinedFromElement;
import org.hibernate.sqm.query.from.QualifiedAttributeJoinFromElement;
import org.hibernate.sqm.query.from.QualifiedEntityJoinFromElement;
import org.hibernate.sqm.query.from.RootEntityFromElement;
import org.hibernate.sqm.query.order.OrderByClause;
import org.hibernate.sqm.query.order.SortSpecification;
import org.hibernate.sqm.query.predicate.AndSqmPredicate;
import org.hibernate.sqm.query.predicate.BetweenSqmPredicate;
import org.hibernate.sqm.query.predicate.GroupedSqmPredicate;
import org.hibernate.sqm.query.predicate.InListSqmPredicate;
import org.hibernate.sqm.query.predicate.InSubQuerySqmPredicate;
import org.hibernate.sqm.query.predicate.LikeSqmPredicate;
import org.hibernate.sqm.query.predicate.NegatedSqmPredicate;
import org.hibernate.sqm.query.predicate.NullnessSqmPredicate;
import org.hibernate.sqm.query.predicate.OrSqmPredicate;
import org.hibernate.sqm.query.predicate.RelationalSqmPredicate;
import org.hibernate.sqm.query.predicate.WhereClause;
import org.hibernate.sqm.query.select.DynamicInstantiation;
import org.hibernate.sqm.query.select.DynamicInstantiationArgument;
import org.hibernate.sqm.query.select.DynamicInstantiationTarget;
import org.hibernate.sqm.query.select.SelectClause;
import org.hibernate.sqm.query.select.Selection;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 * @author John O'Hara
 */
public class SelectStatementInterpreter extends BaseSemanticQueryWalker {

	/**
	 * Main entry point into SQM SelectStatement interpretation
	 *
	 * @param statement The SQM SelectStatement to interpret
	 * @param queryOptions The options to be applied to the interpretation
	 * @param callback to be formally defined
	 *
	 * @return The SQL AST
	 */
	public static SelectQuery interpret(SelectStatement statement, QueryOptions queryOptions, Callback callback) {
		final SelectStatementInterpreter walker = new SelectStatementInterpreter( queryOptions, callback );
		return walker.interpret( statement );
	}

	private final QueryOptions queryOptions;
	private final Callback callback;

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();
	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private SelectQuery sqlAst;

	public SelectStatementInterpreter(QueryOptions queryOptions, Callback callback) {
		this.queryOptions = queryOptions;
		this.callback = callback;
	}

	public SelectQuery interpret(SelectStatement statement) {
		visitSelectStatement( statement );
		return getSelectQuery();
	}

	public SelectQuery getSelectQuery() {
		return sqlAst;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker


	@Override
	public Object visitUpdateStatement(UpdateStatement statement) {
		throw new AssertionFailure( "Not expecting UpdateStatement" );
	}

	@Override
	public Object visitDeleteStatement(DeleteStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public Object visitInsertSelectStatement(InsertSelectStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public SelectQuery visitSelectStatement(SelectStatement statement) {
		if ( sqlAst != null ) {
			throw new AssertionFailure( "SelectQuery already visited" );
		}

		sqlAst = new SelectQuery( visitQuerySpec( statement.getQuerySpec() ) );

		if ( statement.getOrderByClause() != null ) {
			for ( SortSpecification sortSpecification : statement.getOrderByClause().getSortSpecifications() ) {
				sqlAst.addSortSpecification( visitSortSpecification( sortSpecification ) );
			}
		}

		return sqlAst;
	}

	@Override
	public OrderByClause visitOrderByClause(OrderByClause orderByClause) {
		throw new AssertionFailure( "Unexpected visitor call" );
	}

	@Override
	public org.hibernate.sql.ast.sort.SortSpecification visitSortSpecification(SortSpecification sortSpecification) {
		return new org.hibernate.sql.ast.sort.SortSpecification(
				(org.hibernate.sql.ast.expression.Expression) sortSpecification.getSortExpression().accept( this ),
				sortSpecification.getCollation(),
				sortSpecification.getSortOrder()
		);
	}

	private Stack<org.hibernate.sql.ast.QuerySpec> querySpecStack = new Stack<>();

	@Override
	public org.hibernate.sql.ast.QuerySpec visitQuerySpec(QuerySpec querySpec) {
		final org.hibernate.sql.ast.QuerySpec astQuerySpec = new org.hibernate.sql.ast.QuerySpec();
		querySpecStack.push( astQuerySpec );

		fromClauseIndex.pushFromClause( astQuerySpec.getFromClause() );

		try {
			// we want to visit the from-clause first
			visitFromClause( querySpec.getFromClause() );

			final SelectClause selectClause = querySpec.getSelectClause();
			if ( selectClause != null ) {
				visitSelectClause( selectClause );
			}

			final WhereClause whereClause = querySpec.getWhereClause();
			if ( whereClause != null ) {
				querySpecStack.peek().setWhereClauseRestrictions(
						(Predicate) whereClause.getPredicate().accept( this )
				);
			}

			return astQuerySpec;
		}
		finally {
			assert querySpecStack.pop() == astQuerySpec;
			assert fromClauseIndex.popFromClause() == astQuerySpec.getFromClause();
		}
	}

	@Override
	public Void visitFromClause(FromClause fromClause) {
		for ( FromElementSpace fromElementSpace : fromClause.getFromElementSpaces() ) {
			visitFromElementSpace( fromElementSpace );
		}

		return null;
	}

	private TableSpace tableSpace;

	@Override
	public TableSpace visitFromElementSpace(FromElementSpace fromElementSpace) {
		tableSpace = fromClauseIndex.currentFromClause().makeTableSpace();
		try {
			visitRootEntityFromElement( fromElementSpace.getRoot() );
			for ( JoinedFromElement joinedFromElement : fromElementSpace.getJoins() ) {
				tableSpace.addJoinedTableGroup( (TableGroupJoin) joinedFromElement.accept(
						this ) );
			}
			return tableSpace;
		}
		finally {
			tableSpace = null;
		}
	}

	@Override
	public Object visitRootEntityFromElement(RootEntityFromElement rootEntityFromElement) {
		if ( fromClauseIndex.isResolved( rootEntityFromElement ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( rootEntityFromElement );
			return resolvedTableGroup.resolveEntityReference();
		}

		final ImprovedEntityPersister entityPersister = (ImprovedEntityPersister) rootEntityFromElement.getBoundModelType();
		final EntityTableGroup group = entityPersister.buildTableGroup(
				rootEntityFromElement,
				tableSpace,
				sqlAliasBaseManager,
				fromClauseIndex
		);
		tableSpace.setRootTableGroup( group );

		return null;
	}

	@Override
	public Object visitQualifiedAttributeJoinFromElement(QualifiedAttributeJoinFromElement joinedFromElement) {
		if ( fromClauseIndex.isResolved( joinedFromElement ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( joinedFromElement );
			return resolvedTableGroup.resolveEntityReference();
		}

		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		final TableGroup group;

		if ( joinedFromElement.getBoundAttribute() instanceof PluralAttribute ) {
			final ImprovedCollectionPersister improvedCollectionPersister = (ImprovedCollectionPersister) joinedFromElement.getBoundAttribute();
			group = improvedCollectionPersister.buildTableGroup(
					joinedFromElement,
					tableSpace,
					sqlAliasBaseManager,
					fromClauseIndex
			);

			final TableGroup lhsTableGroup = fromClauseIndex.findResolvedTableGroup( joinedFromElement.getAttributeBindingSource() );
			// I *think* it is a valid assumption here that the underlying TableGroup for an attribute is ultimately an EntityTableGroup
			// todo : verify this
			final ColumnBinding[] joinLhsColumns = ( (EntityTableGroup) lhsTableGroup ).resolveIdentifierColumnBindings();
			final ColumnBinding[] joinRhsColumns = ( (CollectionTableGroup) group ).resolveKeyColumnBindings();
			assert joinLhsColumns.length == joinRhsColumns.length;

			for ( int i = 0; i < joinLhsColumns.length; i++ ) {
				predicate.add(
						new RelationalPredicate(
								RelationalPredicate.Operator.EQUAL,
								new ColumnBindingExpression( joinLhsColumns[i] ),
								new ColumnBindingExpression( joinRhsColumns[i] )
						)
				);
			}
		}
		else {
			final SingularAttributeImplementor singularAttribute = (SingularAttributeImplementor) joinedFromElement.getBoundAttribute();
			if ( singularAttribute.getAttributeTypeClassification() == SingularAttribute.Classification.EMBEDDED ) {
				group = fromClauseIndex.findResolvedTableGroup( joinedFromElement.getAttributeBindingSource() );
			}
			else {
				final ImprovedEntityPersister entityPersister = (ImprovedEntityPersister) joinedFromElement.getIntrinsicSubclassIndicator();
				group = entityPersister.buildTableGroup(
						joinedFromElement,
						tableSpace,
						sqlAliasBaseManager,
						fromClauseIndex
				);

				final TableGroup lhsTableGroup = fromClauseIndex.findResolvedTableGroup( joinedFromElement.getAttributeBindingSource() );
				final ColumnBinding[] joinLhsColumns = lhsTableGroup.resolveBindings( singularAttribute );
				final ColumnBinding[] joinRhsColumns;

				final org.hibernate.type.EntityType ormType = (org.hibernate.type.EntityType) singularAttribute.getOrmType();
				if ( ormType.getRHSUniqueKeyPropertyName() == null ) {
					joinRhsColumns = ( (EntityTableGroup) group ).resolveIdentifierColumnBindings();
				}
				else {
					final ImprovedEntityPersister associatedPersister = ( (EntityTableGroup) lhsTableGroup ).getPersister();
					joinRhsColumns = group.resolveBindings(
							(SingularAttribute) associatedPersister.findAttribute( ormType.getRHSUniqueKeyPropertyName() )
					);
				}
				assert joinLhsColumns.length == joinRhsColumns.length;

				for ( int i = 0; i < joinLhsColumns.length; i++ ) {
					predicate.add(
							new RelationalPredicate(
									RelationalPredicate.Operator.EQUAL,
									new ColumnBindingExpression( joinLhsColumns[i] ),
									new ColumnBindingExpression( joinRhsColumns[i] )
							)
					);
				}
			}
		}

		fromClauseIndex.crossReference( joinedFromElement, group );

		// add any additional join restrictions
		if ( joinedFromElement.getOnClausePredicate() != null ) {
			predicate.add( (Predicate) joinedFromElement.getOnClausePredicate().accept( this ) );
		}

		return new TableGroupJoin( joinedFromElement.getJoinType(), group, predicate );
	}

	@Override
	public TableGroupJoin visitCrossJoinedFromElement(CrossJoinedFromElement joinedFromElement) {
		final ImprovedEntityPersister entityPersister = (ImprovedEntityPersister) joinedFromElement.getIntrinsicSubclassIndicator();
		TableGroup group = entityPersister.buildTableGroup(
				joinedFromElement,
				tableSpace,
				sqlAliasBaseManager,
				fromClauseIndex
		);
		return new TableGroupJoin( joinedFromElement.getJoinType(), group, null );
	}

	@Override
	public Object visitQualifiedEntityJoinFromElement(QualifiedEntityJoinFromElement joinedFromElement) {
		throw new NotYetImplementedException();
	}

	@Override
	public org.hibernate.sql.ast.select.SelectClause visitSelectClause(SelectClause selectClause) {
		super.visitSelectClause( selectClause );
		currentQuerySpec().getSelectClause().makeDistinct( selectClause.isDistinct() );
		return currentQuerySpec().getSelectClause();
	}

	private org.hibernate.sql.ast.QuerySpec currentQuerySpec() {
		return querySpecStack.peek();
	}

	@Override
	public org.hibernate.sql.ast.select.Selection visitSelection(Selection selection) {
		org.hibernate.sql.ast.select.Selection ormSelection = new org.hibernate.sql.ast.select.Selection(
				(org.hibernate.sql.ast.expression.Expression) selection.getExpression().accept( this ),
				selection.getAlias()
		);

		currentQuerySpec().getSelectClause().selection( ormSelection );

		return ormSelection;
	}

	@Override
	public org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation visitDynamicInstantiation(DynamicInstantiation dynamicInstantiation) {
		final Class target = interpret( dynamicInstantiation.getInstantiationTarget() );
		org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation sqlTree =
				new org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation( target );

		for ( DynamicInstantiationArgument dynamicInstantiationArgument : dynamicInstantiation.getArguments() ) {
			sqlTree.addArgument(
					dynamicInstantiationArgument.getAlias(),
					(org.hibernate.sql.ast.expression.Expression) dynamicInstantiationArgument.getExpression().accept( this )
			);
		}

		return sqlTree;
	}

	private Class interpret(DynamicInstantiationTarget instantiationTarget) {
		if ( instantiationTarget.getNature() == DynamicInstantiationTarget.Nature.LIST ) {
			return List.class;
		}
		if ( instantiationTarget.getNature() == DynamicInstantiationTarget.Nature.MAP ) {
			return Map.class;
		}
		return instantiationTarget.getJavaType();
	}

	@Override
	public AttributeReference visitAttributeReferenceExpression(AttributeReferenceSqmExpression expression) {
		// WARNING : works on the assumption that the referenced attribute is always singular.
		// I believe that is valid, but we will need to test
		// todo : verify if this is a valid assumption
		final SingularAttributeImplementor attribute = (SingularAttributeImplementor) expression.getBoundAttribute();
		final TableGroup tableGroup = fromClauseIndex.findResolvedTableGroup( expression.getAttributeBindingSource() );
		return tableGroup.resolve( attribute );
	}

	@Override
	public QueryLiteral visitLiteralStringExpression(LiteralStringSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	protected Type extractOrmType(org.hibernate.sqm.domain.Type sqmType) {
		if ( sqmType == null ) {
			return null;
		}

		return ( (SqmTypeImplementor) sqmType ).getOrmType();
	}

	@Override
	public QueryLiteral visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralFloatExpression(LiteralFloatSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralLongExpression(LiteralLongSqmExpression expression) {
		return new QueryLiteral(
				expression.getLiteralValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralTrueExpression(LiteralTrueSqmExpression expression) {
		return new QueryLiteral(
				Boolean.TRUE,
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralFalseExpression(LiteralFalseSqmExpression expression) {
		return new QueryLiteral(
				Boolean.FALSE,
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public QueryLiteral visitLiteralNullExpression(LiteralNullSqmExpression expression) {
		return new QueryLiteral(
				null,
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public Object visitConstantEnumExpression(ConstantEnumSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public Object visitConstantFieldExpression(ConstantFieldSqmExpression expression) {
		return new QueryLiteral(
				expression.getValue(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public NamedParameter visitNamedParameterExpression(NamedParameterSqmExpression expression) {
		return new NamedParameter(
				expression.getName(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public PositionalParameter visitPositionalParameterExpression(PositionalParameterSqmExpression expression) {
		return new PositionalParameter(
				expression.getPosition(),
				extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public AvgFunction visitAvgFunction(AvgFunctionSqmExpression expression) {
		return new AvgFunction(
				(Expression) expression.getArgument().accept( this ),
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public MaxFunction visitMaxFunction(MaxFunctionSqmExpression expression) {
		return new MaxFunction(
				(Expression) expression.getArgument().accept( this ),
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public MinFunction visitMinFunction(MinFunctionSqmExpression expression) {
		return new MinFunction(
				(Expression) expression.getArgument().accept( this ),
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public SumFunction visitSumFunction(SumFunctionSqmExpression expression) {
		return new SumFunction(
				(Expression) expression.getArgument().accept( this ),
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public CountFunction visitCountFunction(CountFunctionSqmExpression expression) {
		return new CountFunction(
				(Expression) expression.getArgument().accept( this ),
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public CountStarFunction visitCountStarFunction(CountStarFunctionSqmExpression expression) {
		return new CountStarFunction(
				expression.isDistinct(),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	@Override
	public Object visitUnaryOperationExpression(UnaryOperationSqmExpression expression) {
		return new UnaryOperationExpression(
				interpret( expression.getOperation() ),
				(Expression) expression.getOperand().accept( this ),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	private UnaryOperationExpression.Operation interpret(UnaryOperationSqmExpression.Operation operation) {
		switch ( operation ) {
			case PLUS: {
				return UnaryOperationExpression.Operation.PLUS;
			}
			case MINUS: {
				return UnaryOperationExpression.Operation.MINUS;
			}
		}

		throw new IllegalStateException( "Unexpected UnaryOperationExpression Operation : " + operation );
	}

	@Override
	public Expression visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression) {
		if ( expression.getOperation() == BinaryArithmeticSqmExpression.Operation.MODULO ) {
			return new NonStandardFunctionExpression(
					"mod",
					(BasicType) extractOrmType( expression.getExpressionType() ),
					(Expression) expression.getLeftHandOperand().accept( this ),
					(Expression) expression.getRightHandOperand().accept( this )
			);
		}
		return new BinaryArithmeticExpression(
				interpret( expression.getOperation() ),
				(Expression) expression.getLeftHandOperand().accept( this ),
				(Expression) expression.getRightHandOperand().accept( this ),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	private BinaryArithmeticExpression.Operation interpret(BinaryArithmeticSqmExpression.Operation operation) {
		switch ( operation ) {
			case ADD: {
				return BinaryArithmeticExpression.Operation.ADD;
			}
			case SUBTRACT: {
				return BinaryArithmeticExpression.Operation.SUBTRACT;
			}
			case MULTIPLY: {
				return BinaryArithmeticExpression.Operation.MULTIPLY;
			}
			case DIVIDE: {
				return BinaryArithmeticExpression.Operation.DIVIDE;
			}
			case QUOT: {
				return BinaryArithmeticExpression.Operation.QUOT;
			}
		}

		throw new IllegalStateException( "Unexpected BinaryArithmeticExpression Operation : " + operation );
	}

	@Override
	public CoalesceExpression visitCoalesceExpression(CoalesceSqmExpression expression) {
		final CoalesceExpression result = new CoalesceExpression();
		for ( SqmExpression value : expression.getValues() ) {
			result.value( (Expression) value.accept( this ) );
		}

		return result;
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(CaseSimpleSqmExpression expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				extractOrmType( expression.getExpressionType() ),
				(Expression) expression.getFixture().accept( this )
		);

		for ( CaseSimpleSqmExpression.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Expression) whenFragment.getCheckValue().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(CaseSearchedSqmExpression expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression( extractOrmType( expression.getExpressionType() ) );

		for ( CaseSearchedSqmExpression.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Predicate) whenFragment.getPredicate().accept( this ),
					(Expression) whenFragment.getResult().accept( this )
			);
		}

		result.otherwise( (Expression) expression.getOtherwise().accept( this ) );

		return result;
	}

	@Override
	public NullifExpression visitNullifExpression(NullifSqmExpression expression) {
		return new NullifExpression(
				(Expression) expression.getFirstArgument().accept( this ),
				(Expression) expression.getSecondArgument().accept( this )
		);
	}

	@Override
	public ConcatExpression visitConcatExpression(ConcatSqmExpression expression) {
		return new ConcatExpression(
				(Expression) expression.getLeftHandOperand().accept( this ),
				(Expression) expression.getLeftHandOperand().accept( this ),
				(BasicType) extractOrmType( expression.getExpressionType() )
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	public GroupedPredicate visitGroupedPredicate(GroupedSqmPredicate predicate) {
		return new GroupedPredicate ( (Predicate ) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitAndPredicate(AndSqmPredicate predicate) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		conjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		conjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return conjunction;
	}

	@Override
	public Junction visitOrPredicate(OrSqmPredicate predicate) {
		final Junction disjunction = new Junction( Junction.Nature.DISJUNCTION );
		disjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		disjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return disjunction;
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(NegatedSqmPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public RelationalPredicate visitRelationalPredicate(RelationalSqmPredicate predicate) {
		return new RelationalPredicate(
				interpret( predicate.getOperator() ),
				(Expression) predicate.getLeftHandExpression().accept( this ),
				(Expression) predicate.getRightHandExpression().accept( this )
		);
	}

	private RelationalPredicate.Operator interpret(RelationalSqmPredicate.Operator operator) {
		switch ( operator ) {
			case EQUAL: {
				return RelationalPredicate.Operator.EQUAL;
			}
			case NOT_EQUAL: {
				return RelationalPredicate.Operator.NOT_EQUAL;
			}
			case GREATER_THAN_OR_EQUAL: {
				return RelationalPredicate.Operator.GE;
			}
			case GREATER_THAN: {
				return RelationalPredicate.Operator.GT;
			}
			case LESS_THAN_OR_EQUAL: {
				return RelationalPredicate.Operator.LE;
			}
			case LESS_THAN: {
				return RelationalPredicate.Operator.LT;
			}
		}

		throw new IllegalStateException( "Unexpected RelationalPredicate Type : " + operator );
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(BetweenSqmPredicate predicate) {
		return new BetweenPredicate(
				(Expression) predicate.getExpression().accept( this ),
				(Expression) predicate.getLowerBound().accept( this ),
				(Expression) predicate.getUpperBound().accept( this ),
				predicate.isNegated()
		);
	}

	@Override
	public LikePredicate visitLikePredicate(LikeSqmPredicate predicate) {
		final Expression escapeExpression = predicate.getEscapeCharacter() == null
				? null
				: (Expression) predicate.getEscapeCharacter().accept( this );

		return new LikePredicate(
				(Expression) predicate.getMatchExpression().accept( this ),
				(Expression) predicate.getPattern().accept( this ),
				escapeExpression,
				predicate.isNegated()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(NullnessSqmPredicate predicate) {
		return new NullnessPredicate(
				(Expression) predicate.getExpression().accept( this ),
				predicate.isNegated()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(InListSqmPredicate predicate) {
		final InListPredicate inPredicate = new InListPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				predicate.isNegated()
		);
		for ( SqmExpression expression : predicate.getListExpressions() ) {
			inPredicate.addExpression( (Expression) expression.accept( this ) );
		}
		return inPredicate;
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(InSubQuerySqmPredicate predicate) {
		return new InSubQueryPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				(org.hibernate.sql.ast.QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}
}
