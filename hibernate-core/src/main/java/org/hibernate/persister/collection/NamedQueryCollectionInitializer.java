/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

/**
 * A wrapper around a named query.
 *
 * @author Gavin King
 */
public final class NamedQueryCollectionInitializer implements CollectionInitializer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( NamedQueryCollectionInitializer.class );

	private final String queryName;
	private final CollectionPersister persister;

	public NamedQueryCollectionInitializer(String queryName, CollectionPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public void initialize(Object key, SharedSessionContractImplementor session) throws HibernateException {
		LOG.debugf( "Initializing collection: %s using named query: %s", persister.getRole(), queryName );

		NativeQueryImplementor nativeQuery = session.getNamedNativeQuery( queryName );

		if ( nativeQuery.getParameterMetadata().hasNamedParameters() ) {
			nativeQuery.setParameter(
					nativeQuery.getParameterMetadata().getNamedParameterNames().iterator().next(),
					key,
					(AllowableParameterType) persister.getKeyType()
			);
		}
		else {
			nativeQuery.setParameter( 1, key, (AllowableParameterType) persister.getKeyType() );
		}

		nativeQuery.setCollectionKey( key ).setHibernateFlushMode( FlushMode.MANUAL ).list();
	}
}
