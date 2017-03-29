/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Calendar;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class CalendarVersionSupport implements VersionSupport<Calendar> {
	@Override
	public Calendar seed(SharedSessionContractImplementor session) {
		return Calendar.getInstance();
	}

	@Override
	public Calendar next(Calendar current, SharedSessionContractImplementor session) {
		return Calendar.getInstance();
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.CALENDAR.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(Calendar x, Calendar y) throws HibernateException {
		return StandardSpiBasicTypes.CALENDAR.isEqual( x, y );
	}
}
