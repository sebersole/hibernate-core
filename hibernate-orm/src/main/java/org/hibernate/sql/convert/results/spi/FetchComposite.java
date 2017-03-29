/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

/**
 * Models the requested fetching of a composition
 * (component/embeddable), which may or may not be an attribute.
 *
 * @author Steve Ebersole
 */
public interface FetchComposite extends Fetch, FetchParent {
}
