/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.metamodel.model.domain.internal.PersistentListDescriptorImpl;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class ListInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler listIndexAssembler;
	private final DomainResultAssembler elementAssembler;

	public ListInitializer(
			PersistentListDescriptorImpl listDescriptor,
			FetchParentAccess parentAccess,
			boolean joined,
			DomainResultAssembler collectionKeyAssembler,
			DomainResultAssembler listIndexAssembler,
			DomainResultAssembler elementAssembler) {
		super( listDescriptor, parentAccess, joined, collectionKeyAssembler );
		this.listIndexAssembler = listIndexAssembler;
		this.elementAssembler = elementAssembler;
	}

	@Override
	public PersistentList getCollectionInstance() {
		return (PersistentList) super.getCollectionInstance();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void readCollectionRow(RowProcessingState rowProcessingState) {
		getCollectionInstance().load(
				(int) listIndexAssembler.assemble( rowProcessingState ),
				elementAssembler.assemble( rowProcessingState )
		);
	}
}
