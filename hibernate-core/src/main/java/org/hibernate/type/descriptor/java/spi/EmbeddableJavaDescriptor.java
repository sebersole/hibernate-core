/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Models information about an embeddable.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableJavaDescriptor<T> extends ManagedJavaDescriptor<T> {
	@Override
	EmbeddableJavaDescriptor<? super T> getSuperType();

}
