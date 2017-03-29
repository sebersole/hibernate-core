/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base for types which map associations to persistent entities.
 *
 * @author Gavin King
 */
public abstract class AbstractEntityType extends AbstractType implements EntityType {

	private final TypeConfiguration typeConfiguration;
	private final String associatedEntityName;
	protected final String uniqueKeyPropertyName;
	private final boolean eager;
	private final boolean unwrapProxy;
	private final boolean referenceToPrimaryKey;
	private final Comparator comparator;
	/**
	 * Cached because of performance
	 *
	 * @see #getIdentifierType
	 * @see #getIdentifierType
	 */
	private transient volatile Type associatedIdentifierType;

	/**
	 * Cached because of performance
	 *
	 * @see #getAssociatedEntityPersister
	 */
	private transient volatile EntityPersister associatedEntityPersister;

	private transient Class returnedClass;

	/**
	 * Constructs the requested entity type mapping.
	 *
	 * @param typeConfiguration The type typeConfiguration
	 * @param entityName The name of the associated entity.
	 * @param referenceToPrimaryKey True if association references a primary key.
	 * @param uniqueKeyPropertyName The property-ref name, or null if we
	 * reference the PK of the associated entity.
	 * @param eager Is eager fetching enabled.
	 * @param unwrapProxy Is unwrapping of proxies allowed for this association; unwrapping
	 * says to return the "implementation target" of lazy prooxies; typically only possible
	 * with lazy="no-proxy".
	 */
	protected AbstractEntityType(
			TypeConfiguration typeConfiguration,
			String entityName,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean eager,
			boolean unwrapProxy) {
		this.typeConfiguration = typeConfiguration;
		this.associatedEntityName = entityName;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
		this.eager = eager;
		this.unwrapProxy = unwrapProxy;
		this.referenceToPrimaryKey = referenceToPrimaryKey;
		this.comparator = new EntityComparator();
	}

	protected TypeConfiguration typeConfiguration() {
		return typeConfiguration;
	}

	/**
	 * An entity type is a type of association type
	 *
	 * @return True.
	 */
	@Override
	public boolean isAssociationType() {
		return true;
	}

