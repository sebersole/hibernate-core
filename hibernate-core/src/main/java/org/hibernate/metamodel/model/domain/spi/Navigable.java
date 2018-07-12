/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;
import org.hibernate.sql.results.spi.SqlSelectionResolutionContext;

/**
 * Models a "piece" of the application's domain model that can be navigated
 * as part of a query or the {@link NavigableVisitationStrategy} contract.
 *
 * @author Steve Ebersole
 */
public interface Navigable<T> extends DomainType<T> {
	/**
	 * The NavigableContainer which contains this Navigable.
	 */
	NavigableContainer<?> getContainer();

	/**
	 * The role for this Navigable which is unique across all
	 * Navigables in the given TypeConfiguration.
	 */
	NavigableRole getNavigableRole();

	default String getNavigableName() {
		return getNavigableRole().getNavigableName();
	}

	/**
	 * Visitation (walking) contract
	 *
	 * @param visitor The "visitor" responsibility in the Visitor pattern
	 */
	void visitNavigable(NavigableVisitationStrategy visitor);

	/**
	 * Finish initialization step.
	 *
	 * It's important to understand that the runtime creation process will call this
	 * method on the navigable until one of several conditions are met
	 *
	 * <ul>
	 *     <li>All navigables have returned true.</li>
	 *     <li>The navigable returned false and needs to wait for other dependencies.</li>
	 *     <li>The list of navigables have some missing or cyclic dependency</li>
	 * </ul>
	 *
	 * @return true if initialization complete, false if not yet done.
	 */
	default boolean finishInitialization() {
		return true;
	}

	default SqmNavigableReference createSqmExpression(
			// todo (6.0) : remove `sourceSqmFrom` - we should be able to deduce this based on the `containerReference` and this implementation
			//		and passing it in here makes it impossible for the SqmNavigableReference to create these as proper
			//		via SqmFromElementBuilder (`creationContext#getCurrent
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
	}

	default QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
	}

	default List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlSelectionResolutionContext resolutionContext) {
		throw new NotYetImplementedFor6Exception();
	}

	SqlSelectionGroupNode resolveSqlSelections(
			ColumnReferenceQualifier qualifier,
			SqlSelectionResolutionContext resolutionContext);

	/**
	 * Obtain a loggable representation.
	 *
	 * @return The loggable representation of this reference
	 */
	String asLoggableText();
}
