/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.Clause;

/**
 * Specialized SemanticQueryWalker (SQM visitor) for producing SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqmToSqlAstConverter extends SemanticQueryWalker<Object>, SqlAstCreationState {
	Stack<Clause> getCurrentClauseStack();
}
