/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Map;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.Type;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private String identifierTypeName;
	private String metaTypeName = "string";
	private Map<Object,String> metaValueToEntityNameMap;
	private boolean lazy = true;

	/**
	 * @deprecated Use {@link Any#Any(MetadataBuildingContext, Table)} instead.
	 */
	@Deprecated
	public Any(MetadataImplementor metadata, Table table) {
		super( metadata, table );
	}

	public Any(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public Type getType() throws MappingException {
		final Type metaType = getMetadata().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( metaTypeName );
		final Type identifierType = getMetadata().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( identifierTypeName );

		return MappingHelper.anyMapping(
				metaType,
				identifierType,
				metaValueToEntityNameMap,
				isLazy(),
				getBuildingContext()
		);
	}

	public void setTypeByReflection(String propertyClass, String propertyName) {}

	public String getMetaType() {
		return metaTypeName;
	}

	public void setMetaType(String type) {
		metaTypeName = type;
	}

	public Map getMetaValues() {
		return metaValueToEntityNameMap;
	}

	public void setMetaValues(Map metaValueToEntityNameMap) {
		//noinspection unchecked
		this.metaValueToEntityNameMap = metaValueToEntityNameMap;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Any && isSame( (Any) other );
	}

	public boolean isSame(Any other) {
		return super.isSame( other )
				&& Objects.equals( identifierTypeName, other.identifierTypeName )
				&& Objects.equals( metaTypeName, other.metaTypeName )
				&& Objects.equals( metaValueToEntityNameMap, other.metaValueToEntityNameMap )
				&& lazy == other.lazy;
	}
}
