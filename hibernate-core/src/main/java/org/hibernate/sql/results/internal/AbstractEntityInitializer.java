/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.WrongClassException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.EntitySqlSelectionGroup;
import org.hibernate.sql.results.spi.LoadingEntityEntry;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.internal.TypeHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityInitializer implements EntityInitializer {
	private static final Logger log = Logger.getLogger( AbstractEntityInitializer.class );
	private static final boolean debugEnabled = log.isDebugEnabled();


	// NOTE : even though we only keep the EntityDescriptor here, rather than EntityReference
	//		the "scope" of this initializer is a specific EntityReference.
	//
	//		The full EntityReference is simply not needed here, and so we just keep
	//		the EntityDescriptor here to avoid chicken/egg issues in the coe creating
	// 		these

	private final EntityDescriptor<?> entityDescriptor;
	private final EntitySqlSelectionGroup sqlSelectionMappings;
	private final LockMode lockMode;
	private final boolean isShallow;

	// in-flight processing state.  reset after each row
	private Object identifierHydratedState;
	private EntityDescriptor <?> concreteDescriptor;
	private EntityKey entityKey;
	private Object[] hydratedEntityState;
	private Object entityInstance;
	private LoadingEntityEntry loadingEntityEntry;

	@SuppressWarnings("WeakerAccess")
	protected AbstractEntityInitializer(
			EntityDescriptor entityDescriptor,
			EntitySqlSelectionGroup sqlSelectionMappings,
			LockMode lockMode,
			boolean isShallow) {
		this.entityDescriptor = entityDescriptor;
		this.sqlSelectionMappings = sqlSelectionMappings;
		this.lockMode = lockMode;
		this.isShallow = isShallow;
	}

	protected abstract boolean isEntityReturn();

	@Override
	public EntityDescriptor getEntityInitialized() {
		return entityDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public Object getFetchParentInstance() {
		if ( entityInstance == null ) {
			throw new IllegalStateException( "Unexpected state condition - entity instance not yet resolved" );
		}

		return entityInstance;
	}

	@Override
	public void hydrateIdentifier(RowProcessingState rowProcessingState) {
		if ( identifierHydratedState != null ) {
			// its already been read...
			return;
		}

		identifierHydratedState = buildIdentifierHydratedForm( rowProcessingState );
	}

	private Object buildIdentifierHydratedForm(RowProcessingState rowProcessingState) {
		return sqlSelectionMappings.getIdSqlSelections().hydrateStateArray( rowProcessingState );
	}

	@Override
	public void resolveEntityKey(RowProcessingState rowProcessingState) {
		if ( entityKey != null ) {
			// its already been resolved
			return;
		}

		if ( identifierHydratedState == null ) {
			throw new ExecutionException( "Entity identifier state not yet hydrated on call to resolve EntityKey" );
		}

		final SharedSessionContractImplementor persistenceContext = rowProcessingState.getJdbcValuesSourceProcessingState().getPersistenceContext();
		concreteDescriptor = resolveConcreteEntityDescriptor( rowProcessingState, persistenceContext );

		//		1) resolve the value(s) into its identifier representation
		final Object id = concreteDescriptor.getHierarchy()
				.getIdentifierDescriptor()
				.resolveHydratedState( identifierHydratedState, rowProcessingState, persistenceContext.getSession(), null );

		//		2) build and register an EntityKey
		this.entityKey = new EntityKey( id, concreteDescriptor.getEntityDescriptor() );

		//		3) schedule the EntityKey for batch loading, if possible
		if ( shouldBatchFetch() && concreteDescriptor.getEntityDescriptor().isBatchLoadable() ) {
			if ( !persistenceContext.getPersistenceContext().containsEntity( entityKey ) ) {
				persistenceContext.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}
	}

	/**
	 * Should we consider this entity reference batchable?
	 */
	protected boolean shouldBatchFetch() {
		return true;
	}

	// From CollectionType.
	//		todo : expose CollectionType#NOT_NULL_COLLECTION as public
	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );

	@Override
	public void hydrateEntityState(RowProcessingState rowProcessingState) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2

		if ( entityInstance != null ) {
			return;
		}

		if ( entityKey == null ) {
			throw new ExecutionException( "EntityKey not yet resolved on call to hydrated entity state" );
		}

		if ( isShallow ) {
			return;
		}

		final Object rowId;
		if ( concreteDescriptor.getHierarchy().getRowIdDescriptor() != null ) {
			rowId = sqlSelectionMappings.getRowIdSqlSelection().hydrateStateArray( rowProcessingState );

			if ( rowId == null ) {
				throw new HibernateException(
						"Could not read entity row-id from JDBC : " + entityKey
				);
			}
		}
		else {
			rowId = null;
		}

		hydratedEntityState = (Object[]) sqlSelectionMappings.hydrateStateArray( rowProcessingState );

		final SharedSessionContractImplementor persistenceContext = rowProcessingState.getJdbcValuesSourceProcessingState().getPersistenceContext();

		// this isEntityReturn bit is just for entity loaders, not hql/criteria
		if ( isEntityReturn() ) {
			final Object requestedEntityId = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalId();
			if ( requestedEntityId != null && requestedEntityId.equals( entityKey.getIdentifier() ) ) {
				entityInstance = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalObject();
			}
		}

		if ( entityInstance == null ) {
			entityInstance = persistenceContext.instantiate( concreteDescriptor.getEntityName(), entityKey.getIdentifier() );
		}

		loadingEntityEntry = rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity(
				entityKey,
				key -> new LoadingEntityEntry(
						entityKey,
						concreteDescriptor,
						entityInstance,
						rowId,
						hydratedEntityState
				)
		);
	}

	private EntityDescriptor resolveConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) throws WrongClassException {
		final EntityDescriptor descriptor = entityDescriptor;
		if ( descriptor.getHierarchy().getDiscriminatorDescriptor() == null ) {
			return descriptor;
		}

		final Object discriminatorValue = sqlSelectionMappings.getDiscriminatorSqlSelection().hydrateStateArray( rowProcessingState );

		final EntityDescriptor legacyLoadable = descriptor.getEntityDescriptor();
		final String result = legacyLoadable.getHierarchy()
				.getDiscriminatorDescriptor()
				.getDiscriminatorMappings()
				.discriminatorValueToEntityName( discriminatorValue );

		if ( result == null ) {
			//woops we got an instance of another class hierarchy branch
			throw new WrongClassException(
					"Discriminator: " + discriminatorValue,
					(Serializable) identifierHydratedState,
					legacyLoadable.getEntityName()
			);
		}

		return persistenceContext.getFactory().getMetamodel().findEntityDescriptor( result );
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		// reset row state
		identifierHydratedState = null;
		concreteDescriptor = null;
		entityKey = null;
		entityInstance = null;

	}

	private boolean isReadOnly(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) {
		if ( persistenceContext.isDefaultReadOnly() ) {
			return true;
		}


		final Boolean queryOption = rowProcessingState.getJdbcValuesSourceProcessingState().getQueryOptions().isReadOnly();

		return queryOption == null ? false : queryOption;
	}

	@Override
	public void resolveEntityState(RowProcessingState rowProcessingState) {
		final Object entityIdentifier = entityKey.getIdentifier();

		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getPersistenceContext();


		preLoad( rowProcessingState );

		// apply wrapping and conversions

		for ( StateArrayContributor<?> contributor : entityDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();
			final Object value = hydratedEntityState[ position ];

			hydratedEntityState[ position ] = contributor.resolveHydratedState(
					value,
					rowProcessingState,
					session,
					// the container ("owner")... for now just pass null.
					// ultimately we need to account for fetch parent if the
					// current sub-contributor is a fetch
					entityInstance
			);
		}

		entityDescriptor.setIdentifier( entityInstance, entityIdentifier, session );
		entityDescriptor.setPropertyValues( entityInstance, hydratedEntityState );

		final Object version = Versioning.getVersion( hydratedEntityState, entityDescriptor );
		session.getPersistenceContext().addEntity(
				entityKey,
				entityInstance
		);
		final EntityEntry entityEntry = session.getPersistenceContext().addEntry(
				entityInstance,
				Status.LOADING,
				hydratedEntityState,
				loadingEntityEntry.getRowId(),
				entityKey.getIdentifier(),
				version,
				lockMode,
				true,
				entityDescriptor,
				false
		);

		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( cacheAccess != null && session.getCacheMode().isPutEnabled() ) {

			if ( debugEnabled ) {
				log.debugf(
						"Adding entityInstance to second-level cache: %s",
						MessageHelper.infoString( entityDescriptor, entityIdentifier, session.getFactory() )
				);
			}

			final CacheEntry entry = entityDescriptor.buildCacheEntry( entityInstance, hydratedEntityState, version, session );
			final Object cacheKey = cacheAccess.generateCacheKey(
					entityIdentifier,
					entityDescriptor.getHierarchy(),
					factory,
					session.getTenantIdentifier()
			);

			// explicit handling of caching for rows just inserted and then somehow forced to be read
			// from the database *within the same transaction*.  usually this is done by
			// 		1) Session#refresh, or
			// 		2) Session#clear + some form of load
			//
			// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
			if ( session.getPersistenceContext().wasInsertedDuringTransaction( entityDescriptor, entityIdentifier ) ) {
				cacheAccess.update(
						session,
						cacheKey,
						entityDescriptor.getCacheEntryStructure().structure( entry ),
						version,
						version
				);
			}
			else {
				final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
				try {
					eventListenerManager.cachePutStart();
					final boolean put = cacheAccess.putFromLoad(
							session,
							cacheKey,
							entityDescriptor.getCacheEntryStructure().structure( entry ),
							version,
							//useMinimalPuts( session, entityEntry )
							false
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().entityCachePut( entityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
					}
				}
				finally {
					eventListenerManager.cachePutEnd();
				}
			}
		}

		if ( entityDescriptor.getHierarchy().getNaturalIdDescriptor() != null ) {
			session.getPersistenceContext().getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					entityDescriptor,
					entityIdentifier,
					session.getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( hydratedEntityState, entityDescriptor )
			);
		}

		boolean isReallyReadOnly = isReadOnly( rowProcessingState, session );
		if ( !entityDescriptor.getHierarchy().getMutabilityPlan().isMutable() ) {
			isReallyReadOnly = true;
		}
		else {
			final Object proxy = session.getPersistenceContext().getProxy( loadingEntityEntry.getEntityKey() );
			if ( proxy != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReallyReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
			}
		}
		if ( isReallyReadOnly ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			session.getPersistenceContext().setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			TypeHelper.deepCopy(
					entityDescriptor,
					hydratedEntityState,
					hydratedEntityState,
					StateArrayContributor::isUpdatable
			);
			session.getPersistenceContext().setEntryStatus( entityEntry, Status.MANAGED );
		}

		entityDescriptor.afterInitialize( entityInstance, session );

		if ( debugEnabled ) {
			log.debugf(
					"Done materializing entityInstance %s",
					MessageHelper.infoString( entityDescriptor, entityIdentifier, session.getFactory() )
			);
		}

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatistics().loadEntity( entityDescriptor.getEntityName() );
		}


		postLoad( rowProcessingState );
	}

	private void preLoad(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getPersistenceContext();

		final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
		preLoadEvent.reset();

		// Must occur after resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entityInstance )
					.setState( hydratedEntityState )
					.setId( entityKey.getIdentifier() )
					.setDescriptor( entityDescriptor );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}
	}

	private void postLoad(RowProcessingState rowProcessingState) {
		final PostLoadEvent postLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPostLoadEvent();
		postLoadEvent.reset();

		postLoadEvent.setEntity( loadingEntityEntry.getEntityInstance() )
				.setId( loadingEntityEntry.getEntityKey().getIdentifier() )
				.setDescriptor( loadingEntityEntry.getDescriptor() );

		final EventListenerGroup<PostLoadEventListener> listenerGroup = entityDescriptor.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.POST_LOAD );
		for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
			listener.onPostLoad( postLoadEvent );
		}
	}
}
