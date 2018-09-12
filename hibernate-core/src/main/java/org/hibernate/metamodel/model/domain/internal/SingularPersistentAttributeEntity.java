/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.loader.internal.StandardSingleUniqueKeyEntityLoader;
import org.hibernate.loader.spi.SingleEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Helper;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingImpl;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.ForeignKey.ColumnMappings;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.AggregateSqlSelectionGroupNode;
import org.hibernate.sql.results.internal.domain.entity.DelayedEntityFetch;
import org.hibernate.sql.results.internal.domain.entity.EntityFetchImpl;
import org.hibernate.sql.results.internal.domain.entity.ImmediatePkEntityFetch;
import org.hibernate.sql.results.internal.domain.entity.ImmediateUkEntityFetch;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification.MANY_TO_ONE;
import static org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification.ONE_TO_ONE;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements JoinablePersistentAttribute<O,J>, EntityValuedNavigable<J>, Fetchable<J>, AllowableParameterType<J>, TableGroupJoinProducer {

	private final SingularAttributeClassification classification;
	private final PersistentAttributeType persistentAttributeType;
	private final boolean isLogicalOneToOne;
	private final boolean constrained;
	private final NavigableRole navigableRole;
	private final String sqlAliasStem;
	private final EntityDescriptor<J> entityDescriptor;
	private final String referencedUkAttributeName;
	private final FetchStrategy fetchStrategy;

	private final NotFoundAction notFoundAction;

	private StateArrayContributor referencedUkAttribute;
	private SingleEntityLoader singleEntityLoader;
	private ForeignKey foreignKey;

	public SingularPersistentAttributeEntity(
			ManagedTypeDescriptor<O> runtimeModelContainer,
			PersistentAttributeMapping bootModelAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			SingularAttributeClassification classification,
			RuntimeModelCreationContext context) {
		super( runtimeModelContainer, bootModelAttribute, propertyAccess, disposition );

		this.classification = classification;
		this.navigableRole = runtimeModelContainer.getNavigableRole().append( bootModelAttribute.getName() );

		final ToOne valueMapping = (ToOne) bootModelAttribute.getValueMapping();
		referencedUkAttributeName = valueMapping.getReferencedPropertyName();

		if ( valueMapping.getReferencedEntityName() == null ) {
			throw new MappingException(
					"Name of target entity of a to-one association not known : " + navigableRole.getFullPath()
			);
		}

		this.entityDescriptor = context.getInFlightRuntimeModel().findEntityDescriptor( valueMapping.getReferencedEntityName() );
		if ( entityDescriptor == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Cannot create SingularPersistentAttributeEntity [%s] : could not locate target entity descriptor [%s]",
							navigableRole.getFullPath(),
							valueMapping.getReferencedEntityName()
					)
			);
		}

		// todo (6.0) : we need to delay resolving this.
		//		this is essentially a "second pass".  for now we assume it
		// 		points to the target entity's PK
