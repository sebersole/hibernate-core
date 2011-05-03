/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.jboss.logging.Logger;

import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.ejb.internal.boot.EntityManagerFactoryBuilder;
import org.hibernate.ejb.internal.boot.PersistenceUnit;
import org.hibernate.ejb.packaging.JarVisitorFactory;
import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.internal.boot.PersistenceXmlParser;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.ejb.util.PersistenceUtilHelper;
import org.hibernate.service.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * The Hibernate {@link PersistenceProvider} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class HibernatePersistence extends AvailableSettings implements PersistenceProvider {
    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			HibernatePersistence.class.getName()
	);

	private static final String META_INF_ORM_XML = "META-INF/orm.xml";

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@code persistence.xml}
	 */
	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		final Map integration = properties == null
				? Collections.EMPTY_MAP
				: Collections.unmodifiableMap( properties );

		final List<PersistenceUnit> units = PersistenceXmlParser.JEE_INSTANCE.resolvePersistenceUnits( integration );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( PersistenceUnit persistenceUnit : units ) {
			final Scanner scanner = buildScanner( persistenceUnit.getProperties(), integration );
			final URL jarURL = JarVisitorFactory.getJarURLFromURLEntry( persistenceUnit.getPersistenceXmlUrl(), "/META-INF/persistence.xml" );
			if ( persistenceUnit.getName() == null ) {
				persistenceUnit.setName( scanner.getUnqualifiedJarName( jarURL ) );
			}

			boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				continue;
			}

			EntityManagerFactoryBuilder entityManagerFactoryBuilder = new EntityManagerFactoryBuilder();
			entityManagerFactoryBuilder.process( persistenceUnit );

