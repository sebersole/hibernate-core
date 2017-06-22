/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.util.Locale;

/**
 * Enumeration of the various ways that Hibernate can represent the
 * application's domain model.
 *
 * @see org.hibernate.metamodel.model.domain.rep
 *
 * @author Steve Ebersole
 */
public enum Representation {
	POJO( "pojo" ),
	MAP( "map", "dynamic-map" );

	private final String externalName;
	private final String alternativeExternalName;

	Representation(String externalName) {
		this ( externalName, null );
	}

	Representation(String externalName, String alternativeExternalName) {
		this.externalName = externalName;
		this.alternativeExternalName = alternativeExternalName;
	}

	public String getExternalName() {
		return externalName;
	}

	public static Representation fromExternalName(String externalName) {
		if ( externalName == null ) {
			return POJO;
		}

		if ( MAP.externalName.equalsIgnoreCase( externalName )
				|| MAP.alternativeExternalName.equalsIgnoreCase( externalName ) ) {
			return MAP;
		}

		if ( POJO.externalName.equalsIgnoreCase( externalName ) ) {
			return POJO;
		}

		return valueOf( externalName.toUpperCase( Locale.ROOT ) );
	}
}
