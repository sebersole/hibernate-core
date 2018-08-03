/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.persistence.EntityGraph;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.metamodel.model.creation.spi.InFlightRuntimeModel;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.spi.AbstractRuntimeModel;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.ast.produce.sqm.internal.PolymorphicEntityValuedExpressableTypeImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Standard implementation of Hibernate's extension to the JPA
 * {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl extends AbstractRuntimeModel implements MetamodelImplementor, Serializable {
	private static final Logger log = Logger.getLogger( MetamodelImpl.class );

	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();
	public static final String INVALID_IMPORT = "<invalid>";

	private final SessionFactoryImplementor sessionFactory;
	private final TypeConfiguration typeConfiguration;

	// unmodifiable
	private final Map<JavaTypeDescriptor,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();
	private final Map<EmbeddableJavaDescriptor<?>,Set<String>> embeddedRolesByEmbeddableType;
	private final Map<String,Set<PersistentCollectionDescriptor<?,?,?>>> collectionDescriptorsByEntityParticipant = new ConcurrentHashMap<>();
	private final Map<ManagedJavaDescriptor<?>, MappedSuperclassDescriptor<?>> jpaMappedSuperclassTypeMap = new ConcurrentHashMap<>();

	private final ConcurrentMap<EntityNameResolver,Object> entityNameResolvers = new ConcurrentHashMap<>();

	// modifiable
	private final Map<JavaTypeDescriptor, PolymorphicEntityValuedExpressableType<?>> polymorphicEntityReferenceMap = new HashMap<>();

	public MetamodelImpl(
			SessionFactoryImplementor sessionFactory,
			TypeConfiguration typeConfiguration,
			InFlightRuntimeModel inFlightModel) {
		super( inFlightModel );

		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;


		if ( getEmbeddedDescriptorMap().isEmpty() ) {
			this.embeddedRolesByEmbeddableType = Collections.emptyMap();
		}
		else {
			final Map<EmbeddableJavaDescriptor<?>, Set<String>> embeddedRolesByEmbeddableType = new ConcurrentHashMap<>();
			getEmbeddedDescriptorMap().forEach(
					(role, embeddedTypeDescriptor) -> embeddedRolesByEmbeddableType.computeIfAbsent(
							embeddedTypeDescriptor.getJavaTypeDescriptor(),
							k -> new HashSet<>()
					).add( embeddedTypeDescriptor.getNavigableRole().getFullPath() )
			);
			this.embeddedRolesByEmbeddableType = Collections.unmodifiableMap( embeddedRolesByEmbeddableType );
		}

		final Map<JavaTypeDescriptor,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();

		visitEntityDescriptors(
				entityDescriptor -> {
					final Class entityClass = entityDescriptor.getJavaTypeDescriptor().getClass();
					final Class proxyClass = entityDescriptor.getConcreteProxyClass();
					if ( proxyClass != null
							&& proxyClass.isInterface()
							&& !Map.class.isAssignableFrom( proxyClass ) ) {


						if ( entityClass.equals( proxyClass ) ) {
							// this part handles an odd case in the Hibernate test suite where we map an interface
							// as the class and the proxy.  I cannot think of a real life use case for that
							// specific test, but..
							log.debugf( "Entity [%s] mapped same interface [%s] as class and proxy", entityDescriptor.getEntityName(), entityClass );
						}
						else {
							final JavaTypeDescriptor proxyTypeDescriptor = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor(
									proxyClass,
									// todo (6.0) : what to use by default here?  BasicJavaDescriptor?
									(s, registry) ->  null
							);
							final String old = entityProxyInterfaceMap.put( proxyTypeDescriptor, entityDescriptor.getEntityName() );
							if ( old != null ) {
								throw new HibernateException(
										String.format(
												Locale.ENGLISH,
												"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
												old,
												entityDescriptor.getEntityName(),
												proxyClass.getName()
										)
								);
							}
						}
					}
				}
		);

		visitCollectionDescriptors(
				collectionDescriptor -> {
					if ( collectionDescriptor.getElementDescriptor() instanceof CollectionElementEntity ) {
						final EntityDescriptor elementEntityDescriptor = ( (CollectionElementEntity) collectionDescriptor.getElementDescriptor() )
								.getEntityDescriptor();
						collectionDescriptorsByEntityParticipant.computeIfAbsent(
								elementEntityDescriptor.getNavigableRole().getFullPath(),
								s -> new HashSet<>()
						).add( collectionDescriptor );
					}

					if ( collectionDescriptor.getIndexDescriptor() != null && collectionDescriptor.getIndexDescriptor() instanceof CollectionIndexEntity ) {
						final EntityDescriptor elementEntityDescriptor = ( (CollectionIndexEntity) collectionDescriptor.getIndexDescriptor() )
								.getEntityDescriptor();
						collectionDescriptorsByEntityParticipant.computeIfAbsent(
								elementEntityDescriptor.getNavigableRole().getFullPath(),
								s -> new HashSet<>()
						).add( collectionDescriptor );
					}
				}
		);
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityDescriptor descriptor = getEntityDescriptor( cls );
		if ( descriptor == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return descriptor;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> ManagedType<X> managedType(Class<X> cls) {
		final EntityDescriptor entityDescriptor = getEntityDescriptor( cls );
		if ( entityDescriptor != null ) {
			return entityDescriptor;
		}

		final EmbeddedTypeDescriptor embeddedDescriptor = findEmbeddedDescriptor( cls );
		if ( embeddedDescriptor != null ) {
			return embeddedDescriptor;
		}

		final MappedSuperclassDescriptor msDescriptor = findMappedSuperclassDescriptor( cls );
		if ( msDescriptor != null ) {
			return msDescriptor;
		}

		throw new IllegalArgumentException( "Not a managed type: " + cls );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddedTypeDescriptor embeddedDescriptor = findEmbeddedDescriptor( cls );
		if ( embeddedDescriptor != null ) {
			return embeddedDescriptor;
		}

		throw new IllegalArgumentException( "Not an embeddable: " + cls );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<ManagedType<?>> getManagedTypes() {
		final Set<ManagedType<?>> managedTypes = new HashSet<>();
		managedTypes.addAll( getEntityDescriptorMap().values() );
		managedTypes.addAll( getEntityDescriptorMap().values() );
		managedTypes.addAll( getMappedSuperclassDescriptorMap().values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( getEntityDescriptorMap().values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( getEmbeddedDescriptorMap().values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> EntityType<X> entity(String entityName) {
		final EntityDescriptor<X> descriptor = findEntityDescriptor( entityName );
		if ( descriptor == null ) {
			throw new IllegalArgumentException( "Not an entity : " + entityName );
		}
		return descriptor;
	}

	@Override
	public Set<PersistentCollectionDescriptor<?, ?, ?>> findCollectionsByEntityParticipant(EntityDescriptor entityDescriptor) {
		final Set<PersistentCollectionDescriptor<?, ?, ?>> descriptorSet = collectionDescriptorsByEntityParticipant.get( entityDescriptor.getNavigableRole().getFullPath() );
		return descriptorSet == null ? Collections.emptySet() : descriptorSet;
	}

	@Override
	public Set<String> findCollectionRolesByEntityParticipant(EntityDescriptor entityDescriptor) {
		final Set<String> result = new HashSet<>();
		findCollectionsByEntityParticipant( entityDescriptor ).forEach( d -> result.add( d.getNavigableRole().getFullPath() ) );
		return result;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM Query handling
	//		- everything within this "block" of methods relates to SQM
	// 			interpretation of queries and implements its calls accordingly

	@Override
	@SuppressWarnings("unchecked")
	public EntityValuedExpressableType resolveEntityReference(String name) {
		final String rename = getImportedName( name );
		if ( rename != null ) {
			name = rename;
		}

		{
			final EntityDescriptor descriptor = findEntityDescriptor( name );
			if ( descriptor != null ) {
				return descriptor;
			}
		}

		{
			final MappedSuperclassDescriptor descriptor = findMappedSuperclassDescriptor( name );
			if ( descriptor != null ) {
				// todo (6.0) : a better option is to have MappedSuperclassDescriptor extend EntityValuedExpressableType
				//		but that currently causes some conflicts regarding `#getJavaTypeDescriptor`
				throw new NotYetImplementedFor6Exception();
			}
		}

		final Class requestedClass = resolveRequestedClass( name );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Per JPA spec : no entity named " + name );
	}

	private Class resolveRequestedClass(String entityName) {
		try {
			return sessionFactory.getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@Override
	public String getImportedName(String name) {
		final String importedName = getNameImportMap().get( name );

		if ( INVALID_IMPORT.equals( importedName ) ) {
			return null;
		}

		if ( importedName == null ) {
			// todo (6.0) : this is what is done in 5.3 code as well, but it seems wrong
			//		how are entity-names handled?
			//
			// seems like the super call is the best already
			try {
				sessionFactory.getServiceRegistry().getService( ClassLoaderService.class ).classForName( name );
				getNameImportMap().put( name, name );
				return name;
			}
			catch (ClassLoadingException cnfe) {
				getNameImportMap().put( name, INVALID_IMPORT );
				return null;
			}
		}

		return importedName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EntityValuedExpressableType<T> resolveEntityReference(Class<T> javaType) {
		// see if we know of this Class by name as an EntityDescriptor key
		if ( getEntityDescriptorMap().containsKey( javaType.getName() ) ) {
			// and if so, return that descriptor
			return (EntityValuedExpressableType<T>) getEntityDescriptorMap().get( javaType.getName() );
		}

		final JavaTypeDescriptor<T> jtd = typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( javaType );
		if ( jtd == null ) {
			throw new HibernateException( "Could not locate JavaTypeDescriptor : " + javaType.getName() );
		}

		// next check entityProxyInterfaceMap
		final String proxyEntityName = entityProxyInterfaceMap.get( jtd );
		if ( proxyEntityName != null ) {
			return (EntityValuedExpressableType<T>) getEntityDescriptorMap().get( proxyEntityName );
		}

		// otherwise, trye to handle it as a polymorphic reference
		if ( polymorphicEntityReferenceMap.containsKey( jtd ) ) {
			return (EntityValuedExpressableType<T>) polymorphicEntityReferenceMap.get( jtd );
		}

		final Set<EntityDescriptor<?>> implementors = getImplementors( javaType );
		if ( !implementors.isEmpty() ) {
			final PolymorphicEntityValuedExpressableTypeImpl entityReference = new PolymorphicEntityValuedExpressableTypeImpl(
					jtd,
					implementors
			);
			polymorphicEntityReferenceMap.put( jtd, entityReference );
			return entityReference;
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}

	@SuppressWarnings("unchecked")
	private Set<EntityDescriptor<?>> getImplementors(Class javaType) {
		// if the javaType refers directly to an EntityDescriptor by Class name, return just it.
		final EntityDescriptor<?> exactMatch = getEntityDescriptorMap().get( javaType.getName() );
		if ( exactMatch != null ) {
			return Collections.singleton( exactMatch );
		}

		final HashSet<EntityDescriptor<?>> matchingDescriptors = new HashSet<>();

		for ( EntityDescriptor entityDescriptor : getEntityDescriptorMap().values() ) {
			if ( entityDescriptor.getJavaType() == null ) {
				continue;
			}

			// todo : explicit/implicit polymorphism...
			// todo : handle "duplicates" within a hierarchy
			// todo : in fact we may want to cycle through descriptors via entityHierarchies and walking the subclass graph rather than walking each descriptor linearly (in random order)

			if ( javaType.isAssignableFrom( entityDescriptor.getJavaType() ) ) {
				matchingDescriptors.add( entityDescriptor );
			}
		}

		return matchingDescriptors;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		if ( entityGraph instanceof EntityGraphImplementor ) {
			entityGraph = ( (EntityGraphImplementor<T>) entityGraph ).makeImmutableCopy( graphName );
		}
		final EntityGraphImplementor old = getEntityGraphMap().put( graphName, (EntityGraphImplementor<?>) entityGraph );
		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	@Override
	public void close() {
		// anything to do ?
	}

	@Override
	public AllowableParameterType resolveAllowableParamterType(Class clazz) {
		BasicType basicType = typeConfiguration.getBasicTypeRegistry().getBasicType( clazz.getName() );
		if ( basicType != null ) {
			return basicType;
		}
		EntityDescriptor entityDescriptor = findEntityDescriptor( clazz );
		if ( entityDescriptor != null ) {
			return entityDescriptor.getIdentifierDescriptor();
		}
		return findEmbeddedDescriptor( clazz );
	}
}
