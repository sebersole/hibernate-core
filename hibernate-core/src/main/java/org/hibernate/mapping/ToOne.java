/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.ReflectHelper;

/**
 * A simple-point association (ie. a reference to another entity).
 * @author Gavin King
 */
public abstract class ToOne extends SimpleValue implements Fetchable {
	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private boolean lazy = true;
	protected boolean unwrapProxy;
	protected boolean referenceToPrimaryKey = true;

	/**
	 *
	 * @deprecated since 6.0, use {@link #ToOne(MetadataBuildingContext, MappedTable)} instead
	 */
	@Deprecated
	protected ToOne(MetadataBuildingContext metadata, Table table) {
		super( metadata, table );
	}

	protected ToOne(MetadataBuildingContext metadata, MappedTable table) {
		super( metadata, table );
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public abstract void createForeignKey() throws MappingException;

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? 
				null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if (referencedEntityName == null) {
			final ClassLoaderService cls  = getBuildingContext().getBootstrapContext()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			referencedEntityName = ReflectHelper.reflectedPropertyClass( className, propertyName, cls ).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}

	@Override
	public boolean isValid() throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("association must specify the referenced entity");
		}
		return super.isValid( );
	}

	public boolean isLazy() {
		return lazy;
	}
	
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	public void setReferenceToPrimaryKey(boolean referenceToPrimaryKey) {
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}
	
}
