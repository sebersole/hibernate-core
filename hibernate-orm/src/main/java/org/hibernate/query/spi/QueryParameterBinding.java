/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.type.spi.Type;

/**
 * The value/type binding information for a particular query parameter.  Supports
 * both single-valued and multi-valued binds
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBinding<T> {
	boolean isBound();

	boolean allowsMultiValued();

	boolean isMultiValued();

	/**
	 * Get the Type currently associated with this binding.
	 *
	 * @return The currently associated Type
	 */
	Type getBindType();

	/**
	 * Sets the parameter binding value.  The inherent parameter type (if known) is assumed
	 *
	 * @param value The bind value
	 */
	void setBindValue(T value);

	/**
	 * Sets the parameter binding value using the explicit Type.
	 *
	 * @param value The bind value
	 * @param clarifiedType The explicit Type to use
	 */
	void setBindValue(T value, Type clarifiedType);

	/**
	 * Sets the parameter binding value using the explicit TemporalType.
	 *
	 * @param value The bind value
	 * @param clarifiedTemporalType The temporal type to use
	 */
	void setBindValue(T value, TemporalType clarifiedTemporalType);

	/**
	 * Get the value current bound.
	 *
	 * @return The currently bound value
	 */
	T getBindValue();
	/**
	 * Sets the parameter binding values.  The inherent parameter type (if known) is assumed in regards to the
	 * individual values.
	 *
	 * @param values The bind values
	 */
	void setBindValues(Collection<T> values);

	/**
	 * Sets the parameter binding values using the explicit Type in regards to the individual values.
	 *
	 * @param values The bind values
	 * @param clarifiedType The explicit Type to use
	 */
	void setBindValues(Collection<T> values, Type clarifiedType);

	/**Sets the parameter binding value using the explicit TemporalType in regards to the individual values.
	 *
	 *
	 * @param values The bind values
	 * @param clarifiedTemporalType The temporal type to use
	 */
	void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType);

	/**
	 * Get the values currently bound.
	 *
	 * @return The currently bound values
	 */
	Collection<T> getBindValues();

}
