/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionElement<J> implements CollectionElement<J> {
	private final CollectionPersister persister;
	private final List<Column> columns;
	private final NavigableRole navigableRole;

	public AbstractCollectionElement(
			CollectionPersister persister,
			List<Column> columns) {
		this.persister = persister;
		this.columns = columns;

		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public CollectionPersister getContainer() {
		return persister;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public String asLoggableText() {
		return NAVIGABLE_NAME;
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBaseManager.SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector) {
		// only relevant for ONE-TO-MANY and MANY-TO-MANY - noop in the general case
	}
}