	/**
	 * Explicitly, an entity type is an entity type ;)
	 *
	 * @return True.
	 */
	@Override
	public final boolean isEntityType() {
		return true;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	/**
	 * Generates a string representation of this type.
	 *
	 * @return string rep
	 */
	@Override
	public String toString() {
		return getClass().getName() + '(' + getAssociatedEntityName() + ')';
	}

	/**
	 * For entity types, the name correlates to the associated entity name.
	 */
	@Override
	public String getName() {
		return associatedEntityName;
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	@Override
	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

//	@Override
//	public String getRHSUniqueKeyPropertyName() {
//		// Return null if this type references a PK.  This is important for
//		// associations' use of mappedBy referring to a derived ID.
//		return referenceToPrimaryKey ? null : uniqueKeyPropertyName;
//	}

	@Override
	public String getLHSPropertyName() {
		return null;
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@Override
	public final String getAssociatedEntityName() {
		return associatedEntityName;
	}

	/**
	 * The name of the associated entity.
	 *
	 * @param factory The session factory, for resolution.
	 *
	 * @return The associated entity name.
	 */
	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory) {
		return getAssociatedEntityName();
	}

	/**
	 * Retrieves the {@link Joinable} defining the associated entity.
	 *
	 * @param factory The session factory.
	 *
	 * @return The associated joinable
	 *
	 * @throws MappingException Generally indicates an invalid entity name.
	 */
	@Override
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException {
		return (Joinable) getAssociatedEntityPersister( factory );
	}

	/**
	 * This returns the wrong class for an entity with a proxy, or for a named
	 * entity.  Theoretically it should return the proxy class, but it doesn't.
	 * <p/>
	 * The problem here is that we do not necessarily have a ref to the associated
	 * entity persister (nor to the session factory, to look it up) which is really
	 * needed to "do the right thing" here...
	 *
	 * @return The entiyt class.
	 */
	@Override
	public final Class getReturnedClass() {
		if ( returnedClass == null ) {
			returnedClass = determineAssociatedEntityClass();
		}
		return returnedClass;
	}

	private Class determineAssociatedEntityClass() {
		final String entityName = getAssociatedEntityName();
		try {
			return ReflectHelper.classForName( entityName );
		}
		catch (ClassNotFoundException cnfe) {
			return typeConfiguration.getSessionFactory().getMetamodel().entityPersister( entityName ).
					getEntityTuplizer().getMappedClass();
		}
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	/**
	 * Two entities are considered the same when their instances are the same.
	 *
	 * @param x One entity instance
	 * @param y Another entity instance
	 *
	 * @return True if x == y; false otherwise.
	 */
	@Override
	public final boolean isSame(Object x, Object y) {
		return x == y;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value; //special case ... this is the leaf of the containment graph, even though not immutable
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		if ( original == null ) {
			return null;
		}
		Object cached = copyCache.get( original );
		if ( cached == null ) {
			// Avoid creation of invalid managed -> managed mapping in copyCache when traversing
			// cascade loop (@OneToMany(cascade=ALL) with associated @ManyToOne(cascade=ALL)) in entity graph
			if ( copyCache.containsValue( original ) ) {
				cached = original;
			}
		}
		if ( cached != null ) {
			return cached;
		}
		else {
			if ( original == target ) {
				return target;
			}
			if ( session.getContextEntityIdentifier( original ) == null &&
					ForeignKeys.isTransient( associatedEntityName, original, Boolean.FALSE, session ) ) {
				final Object copy = session.getEntityPersister( associatedEntityName, original )
						.instantiate( null, session );
				copyCache.put( original, copy );
				return copy;
			}
			else {
				Object id = getIdentifier( original, session );
				if ( id == null ) {
					throw new AssertionFailure(
							"non-transient entity has a null id: " + original.getClass()
									.getName()
					);
				}
				id = getIdentifierOrUniqueKeyType()
						.replace( id, null, session, owner, copyCache );
				return resolve( id, session, owner );
			}
		}
	}

	@Override
	public int getHashCode(Object value) {
		EntityPersister persister = getAssociatedEntityPersister( typeConfiguration.getSessionFactory() );
		if ( !persister.canExtractIdOutOfEntity() ) {
			return super.getHashCode( value );
		}

		final Serializable id;
		if ( value instanceof HibernateProxy ) {
			id = ( (HibernateProxy) value ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			final Class mappedClass = persister.getMappedClass();
			if ( mappedClass.isAssignableFrom( value.getClass() ) ) {
				id = persister.getIdentifier( value );
			}
			else {
				id = (Serializable) value;
			}
		}
		return persister.getIdentifierType().getHashCode( id );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		// associations (many-to-one and one-to-one) can be null...
		if ( x == null || y == null ) {
			return x == y;
		}

		EntityPersister persister = getAssociatedEntityPersister( factory );
		if ( !persister.canExtractIdOutOfEntity() ) {
			return super.isEqual( x, y );
		}

		final Class mappedClass = persister.getMappedClass();
		Serializable xid;
		if ( x instanceof HibernateProxy ) {
			xid = ( (HibernateProxy) x ).getHibernateLazyInitializer()
					.getIdentifier();
		}
		else {
			if ( mappedClass.isAssignableFrom( x.getClass() ) ) {
				xid = persister.getIdentifier( x );
			}
			else {
				//JPA 2 case where @IdClass contains the id and not the associated entity
				xid = (Serializable) x;
			}
		}

		Serializable yid;
		if ( y instanceof HibernateProxy ) {
			yid = ( (HibernateProxy) y ).getHibernateLazyInitializer()
					.getIdentifier();
		}
		else {
			if ( mappedClass.isAssignableFrom( y.getClass() ) ) {
				yid = persister.getIdentifier( y );
			}
			else {
				//JPA 2 case where @IdClass contains the id and not the associated entity
				yid = (Serializable) y;
			}
		}

		return persister.getIdentifierType()
				.isEqual( xid, yid, factory );
	}

	@Override
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) {
		return getOnCondition( alias, factory, enabledFilters, null );
	}

	@Override
	public String getOnCondition(
			String alias,
			SessionFactoryImplementor factory,
			Map enabledFilters,
			Set<String> treatAsDeclarations) {
		if ( isReferenceToPrimaryKey() && ( treatAsDeclarations == null || treatAsDeclarations.isEmpty() ) ) {
			return "";
		}
		else {
			return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters, treatAsDeclarations );
		}
	}

	/**
	 * Resolve an identifier or unique key value
	 */
	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		if ( value != null && !isNull( owner, session ) ) {
			if ( isReferenceToPrimaryKey() ) {
				return resolveIdentifier( (Serializable) value, session );
			}
			else if ( uniqueKeyPropertyName != null ) {
				return loadByUniqueKey( getAssociatedEntityName(), uniqueKeyPropertyName, value, session );
			}
		}

		return null;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return getAssociatedEntityPersister( factory ).getIdentifierType();
	}

	protected EntityPersister getAssociatedEntityPersister(final SessionFactoryImplementor factory) {
		final EntityPersister persister = associatedEntityPersister;
		//The following branch implements a simple lazy-initialization, but rather than the canonical
		//form it returns the local variable to avoid a second volatile read: associatedEntityPersister
		//needs to be volatile as the initialization might happen by a different thread than the readers.
		if ( persister == null ) {
			associatedEntityPersister = factory.getMetamodel().entityPersister( getAssociatedEntityName() );
			return associatedEntityPersister;
		}
		else {
			return persister;
		}
	}

	protected final Object getIdentifier(Object value, SharedSessionContractImplementor session) throws HibernateException {
		if ( isReferenceToPrimaryKey() || uniqueKeyPropertyName == null ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved(
					getAssociatedEntityName(),
					value,
					session
			); //tolerates nulls
		}
		else if ( value == null ) {
			return null;
		}
		else {
			EntityPersister entityPersister = getAssociatedEntityPersister( session.getFactory() );
			Object propertyValue = entityPersister.getPropertyValue( value, uniqueKeyPropertyName );
			// We now have the value of the property-ref we reference.  However,
			// we need to dig a little deeper, as that property might also be
			// an entity type, in which case we need to resolve its identitifier
			Type type = entityPersister.getPropertyType( uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				propertyValue = ( (AbstractEntityType) type ).getIdentifier( propertyValue, session );
			}

			return propertyValue;
		}
	}

	/**
	 * Generate a loggable representation of an instance of the value mapped by this type.
	 *
	 * @param value The instance to be logged.
	 * @param factory The session factory.
	 *
	 * @return The loggable string.
	 *
	 * @throws HibernateException Generally some form of resolution problem.
	 */
	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}

