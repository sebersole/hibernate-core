/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NativeQueryImplementor<R> extends QueryImplementor<R>, NativeQuery<R> {
	default NamedNativeQueryDescriptor toNamedDescriptor(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	NativeQueryImplementor setCollectionKey(Serializable key);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - NativeQuery

	@Override
	NativeQueryImplementor<R> addScalar(String columnAlias);

	@Override
	NativeQueryImplementor<R> addScalar(String columnAlias, AllowableParameterType type);

	@Override
	RootReturn addRoot(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, String entityName);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, String entityName, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addEntity(Class entityType);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, Class entityType);

	@Override
	NativeQueryImplementor<R> addEntity(String tableAlias, Class entityClass, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addJoin(String tableAlias, String path);

	@Override
	NativeQueryImplementor<R> addJoin(
			String tableAlias,
			String ownerTableAlias,
			String joinPropertyName);

	@Override
	NativeQueryImplementor<R> addJoin(String tableAlias, String path, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQueryImplementor<R> addSynchronizedEntityClass(Class entityClass) throws MappingException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - Query / QueryImplementor

	@Override
	NativeQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	NativeQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	NativeQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	NativeQueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	NativeQueryImplementor<R> setTimeout(int timeout);

	@Override
	NativeQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	NativeQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	NativeQueryImplementor<R> setLockOptions(LockOptions lockOptions);

	@Override
	NativeQueryImplementor<R> setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQueryImplementor<R> setComment(String comment);

	@Override
	NativeQueryImplementor<R> addQueryHint(String hint);

	@Override
	<P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val);

	@Override
	<P> NativeQueryImplementor<R> setParameter(Parameter<P> param, P value);

	@Override
	NativeQueryImplementor<R> setParameter(String name, Object val);

	@Override
	default <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Type type) {
		return setParameter( parameter, val, (AllowableParameterType) type );
	}

	@Override
	<P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, AllowableParameterType type);

	@Override
	NativeQueryImplementor<R> setParameter(int position, Object val);

	@Override
	default NativeQueryImplementor<R> setParameter(String name, Object val, Type type) {
		return setParameter( name, val, (AllowableParameterType) type );
	}

	@Override
	NativeQueryImplementor<R> setParameter(String name, Object val, AllowableParameterType type);

	@Override
	default NativeQueryImplementor<R> setParameter(int position, Object val, Type type) {
		return setParameter( position, val, (AllowableParameterType) type );
	}

	@Override
	NativeQueryImplementor<R> setParameter(int position, Object val, AllowableParameterType type);

	@Override
	<P> NativeQueryImplementor<R> setParameter(
			QueryParameter<P> parameter,
			P val,
			TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(String name, Object val, TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(int position, Object val, TemporalType temporalType);

	@Override
	<P> NativeQueryImplementor<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<P> values);

	@Override
	NativeQueryImplementor<R> setParameterList(String name, Collection values);

	@Override
	default NativeQueryImplementor<R> setParameterList(String name, Collection values, Type type) {
		return setParameterList( name, values, (AllowableParameterType) type );
	}

	@Override
	NativeQueryImplementor<R> setParameterList(String name, Collection values, AllowableParameterType type);

	@Override
	default NativeQueryImplementor<R> setParameterList(String name, Object[] values, Type type) {
		return setParameterList( name, values, (AllowableParameterType) type );
	}

	@Override
	NativeQueryImplementor<R> setParameterList(String name, Object[] values, AllowableParameterType type);

	@Override
	NativeQueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	NativeQueryImplementor<R> setProperties(Object bean);

	@Override
	NativeQueryImplementor<R> setProperties(Map bean);

	@Override
	NativeQueryImplementor<R> setParameter(
			Parameter<Date> param,
			Date value,
			TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(
			Parameter<Calendar> param,
			Calendar value,
			TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(
			String name,
			Calendar value,
			TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(
			int position,
			Calendar value,
			TemporalType temporalType);

	@Override
	NativeQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

}
