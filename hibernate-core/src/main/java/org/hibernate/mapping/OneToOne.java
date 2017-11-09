/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A one-to-one association mapping
 * @author Gavin King
 */
public class OneToOne extends ToOne {

	private boolean constrained;
	private ForeignKeyDirection foreignKeyType;
	private KeyValue identifier;
	private String propertyName;
	private String entityName;

	/**
	 *
	 * @deprecated since 6.0, use {@link #OneToOne(MetadataBuildingContext, MappedTable, PersistentClass)} instead
	 */
	@Deprecated
	public OneToOne(MetadataBuildingContext metadata, Table table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();
	}

	public OneToOne(MetadataBuildingContext metadata, MappedTable table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ? null : propertyName.intern();
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String propertyName) {
		this.entityName = entityName==null ? null : entityName.intern();
	}

	public void createForeignKey() throws MappingException {
		if ( constrained && referencedPropertyName==null) {
			//TODO: handle the case of a foreign key to something other than the pk
			createForeignKeyOfEntity( getReferencedEntityName() );
		}
	}

	@Override
	protected void setTypeDescriptorResolver(Column column) {
		column.setTypeDescriptorResolver( new OneToOneTypeDescriptorResolverImpl( columns.size() - 1 ) );
	}

	public class OneToOneTypeDescriptorResolverImpl implements TypeDescriptorResolver {

		private int index;

		public OneToOneTypeDescriptorResolverImpl(int index) {
			this.index = index;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			final List<Selectable> mappedColumns = getMappedColumns();
			if ( mappedColumns.size() == 0 ) {
				throw new IllegalStateException( "No SqlType code to resolve for " + entityName );

			}
			final PersistentClass referencedPersistentClass = getMetadataBuildingContext()
					.getMetadataCollector()
					.getEntityBinding( getReferencedEntityName() );

			if ( referenceToPrimaryKey || referencedPropertyName == null ) {
				return ( (Column) referencedPersistentClass.getIdentifier()
						.getMappedColumns()
						.get( index ) ).getSqlTypeDescriptor();
			}
			else {
				final Property referencedProperty = referencedPersistentClass.getReferencedProperty(
						getReferencedPropertyName() );
				return ( (Column) referencedProperty.getValue()
						.getMappedColumns().get( index ) ).getSqlTypeDescriptor();
			}
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return getJavaTypeDescriptor();
		}
	}

	public java.util.List<Selectable> getConstraintColumns() {
		final ArrayList<Selectable> list = new ArrayList();
		identifier.getMappedColumns().forEach( o -> list.add( (Selectable) o ) );
		return list;
	}
	/**
	 * Returns the constrained.
	 * @return boolean
	 */
	public boolean isConstrained() {
		return constrained;
	}

	/**
	 * Returns the foreignKeyType.
	 * @return AssociationType.ForeignKeyType
	 */
	public ForeignKeyDirection getForeignKeyType() {
		return foreignKeyType;
	}

	/**
	 * Returns the identifier.
	 * @return Value
	 */
	public KeyValue getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the constrained.
	 * @param constrained The constrained to set
	 */
	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}

	/**
	 * Sets the foreignKeyType.
	 * @param foreignKeyType The foreignKeyType to set
	 */
	public void setForeignKeyType(ForeignKeyDirection foreignKeyType) {
		this.foreignKeyType = foreignKeyType;
	}

	/**
	 * Sets the identifier.
	 * @param identifier The identifier to set
	 */
	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public boolean isNullable() {
		return !constrained;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		final PersistentClass referencedPersistentClass = getMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityBinding( getReferencedEntityName() );

		if ( referenceToPrimaryKey || referencedPropertyName == null ) {
			return referencedPersistentClass.getIdentifier().getJavaTypeDescriptor();
		}
		else {
			return referencedPersistentClass.getReferencedProperty( getReferencedPropertyName() )
					.getValue()
					.getJavaTypeDescriptor();
		}
	}
}
