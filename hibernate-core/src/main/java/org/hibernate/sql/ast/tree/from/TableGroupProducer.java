/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;

/**
 * Marker interface for anything which produces a TableGroup
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface TableGroupProducer extends ModelPartContainer, TableReferenceContributor {
	/**
	 * Get the "stem" used as the base for generating SQL table aliases for table
	 * references that are part of the TableGroup being generated
	 * <p/>
	 * Note that this is a metadata-ive value.  It is only ever used internal to
	 * the producer producing its TableGroup.
	 *
	 * @see SqlAliasBaseManager#createSqlAliasBase
	 */
	String getSqlAliasStem();
}
