/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.sql.convert.results.spi.FetchCompositeAttribute;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.exec.results.process.internal.CompositeReferenceInitializerImpl;
import org.hibernate.sql.exec.results.process.spi.CompositeReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.InitializerCollector;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class FetchCompositeAttributeImpl extends AbstractFetchParent implements FetchCompositeAttribute {
	private final FetchParent fetchParent;
	private final SingularPersistentAttributeEmbedded fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	private final CompositeReferenceInitializer initializer;

	public FetchCompositeAttributeImpl(
			FetchParent fetchParent,
			SingularPersistentAttributeEmbedded fetchedAttribute,
			FetchStrategy fetchStrategy) {
		super(
				fetchParent.getNavigablePath().append( fetchedAttribute.getAttributeName() ),
				fetchParent.getTableGroupUniqueIdentifier()
		);
		this.fetchParent = fetchParent;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;

		this.initializer = new CompositeReferenceInitializerImpl(
				fetchParent.getInitializerParentForFetchInitializers()
		);
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public SingularPersistentAttributeEmbedded getFetchedAttributeDescriptor() {
		return fetchedAttribute;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public EmbeddedType getFetchedType() {
		return (EmbeddedType) fetchedAttribute.getOrmType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttribute.isNullable();
	}

	@Override
	public CompositeReferenceInitializer getInitializerParentForFetchInitializers() {
		return initializer;
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( initializer );
	}

	@Override
	public Initializer getInitializer() {
		return null;
	}
}
