/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.Locale;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Representation of an entity being loaded, containing its state
 *
 * @author Steve Ebersole
 */
public class LoadingEntityEntry {
	private final EntityKey entityKey;
	private final EntityDescriptor descriptor;
	private final Object entityInstance;
	private final Object rowId;

	public LoadingEntityEntry(
			EntityKey entityKey,
			EntityDescriptor descriptor,
			Object entityInstance,
			Object rowId) {
		this.entityKey = entityKey;
		this.descriptor = descriptor;
		this.entityInstance = entityInstance;
		this.rowId = rowId;
	}


	public EntityKey getEntityKey() {
		return entityKey;
	}

	public EntityDescriptor getDescriptor() {
		return descriptor;
	}

	public Object getEntityInstance() {
		return entityInstance;
	}

	public Object getRowId() {
		return rowId;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"LoadingEntityEntry(type=%s, id=%s)@%s",
				getDescriptor().getEntityName(),
				getEntityKey().getIdentifier(),
				System.identityHashCode( this )
		);
	}
}
