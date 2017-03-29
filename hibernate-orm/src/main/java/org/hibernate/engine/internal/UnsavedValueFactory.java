/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.VersionSupport;

/**
 * Helper for dealing with unsaved value handling
 *
 * @author Gavin King
 */
public class UnsavedValueFactory {

	/**
	 * Instantiate a class using the provided Constructor
	 *
	 * @param constructor The constructor
	 *
	 * @return The instantiated object
	 *
	 * @throws InstantiationException if something went wrong
	 */
	private static Object instantiate(Constructor constructor) {
		try {
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "could not instantiate test object", constructor.getDeclaringClass(), e );
		}
	}
	
	/**
	 * Return an IdentifierValue for the specified unsaved-value. If none is specified, 
	 * guess the unsaved value by instantiating a test instance of the class and
	 * reading it's id property, or if that is not possible, using the java default
	 * value for the type
	 *
	 * @param unsavedValue The mapping defined unsaved value
	 * @param identifierGetter The getter for the entity identifier attribute
	 * @param identifierType The mapping type for the identifier
	 * @param constructor The constructor for the entity
	 *
	 * @return The appropriate IdentifierValue
	 */
	public static IdentifierValue getUnsavedIdentifierValue(
			String unsavedValue,
			Getter identifierGetter,
			Type identifierType,
			Constructor constructor) {
		if ( unsavedValue == null ) {
			if ( identifierGetter != null && constructor != null ) {
				// use the id value of a newly instantiated instance as the unsaved-value
				final Serializable defaultValue = (Serializable) identifierGetter.get( instantiate( constructor ) );
				return new IdentifierValue( defaultValue );
			}
			else if ( identifierGetter != null && ( identifierType.getJavaTypeDescriptor() instanceof Primitive ) ) {
				final Serializable defaultValue = ( (Primitive) identifierType.getJavaTypeDescriptor() ).getDefaultValue();
				return new IdentifierValue( defaultValue );
			}
			else {
				return IdentifierValue.NULL;
			}
		}
		else if ( "null".equals( unsavedValue ) ) {
			return IdentifierValue.NULL;
		}
		else if ( "undefined".equals( unsavedValue ) ) {
			return IdentifierValue.UNDEFINED;
		}
		else if ( "none".equals( unsavedValue ) ) {
			return IdentifierValue.NONE;
		}
		else if ( "any".equals( unsavedValue ) ) {
			return IdentifierValue.ANY;
		}
		else {
			try {
				return new IdentifierValue( (Serializable) identifierType.getJavaTypeDescriptor()
						.fromString( unsavedValue ) );
			}
			catch ( ClassCastException cce ) {
				throw new MappingException( "Bad identifier type: " + identifierType.getName() );
			}
			catch ( Exception e ) {
				throw new MappingException( "Could not parse identifier unsaved-value: " + unsavedValue );
			}
		}
	}

	/**
	 * Return an IdentifierValue for the specified unsaved-value. If none is specified,
	 * guess the unsaved value by instantiating a test instance of the class and
	 * reading it's version property value, or if that is not possible, using the java default
	 * value for the type
	 *
	 * @param versionUnsavedValue The mapping defined unsaved value
	 * @param versionGetter The version attribute getter
	 * @param versionSupport The VersionSupport
	 * @param constructor The constructor for the entity
	 *
	 * @return The appropriate VersionValue
	 */
	public static VersionValue getUnsavedVersionValue(
			String versionUnsavedValue,
			Getter versionGetter,
			VersionSupport versionSupport,
			Constructor constructor) {
		
		if ( versionUnsavedValue == null ) {
			if ( constructor!=null ) {
				final Object defaultValue = versionGetter.get( instantiate( constructor ) );
				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				return versionSupport.isEqual( versionSupport.seed( null ), defaultValue )
						? VersionValue.UNDEFINED
						: new VersionValue( defaultValue );
			}
			else {
				return VersionValue.UNDEFINED;
			}
		}
		else if ( "undefined".equals( versionUnsavedValue ) ) {
			return VersionValue.UNDEFINED;
		}
		else if ( "null".equals( versionUnsavedValue ) ) {
			return VersionValue.NULL;
		}
		else if ( "negative".equals( versionUnsavedValue ) ) {
			return VersionValue.NEGATIVE;
		}
		else {
			// this should not happen since the DTD prevents it
			throw new MappingException( "Could not parse version unsaved-value: " + versionUnsavedValue );
		}
	}

	private UnsavedValueFactory() {
	}
}
