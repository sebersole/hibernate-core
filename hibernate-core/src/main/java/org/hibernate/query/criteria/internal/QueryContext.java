/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import org.hibernate.query.criteria.JpaCriteriaBuilder;
import org.hibernate.query.sqm.produce.spi.ParsingContext;

/**
 * Context for expressions within a query.
 *
 * @author Christian Beikov
 */
public interface QueryContext {

	JpaCriteriaBuilder criteriaBuilder();

	ParsingContext parsingContext();
}
