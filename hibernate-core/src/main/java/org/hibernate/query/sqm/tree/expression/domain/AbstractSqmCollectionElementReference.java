/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCollectionElementReference extends AbstractSqmNavigableReference implements
		SqmCollectionElementReference {
	private final SqmPluralAttributeReference attributeBinding;
	private final PluralPersistentAttribute pluralAttributeReference;
	private final NavigablePath navigablePath;

	public AbstractSqmCollectionElementReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeBinding = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		this.navigablePath = pluralAttributeBinding.getNavigablePath().append( "{elements}" );
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return getPluralAttributeBinding();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeBinding().getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String asLoggableText() {
		return getNavigablePath().getFullPath();
	}

	@Override
	public ExpressableType getExpressableType() {
		return getPluralAttributeBinding().getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getElementDescriptor();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getPluralAttributeBinding();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most element classifications, the uid should point to the "collection table"...
		return getPluralAttributeBinding().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most element classifications, the "identification variable" (alias)
		// 		associated with elements is the identification variable for the collection reference
		return getPluralAttributeBinding().getIdentificationVariable();
	}

	@Override
	public void setIdentificationVariable(String identificationVariable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		// for most element classifications, there is none
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getReferencedNavigable().getJavaType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeElementBinding( this );
	}
}
