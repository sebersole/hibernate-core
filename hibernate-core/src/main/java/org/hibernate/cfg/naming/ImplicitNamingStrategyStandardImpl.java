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

import org.hibernate.HibernateException;
import org.hibernate.internal.util.StringHelper;

/**
 * Implementation of the LogicalNamingStrategy contract, generally preferring to conform
 * to JPA standards.
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyStandardImpl implements ImplicitNamingStrategy, Serializable {
	public static final ImplicitNamingStrategy INSTANCE = new ImplicitNamingStrategyStandardImpl();

	@Override
	public String determinePrimaryTableName(ImplicitPrimaryTableNameSource source) {
		String tableName;

		final EntityNamingSource entityNamingSource = source.getEntityNamingSource();
		if ( entityNamingSource == null ) {
			// should never happen, but to be defensive...
			throw new HibernateException( "Entity naming information was not provided." );
		}

		if ( StringHelper.isNotEmpty( entityNamingSource.getJpaEntityName() ) ) {
			// prefer the JPA entity name, if specified...
			tableName = entityNamingSource.getJpaEntityName();
		}
		else {
			// otherwise, use the Hibernate entity name
			tableName = entityNamingSource.getEntityName();
		}

		if ( tableName == null ) {
			// todo : add info to error message
			throw new HibernateException( "Could not determine primary table name for entity" );
		}

		return tableName;
	}

	@Override
	public String determineJoinTableName(ImplicitJoinTableNameSource source) {
		// JPA states we should use the following as default:
		//		"The concatenated names of the two associated primary entity tables (owning side
		//		first), separated by an underscore."
		// aka:
		// 		{OWNING SIDE PRIMARY TABLE NAME}_{NON-OWNING SIDE PRIMARY TABLE NAME}

		return  source.getOwningPhysicalTableName()
				+ '_'
				+ source.getNonOwningPhysicalTableName();
	}

	@Override
	public String determineCollectionTableName(ImplicitCollectionTableNameSource source) {
		// JPA states we should use the following as default:
		//      "The concatenation of the name of the containing entity and the name of the
		//       collection attribute, separated by an underscore.
		// aka:
		//     if owning entity has a JPA entity name: {OWNER JPA ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}
		//     otherwise: {OWNER ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}

		final String entityName;
		if ( StringHelper.isNotEmpty( source.getOwningEntityNamingSource().getJpaEntityName() ) ) {
			// prefer the JPA entity name, if specified...
			entityName = source.getOwningEntityNamingSource().getJpaEntityName();
		}
		else {
			// otherwise, use the Hibernate entity name
			entityName = source.getOwningEntityNamingSource().getEntityName();
		}

		return entityName
				+ '_'
				+ StringHelper.unqualify( source.getAssociationOwningAttributePath() );
	}

	@Override
	public String determineAttributeColumnName(ImplicitAttributeColumnNameSource source) {
		// JPA states we shoulduse the following as default:
		//     "The property or field name"
		// aka:
		//     The unqualified attribute path.
		return  StringHelper.unqualify( source.getAttributePath() );
	}

	@Override
	public String determineCollectionJoinColumnName(ImplicitCollectionJoinColumnNameSource source) {
		// JPA states we should use the following as default:
		//     "The concatenation of the following: the name of the entity; "_"; the name of the
		//      referenced primary key column"
		final String entityName;
		if ( StringHelper.isNotEmpty( source.getOwningEntityNamingSource().getJpaEntityName() ) ) {
			// prefer the JPA entity name, if specified...
			entityName = source.getOwningEntityNamingSource().getJpaEntityName();
		}
		else {
			// otherwise, use the Hibernate entity name
			entityName = source.getOwningEntityNamingSource().getEntityName();
		}
		return entityName
				+ '_'
				+ source.getOwningPhysicalReferencedColumnName();
	}
}
