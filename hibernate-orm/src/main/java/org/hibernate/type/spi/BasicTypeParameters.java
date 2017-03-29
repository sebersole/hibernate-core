/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import java.util.Comparator;

import org.hibernate.Incubating;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Essentially acts as a parameter-object to {@link BasicTypeRegistry#resolveBasicType}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BasicTypeParameters<T> {
	BasicJavaDescriptor<T> getJavaTypeDescriptor();
	SqlTypeDescriptor getSqlTypeDescriptor();
	AttributeConverterDefinition getAttributeConverterDefinition();
	MutabilityPlan<T> getMutabilityPlan();
	Comparator<T> getComparator();
	javax.persistence.TemporalType getTemporalPrecision();
}
