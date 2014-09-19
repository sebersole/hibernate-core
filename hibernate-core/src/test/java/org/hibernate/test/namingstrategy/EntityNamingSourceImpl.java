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
package org.hibernate.test.namingstrategy;

import org.hibernate.cfg.naming.EntityNamingSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class EntityNamingSourceImpl implements EntityNamingSource {
	private final String className;
	private final String explicitEntityName;
	private final String jpaEntityName;

	private final String entityName;

	public EntityNamingSourceImpl(String className, String explicitEntityName, String jpaEntityName) {
		this.className = className;
		this.explicitEntityName = explicitEntityName;
		this.jpaEntityName = jpaEntityName;

		if ( explicitEntityName != null ) {
			this.entityName = explicitEntityName;
		}
		else {
			this.entityName = StringHelper.unqualifyEntityName( className );
		}
	}

	public EntityNamingSourceImpl(Class entityClass, String explicitEntityName, String jpaEntityName) {
		this( entityClass.getName(), explicitEntityName, jpaEntityName );
	}

	public EntityNamingSourceImpl(String className) {
		this.className = className;
		this.entityName = StringHelper.unqualifyEntityName( className );

		this.explicitEntityName = null;
		this.jpaEntityName = null;
	}

	public EntityNamingSourceImpl(Class entityClass) {
		this( entityClass.getName() );
	}

	@Override
	public String getEntityClassName() {
		return className;
	}

	@Override
	public String getExplicitEntityName() {
		return explicitEntityName;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}
}
