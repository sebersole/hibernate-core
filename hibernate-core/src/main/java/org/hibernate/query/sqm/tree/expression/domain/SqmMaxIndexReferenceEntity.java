/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmMaxIndexReferenceEntity
		extends AbstractSpecificSqmCollectionIndexReference
		implements SqmMaxIndexReference, SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmMaxIndexReferenceEntity.class );

	private SqmFrom exportedFromElement;

	public SqmMaxIndexReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionIndexEntity getExpressableType() {
		return (CollectionIndexEntity) super.getExpressableType();
	}

	@Override
	public CollectionIndexEntity getInferableType() {
		return getExpressableType();
	}

	@Override
	public CollectionIndexEntity getReferencedNavigable() {
		return (CollectionIndexEntity ) super.getReferencedNavigable();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into MaxIndexBindingEntity [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
	}
}
