/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.spi.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import javax.persistence.TemporalType;

import org.hibernate.type.descriptor.internal.sql.JdbcLiteralFormatterTemporal;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.java.basic.TemporalJavaTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TIMESTAMP TIMESTAMP} handling.
 *
 * @author Steve Ebersole
 */
public class TimestampTypeDescriptor implements SqlTypeDescriptor {
	public static final TimestampTypeDescriptor INSTANCE = new TimestampTypeDescriptor();

	public TimestampTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.TIMESTAMP;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Timestamp.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( (TemporalJavaTypeDescriptor) javaTypeDescriptor, TemporalType.TIMESTAMP );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					st.setTimestamp( index, timestamp, (Calendar) value );
				}
				else {
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp = javaTypeDescriptor.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					st.setTimestamp( name, timestamp, (Calendar) value );
				}
				else {
					st.setTimestamp( name, timestamp );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getTimestamp( name ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTimestamp( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTimestamp( name ), options );
			}
		};
	}
}
