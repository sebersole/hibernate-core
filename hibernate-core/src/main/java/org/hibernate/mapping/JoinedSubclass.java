/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorEntityImplementor;

/**
 * A subclass in a table-per-subclass mapping
 * @author Gavin King
 */
public class JoinedSubclass extends Subclass implements TableOwner {
	private Table table;
	private KeyValue key;

	public JoinedSubclass(
			JavaTypeDescriptorEntityImplementor javaTypeDescriptor,
			PersistentClass superclass,
			MetadataBuildingContext metadataBuildingContext) {
		super( javaTypeDescriptor, superclass, metadataBuildingContext );
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table=table;
		getSuperclass().addSubclassTable(table);
	}

	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate(mapping);
		if ( key!=null && !key.isValid(mapping) ) {
			throw new MappingException(
					"subclass key mapping has wrong number of columns: " +
					getEntityName() +
					" type: " +
					key.getType().getName()
				);
		}
	}

	public Iterator getReferenceablePropertyIterator() {
		return getPropertyIterator();
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
