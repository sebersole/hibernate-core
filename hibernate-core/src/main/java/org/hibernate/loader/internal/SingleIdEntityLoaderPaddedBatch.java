/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderPaddedBatch<T> extends SingleIdEntityLoaderSupport<T> implements Preparable {
	private final int batchSize;

	public SingleIdEntityLoaderPaddedBatch(
			EntityMappingType entityDescriptor,
			int batchSize,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		this.batchSize = batchSize;
	}

	@Override
	public void prepare() {

	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( "Support for " + BatchFetchStyle.PADDED + " not yet implemented" );
	}
}
