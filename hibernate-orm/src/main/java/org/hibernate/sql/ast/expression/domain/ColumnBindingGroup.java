/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import java.util.List;

import org.hibernate.sql.ast.from.ColumnBinding;

/**
 * @author Steve Ebersole
 */
public interface ColumnBindingGroup {
	List<ColumnBinding> getColumnBindings();
}
