/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;

/**
 * @author Steve Ebersole
 */
public interface TableReferenceContributor {
	/**
	 * Apply the Tables mapped by this producer to the collector as TableReferences
	 */
	void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext);
}
