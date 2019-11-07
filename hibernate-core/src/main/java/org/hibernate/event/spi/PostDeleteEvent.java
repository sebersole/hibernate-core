/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after deleting an item from the datastore
 * 
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object id;
	private Object[] deletedState;
	
	public PostDeleteEvent(
			Object entity,
			Object id,
			Object[] deletedState,
			EntityPersister persister,
			EventSource source) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.persister = persister;
		this.deletedState = deletedState;
	}
	
	public Object getId() {
		return id;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	public Object getEntity() {
		return entity;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}
}
