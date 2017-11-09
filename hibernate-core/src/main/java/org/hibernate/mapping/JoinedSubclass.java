/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * A subclass in a table-per-subclass mapping
 * @author Gavin King
 */
public class JoinedSubclass extends Subclass implements TableOwner {
	private MappedTable table;
	private KeyValue key;

	public JoinedSubclass(
			EntityMapping superclass,
			EntityJavaDescriptor javaTypeDescriptor,
			MetadataBuildingContext metadataBuildingContext) {
		super( superclass, javaTypeDescriptor, metadataBuildingContext );
	}


	public Table getTable() {
		return (Table) getMappedTable();
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	public void setMappedTable(MappedTable table) {
		this.table = table;
		getSuperclass().addSubclassTable( table );
	}

	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
	}

	public void validate() throws MappingException {
		super.validate();
		if ( key!=null && !key.isValid() ) {
			throw new MappingException(
					"subclass key mapping has wrong number of columns: " +
					getEntityName() +
					" type: " +
					key.getJavaTypeDescriptor().getTypeName()
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
