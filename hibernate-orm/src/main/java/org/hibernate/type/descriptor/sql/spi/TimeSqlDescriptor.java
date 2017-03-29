/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.Calendar;

import javax.persistence.TemporalType;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterTemporal;

/**
 * Descriptor for {@link Types#TIME TIME} handling.
 *
 * @author Steve Ebersole
 */
public class TimeSqlDescriptor implements TemporalSqlDescriptor {
	public static final TimeSqlDescriptor INSTANCE = new TimeSqlDescriptor();

	public TimeSqlDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.TIME;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> TemporalJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Time.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterTemporal( (TemporalJavaDescriptor) javaTypeDescriptor, TemporalType.TIME );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Time time = javaTypeDescriptor.unwrap( value, Time.class, options );
				if ( value instanceof Calendar ) {
					st.setTime( index, time, (Calendar) value );
				}
				else {
					st.setTime( index, time );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Time time = javaTypeDescriptor.unwrap( value, Time.class, options );
				if ( value instanceof Calendar ) {
					st.setTime( name, time, (Calendar) value );
				}
				else {
					st.setTime( name, time );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getTime( position ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTime( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getTime( name ), options );
			}
		};
	}
}
