/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.time.LocalDateTime;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class LocalDateTimeVersionSupport implements VersionSupport<LocalDateTime> {

	public static final LocalDateTimeVersionSupport INSTANCE = new LocalDateTimeVersionSupport();

	@Override
	public LocalDateTime seed(SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	public LocalDateTime next(LocalDateTime current, SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.LOCAL_DATE_TIME.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(LocalDateTime x, LocalDateTime y) throws HibernateException {
		return StandardSpiBasicTypes.LOCAL_DATE_TIME.isEqual( x, y );
	}
}