//		assert valueMapping.isReferenceToPrimaryKey();

		// taken from ToOneFkSecondPass
		if ( valueMapping.getForeignKey() != null ) {
			this.foreignKey = context.getDatabaseObjectResolver().resolveForeignKey( valueMapping.getForeignKey() );
		}

		if ( MANY_TO_ONE.equals( classification ) ) {
			final ManyToOne manyToOne = (ManyToOne) bootModelAttribute.getValueMapping();
			isLogicalOneToOne = manyToOne.isLogicalOneToOne();
			persistentAttributeType = isLogicalOneToOne
					? PersistentAttributeType.ONE_TO_ONE
					: PersistentAttributeType.MANY_TO_ONE;
			constrained = true;
			notFoundAction = manyToOne.isIgnoreNotFound()
					? NotFoundAction.IGNORE
					: NotFoundAction.EXCEPTION;
		}
		else {
			isLogicalOneToOne = false;
			persistentAttributeType = PersistentAttributeType.ONE_TO_ONE;
			constrained = ( (OneToOne) valueMapping ).isConstrained();
			notFoundAction = NotFoundAction.EXCEPTION;
		}

		this.sqlAliasStem =  SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootModelAttribute.getName() );

		context.registerNavigable( this );
		instantiationComplete( bootModelAttribute, context );

		this.fetchStrategy = Helper.determineFetchStrategy( bootModelAttribute, runtimeModelContainer, entityDescriptor );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean finishInitialization(RuntimeModelCreationContext creationContext) {
		if ( referencedUkAttributeName == null ) {
			singleEntityLoader = getAssociatedEntityDescriptor().getSingleIdLoader();
		}
		else {
			singleEntityLoader = new StandardSingleUniqueKeyEntityLoader(
					getContainer().findPersistentAttribute( referencedUkAttributeName ),
					this
			);
		}

		if ( this.foreignKey == null ) {
			SingularPersistentAttributeEntity foreignKeyOwningAttribute = (SingularPersistentAttributeEntity)
					entityDescriptor.findNavigable( referencedUkAttributeName );

			if ( foreignKeyOwningAttribute != null && foreignKeyOwningAttribute.getForeignKey() != null ) {
				final ForeignKey foreignKeyOwning = foreignKeyOwningAttribute.getForeignKey();

				List<ColumnMappings.ColumnMapping> columns = new ArrayList<>();
				for ( ColumnMappings.ColumnMapping columnMapping : foreignKeyOwning.getColumnMappings().getColumnMappings() ) {
					columns.add( new ColumnMappingImpl( columnMapping.getTargetColumn(), columnMapping.getReferringColumn() ) );
				}

				this.foreignKey = new ForeignKey(
						foreignKeyOwning.getName(),
						false,
						foreignKeyOwning.getKeyDefinition(),
						false,
						false,
						foreignKeyOwning.getTargetTable(),
						foreignKeyOwning.getReferringTable(),
						new ColumnMappingsImpl(
								foreignKeyOwning.getTargetTable(),
								foreignKeyOwning.getReferringTable(),
								columns
						)
				);

				return true;
			}

			return false;
		}

		return true;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public EntityValuedExpressableType<J> getType() {
		return (EntityValuedExpressableType<J>) super.getType();
	}

	@Override
	public String getJpaEntityName() {
		return entityDescriptor.getJpaEntityName();
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return entityDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityDescriptor.findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityDescriptor.visitNavigables( visitor );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityDescriptor getAssociatedEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityDescriptor.getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		// todo (6.0) : ultimately all attributes/StateArrayContributors will need to be Fetchable - including basic-typed ones
		getEntityDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmSingularAttributeReferenceEntity(
				containerReference,
				this,
				creationContext
		);
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		return entityDescriptor.createDomainResult(
				navigableReference,
				resultVariable,
				creationContext,
				creationState
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		/// todo (6.0) : calculate this and cache as state

		switch ( getAttributeTypeClassification() ) {
			case MANY_TO_ONE: {
				return isLogicalOneToOne
						? ForeignKeyDirection.FROM_PARENT
						: ForeignKeyDirection.TO_PARENT;
			}
			case ONE_TO_ONE: {
				return constrained
						? ForeignKeyDirection.TO_PARENT
						: ForeignKeyDirection.FROM_PARENT;

			}
			case ANY: {
				return ForeignKeyDirection.FROM_PARENT;
			}
		}

		throw new HibernateException(
				"Unexpected classification [" + getAttributeTypeClassification() +
						"] found in entity-valued attribute descriptor"
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean isJoinFetch,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {

		// types of entity fetches:
		//		1) joined
		//		2) lazy
		//		3) selected

		final Stack<NavigableReference> navigableReferenceStack = creationState.getNavigableReferenceStack();

		if ( fetchTiming == FetchTiming.DELAYED ) {
			// todo (6.0) : need general laziness metadata - currently only done for entity
			final boolean isContainerEnhancedForLazy = getContainer() instanceof EntityDescriptor<?>
					&& ( (EntityDescriptor) getContainer() ).getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();

			final boolean cannotBeLazy = ( getAttributeTypeClassification() == ONE_TO_ONE && isOptional() ) || isContainerEnhancedForLazy;

			if ( cannotBeLazy ) {
				return generateImmediateFetch( fetchParent, creationState, creationContext );
			}
			else {
				return generateDelayedFetch( fetchParent, creationState, creationContext );
			}
		}
		else {
			if ( isJoinFetch ) {
				return generateJoinFetch(
						fetchParent,
						lockMode,
						resultVariable,
						creationState,
						creationContext,
						navigableReferenceStack
				);
			}
			else {
				return generateImmediateFetch( fetchParent, creationState, creationContext );
			}
		}
	}

	private Fetch generateDelayedFetch(
			FetchParent fetchParent,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new DelayedEntityFetch(
				fetchParent,
				this,
				createKeyDomainResult( creationState, creationContext )
		);
	}

	private ForeignKeyDomainResult createKeyDomainResult(
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		// make sure columns for the FK, if one, are added to the SQL AST as selections
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		final List<SqlSelection> sqlSelections = new ArrayList<>();

		List<Column> columns = getColumns();
		if ( getColumns() == null || getColumns().isEmpty() ) {
			columns = getAssociatedEntityDescriptor().getIdentifierDescriptor().getColumns();
		}

		for ( Column column : columns ) {
			sqlSelections.add(
					sqlExpressionResolver.resolveSqlSelection(
							sqlExpressionResolver.resolveSqlExpression(
									creationState.getColumnReferenceQualifierStack().getCurrent(),
									column
							),
							column.getJavaTypeDescriptor(),
							creationContext.getSessionFactory().getTypeConfiguration()
					)
			);
		}

		return new ForeignKeyDomainResult( null, sqlSelections );
	}

	private Fetch generateImmediateFetch(
			FetchParent fetchParent,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		if ( referencedUkAttributeName == null
				|| referencedUkAttributeName.equals( EntityIdentifier.NAVIGABLE_ID )
				|| referencedUkAttributeName.equals( EntityIdentifier.LEGACY_NAVIGABLE_ID ) ) {
			return new ImmediatePkEntityFetch(
					fetchParent,
					this,
					singleEntityLoader,
					createKeyDomainResult( creationState, creationContext ),
					notFoundAction
			);
		}
		else {
			final Navigable ukTargetNavigable;
			if ( referencedUkAttribute == null ) {
				ukTargetNavigable = getAssociatedEntityDescriptor().getIdentifierDescriptor();
			}
			else {
				ukTargetNavigable = referencedUkAttribute;
			}
			return new ImmediateUkEntityFetch(
					fetchParent,
					this,
					singleEntityLoader,
					createKeyDomainResult( creationState, creationContext ),
					(key, sessionContractImplementor) -> new EntityUniqueKey(
							getAssociatedEntityDescriptor().getEntityName(),
							referencedUkAttributeName,
							key,
							getAssociatedEntityDescriptor().getIdentifierDescriptor().getJavaTypeDescriptor(),
							ukTargetNavigable.getJavaTypeDescriptor(),
							getAssociatedEntityDescriptor().getHierarchy().getRepresentation(),
							creationContext.getSessionFactory()
					),
					notFoundAction
			);
		}
	}

	private Fetch generateJoinFetch(
			FetchParent fetchParent,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext,
			Stack<NavigableReference> navigableReferenceStack) {
		final NavigableContainerReference parentReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();

		final NavigablePath navigablePath = fetchParent.getNavigablePath().append( getNavigableName() );

		// if there is an existing NavigableReference this fetch can use, use it.  otherwise create one
		NavigableReference navigableReference = parentReference.findNavigableReference( getNavigableName() );
		if ( navigableReference == null ) {
			// this creates the SQL AST join(s) in the from clause
			final TableGroupJoin tableGroupJoin = createTableGroupJoin(
					creationState.getSqlAliasBaseGenerator(),
					creationState.getColumnReferenceQualifierStack().getCurrent(),
					navigablePath,
					isNullable() ? JoinType.LEFT : JoinType.INNER,
					resultVariable,
					lockMode,
					creationState.getCurrentTableSpace()
			);
			creationState.getCurrentTableSpace().addJoinedTableGroup( tableGroupJoin );
			navigableReference = tableGroupJoin.getJoinedGroup().getNavigableReference();
			parentReference.addNavigableReference( navigableReference );
		}

		assert navigableReference.getNavigable() == this;


		creationState.getNavigableReferenceStack().push( navigableReference );
		creationState.getColumnReferenceQualifierStack().push( navigableReference.getColumnReferenceQualifier() );

		try {
			return new EntityFetchImpl(
					fetchParent,
					this,
					lockMode,
					fetchParent.getNavigablePath().append( getNavigableName() ),
					creationContext,
					creationState
			);
		}
		finally {
			creationState.getColumnReferenceQualifierStack().pop();
			creationState.getNavigableReferenceStack().pop();
		}
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		// handle optional entity references to be outer joins.
		if ( isNullable() && JoinType.INNER.equals( joinType ) ) {
			joinType = JoinType.LEFT;
		}

		return createTableGroupJoin(
				tableGroupJoinContext.getSqlAliasBaseGenerator(),
				tableGroupJoinContext.getLhs(),
				tableGroupJoinContext.getNavigablePath(),
				joinType,
				tableGroupInfoSource.getIdentificationVariable(),
				tableGroupJoinContext.getLockOptions().getEffectiveLockMode( tableGroupInfoSource.getIdentificationVariable() ),
				tableGroupJoinContext.getTableSpace()
		);
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			ColumnReferenceQualifier lhs,
			NavigablePath navigablePath,
			JoinType joinType,
			String identificationVariable,
			LockMode lockMode,
			TableSpace tableSpace) {
		final SqlAliasBase sqlAliasBase = sqlAliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );

		final TableReferenceJoinCollectorImpl joinCollector = new TableReferenceJoinCollectorImpl(
				tableSpace,
				lhs,
				navigablePath,
				identificationVariable,
				lockMode
		);

		getEntityDescriptor().applyTableReferenceJoins(
				lhs,
				joinType,
				sqlAliasBase,
				joinCollector
		);

		// handle optional entity references to be outer joins.
		if ( isNullable() && JoinType.INNER.equals( joinType ) ) {
			joinType = JoinType.LEFT;
		}

		return joinCollector.generateTableGroup( joinType, navigablePath.getFullPath() );

	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( ! getEntityDescriptor().isInstance( value ) ) {
			throw new HibernateException( "Unexpected value for unresolve [" + value + "], expecting entity instance" );
		}

		if ( referencedUkAttributeName == null || classification.equals( SingularAttributeClassification.ONE_TO_ONE ) ) {
			return getAssociatedEntityDescriptor().getIdentifierDescriptor().unresolve(
					getAssociatedEntityDescriptor().getIdentifier( value, session ),
					session
			);
		}
		else {
			final NonIdPersistentAttribute referencedAttribute = getAssociatedEntityDescriptor()
					.findPersistentAttribute( referencedUkAttributeName );
			return referencedAttribute.unresolve(
					referencedAttribute.getPropertyAccess().getGetter().get( value ),
					session
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		if ( !clause.getInclusionChecker().test( this ) ) {
			return;
		}

		if ( classification == ONE_TO_ONE && isOptional() && !constrained ) {
			return;
		}

		final ExpressableType writeable;

		if ( referencedUkAttributeName == null ) {
			writeable = getAssociatedEntityDescriptor().getIdentifierDescriptor();
		}
		else {
			writeable = getAssociatedEntityDescriptor().findPersistentAttribute( referencedUkAttributeName );
		}

		if ( writeable != null ) {
			final Iterator<Column> columnItr = foreignKey.getColumnMappings().getReferringColumns().iterator();
			writeable.dehydrate(
					value,
					(jdbcValue, sqlExpressableType, boundColumn) -> {
						assert columnItr.hasNext();
						jdbcValueCollector.collect(
								jdbcValue,
								sqlExpressableType,
								columnItr.next()
						);
					},
					clause,
					session
			);
			assert !columnItr.hasNext();
		}
	}

	@Override
	public List<Column> getColumns() {
		// todo (6.0) - is this really necessary to use export-enabled
//		if ( foreignKey.isExportationEnabled() ) {
			return foreignKey.getColumnMappings().getReferringColumns();
//		}
//		return new ArrayList<>();
	}

	private class TableReferenceJoinCollectorImpl implements TableReferenceJoinCollector {
		private final TableSpace tableSpace;
		private final ColumnReferenceQualifier lhs;
		private final NavigablePath navigablePath;
		private final String identificationVariable;
		private final LockMode lockMode;

		private TableReference rootTableReference;
		private List<TableReferenceJoin> tableReferenceJoins;
		private Predicate predicate;

		@SuppressWarnings("WeakerAccess")
		public TableReferenceJoinCollectorImpl(
				TableSpace tableSpace,
				ColumnReferenceQualifier lhs,
				NavigablePath navigablePath,
				String identificationVariable,
				LockMode lockMode) {
			this.tableSpace = tableSpace;
			this.lhs = lhs;
			this.navigablePath = navigablePath;
			this.identificationVariable = identificationVariable;
			this.lockMode = lockMode;
		}

		@Override
		public void addRoot(TableReference root) {
			if ( rootTableReference == null ) {
				rootTableReference = root;
			}
			else {
				collectTableReferenceJoin( makeJoin( lhs, rootTableReference ) );
			}

			predicate = makePredicate( lhs, rootTableReference );
		}

		private TableReferenceJoin makeJoin(ColumnReferenceQualifier lhs, TableReference rootTableReference) {
			return new TableReferenceJoin(
					JoinType.LEFT,
					rootTableReference,
					makePredicate( lhs, rootTableReference )
			);
		}

		private Predicate makePredicate(ColumnReferenceQualifier lhs, TableReference rhs) {
			final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

			for ( ColumnMappings.ColumnMapping columnMapping: foreignKey.getColumnMappings().getColumnMappings() ) {
				final ColumnReference referringColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
				final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

				// todo (6.0) : we need some kind of validation here that the column references are properly defined

				// todo (6.0) : we could also handle this using SQL row-value syntax, e.g.:
				//		`... where ... [ (rCol1, rCol2, ...) = (tCol1, tCol2, ...) ] ...`

				conjunction.add(
						new RelationalPredicate(
								RelationalPredicate.Operator.EQUAL,
								referringColumnReference,
								targetColumnReference
						)
				);
			}

			return conjunction;
		}

		@Override
		public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
			if ( tableReferenceJoins == null ) {
				tableReferenceJoins = new ArrayList<>();
			}
			tableReferenceJoins.add( tableReferenceJoin );
		}

		@SuppressWarnings("WeakerAccess")
		public TableGroupJoin generateTableGroup(JoinType joinType, String uid) {
			final EntityTableGroup joinedTableGroup = new EntityTableGroup(
					uid,
					tableSpace,
					SingularPersistentAttributeEntity.this,
					lockMode,
					navigablePath,
					rootTableReference,
					tableReferenceJoins,
					lhs
			);
			return new TableGroupJoin( joinType, joinedTableGroup, predicate );
		}
	}

	@Override
	public SqlSelectionGroupNode resolveSqlSelections(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		// todo (6.0) : handle fetching here?  or at a "higher level"?
		//
		// 		for now we just load the FK
		//
		// todo (6.0) : we need to know the corresponding BasicJavaDescriptor per Column
		//		how to implement that?

		if ( foreignKey.getColumnMappings().getReferringColumns().size() == 1 ) {
			final Column column = foreignKey.getColumnMappings().getReferringColumns().get( 0 );
			return resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
					resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
							qualifier,
							column
					),
					column.getJavaTypeDescriptor(),
					resolutionContext.getSessionFactory().getTypeConfiguration()
			);
		}

		final List<SqlSelection> selections = new ArrayList<>();
		for ( Column column: foreignKey.getColumnMappings().getReferringColumns() ) {
			selections.add(
					resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
							resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
									qualifier,
									column
							),
							column.getJavaTypeDescriptor(),
							resolutionContext.getSessionFactory().getTypeConfiguration()
					)
			);
		}

		return new AggregateSqlSelectionGroupNode( selections );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		if ( hydratedForm == null ) {
			return null;
		}

		// todo (6.0) : WrongClassException?

		if ( foreignKey.isReferenceToPrimaryKey() ) {
			// step 1 - generate EntityKey based on hydrated id form
			final Object resolvedIdentifier = getEntityDescriptor().getHierarchy()
					.getIdentifierDescriptor()
					.resolveHydratedState(
							hydratedForm,
							executionContext,
							session,
							null
					);
			final EntityKey entityKey = new EntityKey( resolvedIdentifier, getEntityDescriptor() );


			// step 2 - look for a matching entity (by EntityKey) on the context
			//		NOTE - we pass `! isOptional()` as `eager` to `resolveEntityInstance` because
			//		if it were being fetched dynamically (join fetch) that would have lead to an
			//		EntityInitializer for this entity being created and it would be available in the
			//		`resolutionContext`
			final Object entityInstance = executionContext.resolveEntityInstance( entityKey, !isOptional() );
			if ( entityInstance != null ) {
				return entityInstance;
			}

			// try loading it...
			//
			// todo (6.0) : need to make sure that the "JdbcValues" we are processing here have been added to the Session's stack of "load contexts"
			//		that allows the loader(s) to resolve entity's that are being loaded here.
			//
			//		NOTE : this is how we get around having to register a "holder" EntityEntry with the PersistenceContext
			//		but still letting other (recursive) loads find references we are loading.

			J loaded = getEntityDescriptor().getSingleIdLoader().load(
					resolvedIdentifier,
					LockOptions.READ,
					session
			);
			if ( loaded != null ) {
				return loaded;
			}
			throw new EntityNotFoundException(
					String.format(
							Locale.ROOT,
							"Unable to resolve entity-valued association [%s] foreign key value [%s] to associated entity instance of type [%s]",
							getNavigableRole(),
							resolvedIdentifier,
							getEntityDescriptor().getJavaTypeDescriptor()
					)
			);
		}
		else if ( referencedUkAttributeName != null ) {
			SingleUniqueKeyEntityLoader<J> loader = new StandardSingleUniqueKeyEntityLoader(
					referencedUkAttribute,
					this
			);
			EntityUniqueKey euk = new EntityUniqueKey(
					getEntityDescriptor().getEntityName(),
					referencedUkAttributeName,
					hydratedForm,
					getAssociatedEntityDescriptor().getIdentifierDescriptor().getJavaTypeDescriptor(),
					referencedUkAttribute.getJavaTypeDescriptor(),
					getEntityDescriptor().getHierarchy().getRepresentation(),
					session.getFactory()
			);
			// todo (6.0) : look into resolutionContext
			J loaded = loader.load( hydratedForm, LockOptions.READ, session );
			// todo (6.0) : if loaded != null add it to the resolutionContext
			return loaded;
		}
		return null;
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		if ( isNullable()
				&& getAttributeTypeClassification() != ONE_TO_ONE
				&& nullifier.isNullifiable( getEntityDescriptor().getEntityName(), value ) ) {
			nonNullableTransientEntities.add( getEntityDescriptor().getEntityName(), value );
		}
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return foreignKey.getColumnMappings().getColumnMappings().size();
	}

	public AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "ManyToOne cannot be treated as temporal type" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		if ( EqualsHelper.areEqual( originalValue, currentValue ) ) {
			return false;
		}

		Object oldIdentifier = extractFkValue( originalValue, session );
		Object newIdentifier = extractFkValue( currentValue, session );

		return !getEntityDescriptor()
				.getIdentifierDescriptor()
				.getJavaTypeDescriptor()
				.areEqual( oldIdentifier, newIdentifier );
	}

	public Object extractFkValue(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( ! getEntityDescriptor().isInstance( value ) ) {
			throw new HibernateException( "Unexpected value for unresolve [" + value + "], expecting entity instance" );
		}

		if ( referencedUkAttributeName == null || classification.equals( SingularAttributeClassification.ONE_TO_ONE ) ) {
			return getAssociatedEntityDescriptor().getIdentifier( value, session );
		}
		else {
			return getAssociatedEntityDescriptor()
					.findPersistentAttribute( referencedUkAttributeName )
					.getPropertyAccess()
					.getGetter()
					.get( value );
		}
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		List<ColumnReference> columnReferences = new ArrayList<>();
		for ( Column column: foreignKey.getColumnMappings().getReferringColumns() ) {
			columnReferences.add( new ColumnReference( qualifier, column ) );
		}
		return columnReferences;
	}

	@Override
	protected void instantiationComplete(
			PersistentAttributeMapping bootModelAttribute,
			RuntimeModelCreationContext context) {
		super.instantiationComplete( bootModelAttribute, context );

		// todo (6.0) : determine mutability plan based
		// for now its set to immutable

		this.mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
	}

	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	private interface FkValueExtractor {
		Object extractFkValue(Object owner, ExecutionContext executionContext);
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		for ( ColumnMappings.ColumnMapping columnMapping : getForeignKey().getColumnMappings().getColumnMappings() ) {
			final Column column = columnMapping.getReferringColumn();
			action.accept( column.getExpressableType(), column );
		}
	}
}
