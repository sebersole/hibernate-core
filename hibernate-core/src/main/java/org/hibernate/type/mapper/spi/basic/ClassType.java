/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.type.descriptor.spi.java.basic.ClassTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.VarcharTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Class}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClassType extends BasicTypeImpl<Class> {
	public static final ClassType INSTANCE = new ClassType();

	public ClassType() {
		super( ClassTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "class";
	}

	@Override
	public JdbcLiteralFormatter<Class> getJdbcLiteralFormatter() {
		return VarcharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( ClassTypeDescriptor.INSTANCE );
	}
}
