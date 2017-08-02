/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ClassLoaderDelegate;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StandardClassLoaderDelegateImpl;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.relational.MappedAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.metamodel.model.domain.spi.InstantiatorFactory;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Standard implementation of the BootstrapContext copntract.
 *
 * @author Steve Ebersole
 */
public class BootstrapContextImpl implements BootstrapContext {
	private static final Logger log = Logger.getLogger( BootstrapContextImpl.class );

	private final StandardServiceRegistry serviceRegistry;

	private final TypeConfiguration typeConfiguration;

	private InstantiatorFactory instantiatorFactory;

	private final ClassLoaderAccessImpl classLoaderAccess;

	private final JavaReflectionManager hcannReflectionManager;
	private final ClassmateContext classmateContext;
	private final MetadataBuildingOptions metadataBuildingOptions;

	private boolean isJpaBootstrap;

	private ScanOptions scanOptions;
	private ScanEnvironment scanEnvironment;
	private Object scannerSetting;
	private ArchiveDescriptorFactory archiveDescriptorFactory;

	private IndexView jandexView;

	private HashMap<String,SqmFunctionTemplate> sqlFunctionMap;
	private ArrayList<MappedAuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
	private HashMap<Class,AttributeConverterDefinition> attributeConverterDefinitionsByClass;
	private ArrayList<CacheRegionDefinition> cacheRegionDefinitions;

