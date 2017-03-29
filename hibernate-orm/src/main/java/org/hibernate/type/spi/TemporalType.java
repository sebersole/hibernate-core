/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;

/**
 * Further classification of a BasicType that models a temporal (date, time, instant, etc) value type.
 *
 * @author Steve Ebersole
 */
public interface TemporalType<T> extends BasicType<T> {
	@Override
	TemporalJavaDescriptor<T> getJavaTypeDescriptor();

	javax.persistence.TemporalType getPrecision();

	<X> TemporalType<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeConfiguration typeConfiguration);

	@Override
	default String getTypeName() {
		return "TemporalTypeImpl( " +
				getJavaTypeDescriptor().getTypeName() + ", " +
				getColumnMapping().getSqlTypeDescriptor().getSqlType() + " [" +
				(getMutabilityPlan() == null ? "<no-mutability-plan>" : getMutabilityPlan().getClass().getSimpleName() ) + ", " +
				(getPrecision() == null ? "<no-precision>" : getPrecision()) + "])";
	}
}