		final EntityPersister persister = getAssociatedEntityPersister( factory );
		if ( !persister.getEntityTuplizer().isInstance( value ) ) {
			// it should be the id type...
			if ( persister.getIdentifierType().getReturnedClass().isInstance( value ) ) {
				return associatedEntityName + "#" + value;
			}
		}

		final StringBuilder result = new StringBuilder().append( associatedEntityName );

		if ( persister.hasIdentifierProperty() ) {
			final Serializable id;
			if ( value instanceof HibernateProxy ) {
				HibernateProxy proxy = (HibernateProxy) value;
				id = proxy.getHibernateLazyInitializer().getIdentifier();
			}
			else {
				id = persister.getIdentifier( value );
			}

			result.append( '#' )
					.append( persister.getIdentifierType().toLoggableString( id, factory ) );
		}

		return result.toString();
	}

	@Override
	public boolean isLogicalOneToOne() {
		return isOneToOne();
	}

	/**
	 * Convenience method to locate the identifier type of the associated entity.
	 *
	 * @param factory The mappings...
	 *
	 * @return The identifier type
	 */
	Type getIdentifierType(final Mapping factory) {
		final Type type = associatedIdentifierType;
		//The following branch implements a simple lazy-initialization, but rather than the canonical
		//form it returns the local variable to avoid a second volatile read: associatedIdentifierType
		//needs to be volatile as the initialization might happen by a different thread than the readers.
		if ( type == null ) {
			associatedIdentifierType = factory.getIdentifierType( getAssociatedEntityName() );
			return associatedIdentifierType;
		}
		else {
			return type;
		}
	}

	/**
	 * Convenience method to locate the identifier type of the associated entity.
	 *
	 * @param session The originating session
	 *
	 * @return The identifier type
	 */
	Type getIdentifierType(final SharedSessionContractImplementor session) {
		final Type type = associatedIdentifierType;
		if ( type == null ) {
			associatedIdentifierType = getIdentifierType( session.getFactory() );
			return associatedIdentifierType;
		}
		else {
			return type;
		}
	}

	@Override
	public final Type getIdentifierOrUniqueKeyType() throws MappingException {
		final SessionFactoryImplementor factory = typeConfiguration.getSessionFactory();
		if ( isReferenceToPrimaryKey() || uniqueKeyPropertyName == null ) {
			return getIdentifierType( factory );
		}
		else {
			Type type = factory.getReferencedPropertyType( getAssociatedEntityName(), uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				type = ( (AbstractEntityType) type ).getIdentifierOrUniqueKeyType();
			}
			return type;
		}
	}

	@Override
	public final String getIdentifierOrUniqueKeyPropertyName()
			throws MappingException {
		final SessionFactoryImplementor factory = typeConfiguration.getSessionFactory();

		if ( isReferenceToPrimaryKey() || uniqueKeyPropertyName == null ) {
			return factory.getIdentifierPropertyName( getAssociatedEntityName() );
		}
		else {
			return uniqueKeyPropertyName;
		}
	}

	protected abstract boolean isNullable();

	/**
	 * Resolve an identifier via a load.
	 *
	 * @param id The entity id to resolve
	 * @param session The orginating session.
	 *
	 * @return The resolved identifier (i.e., loaded entity).
	 *
	 * @throws org.hibernate.HibernateException Indicates problems performing the load.
	 */
	protected final Object resolveIdentifier(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		boolean isProxyUnwrapEnabled = unwrapProxy &&
				getAssociatedEntityPersister( session.getFactory() )
						.isInstrumented();

		Object proxyOrEntity = session.internalLoad(
				getAssociatedEntityName(),
				id,
				eager,
				isNullable() && !isProxyUnwrapEnabled
		);

		if ( proxyOrEntity instanceof HibernateProxy ) {
			( (HibernateProxy) proxyOrEntity ).getHibernateLazyInitializer()
					.setUnwrap( isProxyUnwrapEnabled );
		}

		return proxyOrEntity;
	}

	protected boolean isNull(Object owner, SharedSessionContractImplementor session) {
		return false;
	}

	/**
	 * Load an instance by a unique key that is not the primary key.
	 *
	 * @param entityName The name of the entity to load
	 * @param uniqueKeyPropertyName The name of the property defining the uniqie key.
	 * @param key The unique key property value.
	 * @param session The originating session.
	 *
	 * @return The loaded entity
	 *
	 * @throws HibernateException generally indicates problems performing the load.
	 */
	public Object loadByUniqueKey(
			String entityName,
			String uniqueKeyPropertyName,
			Object key,
			SharedSessionContractImplementor session) throws HibernateException {
		final SessionFactoryImplementor factory = session.getFactory();
		UniqueKeyLoadable persister = (UniqueKeyLoadable) factory.getMetamodel().entityPersister( entityName );

		//TODO: implement caching?! proxies?!

		EntityUniqueKey euk = new EntityUniqueKey(
				entityName,
				uniqueKeyPropertyName,
				key,
				getIdentifierOrUniqueKeyType(),
				persister.getEntityMode(),
				session.getFactory()
		);

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		Object result = persistenceContext.getEntity( euk );
		if ( result == null ) {
			result = persister.loadByUniqueKey( uniqueKeyPropertyName, key, session );
		}
		return result == null ? null : persistenceContext.proxyFor( result );
	}

	@Override
	public String getEntityName() {
		return associatedEntityName;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.of( this );
	}

	@Override
	public String asLoggableText() {
		return "Entity(" + getEntityName() + ")";
	}

	public static class EntityComparator implements Comparator<Object> {
		@Override
		public int compare(Object x, Object y) {
			return 0; //TODO: entities CAN be compared, by PK, fix this! -> only if/when we can extract the id values....
		}
	}

}
