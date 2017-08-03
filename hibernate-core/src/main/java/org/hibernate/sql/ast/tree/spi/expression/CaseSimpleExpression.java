/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.results.spi.SqlSelectable;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class CaseSimpleExpression implements Expression, Selectable, SqlSelectable {
	private final ExpressableType type;
	private final Expression fixture;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;

	public CaseSimpleExpression(ExpressableType type, Expression fixture) {
		this.type = type;
		this.fixture = fixture;
	}

	public Expression getFixture() {
		return fixture;
	}

	@Override
	public BasicType getType() {
		return (BasicType) type;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCaseSimpleExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		throw new NotYetImplementedException(  );
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public Expression getOtherwise() {
		return otherwise;
	}

	public void otherwise(Expression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
	}

	public void when(Expression test, Expression result) {
		whenFragments.add( new WhenFragment( test, result ) );
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}

	public static class WhenFragment {
		private final Expression checkValue;
		private final Expression result;

		public WhenFragment(Expression checkValue, Expression result) {
			this.checkValue = checkValue;
			this.result = result;
		}

		public Expression getCheckValue() {
			return checkValue;
		}

		public Expression getResult() {
			return result;
		}
	}
}
