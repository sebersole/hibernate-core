/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;


/**
 * Represents a <tt>pre-delete</tt> event, which occurs just prior to
 * performing the deletion of an entity from the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreDeleteEvent
		extends AbstractPreDatabaseOperationEvent
		implements PermissionCheckEntityInformation {

	private Object[] deletedState;

	/**
	 * Constructs an event containing the pertinent information.
	 *  @param entity The entity to be deleted.
	 * @param id The id to use in the deletion.
	 * @param deletedState The entity's state at deletion time.
	 * @param descriptor The entity's descriptor.
	 * @param source The session from which the event originated.
	 */
	public PreDeleteEvent(
			Object entity,
			Object id,
			Object[] deletedState,
			EntityDescriptor descriptor,
			EventSource source) {
		super( source, entity, id, descriptor );
		this.deletedState = deletedState;
	}

	/**
	 * Getter for property 'deletedState'.  This is the entity state at the
	 * time of deletion (useful for optomistic locking and such).
	 *
	 * @return Value for property 'deletedState'.
	 */
	public Object[] getDeletedState() {
		return deletedState;
	}

}
