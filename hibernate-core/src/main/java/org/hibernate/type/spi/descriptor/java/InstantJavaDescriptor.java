/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.TemporalType;

import org.hibernate.type.descriptor.java.spi.AbstractBasicTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class InstantJavaDescriptor extends AbstractBasicTypeDescriptor<Instant> implements TemporalTypeDescriptor<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantJavaDescriptor INSTANCE = new InstantJavaDescriptor();

	@SuppressWarnings("unchecked")
	public InstantJavaDescriptor() {
		super( Instant.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TIMESTAMP );
	}

	@Override
	public String toString(Instant value) {
		return DateTimeFormatter.ISO_INSTANT.format( value );
	}

	@Override
	public Instant fromString(String string) {
		return Instant.from( DateTimeFormatter.ISO_INSTANT.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Instant instant, Class<X> type, WrapperOptions options) {
		if ( instant == null ) {
			return null;
		}

		if ( Instant.class.isAssignableFrom( type ) ) {
			return (X) instant;
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final ZoneId zoneId = ZoneId.ofOffset( "UTC", ZoneOffset.UTC );
			return (X) GregorianCalendar.from( instant.atZone( zoneId ) );
		}

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( instant );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.from( instant );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.from( instant );
		}

		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Instant wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Instant.class.isInstance( value ) ) {
			return (Instant) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return ts.toInstant();
		}

		if ( Long.class.isInstance( value ) ) {
			return Instant.ofEpochMilli( (Long) value );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return ZonedDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toInstant();
		}

		if ( java.util.Date.class.isInstance( value ) ) {
			return ( (java.util.Date) value ).toInstant();
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalTypeDescriptor<X> resolveTypeForPrecision(TemporalType precision, TypeDescriptorRegistryAccess scope) {
		return (TemporalTypeDescriptor<X>) this;
	}
}
