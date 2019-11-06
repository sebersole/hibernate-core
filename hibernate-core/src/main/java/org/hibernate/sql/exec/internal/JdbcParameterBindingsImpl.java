/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Standard implementation of JdbcParameterBindings
 *
 * @author Steve Ebersole
 */
public class JdbcParameterBindingsImpl implements JdbcParameterBindings {
	private Map<JdbcParameter, JdbcParameterBinding> bindingMap;

	public JdbcParameterBindingsImpl(int expectedParameterCount) {
		if ( expectedParameterCount > 0 ) {
			bindingMap = new IdentityHashMap<>( expectedParameterCount );
		}
	}

	@Override
	public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		if ( bindingMap == null ) {
			bindingMap = new IdentityHashMap<>();
		}

		bindingMap.put( parameter, binding );
	}

	@Override
	public Collection<JdbcParameterBinding> getBindings() {
		return bindingMap == null ? Collections.emptyList() : bindingMap.values();
	}

	@Override
	public JdbcParameterBinding getBinding(JdbcParameter parameter) {
		if ( bindingMap == null ) {
			// no bindings
			return null;
		}
		return bindingMap.get( parameter );
	}

	@Override
	public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		for ( Map.Entry<JdbcParameter, JdbcParameterBinding> entry : bindingMap.entrySet() ) {
			action.accept( entry.getKey(), entry.getValue() );
		}
	}

	public void clear() {
		if ( bindingMap != null ) {
			bindingMap.clear();
		}
	}
}
