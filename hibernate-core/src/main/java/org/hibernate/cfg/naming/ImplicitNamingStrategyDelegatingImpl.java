/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.naming;

import java.io.Serializable;

import org.hibernate.cfg.NamingStrategy;

/**
 * LogicalNamingStrategy implementation that delegates to a user-specified NamingStrategy.
 *
 * @deprecated Needed as a transitory implementation until the deprecated NamingStrategy contract
 * can be removed.
 *
 * @author Steve Ebersole
 */
@Deprecated
public class ImplicitNamingStrategyDelegatingImpl implements ImplicitNamingStrategy, Serializable {
	private final NamingStrategy namingStrategy;

	public ImplicitNamingStrategyDelegatingImpl(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	@Override
	public String determinePrimaryTableName(ImplicitPrimaryTableNameSource source) {
		return namingStrategy.classToTableName( source.getEntityNamingSource().getEntityClassName() );
	}

	@Override
	public String determineJoinTableName(ImplicitJoinTableNameSource source) {
		return namingStrategy.collectionTableName(
				source.getOwningEntityNamingSource().getEntityClassName(),
				source.getOwningPhysicalTableName(),
				source.getNonOwningEntityNamingSource().getEntityClassName(),
				source.getNonOwningPhysicalTableName(),
				source.getAssociationOwningAttributeName()
		);
	}

	@Override
	public String determineCollectionTableName(ImplicitCollectionTableNameSource source) {
		return namingStrategy.collectionTableName(
				source.getOwningEntityNamingSource().getEntityClassName(),
				source.getOwningPhysicalTableName(),
				null,
				null,
				source.getAssociationOwningAttributePath()
		);
	}

	@Override
	public String determineAttributeColumnName(ImplicitAttributeColumnNameSource source) {
		return namingStrategy.propertyToColumnName( source.getAttributePath() );
	}

	@Override
	public String determineCollectionJoinColumnName(ImplicitCollectionJoinColumnNameSource source) {
		String entityName = source.getOwningEntityNamingSource().getEntityClassName();
		if ( entityName == null ) {
			entityName = source.getOwningEntityNamingSource().getExplicitEntityName();
		}
		if ( entityName == null ) {
			entityName = source.getOwningEntityNamingSource().getEntityName();
		}

		return namingStrategy.foreignKeyColumnName(
				source.getAssociationOwningAttributeName(),
				entityName,
				source.getOwningEntityNamingSource().getEntityName(),
				source.getOwningPhysicalReferencedColumnName()
		);
	}
}
