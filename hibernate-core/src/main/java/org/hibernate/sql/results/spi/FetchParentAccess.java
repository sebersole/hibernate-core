/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * Serves as a link to a fetch's parent providing access to the parent
 * instance in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface FetchParentAccess {
	/**
	 * Access to the fetch's parent instance.
	 */
	Object getFetchParentInstance();

	Object getResolvedState(Navigable navigable, RowProcessingState processingState);
}
