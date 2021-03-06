/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimeTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class LiteralHelper {
	public static SqmLiteral<Timestamp> timestampLiteralFrom(String literalText, SqmCreationState creationState) {
		final Timestamp literal = Timestamp.valueOf(
				LocalDateTime.from( JdbcTimestampTypeDescriptor.LITERAL_FORMATTER.parse( literalText ) )
		);

		//noinspection unchecked
		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( Timestamp.class ),
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, SqmCreationState creationState) {
		return integerLiteral( literalText, creationState.getCreationContext().getQueryEngine() );
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, QueryEngine queryEngine) {
		return integerLiteral( Integer.parseInt( literalText ), queryEngine );
	}

	public static SqmLiteral<Integer> integerLiteral(int value, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmLiteral(
				value,
				StandardBasicTypes.INTEGER,
				queryEngine.getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Date> dateLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalDate localDate = LocalDate.from( JdbcDateTypeDescriptor.LITERAL_FORMATTER.parse( literalText ) );
		final Date literal = new Date( localDate.toEpochDay() );

		//noinspection unchecked
		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( Date.class ),
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Time> timeLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalTime localTime = LocalTime.from( JdbcTimeTypeDescriptor.LITERAL_FORMATTER.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );

		//noinspection unchecked
		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getJpaMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( Time.class ),
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}
}
