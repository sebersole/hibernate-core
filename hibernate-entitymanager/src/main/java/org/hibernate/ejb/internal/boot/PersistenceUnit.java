/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.internal.boot;

import javax.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.hibernate.ejb.packaging.NamedInputStream;

/**
 * Models the {@code <persistence-unit/>} element in a {@code persistence.xml} file as parsed by Hibernate.
 * <p/>
 * Essentially a corollary to {@link javax.persistence.spi.PersistenceUnitInfo}, which is passed to Hibernate
 * as part of the JEE container SPI contract.
 *
 * @author Steve Ebersole
 */
public class PersistenceUnit  {
	private final URL persistenceXmlUrl;

	private String name;
	private String nonJtaDataSource;
	private String jtaDataSource;
	private String provider;
	private PersistenceUnitTransactionType transactionType;
	private boolean useQuotedIdentifiers = false; // the spec (erroneously?) calls this delimited-identifiers
	private boolean excludeUnlistedClasses = false;
	private String validationMode;
	private String sharedCacheMode;
	private Properties properties = new Properties();

	private List<String> classes = new ArrayList<String>();
	private List<String> packages = new ArrayList<String>();
	private List<String> mappingFiles = new ArrayList<String>();
	private Set<String> jarFiles = new HashSet<String>();
	private List<NamedInputStream> hbmFileStreams = new ArrayList<NamedInputStream>();

	public PersistenceUnit(URL persistenceXmlUrl) {
		this.persistenceXmlUrl = persistenceXmlUrl;
	}

	public URL getPersistenceXmlUrl() {
		return persistenceXmlUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public void setNonJtaDataSource(String nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	public String getJtaDataSource() {
		return jtaDataSource;
	}

	public void setJtaDataSource(String jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public boolean isUseQuotedIdentifiers() {
		return useQuotedIdentifiers;
	}

	public void setUseQuotedIdentifiers(boolean useQuotedIdentifiers) {
		this.useQuotedIdentifiers = useQuotedIdentifiers;
	}

	public Properties getProperties() {
		return properties;
	}

	public boolean isExcludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	public String getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(String validationMode) {
		this.validationMode = validationMode;
	}

	public String getSharedCacheMode() {
		return sharedCacheMode;
	}

	public void setSharedCacheMode(String sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public List<String> getClasses() {
		return classes;
	}

	public void addClasses(String... classes) {
		addClasses( Arrays.asList( classes ) );
	}

	public void addClasses(List<String> classes) {
		this.classes.addAll( classes );
	}

	public List<String> getPackages() {
		return packages;
	}

	public void addPackages(String... packageNames) {
		addPackages( Arrays.asList( packageNames ) );
	}

	public void addPackages(List<String> packageNames) {
		this.packages.addAll( packageNames );
	}

	public List<String> getMappingFiles() {
		return mappingFiles;
	}

	public void addMappingFiles(String... mappingFiles) {
		addMappingFiles( Arrays.asList( mappingFiles ) );
	}

	public void addMappingFiles(List<String> mappingFiles) {
		this.mappingFiles.addAll( mappingFiles );
	}

	public Set<String> getJarFiles() {
		return jarFiles;
	}

	public void addJarFiles(String... jarFiles) {
		addJarFiles( Arrays.asList( jarFiles ) );
	}

	public void addJarFiles(Collection<String> jarFiles) {
		this.jarFiles.addAll( jarFiles );
	}

	public List<NamedInputStream> getHbmFileStreams() {
		return hbmFileStreams;
	}

	public void addHbmFileStreams(NamedInputStream... hbmFileStreams) {
		addHbmFileStreams( Arrays.asList( hbmFileStreams ) );
	}

	public void addHbmFileStreams(List<NamedInputStream> hbmFileStreams) {
		this.hbmFileStreams = hbmFileStreams;
	}
}
