/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public interface ForeignKeyDescriptor extends VirtualModelPart {
	String PART_NAME = "{fk}";

	DomainResult createDomainResult(NavigablePath collectionPath, TableGroup tableGroup, DomainResultCreationState creationState);

	Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			JoinType joinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext);

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	/**
	 * Visits the FK "referring" columns
	 */
	@Override
	default void visitColumns(ColumnConsumer consumer) {
		visitReferringColumns( consumer );
	}

	void visitReferringColumns(ColumnConsumer consumer);

	void visitTargetColumns(ColumnConsumer consumer);

	void visitColumnMappings(FkColumnMappingConsumer consumer);

	interface FkColumnMappingConsumer {
		void consume(
				String referringTable,
				String referringColumn,
				String targetTable,
				String targetColumn,
				JdbcMapping jdbcMapping);
	}
}
