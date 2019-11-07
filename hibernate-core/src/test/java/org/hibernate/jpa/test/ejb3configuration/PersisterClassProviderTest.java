/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.SettingsGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersisterClassProviderTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testPersisterClassProvider() {
		Map settings = SettingsGenerator.generateSettings(
				PersisterClassResolverInitiator.IMPL_NAME, GoofyPersisterClassProvider.class,
				AvailableSettings.LOADED_CLASSES, Arrays.asList( Bell.class )
		);
		try {
			EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
					new PersistenceUnitDescriptorAdapter(),
					settings
			).build();
			entityManagerFactory.close();
		}
		catch ( PersistenceException e ) {
            Assert.assertNotNull( e.getCause() );
			Assert.assertNotNull( e.getCause().getCause() );
			Assert.assertEquals( GoofyException.class, e.getCause().getCause().getClass() );

		}
	}

	public static class GoofyPersisterClassProvider implements PersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
			return GoofyProvider.class;
		}

		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
			return null;
		}
	}

	public static class GoofyProvider implements EntityPersister {

		@SuppressWarnings( {"UnusedParameters"})
		public GoofyProvider(
				org.hibernate.mapping.PersistentClass persistentClass,
				EntityDataAccess entityDataAccessstrategy,
				NaturalIdDataAccess naturalIdRegionAccessStrategy,
				PersisterCreationContext creationContext) {
			throw new GoofyException();
		}

		@Override
		public EntityMode getEntityMode() {
			return null;
		}

		@Override
		public EntityTuplizer getEntityTuplizer() {
			return null;
		}

		@Override
		public BytecodeEnhancementMetadata getInstrumentationMetadata() {
			return new BytecodeEnhancementMetadataNonPojoImpl( getEntityName() );
		}

		@Override
		public void generateEntityDefinition() {
		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public NavigableRole getNavigableRole() {
			return null;
		}

		@Override
		public EntityEntryFactory getEntityEntryFactory() {
			return MutableEntityEntryFactory.INSTANCE;
		}

		@Override
		public String getRootEntityName() {
			return null;
		}

		@Override
		public String getEntityName() {
			return null;
		}

		@Override
		public EntityMetamodel getEntityMetamodel() {
			return null;
		}

		@Override
		public boolean isSubclassEntityName(String entityName) {
			return false;
		}

		@Override
		public Serializable[] getPropertySpaces() {
			return new Serializable[0];
		}

		@Override
		public Serializable[] getQuerySpaces() {
			return new Serializable[0];
		}

		@Override
		public boolean hasProxy() {
			return false;
		}

		@Override
		public boolean hasCollections() {
			return false;
		}

		@Override
		public boolean hasMutableProperties() {
			return false;
		}

		@Override
		public boolean hasSubselectLoadableCollections() {
			return false;
		}

		@Override
		public boolean hasCascades() {
			return false;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public boolean isInherited() {
			return false;
		}

		@Override
		public boolean isIdentifierAssignedByInsert() {
			return false;
		}

		@Override
		public Type getPropertyType(String propertyName) throws MappingException {
			return null;
		}

		@Override
		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session) {
			return new int[0];
		}

		@Override
		public int[] findModified(Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
			return new int[0];
		}

		@Override
		public boolean hasIdentifierProperty() {
			return false;
		}

		@Override
		public boolean canExtractIdOutOfEntity() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		public Comparator getVersionComparator() {
			return null;
		}

		@Override
		public VersionType getVersionType() {
			return null;
		}

		@Override
		public int getVersionProperty() {
			return 0;
		}

		@Override
		public boolean hasNaturalIdentifier() {
			return false;
		}
		
        @Override
		public int[] getNaturalIdentifierProperties() {
			return new int[0];
		}

		@Override
		public Object[] getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
				SharedSessionContractImplementor session) {
			return null;
		}
		
		@Override
        public boolean hasNaturalIdCache() {
            return false;
        }

        @Override
        public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
            return null;
        }

        @Override
		public IdentifierGenerator getIdentifierGenerator() {
			return null;
		}

		@Override
		public boolean hasLazyProperties() {
			return false;
		}

		@Override
		public Object load(Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public List multiLoad(Object[] ids, SharedSessionContractImplementor session, MultiLoadOptions loadOptions) {
			return Collections.emptyList();
		}

		@Override
		public void lock(Object id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session) {
		}

		@Override
		public void lock(Object id, Object version, Object object, LockOptions lockOptions, SharedSessionContractImplementor session) {
		}

		@Override
		public void insert(Object id, Object[] fields, Object object, SharedSessionContractImplementor session) {
		}

		@Override
		public Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void delete(Object id, Object version, Object object, SharedSessionContractImplementor session) {
		}

		@Override
		public void update(Object id, Object[] fields, int[] dirtyFields, boolean hasDirtyCollection, Object[] oldFields, Object oldVersion, Object object, Object rowId, SharedSessionContractImplementor session) {
		}

		@Override
		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		@Override
		public String[] getPropertyNames() {
			return new String[0];
		}

		@Override
		public boolean[] getPropertyInsertability() {
			return new boolean[0];
		}

		@Override
		public ValueInclusion[] getPropertyInsertGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public boolean[] getPropertyUpdateability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyCheckability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyNullability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyVersionability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyLaziness() {
			return new boolean[0];
		}

		@Override
		public CascadeStyle[] getPropertyCascadeStyles() {
			return new CascadeStyle[0];
		}

		@Override
		public Type getIdentifierType() {
			return null;
		}

		@Override
		public String getIdentifierPropertyName() {
			return null;
		}

		@Override
		public boolean isCacheInvalidationRequired() {
			return false;
		}

		@Override
		public boolean isLazyPropertiesCacheable() {
			return false;
		}

		@Override
		public boolean canReadFromCache() {
			return false;
		}

		@Override
		public boolean canWriteToCache() {
			return false;
		}

		@Override
		public boolean hasCache() {
			return false;
		}

		@Override
		public EntityDataAccess getCacheAccessStrategy() {
			return null;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata() {
			return null;
		}

		@Override
		public boolean isBatchLoadable() {
			return false;
		}

		@Override
		public boolean isSelectBeforeUpdateRequired() {
			return false;
		}

		@Override
		public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return new Object[0];
		}

		@Override
		public Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session) {
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public boolean isInstrumented() {
			return false;
		}

		@Override
		public boolean hasInsertGeneratedProperties() {
			return false;
		}

		@Override
		public boolean hasUpdateGeneratedProperties() {
			return false;
		}

		@Override
		public boolean isVersionPropertyGenerated() {
			return false;
		}

		@Override
		public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		}

		@Override
		public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		}

		@Override
		public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public void processInsertGeneratedProperties(Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		}

		@Override
		public void processUpdateGeneratedProperties(Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		}

		@Override
		public Class getMappedClass() {
			return null;
		}

		@Override
		public boolean implementsLifecycle() {
			return false;
		}

		@Override
		public Class getConcreteProxyClass() {
			return null;
		}

		@Override
		public void setPropertyValues(Object object, Object[] values) {
		}

		@Override
		public void setPropertyValue(Object object, int i, Object value) {
		}

		@Override
		public Object[] getPropertyValues(Object object) {
			return new Object[0];
		}

		@Override
		public Object getPropertyValue(Object object, int i) {
			return null;
		}

		@Override
		public Object getPropertyValue(Object object, String propertyName) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object object) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		}

		@Override
		public Object getVersion(Object object) {
			return null;
		}

		@Override
		public Object instantiate(Object id, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public boolean isInstance(Object object) {
			return false;
		}

		@Override
		public boolean hasUninitializedLazyProperties(Object object) {
			return false;
		}

		@Override
		public void resetIdentifier(Object entity, Object currentId, Object currentVersion, SharedSessionContractImplementor session) {
		}

		@Override
		public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
			return null;
		}

		@Override
		public EntityRepresentationStrategy getRepresentationStrategy() {
			return null;
		}

		@Override
		public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
			return null;
		}

		@Override
		public int[] resolveAttributeIndexes(String[] attributeNames) {
			return new int[0];
		}

		@Override
		public boolean canUseReferenceCacheEntries() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return this;
		}

		@Override
		public EntityIdentifierDefinition getEntityKeyDefinition() {
			return null;
		}

		@Override
		public Iterable<AttributeDefinition> getAttributes() {
			return null;
		}

		@Override
		public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
			return false;
		}

		@Override
		public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
			return false;
		}

		@Override
		public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
			return false;
		}

		@Override
		public void linkWithSuperType(MappingModelCreationProcess creationProcess) {

		}

		@Override
		public void prepareMappingModel(MappingModelCreationProcess creationProcess) {

		}

		@Override
		public EntityIdentifierMapping getIdentifierMapping() {
			return null;
		}

		@Override
		public EntityVersionMapping getVersionMapping() {
			return null;
		}

		@Override
		public NaturalIdMapping getNaturalIdMapping() {
			return null;
		}

		@Override
		public boolean isTypeOrSuperType(EntityMappingType targetType) {
			return targetType == this;
		}

		@Override
		public java.util.Collection<AttributeMapping> getAttributeMappings() {
			return null;
		}

		@Override
		public void visitAttributeMappings(Consumer<AttributeMapping> action) {

		}

		@Override
		public JavaTypeDescriptor getMappedJavaTypeDescriptor() {
			return null;
		}

		@Override
		public void applyTableReferences(
				SqlAliasBase sqlAliasBase,
				JoinType baseJoinType,
				TableReferenceCollector collector,
				SqlExpressionResolver sqlExpressionResolver,
				SqlAstCreationContext creationContext) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}
	}

	public static class GoofyException extends RuntimeException {

	}
}
