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

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.jboss.logging.Logger;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.ejb.event.JpaIntegrator;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class EntityManagerFactoryBuilder {
    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			EntityManagerFactoryBuilder.class.getName()
	);

	private Map configuration = new HashMap();

	private NamingStrategy namingStrategy = EJB3NamingStrategy.INSTANCE;

	private SettingsImpl settings = new SettingsImpl();

	private ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();

	public void process(PersistenceUnit persistenceUnit) {
		final Properties props = persistenceUnit.getProperties();
		configuration.putAll( props );
		serviceRegistryBuilder.applySettings( props );

		// JDBC connections
		if ( StringHelper.isNotEmpty( persistenceUnit.getJtaDataSource() ) ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getJtaDataSource() );
		}
		else if ( StringHelper.isNotEmpty( persistenceUnit.getNonJtaDataSource() ) ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getNonJtaDataSource() );
		}
		else {
			final String driver = props.getProperty( AvailableSettings.JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				serviceRegistryBuilder.applySetting( Environment.DRIVER, driver );
			}
			final String url = props.getProperty( AvailableSettings.JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				serviceRegistryBuilder.applySetting( Environment.URL, url );
			}
			final String user = props.getProperty( AvailableSettings.JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				serviceRegistryBuilder.applySetting( Environment.USER, user );
			}
			final String pass = props.getProperty( AvailableSettings.JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				serviceRegistryBuilder.applySetting( Environment.PASS, pass );
			}
		}

		// Transactions
		settings.setTransactionType( persistenceUnit.getTransactionType() );
		boolean hasTxStrategy = StringHelper.isNotEmpty( props.getProperty( Environment.TRANSACTION_STRATEGY ) );
		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( Environment.TRANSACTION_STRATEGY );
		}
		else {
			if ( settings.getTransactionType() == PersistenceUnitTransactionType.JTA ) {
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class );
			}
			else if ( settings.getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class );
			}
		}

		processProperties( props );
	}

	private void processProperties(Properties props) {
		if ( props.containsKey( AvailableSettings.CFG_FILE ) ) {
			serviceRegistryBuilder.configure( props.getProperty( AvailableSettings.CFG_FILE ) );
		}

		// flush before completion validation
		if ( props.getProperty( Environment.FLUSH_BEFORE_COMPLETION ).equals( "true" ) ) {
			serviceRegistryBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
            LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

		for ( Map.Entry entry : props.entrySet() ) {
			if ( entry.getKey() instanceof String ) {
				final String keyString = (String) entry.getKey();
				if ( keyString.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					processCacheStrategy( keyString, (String) entry.getValue(), true );
				}
				else if ( keyString.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					processCacheStrategy( keyString, (String) entry.getValue(), false );
				}
			}
		}

		Iterator propertyIt = preparedProperties.keySet().iterator();
		while ( propertyIt.hasNext() ) {
			Object uncastObject = propertyIt.next();
			//had to be safe
			if ( uncastObject != null && uncastObject instanceof String ) {
				String propertyKey = (String) uncastObject;
				if ( propertyKey.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, true, workingVars );
				}
				else if ( propertyKey.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, false, workingVars );
				}
				else if ( propertyKey.startsWith( AvailableSettings.JACC_PREFIX )
						&& ! ( propertyKey.equals( AvailableSettings.JACC_CONTEXT_ID )
						|| propertyKey.equals( AvailableSettings.JACC_ENABLED ) ) ) {
					jaccKeys.add( propertyKey );
				}
			}
		}
		final Interceptor interceptor = instantiateCustomClassFromConfiguration(
				props,
				defaultInterceptor,
				cfg.getInterceptor(),
				AvailableSettings.INTERCEPTOR,
				"interceptor",
				Interceptor.class
		);
		if ( interceptor != null ) {
			cfg.setInterceptor( interceptor );
		}
		final NamingStrategy namingStrategy = instantiateCustomClassFromConfiguration(
				preparedProperties,
				defaultNamingStrategy,
				cfg.getNamingStrategy(),
				AvailableSettings.NAMING_STRATEGY,
				"naming strategy",
				NamingStrategy.class
		);
		if ( namingStrategy != null ) {
			cfg.setNamingStrategy( namingStrategy );
		}

		final SessionFactoryObserver observer = instantiateCustomClassFromConfiguration(
				preparedProperties,
				null,
				cfg.getSessionFactoryObserver(),
				AvailableSettings.SESSION_FACTORY_OBSERVER,
				"SessionFactory observer",
				SessionFactoryObserver.class
		);
		if ( observer != null ) {
			cfg.setSessionFactoryObserver( observer );
		}

		if ( jaccKeys.size() > 0 ) {
			addSecurity( jaccKeys, preparedProperties, workingVars );
		}

		//some spec compliance checking
		//TODO centralize that?
        if (!"true".equalsIgnoreCase(cfg.getProperty(Environment.AUTOCOMMIT))) LOG.jdbcAutoCommitFalseBreaksEjb3Spec(Environment.AUTOCOMMIT);
        discardOnClose = preparedProperties.getProperty(AvailableSettings.DISCARD_PC_ON_CLOSE).equals("true");

	}




	private void processCacheStrategy(String propertyKey, String value, boolean isClass) {
		String role = propertyKey.substring(
				( isClass ? AvailableSettings.CLASS_CACHE_PREFIX
						.length() : AvailableSettings.COLLECTION_CACHE_PREFIX.length() )
						+ 1
		);
		//dot size added
		String value = (String) properties.get( propertyKey );
		StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			error.append(
					isClass ? AvailableSettings.CLASS_CACHE_PREFIX : AvailableSettings.COLLECTION_CACHE_PREFIX
			);
			error.append( ": " ).append( propertyKey ).append( " " ).append( value );
			throw new PersistenceException( getExceptionHeader() + error.toString() );
		}
		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		if ( isClass ) {
			boolean lazyProperty = true;
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
			cfg.setCacheConcurrencyStrategy( role, usage, region, lazyProperty );
		}
		else {
			cfg.setCollectionCacheConcurrencyStrategy( role, usage, region );
		}
	}

	public EntityManagerFactory buildEntityManagerFactory() {
		BasicServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
		serviceRegistry.getService( IntegratorService.class ).addIntegrator( new JpaIntegrator() );

		// todo : account for configure naming strategy and processing order

		SessionFactory sessionFactory = new MetadataSources( serviceRegistry )
				.getMetadataBuilder()
				.buildMetadata()
				.buildSessionFactory();

		( (SessionFactoryImplementor) sessionFactory ).addObserver( new ServiceRegistryCloser() );

		return new EntityManagerFactoryImpl(
				transactionType,
				discardOnClose,
				getSessionInterceptorClass( cfg.getProperties() ),
				cfg,
				serviceRegistry
		);

	}

	public static class ServiceRegistryCloser implements SessionFactoryObserver {
		@Override
		public void sessionFactoryCreated(SessionFactory sessionFactory) {
			// nothing to do
		}

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			SessionFactoryImplementor sfi = ( (SessionFactoryImplementor) sessionFactory );
			sfi.getServiceRegistry().destroy();
			ServiceRegistry basicRegistry = sfi.getServiceRegistry().getParentServiceRegistry();
			( (ServiceRegistryImplementor) basicRegistry ).destroy();
		}
	}



	private Class getSessionInterceptorClass(Properties properties) {
		String sessionInterceptorClassname = (String) properties.get( AvailableSettings.SESSION_INTERCEPTOR );
		if ( StringHelper.isNotEmpty( sessionInterceptorClassname ) ) {
			try {
				Class interceptorClass = ReflectHelper.classForName(
						sessionInterceptorClassname, Ejb3Configuration.class
				);
				interceptorClass.newInstance();
				return interceptorClass;
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to load "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to instanciate "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
			catch (InstantiationException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to instanciate "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
        }
        return null;
	}
}