// todo : apply scan information.
//		does that include the stuff from persistence.xml?  or do we need to add that manually?
//
//			//scan main JAR
//			ScanningContext mainJarScanCtx = new ScanningContext()
//					.scanner( scanner )
//					.url( jarURL )
//					.explicitMappingFiles( persistenceUnit.getMappingFiles() )
//					.searchOrm( true );
//			setDetectedArtifactsOnScanningContext(
//					mainJarScanCtx,
//					persistenceUnit.getProperties(),
//					integration,
//					persistenceUnit.isExcludeUnlistedClasses()
//			);
//			addMetadataFromScan( mainJarScanCtx, persistenceUnit );
//
//			ScanningContext otherJarScanCtx = new ScanningContext()
//					.scanner( scanner )
//					.explicitMappingFiles( persistenceUnit.getMappingFiles() )
//					.searchOrm( true );
//			setDetectedArtifactsOnScanningContext(
//					otherJarScanCtx,
//					persistenceUnit.getProperties(),
//					integration,
//					false
//			);
//			for ( String jarFile : persistenceUnit.getJarFiles() ) {
//				otherJarScanCtx.url( JarVisitorFactory.getURLFromPath( jarFile ) );
//				addMetadataFromScan( otherJarScanCtx, persistenceUnit );
//			}

			return entityManagerFactoryBuilder.buildEntityManagerFactory();

		}
		return null;
	}

	private static class ScanningContext {
		//boolean excludeUnlistedClasses;
		private Scanner scanner;
		private URL url;
		private List<String> explicitMappingFiles;
		private boolean detectClasses;
		private boolean detectHbmFiles;
		private boolean searchOrm;

		public ScanningContext scanner(Scanner scanner) {
			this.scanner = scanner;
			return this;
		}

		public ScanningContext url(URL url) {
			this.url = url;
			return this;
		}

		public ScanningContext explicitMappingFiles(List<String> explicitMappingFiles) {
			this.explicitMappingFiles = explicitMappingFiles;
			return this;
		}

		public ScanningContext detectClasses(boolean detectClasses) {
			this.detectClasses = detectClasses;
			return this;
		}

		public ScanningContext detectHbmFiles(boolean detectHbmFiles) {
			this.detectHbmFiles = detectHbmFiles;
			return this;
		}

		public ScanningContext searchOrm(boolean searchOrm) {
			this.searchOrm = searchOrm;
			return this;
		}
	}

	private void setDetectedArtifactsOnScanningContext(
			ScanningContext context,
			Properties properties,
			Map overridenProperties,
			boolean excludeIfNotOverriden) {
		boolean detectClasses = false;
		boolean detectHbm = false;
		String detectSetting = overridenProperties != null ?
				(String) overridenProperties.get( AvailableSettings.AUTODETECTION ) :
				null;
		detectSetting = detectSetting == null ?
				properties.getProperty( AvailableSettings.AUTODETECTION) :
				detectSetting;
		if ( detectSetting == null && excludeIfNotOverriden) {
			//not overriden through HibernatePersistence.AUTODETECTION so we comply with the spec excludeUnlistedClasses
			context.detectClasses( false ).detectHbmFiles( false );
			return;
		}

		if ( detectSetting == null){
			detectSetting = "class,hbm";
		}
		StringTokenizer st = new StringTokenizer( detectSetting, ", ", false );
		while ( st.hasMoreElements() ) {
			String element = (String) st.nextElement();
			if ( "class".equalsIgnoreCase( element ) ) {
				detectClasses = true;
			}
			if ( "hbm".equalsIgnoreCase( element ) ) {
				detectHbm = true;
			}
		}
        LOG.debugf("Detect class: %s; detect hbm: %s", detectClasses, detectHbm);
		context.detectClasses( detectClasses ).detectHbmFiles( detectHbm );
	}

	private static void addMetadataFromScan(ScanningContext scanningContext, PersistenceMetadata metadata) {
		List<String> classes = metadata.getClasses();
		List<String> packages = metadata.getPackages();
		List<NamedInputStream> hbmFiles = metadata.getHbmfiles();
		List<String> mappingFiles = metadata.getMappingFiles();
		addScannedEntries( scanningContext, classes, packages, hbmFiles, mappingFiles );
	}

	private static void addScannedEntries(ScanningContext scanningContext, List<String> classes, List<String> packages, List<NamedInputStream> hbmFiles, List<String> mappingFiles) {
		Scanner scanner = scanningContext.scanner;
		if (scanningContext.detectClasses) {
			Set<Class<? extends Annotation>> annotationsToExclude = new HashSet<Class<? extends Annotation>>(3);
			annotationsToExclude.add( Entity.class );
			annotationsToExclude.add( MappedSuperclass.class );
			annotationsToExclude.add( Embeddable.class );
			Set<Class<?>> matchingClasses = scanner.getClassesInJar( scanningContext.url, annotationsToExclude );
			for (Class<?> clazz : matchingClasses) {
				classes.add( clazz.getName() );
			}

			Set<Package> matchingPackages = scanner.getPackagesInJar( scanningContext.url, new HashSet<Class<? extends Annotation>>(0) );
			for (Package pkg : matchingPackages) {
				packages.add( pkg.getName() );
			}
		}
		Set<String> patterns = new HashSet<String>();
		if (scanningContext.searchOrm) {
			patterns.add( META_INF_ORM_XML );
		}
		if (scanningContext.detectHbmFiles) {
			patterns.add( "**/*.hbm.xml" );
		}
		if ( mappingFiles != null) {
			patterns.addAll( mappingFiles );
		}
		if (patterns.size() !=0) {
			Set<NamedInputStream> files = scanner.getFilesInJar( scanningContext.url, patterns );
			for (NamedInputStream file : files) {
				hbmFiles.add( file );
				if (mappingFiles != null) {
					mappingFiles.remove( file.getName() );
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@link PersistenceUnitInfo}
	 */
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( info, properties );
		return configured != null ? configured.buildEntityManagerFactory() : null;
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property, cache );
		}

		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property, cache );
		}

		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

	private Scanner buildScanner(Properties properties, Map<?,?> integration) {
		//read the String or Instance from the integration map first and use the properties as a backup.
		Object value = integration.get( AvailableSettings.SCANNER );
		if ( value == null) {
			value = properties.getProperty( AvailableSettings.SCANNER );
		}

		if ( value == null ) {
			return new NativeScanner();
		}

		if ( value instanceof Scanner ) {
			return (Scanner) value;
		}

		final Class<? extends Scanner> scannerClass;
		if ( value instanceof Class ) {
			scannerClass = (Class<? extends Scanner>) value;
		}
		else {
			try {
				scannerClass = ClassLoaderServiceImpl.DEFAULT.classForName( value.toString() );
			}
			catch (ClassLoadingException e) {
				throw new PersistenceException( "Cannot find scanner class. " + AvailableSettings.SCANNER + "=" + value, e );
			}
		}

		try {
			return scannerClass.newInstance();
		}
		catch ( InstantiationException e ) {
			throw new PersistenceException( "Unable to instantiate Scanner class: " + scannerClass, e );
		}
		catch ( IllegalAccessException e ) {
			throw new PersistenceException( "Unable to access Scanner class: " + scannerClass, e );
		}
	}
}