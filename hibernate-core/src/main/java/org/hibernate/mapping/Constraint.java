/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;

/**
 * A relational constraint.
 *
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements Serializable {

	private String name;
	private final ArrayList<Column> columns = new ArrayList<>();
	private MappedTable table;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 *
	 * @return String The generated name
	 */
	public static String generateName(String prefix, MappedTable table, Column... columns) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder( "table`" + table.getName() + "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		Column[] alphabeticalColumns = columns.clone();
		Arrays.sort( alphabeticalColumns, ColumnComparator.INSTANCE );
		for ( Column column : alphabeticalColumns ) {
			String columnName = column == null ? "" : column.getName().getText();
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
	}

	/**
	 * Helper method for {@link #generateName(String, MappedTable, Column...)} .
	 *
	 * @return String The generated name
	 */
	public static String generateName(String prefix, MappedTable table, List<Column> columns) {
		return generateName( prefix, table, columns.toArray( new Column[columns.size()] ) );
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 *
	 * @param s The name to be hashed.
	 *
	 * @return String The hased name.
	 */
	public static String hashedName(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( s.getBytes() );
			byte[] digest = md.digest();
			BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch (NoSuchAlgorithmException e) {
			throw new HibernateException( "Unable to generate a hashed Constraint name!", e );
		}
	}

	private static class ColumnComparator implements Comparator<Column> {
		public static ColumnComparator INSTANCE = new ColumnComparator();

		public int compare(Column col1, Column col2) {
			return col1.getName().compareTo( col2.getName() );
		}
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	/**
	 * @deprecated since 6.0, use {@link #addColumns(List)} )}.
	 */
	@Deprecated	public void addColumns(Iterator columnIterator) {
		while ( columnIterator.hasNext() ) {
			Selectable col = (Selectable) columnIterator.next();
			if ( !col.isFormula() ) {
				addColumn( (Column) col );
			}
		}
	}

	public void addColumns(List<MappedColumn> columns) {
		columns.stream()
				.filter( mappedColumn -> !mappedColumn.isFormula() )
				.forEach( column -> addColumn( (Column) column ) );
	}

	/**
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Column getColumn(int i) {
		return columns.get( i );
	}

	//todo duplicated method, remove one
	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public Iterator<Column> columnIterator() {
		return columns.iterator();
	}

	/**
	 * @deprecated since 6.0 use {@link #getMappedTable()}.
	 */
	@Deprecated
	public Table getTable() {
		return (Table) getMappedTable();
	}

	public void setTable(MappedTable table) {
		this.table = table;
	}

	public MappedTable getMappedTable() {
		return table;
	}

	public boolean isGenerated(Dialect dialect) {
		return true;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public String toString() {
		return getClass().getName() + '(' + getMappedTable().getName() + getColumns() + ") as " + name;
	}

	/**
	 * @return String The prefix to use in generated constraint names.  Examples:
	 * "UK_", "FK_", and "PK_".
	 */
	public abstract String generatedConstraintNamePrefix();
}
