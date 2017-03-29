/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a class, indicates that all of its properties should be audited.
 * When applied to a field, indicates that this field should be audited.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Tomasz Bech
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Audited {
	/**
	 * Specifies modification store to use
	 */
	ModificationStore modStore() default ModificationStore.FULL;

	/**
	 * Specifies if the entity that is the target of the relation should be audited or not. If not, then when
	 * reading a historic version an audited entity, the relation will always point to the "current" entity.
	 * This is useful for dictionary-like entities, which don't change and don't need to be audited.
	 */
	RelationTargetAuditMode targetAuditMode() default RelationTargetAuditMode.AUDITED;

	/**
	 * Should a modification flag be stored for each property in the annotated class or for the annotated
	 * property. The flag stores information if a property has been changed at a given revision.
	 * This can be used for example in queries.
	 */
	boolean withModifiedFlag() default false;

	/**
	 * The column name of the modified field. Analogous to the name attribute of the @{@link javax.persistence.Column}
	 * annotation. Ignored if withModifiedFlag is false.
	 */
	String modifiedColumnName() default "";
}
