/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;
import java.io.Serializable;
import java.sql.ResultSet;

import org.hibernate.type.spi.Type;

/**
 * An optional contract for {@link Type} or
 * {@link org.hibernate.usertype.UserType} implementations to handle generated
 * id values any way they see fit as opposed to being limited to the discrete set of
 * numeric types handled by {@link IdentifierGeneratorHelper}
 *
 * @author Steve Ebersole
 */
public interface ResultSetIdentifierConsumer {
	/**
	 * Given a result set, consume/extract the necessary values and construct an
	 * appropriate identifier value.
	 *
	 * @param resultSet The result set containing the value(s) to be used in building
	 * the identifier value.
	 * @return The identifier value.
	 */
	public Serializable consumeIdentifier(ResultSet resultSet);
}
