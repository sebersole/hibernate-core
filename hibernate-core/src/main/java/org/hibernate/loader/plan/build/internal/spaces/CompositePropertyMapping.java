/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * A PropertyMapping for handling composites!  Woohoo!
 * <p/>
 * TODO : Consider moving this into the attribute/association walking SPI (org.hibernate.persister.walking) and
 * having the notion of PropertyMapping built and exposed there
 * <p/>
 * There is duplication here too wrt {@link org.hibernate.hql.internal.ast.tree.ComponentJoin.ComponentPropertyMapping}.
 * like above, consider moving to a singly-defined CompositePropertyMapping in the attribute/association walking SPI
 *
 * @author Steve Ebersole
 */
public class CompositePropertyMapping implements PropertyMapping {
	private final CompositeType compositeType;
	private final PropertyMapping parentPropertyMapping;
	private final String parentPropertyName;

	/**
	 * Builds a CompositePropertyMapping
	 *
	 * @param compositeType The composite being described by this PropertyMapping
	 * @param parentPropertyMapping The PropertyMapping of our parent (composites have to have a parent/owner)
	 * @param parentPropertyName The name of this composite within the parentPropertyMapping
	 */
	public CompositePropertyMapping(
			CompositeType compositeType,
			PropertyMapping parentPropertyMapping,
			String parentPropertyName) {
		this.compositeType = compositeType;
		this.parentPropertyMapping = parentPropertyMapping;
		this.parentPropertyName = parentPropertyName;
	}

	@Override
	public Type toType(String propertyName) throws QueryException {
		return parentPropertyMapping.toType( toParentPropertyPath( propertyName ) );
	}

	/**
	 * Used to build a property path relative to {@link #parentPropertyMapping}.  First, the incoming
	 * propertyName argument is validated (using {@link #checkIncomingPropertyName}).  Then the
	 * relative path is built (using {@link #resolveParentPropertyPath}).
	 *
	 * @param propertyName The incoming propertyName.
	 *
	 * @return The relative path.
	 */
	protected String toParentPropertyPath(String propertyName) {
		checkIncomingPropertyName( propertyName );
		return resolveParentPropertyPath( propertyName );
	}

	/**
	 * Used to check the validity of the propertyName argument passed into {@link #toType(String)},
	 * {@link #toColumns(String, String)} and {@link #toColumns(String)}.
	 *
	 * @param propertyName The incoming propertyName argument to validate
	 */
	protected void checkIncomingPropertyName(String propertyName) {
		if ( propertyName == null ) {
			throw new NullPointerException( "Provided property name cannot be null" );
		}

		//if ( propertyName.contains( "." ) ) {
		//	throw new IllegalArgumentException(
		//			"Provided property name cannot contain paths (dots) [" + propertyName + "]"
		//	);
		//}
	}

	/**
	 * Builds the relative path.  Used to delegate {@link #toType(String)},
	 * {@link #toColumns(String, String)} and {@link #toColumns(String)} calls out to {@link #parentPropertyMapping}.
	 * <p/>
	 * Called from {@link #toParentPropertyPath}.
	 * <p/>
	 * Override this to adjust how the relative property path is built for this mapping.
	 *
	 * @param propertyName The incoming property name to "path append".
	 *
	 * @return The relative path
	 */
	protected String resolveParentPropertyPath(String propertyName) {
		if ( StringHelper.isEmpty( parentPropertyName ) ) {
			return propertyName;
		}
		else {
			return parentPropertyName + '.' + propertyName;
		}
	}

	@Override
	public String[] toColumns(String alias, String propertyName) throws QueryException {
		return parentPropertyMapping.toColumns( alias, toParentPropertyPath( propertyName ) );
	}

	@Override
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
		return parentPropertyMapping.toColumns( toParentPropertyPath( propertyName ) );
	}

}
