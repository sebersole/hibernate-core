/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A one-to-one association that maps to specific formula(s)
 * instead of the primary key column of the owning entity.
 * 
 * @author Gavin King
 */
public class SpecialOneToOneType extends OneToOneType {
	
	public SpecialOneToOneType(
			TypeConfiguration typeConfiguration,
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey, 
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		super(
				typeConfiguration,
				referencedEntityName, 
				foreignKeyType,
				referenceToPrimaryKey, 
				uniqueKeyPropertyName, 
				lazy,
				unwrapProxy,
				entityName, 
				propertyName
			);
	}
	
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return super.getIdentifierOrUniqueKeyType().getColumnSpan();
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		return super.getIdentifierOrUniqueKeyType().sqlTypes();
	}

	@Override
	public ColumnMapping[] getColumnMappings() {
		return getIdentifierOrUniqueKeyType().getColumnMappings();
	}

	public boolean useLHSPrimaryKey() {
		return false;
	}

	@Override
	public Object hydrate(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
	throws HibernateException, SQLException {
		return super.getIdentifierOrUniqueKeyType()
			.nullSafeGet(rs, names, session, owner);
	}
	
	// TODO: copy/paste from ManyToOneType
	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
	throws HibernateException {

		if (value==null) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			Object id = ForeignKeys.getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session );
			if (id==null) {
				throw new AssertionFailure(
						"cannot cache a reference to an object with a null id: " + 
						getAssociatedEntityName() 
				);
			}
			return getIdentifierType(session).disassemble(id, session, owner);
		}
	}

	@Override
	public Object assemble(Serializable oid, SharedSessionContractImplementor session, Object owner)
	throws HibernateException {
		//TODO: currently broken for unique-key references (does not detect
		//      change to unique key property of the associated object)
		Serializable id = (Serializable) getIdentifierType(session).assemble(oid, session, null); //the owner of the association is not the owner of the id

		if (id==null) {
			return null;
		}
		else {
			return resolveIdentifier(id, session);
		}
	}
}
