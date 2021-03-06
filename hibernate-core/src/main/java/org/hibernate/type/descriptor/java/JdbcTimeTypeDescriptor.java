/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Time} handling.
 *
 * @author Steve Ebersole
 */
public class JdbcTimeTypeDescriptor extends AbstractTypeDescriptor<Date> {
	public static final JdbcTimeTypeDescriptor INSTANCE = new JdbcTimeTypeDescriptor();

	@SuppressWarnings("WeakerAccess")
	public static final String TIME_FORMAT = "HH:mm:ss.SSS";

	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	/**
	 * Alias for {@link java.time.format.DateTimeFormatter#ISO_LOCAL_TIME}.
	 *
	 * Intended for use with logging
	 *
	 * @see #LITERAL_FORMATTER
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LOGGABLE_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	public static class TimeMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final TimeMutabilityPlan INSTANCE = new TimeMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return value instanceof Time
					? new Time( value.getTime() )
					: new Date( value.getTime() );
		}
	}

	@SuppressWarnings("WeakerAccess")
	public JdbcTimeTypeDescriptor() {
		super( Date.class, TimeMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Date value) {
		return new SimpleDateFormat( TIME_FORMAT ).format( value );
	}

	@Override
	public java.util.Date fromString(String string) {
		try {
			return new Time( new SimpleDateFormat( TIME_FORMAT ).parse( string ).getTime() );
		}
		catch ( ParseException pe ) {
			throw new HibernateException( "could not parse time string" + string, pe );
		}
	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.HOUR_OF_DAY );
		hashCode = 31 * hashCode + calendar.get( Calendar.MINUTE );
		hashCode = 31 * hashCode + calendar.get( Calendar.SECOND );
		hashCode = 31 * hashCode + calendar.get( Calendar.MILLISECOND );
		return hashCode;
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		if ( one.getTime() == another.getTime() ) {
			return true;
		}

		Calendar calendar1 = Calendar.getInstance();
		Calendar calendar2 = Calendar.getInstance();
		calendar1.setTime( one );
		calendar2.setTime( another );

		return calendar1.get( Calendar.HOUR_OF_DAY ) == calendar2.get( Calendar.HOUR_OF_DAY )
				&& calendar1.get( Calendar.MINUTE ) == calendar2.get( Calendar.MINUTE )
				&& calendar1.get( Calendar.SECOND ) == calendar2.get( Calendar.SECOND )
				&& calendar1.get( Calendar.MILLISECOND ) == calendar2.get( Calendar.MILLISECOND );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Time.class.isAssignableFrom( type ) ) {
			final Time rtn = value instanceof Time
					? ( Time ) value
					: new Time( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			final java.sql.Date rtn = value instanceof java.sql.Date
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			final java.sql.Timestamp rtn = value instanceof java.sql.Timestamp
					? ( java.sql.Timestamp ) value
					: new java.sql.Timestamp( value.getTime() );
			return (X) rtn;
		}
		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return (X) cal;
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.getTime() );
		}
		throw unknownUnwrap( type );
	}
	@Override
	public <X> Date wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Time ) {
			return (Time) value;
		}

		if ( value instanceof Long ) {
			return new Time( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return new Time( ( (Calendar) value ).getTimeInMillis() );
		}

		if ( value instanceof Date ) {
			return new Time( ( (Date) value ).getTime() );
		}

		throw unknownWrap( value.getClass() );
	}
}
