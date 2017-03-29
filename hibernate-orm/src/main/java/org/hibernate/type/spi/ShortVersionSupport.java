/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class ShortVersionSupport implements VersionSupport<Short> {

	public static final ShortVersionSupport INSTANCE = new ShortVersionSupport();

	private static final Short ZERO = (short) 0;

	@Override
	public Short seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Short next(Short current, SharedSessionContractImplementor session) {
		return (short) ( current + 1 );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.SHORT.toLoggableString( value,factory );
	}

	@Override
	public boolean isEqual(Short x, Short y) throws HibernateException {
		return StandardSpiBasicTypes.SHORT.isEqual( x, y );
	}
}
