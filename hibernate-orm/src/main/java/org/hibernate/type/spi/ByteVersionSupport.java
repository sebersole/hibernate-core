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
public class ByteVersionSupport implements VersionSupport<Byte> {

	public static final ByteVersionSupport INSTANCE = new ByteVersionSupport();

	private static final Byte ZERO = (byte) 0;

	@Override
	public Byte seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Byte next(Byte current, SharedSessionContractImplementor session) {
		return (byte) ( current + 1 );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.BYTE.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(Byte x, Byte y) throws HibernateException {
		return StandardSpiBasicTypes.BYTE.isEqual( x,y );
	}
}
