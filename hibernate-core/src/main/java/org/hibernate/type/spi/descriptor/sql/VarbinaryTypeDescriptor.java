/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.ValueBinder;
import org.hibernate.type.spi.descriptor.ValueExtractor;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#VARBINARY VARBINARY} handling.
 *
 * @author Steve Ebersole
 */
public class VarbinaryTypeDescriptor implements SqlTypeDescriptor {
	public static final VarbinaryTypeDescriptor INSTANCE = new VarbinaryTypeDescriptor();

	public VarbinaryTypeDescriptor() {
	}

	public int getSqlType() {
		return Types.VARBINARY;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( byte[].class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		// literal values for binary data of any kind is not supported.
		return null;
	}

	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setBytes( index, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setBytes( name, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
			}
		};
	}

	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBytes( name ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBytes( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBytes( name ), options );
			}
		};
	}
}
