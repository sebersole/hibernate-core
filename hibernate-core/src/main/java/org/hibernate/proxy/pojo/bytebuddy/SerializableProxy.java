/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.lang.reflect.Method;

import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;

public final class SerializableProxy extends AbstractSerializableProxy {
	private final Class persistentClass;
	private final Class[] interfaces;

	private final String identifierGetterMethodName;
	private final Class identifierGetterMethodClass;

	private final String identifierSetterMethodName;
	private final Class identifierSetterMethodClass;
	private final Class[] identifierSetterMethodParams;

	private final EmbeddedTypeDescriptor componentIdType;

	public SerializableProxy(
			String entityName,
			Class persistentClass,
			Class[] interfaces,
			Object id,
			Boolean readOnly,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			EmbeddedTypeDescriptor componentIdType) {
		super( entityName, id, readOnly );
		this.persistentClass = persistentClass;
		this.interfaces = interfaces;
		if ( getIdentifierMethod != null ) {
			identifierGetterMethodName = getIdentifierMethod.getName();
			identifierGetterMethodClass = getIdentifierMethod.getDeclaringClass();
		}
		else {
			identifierGetterMethodName = null;
			identifierGetterMethodClass = null;
		}

		if ( setIdentifierMethod != null ) {
			identifierSetterMethodName = setIdentifierMethod.getName();
			identifierSetterMethodClass = setIdentifierMethod.getDeclaringClass();
			identifierSetterMethodParams = setIdentifierMethod.getParameterTypes();
		}
		else {
			identifierSetterMethodName = null;
			identifierSetterMethodClass = null;
			identifierSetterMethodParams = null;
		}

		this.componentIdType = componentIdType;
	}

	@Override
	protected String getEntityName() {
		return super.getEntityName();
	}

	@Override
	protected Object getId() {
		return super.getId();
	}

	protected Class getPersistentClass() {
		return persistentClass;
	}

	protected Class[] getInterfaces() {
		return interfaces;
	}

	protected String getIdentifierGetterMethodName() {
		return identifierGetterMethodName;
	}

	protected Class getIdentifierGetterMethodClass() {
		return identifierGetterMethodClass;
	}

	protected String getIdentifierSetterMethodName() {
		return identifierSetterMethodName;
	}

	protected Class getIdentifierSetterMethodClass() {
		return identifierSetterMethodClass;
	}

	protected Class[] getIdentifierSetterMethodParams() {
		return identifierSetterMethodParams;
	}

	protected EmbeddedTypeDescriptor getComponentIdType() {
		return componentIdType;
	}

	private Object readResolve() {
		HibernateProxy proxy = ByteBuddyProxyFactory.deserializeProxy( this );
		setReadOnlyBeforeAttachedToSession( (ByteBuddyInterceptor) proxy.getHibernateLazyInitializer() );
		return proxy;
	}
}
