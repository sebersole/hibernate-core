/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.sql.Update;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.spi.Type;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Audit strategy which persists and retrieves audit information using a validity algorithm, based on the
 * start-revision and end-revision of a row in the audit tables.
 * <p>This algorithm works as follows:
 * <ul>
 * <li>For a <strong>new row</strong> that is persisted in an audit table, only the <strong>start-revision</strong> column of that row is set</li>
 * <li>At the same time the <strong>end-revision</strong> field of the <strong>previous</strong> audit row is set to this revision</li>
 * <li>Queries are retrieved using 'between start and end revision', instead of a subquery.</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * This has a few important consequences that need to be judged against against each other:
 * <ul>
 * <li>Persisting audit information is a bit slower, because an extra row is updated</li>
 * <li>Retrieving audit information is a lot faster</li>
 * </ul>
 * </p>
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class ValidityAuditStrategy implements AuditStrategy {
	/**
	 * getter for the revision entity field annotated with @RevisionTimestamp
	 */
	private Getter revisionTimestampGetter;

	private final SessionCacheCleaner sessionCacheCleaner;

	public ValidityAuditStrategy() {
		sessionCacheCleaner = new SessionCacheCleaner();
	}

	@Override
	public void perform(
			final Session session,
			final String entityName,
			final AuditService auditService,
			final Serializable id,
			final Object data,
			final Object revision) {
		final String auditedEntityName = auditService.getAuditEntityName( entityName );
		final AuditServiceOptions options = auditService.getOptions();

		// When application reuses identifiers of previously removed entities:
		// The UPDATE statement will no-op if an entity with a given identifier has been
		// inserted for the first time. But in case a deleted primary key value was
		// reused, this guarantees correct strategy behavior: exactly one row with
		// null end date exists for each identifier.
		final boolean reuseIdentifierNames = options.isAllowIdentifierReuseEnabled();

		// Save the audit data
		session.save( auditedEntityName, data );

		// Update the end date of the previous row.
		if ( reuseIdentifierNames || getRevisionType( options, data ) != RevisionType.ADD ) {
			// Register transaction completion process to guarantee execution of UPDATE statement after INSERT.
			( (EventSource) session ).getActionQueue().registerProcess(
					new BeforeTransactionCompletionProcess() {
						@Override
						public void doBeforeTransactionCompletion(final SessionImplementor sessionImplementor) {
							// construct the update context.
							final UpdateContext updateContext = getUpdateContext(
									entityName,
									auditedEntityName,
									sessionImplementor,
									auditService,
									id,
									revision
							);
							// execute the update
							if ( executeUpdate( sessionImplementor, updateContext ) != 1 ) {
								final RevisionType revisionType = getRevisionType( options, data );
								if ( !reuseIdentifierNames || revisionType != RevisionType.ADD ) {
									throw new RuntimeException(
											String.format(
													"Cannot update previous revision for entity %s and id %s",
													auditedEntityName,
													id
											)
									);
								}
							}
						}
					}
			);
		}

		sessionCacheCleaner.scheduleAuditDataRemoval( session, data );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			AuditService auditService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		final AuditServiceOptions options = auditService.getOptions();

		final QueryBuilder qb = new QueryBuilder( persistentCollectionChangeData.getEntityName(), MIDDLE_ENTITY_ALIAS );

		final String originalIdPropName = options.getOriginalIdPropName();
		final Map<String, Object> originalId = (Map<String, Object>) persistentCollectionChangeData.getData().get(
				originalIdPropName
		);
		final String revisionFieldName = options.getRevisionFieldName();
		final String revisionTypePropName = options.getRevisionTypePropName();

		// Adding a parameter for each id component, except the rev number and type.
		originalId.entrySet().forEach( entry -> {
			if ( !revisionFieldName.equals( entry.getKey() ) && !revisionTypePropName.equals( entry.getKey() ) ) {
				qb.getRootParameters().addWhereWithParam(
						originalIdPropName + "." + entry.getKey(),
						true,
						"=",
						entry.getValue()
				);
			}
		} );

		final SessionFactoryImplementor sessionFactory = ( (SessionImplementor) session ).getFactory();
		final Type propertyType = sessionFactory.getMetamodel().entityPersister( entityName ).getPropertyType( propertyName );
		if ( propertyType.isCollectionType() ) {
			CollectionType collectionPropertyType = (CollectionType) propertyType;
			// Handling collection of components.
			if ( collectionPropertyType.getElementType( sessionFactory ) instanceof ComponentType ) {
				// Adding restrictions to compare data outside of primary key.
				persistentCollectionChangeData.getData().entrySet().forEach( entry -> {
					if ( !originalIdPropName.equals( entry.getKey() ) ) {
						qb.getRootParameters().addWhereWithParam(
								entry.getKey(),
								true,
								"=",
								entry.getValue()
						);
					}
				} );
			}
		}

		addEndRevisionNullRestriction( options, qb.getRootParameters() );

		final List<Object> l = qb.toQuery( session ).setLockOptions( LockOptions.UPGRADE ).list();

		// Update the last revision if one exists.
		// HHH-5967: with collections, the same element can be added and removed multiple times. So even if it's an
		// ADD, we may need to update the last revision.
		if ( l.size() > 0 ) {
			updateLastRevision(
					session, options, l, originalId, persistentCollectionChangeData.getEntityName(), revision
			);
		}

		// Save the audit data
		session.save( persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData() );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, persistentCollectionChangeData.getData() );
	}

	@Override
	public void addEntityAtRevisionRestriction(
			AuditServiceOptions options,
			QueryBuilder rootQueryBuilder,
			Parameters parameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData idData,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			String alias2,
			boolean inclusive) {
		addRevisionRestriction( parameters, revisionProperty, revisionEndProperty, addAlias, inclusive );
	}

	@Override
	public void addAssociationAtRevisionRestriction(
			QueryBuilder rootQueryBuilder,
			Parameters parameters, String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData referencingIdData,
			String versionsMiddleEntityName,
			String eeOriginalIdPropertyPath,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			boolean inclusive,
			MiddleComponentData... componentDatas) {
		addRevisionRestriction( parameters, revisionProperty, revisionEndProperty, addAlias, inclusive );
	}

	public void setRevisionTimestampGetter(Getter revisionTimestampGetter) {
		this.revisionTimestampGetter = revisionTimestampGetter;
	}

	private void addEndRevisionNullRestriction(AuditServiceOptions options, Parameters rootParameters) {
		rootParameters.addWhere( options.getRevisionEndFieldName(), true, "is", "null", false );
	}

	private void addRevisionRestriction(
			Parameters rootParameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			boolean inclusive) {
		// e.revision <= _revision and (e.endRevision > _revision or e.endRevision is null)
		Parameters subParm = rootParameters.addSubParameters( "or" );
		rootParameters.addWhereWithNamedParam( revisionProperty, addAlias, inclusive ? "<=" : "<", REVISION_PARAMETER );
		subParm.addWhereWithNamedParam( revisionEndProperty + ".id", addAlias, inclusive ? ">" : ">=", REVISION_PARAMETER );
		subParm.addWhere( revisionEndProperty, addAlias, "is", "null", false );
	}

	@SuppressWarnings({"unchecked"})
	private RevisionType getRevisionType(AuditServiceOptions options, Object data) {
		return (RevisionType) ( (Map<String, Object>) data ).get( options.getRevisionTypePropName() );
	}

	@SuppressWarnings({"unchecked"})
	private void updateLastRevision(
			Session session,
			AuditServiceOptions options,
			List<Object> l,
			Object id,
			String auditedEntityName,
			Object revision) {
		// There should be one entry
		if ( l.size() == 1 ) {
			// Setting the end revision to be the current rev
			Object previousData = l.get( 0 );
			String revisionEndFieldName = options.getRevisionEndFieldName();
			( (Map<String, Object>) previousData ).put( revisionEndFieldName, revision );

			if ( options.isRevisionEndTimestampEnabled() ) {
				// Determine the value of the revision property annotated with @RevisionTimestamp
				String revEndTimestampFieldName = options.getRevisionEndTimestampFieldName();
				Object revEndTimestampObj = this.revisionTimestampGetter.get( revision );
				Date revisionEndTimestamp = convertRevEndTimestampToDate( revEndTimestampObj );

				// Setting the end revision timestamp
				( (Map<String, Object>) previousData ).put( revEndTimestampFieldName, revisionEndTimestamp );
			}

			// Saving the previous version
			session.save( auditedEntityName, previousData );
			sessionCacheCleaner.scheduleAuditDataRemoval( session, previousData );
		}
		else {
			throw new RuntimeException( "Cannot find previous revision for entity " + auditedEntityName + " and id " + id );
		}
	}

	private Date convertRevEndTimestampToDate(Object revEndTimestampObj) {
		// convert to a java.util.Date
		if ( revEndTimestampObj instanceof Date ) {
			return (Date) revEndTimestampObj;
		}
		return new Date( (Long) revEndTimestampObj );
	}

	/**
	 * Executes the {@link UpdateContext} within the bounds of the specified {@link SessionImplementor}.
	 *
	 * @param session The session.
	 * @param updateContext The UpdateContext.
	 * @return the number of rows affected.
	 */
	private int executeUpdate(SessionImplementor session, UpdateContext updateContext) {
		final String updateSql = updateContext.toStatementString();
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final PreparedStatement preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( updateSql );
		return session.doReturningWork(
				new ReturningWork<Integer>() {
					@Override
					public Integer execute(Connection connection) throws SQLException {
						try {
							int index = 1;
							for ( QueryParameterBinding binding : updateContext.getBindings() ) {
								index += binding.bind( index, preparedStatement, session );
							}
							return jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement );
						}
						finally {
							jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
							jdbcCoordinator.afterStatementExecution();
						}
					}
				}
		);
	}

	/**
	 * Creates the update context used to modify the revision end and potentially timestamp values.
	 *
	 * @param entityName The entity name.
	 * @param auditedEntityName The audited entity name.
	 * @param session The session.
	 * @param auditService The AuditService.
	 * @param id The entity identifier.
	 * @param revision The revision entity.
	 * @return The {@link UpdateContext} instance.
	 */
	private UpdateContext getUpdateContext(String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Serializable id,
			Object revision) {

		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();

		final Queryable entityQueryable = getQueryable( entityName, session );
		final Queryable rootEntityQueryable = getQueryable( entityQueryable.getRootEntityName(), session );
		final Queryable auditedEntityQueryable = getQueryable( auditedEntityName, session );
		final Queryable rootAuditedEntityQueryable = getQueryable( auditedEntityQueryable.getRootEntityName(), session );

		final AuditServiceOptions options = auditService.getOptions();
		int index = 1;

		// The expected output from this method is an UPDATE statement that follows:
		// UPDATE audit_ent
		//	SET REVEND = ?
		//	[, REVEND_TSTMP = ?]
		//	WHERE (entity_id) = ?
		//	AND REV <> ?
		//	AND REVEND is null

		// UPDATE audit_ent
		final UpdateContext update = new UpdateContext( sessionFactory );
		update.setTableName( getUpdateTableName( rootEntityQueryable, rootAuditedEntityQueryable, auditedEntityQueryable ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET REVEND = ?
		final Number revisionNumber = auditService.getRevisionInfoNumberReader().getRevisionNumber( revision );
		final Type revisionNumberType = metamodel.entityPersister( options.getRevisionInfoEntityName() ).getIdentifierType();
		update.addColumn( rootAuditedEntityQueryable.toColumns( options.getRevisionEndFieldName() )[0] );
		update.bind( revisionNumber, revisionNumberType );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// [, REVEND_TSTMP = ?]
		if ( options.isRevisionEndTimestampEnabled() ) {
			final Type timestampType = rootAuditedEntityQueryable.getPropertyType( options.getRevisionEndTimestampFieldName() );
			final Date timestampValue = convertRevEndTimestampToDate( revisionTimestampGetter.get( revision ) );
			update.addColumn( rootAuditedEntityQueryable.toColumns( options.getRevisionEndTimestampFieldName() )[0] );
			update.bind( timestampValue, timestampType );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE (entity_id) = ? AND REV <> ? AND REVEND is null
		update.addPrimaryKeyColumns( rootEntityQueryable.getIdentifierColumnNames() );
		update.bind( id, rootEntityQueryable.getIdentifierType() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REV <> ?
		update.addWhereColumn( rootAuditedEntityQueryable.toColumns( options.getRevisionNumberPath() )[0], " <> ?" );
		update.bind( revisionNumber, rootAuditedEntityQueryable.getPropertyType( options.getRevisionNumberPath() ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REVEND is null
		update.addWhereColumn( rootAuditedEntityQueryable.toColumns( options.getRevisionEndFieldName() )[0], " is null" );

		return update;
	}

	private Queryable getQueryable(String entityName, SessionImplementor sessionImplementor) {
		return (Queryable) sessionImplementor.getFactory().getMetamodel().entityPersister( entityName );
	}

	/**
	 * Get the update table name based on the various queryable instances.
	 *
	 * @param rootProductionEntityQueryable The root entity queryable.
	 * @param rootAuditedEntityQueryable The root audited entity queryable.
	 * @param auditedEntityQueryable The audited entity queryable.
	 * @return the update table name.
	 */
	private String getUpdateTableName(Queryable rootProductionEntityQueryable,
			Queryable rootAuditedEntityQueryable,
			Queryable auditedEntityQueryable) {
		if ( UnionSubclassEntityPersister.class.isInstance( rootProductionEntityQueryable ) ) {
			// this is the condition causing all the problems in terms of the generated SQL UPDATE
			// the problem being that we currently try to update the in-line view made up of the union query
			//
			// this is extremely hacky means to get the root table name for the union subclass style entities.
			// hacky because it relies on internal behavior of UnionSubclassEntityPersister
			// !!!!!! NOTICE - using subclass persister, not root !!!!!!
			return auditedEntityQueryable.getSubclassTableName( 0 );
		}
		else {
			return rootAuditedEntityQueryable.getTableName();
		}
	}

	/**
	 * Represents the audit strategy's update.
	 */
	private class UpdateContext extends Update {
		private final List<QueryParameterBinding> bindings = new ArrayList<>();
		private final SessionFactoryImplementor sessionFactory;

		public UpdateContext(SessionFactoryImplementor sessionFactory) {
			super( sessionFactory.getJdbcServices().getDialect() );
			this.sessionFactory = sessionFactory;
		}

		/**
		 * Get the query parameter bindings.
		 *
		 * @return list of query parameter bindings.
		 */
		public List<QueryParameterBinding> getBindings() {
			return bindings;
		}

		/**
		 * Utility method for binding a new query parameter.
		 *
		 * @param value the value to be bound.
		 * @param type the type to be bound.
		 */
		public void bind(Object value, Type type) {
			this.bindings.add( new QueryParameterBinding( value, type ) );
		}
	}

	/**
	 * Represents a query parameter binding.
	 */
	private class QueryParameterBinding {
		private final Type type;
		private final Object value;

		public QueryParameterBinding(Object value, Type type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * Bind the parameter to the provided statement.
		 *
		 * @param index the index to be bound.
		 * @param ps The prepared statement.
		 * @param session The session.
		 * @return the number of column spans based on this binding.
		 * @throws SQLException Thrown if a SQL exception occured.
		 */
		public int bind(int index, PreparedStatement ps, SessionImplementor session) throws SQLException {
			type.nullSafeSet( ps, value, index, session );
			return type.getColumnSpan();
		}
	}
}