	public BootstrapContextImpl(
			StandardServiceRegistry serviceRegistry,
			ClassmateContext classmateContext,
			MetadataBuildingOptions metadataBuildingOptions) {
		this.serviceRegistry = serviceRegistry;
		this.classmateContext = classmateContext;
		this.metadataBuildingOptions = metadataBuildingOptions;

		this.typeConfiguration = new TypeConfiguration();

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.classLoaderAccess = new ClassLoaderAccessImpl( classLoaderService );
		this.hcannReflectionManager = generateHcannReflectionManager();


		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

//		this.jandexView = (IndexView) configService.getSettings().get( AvailableSettings.JANDEX_INDEX );

		this.scanOptions = new StandardScanOptions(
				(String) configService.getSettings().get( AvailableSettings.SCANNER_DISCOVERY ),
				false
		);

		// ScanEnvironment must be set explicitly
		this.scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER );
		if ( this.scannerSetting == null ) {
			this.scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER_DEPRECATED );
			if ( this.scannerSetting != null ) {
				DEPRECATION_LOGGER.logDeprecatedScannerSetting();
			}
		}
		this.archiveDescriptorFactory = strategySelector.resolveStrategy(
				ArchiveDescriptorFactory.class,
				configService.getSettings().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER )
		);
	}

	private JavaReflectionManager generateHcannReflectionManager() {
		final JavaReflectionManager reflectionManager = new JavaReflectionManager();
		reflectionManager.setMetadataProvider( new JPAMetadataProvider( this ) );
		reflectionManager.injectClassLoaderDelegate( generateHcannClassLoaderDelegate() );
		return reflectionManager;
	}

	private ClassLoaderDelegate generateHcannClassLoaderDelegate() {
		//	class loading here needs to be drastically different for 7.0
		//		but luckily 7.0 will do away with HCANN use and be easier to
		//		implement this.
		//
		// todo (6.0) : *if possible* make similar change in 6.0
		// 		possibly using the JPA temp class loader or create our own "throw awy" ClassLoader;
		//		the trouble there is that we eventually need to load the Class into the real
		//		ClassLoader prior to use

		final ClassLoaderService classLoaderService = getServiceRegistry().getService( ClassLoaderService.class );

		return new ClassLoaderDelegate() {
			@Override
			public <T> Class<T> classForName(String className) throws ClassLoadingException {
				try {
					return classLoaderService.classForName( className );
				}
				catch (org.hibernate.boot.registry.classloading.spi.ClassLoadingException e) {
					return StandardClassLoaderDelegateImpl.INSTANCE.classForName( className );
				}
			}
		};
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return metadataBuildingOptions;
	}

	@Override
	public InstantiatorFactory getInstantiatorFactory() {
		return instantiatorFactory;
	}

	@Override
	public boolean isJpaBootstrap() {
		return isJpaBootstrap;
	}

	@Override
	public void markAsJpaBootstrap() {
		isJpaBootstrap = true;
	}

	@Override
	public ClassLoader getJpaTempClassLoader() {
		return classLoaderAccess.getJpaTempClassLoader();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return classLoaderAccess;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public ClassmateContext getClassmateContext() {
		return classmateContext;
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return archiveDescriptorFactory;
	}

	@Override
	public ScanOptions getScanOptions() {
		return scanOptions;
	}

	@Override
	public ScanEnvironment getScanEnvironment() {
		return scanEnvironment;
	}

	@Override
	public Object getScanner() {
		return scannerSetting;
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return hcannReflectionManager;
	}

	@Override
	public IndexView getJandexView() {
		return jandexView;
	}

	@Override
	public Map<String, SqmFunctionTemplate> getSqlFunctions() {
		return sqlFunctionMap == null ? Collections.emptyMap() : sqlFunctionMap;
	}

	@Override
	public Collection<MappedAuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return auxiliaryDatabaseObjectList == null ? Collections.emptyList() : auxiliaryDatabaseObjectList;
	}

	@Override
	public Collection<AttributeConverterDefinition> getAttributeConverters() {
		return attributeConverterDefinitionsByClass == null ? Collections.emptyList() : new ArrayList<>( attributeConverterDefinitionsByClass.values() );
	}

	@Override
	public List<CacheRegionDefinition> getCacheRegionDefinitions() {
		return cacheRegionDefinitions == null ? Collections.emptyList() : cacheRegionDefinitions;
	}

	@Override
	public void release() {
		classmateContext.release();
		classLoaderAccess.release();

		scanOptions = null;
		scanEnvironment = null;
		scannerSetting = null;
		archiveDescriptorFactory = null;
		jandexView = null;

		if ( sqlFunctionMap != null ) {
			sqlFunctionMap.clear();
		}

		if ( auxiliaryDatabaseObjectList != null ) {
			auxiliaryDatabaseObjectList.clear();
		}

		if ( attributeConverterDefinitionsByClass != null ) {
			attributeConverterDefinitionsByClass.clear();
		}

		if ( cacheRegionDefinitions != null ) {
			cacheRegionDefinitions.clear();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	void injectJpaTempClassLoader(ClassLoader jpaTempClassLoader) {
		log.debugf( "Injecting JPA temp ClassLoader [%s] into BootstrapContext; was [%s]", jpaTempClassLoader, this.getJpaTempClassLoader() );
		this.classLoaderAccess.injectTempClassLoader( jpaTempClassLoader );
	}

	void injectScanOptions(ScanOptions scanOptions) {
		log.debugf( "Injecting ScanOptions [%s] into BootstrapContext; was [%s]", scanOptions, this.scanOptions );
		this.scanOptions = scanOptions;
	}

	void injectScanEnvironment(ScanEnvironment scanEnvironment) {
		log.debugf( "Injecting ScanEnvironment [%s] into BootstrapContext; was [%s]", scanEnvironment, this.scanEnvironment );
		this.scanEnvironment = scanEnvironment;
	}

	void injectScanner(Scanner scanner) {
		log.debugf( "Injecting Scanner [%s] into BootstrapContext; was [%s]", scanner, this.scannerSetting );
		this.scannerSetting = scanner;
	}

	void injectArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		log.debugf( "Injecting ArchiveDescriptorFactory [%s] into BootstrapContext; was [%s]", factory, this.archiveDescriptorFactory );
		this.archiveDescriptorFactory = factory;
	}

	void injectJandexView(IndexView jandexView) {
		log.debugf( "Injecting Jandex IndexView [%s] into BootstrapContext; was [%s]", jandexView, this.jandexView );
		this.jandexView = jandexView;
	}

	public void addSqlFunction(String functionName, SqmFunctionTemplate function) {
		if ( this.sqlFunctionMap == null ) {
			this.sqlFunctionMap = new HashMap<>();
		}
		this.sqlFunctionMap.put( functionName, function );
	}

	public void addAuxiliaryDatabaseObject(MappedAuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( this.auxiliaryDatabaseObjectList == null ) {
			this.auxiliaryDatabaseObjectList = new ArrayList<>();
		}
		this.auxiliaryDatabaseObjectList.add( auxiliaryDatabaseObject );
	}

	public void addAttributeConverterDefinition(AttributeConverterDefinition definition) {
		if ( this.attributeConverterDefinitionsByClass == null ) {
			this.attributeConverterDefinitionsByClass = new HashMap<>();
		}

		final Object old = this.attributeConverterDefinitionsByClass.put( definition.getAttributeConverter().getClass(), definition );

		if ( old != null ) {
			throw new AssertionFailure(
					String.format(
							"AttributeConverter class [%s] registered multiple times",
							definition.getAttributeConverter().getClass()
					)
			);
		}
	}

	public void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( cacheRegionDefinitions == null ) {
			cacheRegionDefinitions = new ArrayList<>();
		}
		cacheRegionDefinitions.add( cacheRegionDefinition );
	}
}
