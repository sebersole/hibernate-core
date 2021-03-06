/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various.readwriteexpression;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class ColumnTransformerTest extends BaseCoreFunctionalTestCase {
	/**
	 * @implNote Limited to H2 because getting a good expression to use for
	 * {@link Staff#kooky} that works on all databases is challenging and
	 * really what happens on the "database side" here is not relevant -
	 * the issue being tested is how Hibernate applies the table aliases to
	 * column references in the expression.
	 */
	@Test
	@RequiresDialect(H2Dialect.class)
	public void testCustomColumnReadAndWrite() {
		inTransaction( s -> {
			final double HEIGHT_INCHES = 73;
			final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;

			Staff staff = new Staff( HEIGHT_INCHES, HEIGHT_INCHES, HEIGHT_INCHES * 2, 1 );
			s.persist( staff );
			s.flush();

			// Test value conversion during insert
			// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
			// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
			double heightViaSql =
					( (Number) s.createNativeQuery( "select size_in_cm from t_staff where t_staff.id=1" )
							.uniqueResult() )
							.doubleValue();
			assertEquals( HEIGHT_CENTIMETERS, heightViaSql, 0.01d );

			heightViaSql =
					( (Number) s.createNativeQuery( "select radiusS from t_staff where t_staff.id=1" ).uniqueResult() )
							.doubleValue();
			assertEquals( HEIGHT_CENTIMETERS, heightViaSql, 0.01d );

			heightViaSql =
					( (Number) s.createNativeQuery( "select diamet from t_staff where t_staff.id=1" ).uniqueResult() )
							.doubleValue();
			assertEquals( HEIGHT_CENTIMETERS * 2, heightViaSql, 0.01d );

			// Test projection
			Double heightViaHql = (Double) s.createQuery( "select s.sizeInInches from Staff s where s.id = 1" )
					.uniqueResult();
			assertEquals( HEIGHT_INCHES, heightViaHql, 0.01d );

			// Test restriction and entity load via criteria

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Staff> criteria = criteriaBuilder.createQuery( Staff.class );
			Root<Staff> root = criteria.from( Staff.class );
			criteria.where( criteriaBuilder.between(
					root.get( "sizeInInches" ),
					HEIGHT_INCHES - 0.01d,
					HEIGHT_INCHES + 0.01d
			) );
			staff = s.createQuery( criteria ).uniqueResult();


//		staff = (Staff)s.createCriteria(Staff.class)
//			.add( Restrictions.between("sizeInInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//			.uniqueResult();
			assertEquals( HEIGHT_INCHES, staff.getSizeInInches(), 0.01d );

			// Test predicate and entity load via HQL
			staff = (Staff) s.createQuery( "from Staff s where s.sizeInInches between ?1 and ?2" )
					.setParameter( 1, HEIGHT_INCHES - 0.01d )
					.setParameter( 2, HEIGHT_INCHES + 0.01d )
					.uniqueResult();
			assertEquals( HEIGHT_INCHES, staff.getSizeInInches(), 0.01d );

			// Test update
			staff.setSizeInInches( 1 );
			s.flush();
			heightViaSql =
					( (Number) s.createNativeQuery( "select size_in_cm from t_staff where t_staff.id=1" )
							.uniqueResult() )
							.doubleValue();
			assertEquals( 2.54d, heightViaSql, 0.01d );
			s.delete( staff );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Staff.class
		};
	}
}
