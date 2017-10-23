/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.metamodel.model.relational.spi.DerivedTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;

import org.jboss.logging.Logger;

/**
 * A relational table
 *
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class Table implements MappedTable, Serializable {
	private static final Logger log = Logger.getLogger( Table.class );

	private Identifier catalog;
	private Identifier schema;
	private Identifier name;

	/**
	 * contains all columns, including the primary key
	 */
	private Map<String, MappedColumn> columns = new LinkedHashMap();
	private KeyValue idValue;
	private MappedPrimaryKey primaryKey;
	private Map<ForeignKeyKey, ForeignKey> foreignKeyMap = new LinkedHashMap<>();
	private Map<String, MappedIndex> indexes = new LinkedHashMap<>();
	private Map<String,UniqueKey> uniqueKeys = new LinkedHashMap<>();
	private int uniqueInteger;
	private List<String> checkConstraints = new ArrayList<>();
	private String rowId;
	private String subselect;
	private boolean isAbstract;
	private boolean hasDenormalizedTables;
	private String comment;

	private boolean isPhysicalTable;

	private List<InitCommand> initCommands;

	public Table() {
	}

	public Table(String name) {
		setName( name );
	}

	public Table(
			MappedNamespace namespace,
			Identifier tableName,
			boolean isAbstract) {
		this.catalog = namespace.getCatalogName();
		this.schema = namespace.getSchemaName();
		this.name = tableName;
		this.isAbstract = isAbstract;
	}

	public Table(
			Identifier catalog,
			Identifier schema,
			Identifier tableName,
			boolean isAbstract) {
		this.catalog = catalog;
		this.schema = schema;
		this.name = tableName;
		this.isAbstract = isAbstract;
	}

	public Table(MappedNamespace namespace, Identifier tableName, String subselect, boolean isAbstract) {
		this.catalog = namespace.getCatalogName();
		this.schema = namespace.getSchemaName();
		this.name = tableName;
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	public Table(MappedNamespace namespace, String subselect, boolean isAbstract) {
		this.catalog = namespace.getCatalogName();
		this.schema = namespace.getSchemaName();
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	/**
	 * @deprecated Should use {@link QualifiedObjectNameFormatter#format} on QualifiedObjectNameFormatter
	 * obtained from {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment}
	 */
	@Deprecated
	public String getQualifiedName(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( subselect != null ) {
			return "( " + subselect + " )";
		}
		String quotedName = getQuotedName( dialect );
		String usedSchema = schema == null ?
				defaultSchema :
				getQuotedSchema( dialect );
		String usedCatalog = catalog == null ?
				defaultCatalog :
				getQuotedCatalog( dialect );
		return qualify( usedCatalog, usedSchema, quotedName );
	}

	/**
	 * @deprecated Should use {@link QualifiedObjectNameFormatter#format} on QualifiedObjectNameFormatter
	 * obtained from {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment}
	 */
	@Deprecated
	public static String qualify(String catalog, String schema, String table) {
		StringBuilder qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema ).append( '.' );
		}
		return qualifiedName.append( table ).toString();
	}

	@Override
	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	@Override
	public String getName() {
		return name == null ? null : name.getText();
	}

	@Override
	public Identifier getNameIdentifier() {
		return name;
	}

	@Override
	public String getQuotedName() {
		return name == null ? null : name.toString();
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public QualifiedTableName getQualifiedTableName() {
		return name == null ? null : new QualifiedTableName( catalog, schema, name );
	}

	public boolean isQuoted() {
		return name.isQuoted();
	}

	public void setQuoted(boolean quoted) {
		if ( quoted == name.isQuoted() ) {
			return;
		}
		this.name = new Identifier( name.getText(), quoted );
	}

	public void setSchema(String schema) {
		this.schema = Identifier.toIdentifier( schema );
	}

	@Override
	public String getSchema() {
		return schema == null ? null : schema.getText();
	}

	public String getQuotedSchema(Dialect dialect) {
		return schema == null ? null : schema.render( dialect );
	}

	@Override
	public boolean isSchemaQuoted() {
		return schema != null && schema.isQuoted();
	}

	public void setCatalog(String catalog) {
		this.catalog = Identifier.toIdentifier( catalog );
	}

	@Override
	public String getCatalog() {
		return catalog == null ? null : catalog.getText();
	}

	public String getQuotedCatalog(Dialect dialect) {
		return catalog == null ? null : catalog.render( dialect );
	}

	@Override
	public boolean isCatalogQuoted() {
		return catalog != null && catalog.isQuoted();
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with atleast a name.
	 * @return the underlying column or null if not inside this table. Note: the instance *can* be different than the input parameter, but the name will be the same.
	 */
	@Override
	public Column getColumn(Column column) {
		if ( column == null ) {
			return null;
		}

		Column myColumn = (Column) columns.get( column.getCanonicalName() );

		return column.equals( myColumn ) ?
				myColumn :
				null;
	}

	@Override
	public Column getColumn(Identifier name) {
		if ( name == null ) {
			return null;
		}

		return (Column) columns.get( name.getCanonicalName() );
	}

	@Override
	public Column getColumn(int n) {
		Iterator iter = columns.values().iterator();
		for ( int i = 0; i < n - 1; i++ ) {
			iter.next();
		}
		return (Column) iter.next();
	}

	@Override
	public void addColumn(Column column) {
		Column old = getColumn( column );
		if ( old == null ) {
			if ( primaryKey != null ) {
				for ( Column c : primaryKey.getColumns() ) {
					if ( c.getCanonicalName().equals( column.getCanonicalName() ) ) {
						column.setNullable( false );
						log.debugf(
								"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
								column.getCanonicalName(),
								getNameIdentifier().getCanonicalName()
						);
					}
				}
			}
			this.columns.put( column.getCanonicalName(), column );
			column.setUniqueInteger( this.columns.size() );
		}
		else {
			column.setUniqueInteger( old.getUniqueInteger() );
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	@Override
	public Iterator getColumnIterator() {
		return columns.values().iterator();
	}

	@Override
	public Iterator<MappedIndex> getIndexIterator() {
		return indexes.values().iterator();
	}

	public Map<ForeignKeyKey, ForeignKey> getForeignKeyMap() {
		return Collections.unmodifiableMap( foreignKeyMap );
	}

	public Iterator getForeignKeyIterator() {
		return foreignKeyMap.values().iterator();
	}

	public java.util.Collection<ForeignKey> getForeignKeys() {
		return Collections.unmodifiableCollection( foreignKeyMap.values() );
	}

	@Override
	public Iterator<UniqueKey> getUniqueKeyIterator() {
		return getUniqueKeys().values().iterator();
	}

	Map<String, UniqueKey> getUniqueKeys() {
		cleanseUniqueKeyMapIfNeeded();
		return uniqueKeys;
	}

	private int sizeOfUniqueKeyMapOnLastCleanse;

	private void cleanseUniqueKeyMapIfNeeded() {
		if ( uniqueKeys.size() == sizeOfUniqueKeyMapOnLastCleanse ) {
			// nothing to do
			return;
		}
		cleanseUniqueKeyMap();
		sizeOfUniqueKeyMapOnLastCleanse = uniqueKeys.size();
	}

	private void cleanseUniqueKeyMap() {
		// We need to account for a few conditions here...
		// 	1) If there are multiple unique keys contained in the uniqueKeys Map, we need to deduplicate
		// 		any sharing the same columns as other defined unique keys; this is needed for the annotation
		// 		processor since it creates unique constraints automagically for the user
		//	2) Remove any unique keys that share the same columns as the primary key; again, this is
		//		needed for the annotation processor to handle @Id @OneToOne cases.  In such cases the
		//		unique key is unnecessary because a primary key is already unique by definition.  We handle
		//		this case specifically because some databases fail if you try to apply a unique key to
		//		the primary key columns which causes schema export to fail in these cases.
		if ( uniqueKeys.isEmpty() ) {
			// nothing to do
			return;
		}
		else if ( uniqueKeys.size() == 1 ) {
			// we have to worry about condition 2 above, but not condition 1
			final Map.Entry<String,UniqueKey> uniqueKeyEntry = uniqueKeys.entrySet().iterator().next();
			if ( isSameAsPrimaryKeyColumns( uniqueKeyEntry.getValue() ) ) {
				uniqueKeys.remove( uniqueKeyEntry.getKey() );
			}
		}
		else {
			// we have to check both conditions 1 and 2
			final Iterator<Map.Entry<String,UniqueKey>> uniqueKeyEntries = uniqueKeys.entrySet().iterator();
			while ( uniqueKeyEntries.hasNext() ) {
				final Map.Entry<String,UniqueKey> uniqueKeyEntry = uniqueKeyEntries.next();
				final UniqueKey uniqueKey = uniqueKeyEntry.getValue();
				boolean removeIt = false;

				// condition 1 : check against other unique keys
				for ( UniqueKey otherUniqueKey : uniqueKeys.values() ) {
					// make sure its not the same unique key
					if ( uniqueKeyEntry.getValue() == otherUniqueKey ) {
						continue;
					}
					if ( otherUniqueKey.getColumns().containsAll( uniqueKey.getColumns() )
							&& uniqueKey.getColumns().containsAll( otherUniqueKey.getColumns() ) ) {
						removeIt = true;
						break;
					}
				}

				// condition 2 : check against pk
				if ( isSameAsPrimaryKeyColumns( uniqueKeyEntry.getValue() ) ) {
					removeIt = true;
				}

				if ( removeIt ) {
					//uniqueKeys.remove( uniqueKeyEntry.getKey() );
					uniqueKeyEntries.remove();
				}
			}

		}
	}

	private boolean isSameAsPrimaryKeyColumns(UniqueKey uniqueKey) {
		if ( primaryKey == null || ! primaryKey.columnIterator().hasNext() ) {
			// happens for many-to-many tables
			return false;
		}
		return primaryKey.getColumns().containsAll( uniqueKey.getColumns() )
				&& uniqueKey.getColumns().containsAll( primaryKey.getColumns() );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Table && equals((Table) object);
	}

	public boolean equals(Table table) {
		if (null == table) {
			return false;
		}
		if (this == table) {
			return true;
		}

		return Identifier.areEqual( name, table.name )
				&& Identifier.areEqual( schema, table.schema )
				&& Identifier.areEqual( catalog, table.catalog );
	}

	@Override
	public boolean hasPrimaryKey() {
		return getPrimaryKey() != null;
	}

	@Override
	public MappedPrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	@Override
	public void setPrimaryKey(MappedPrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Override
	public MappedIndex getOrCreateIndex(String indexName) {

		MappedIndex index =  indexes.get( indexName );

		if ( index == null ) {
			index = new MappedIndex();
			index.setName( indexName );
			index.setTable( this );
			indexes.put( indexName, index );
		}

		return index;
	}

	public MappedIndex getIndex(String indexName) {
		return  indexes.get( indexName );
	}

	public UniqueKey addUniqueKey(UniqueKey uniqueKey) {
		UniqueKey current = uniqueKeys.get( uniqueKey.getName() );
		if ( current != null ) {
			throw new MappingException( "UniqueKey " + uniqueKey.getName() + " already exists!" );
		}
		uniqueKeys.put( uniqueKey.getName(), uniqueKey );
		return uniqueKey;
	}

	public UniqueKey createUniqueKey(List keyColumns) {
		String keyName = Constraint.generateName( "UK_", this, keyColumns );
		UniqueKey uk = getOrCreateUniqueKey( keyName );
		uk.addColumns( keyColumns.iterator() );
		return uk;
	}

	public UniqueKey getUniqueKey(String keyName) {
		return uniqueKeys.get( keyName );
	}

	@Override
	public UniqueKey getOrCreateUniqueKey(String keyName) {
		UniqueKey uk = uniqueKeys.get( keyName );

		if ( uk == null ) {
			uk = new UniqueKey();
			uk.setName( keyName );
			uk.setTable( this );
			uniqueKeys.put( keyName, uk );
		}
		return uk;
	}

	@Override
	public void createForeignKeys() {
	}

	@Override
	public ForeignKey createForeignKey(
			String keyName,
			List keyColumns,
			String referencedEntityName,
			String keyDefinition) {
		return createForeignKey( keyName, keyColumns, referencedEntityName, keyDefinition, null );
	}

	public ForeignKey createForeignKey(
			String keyName,
			List keyColumns,
			String referencedEntityName,
			String keyDefinition,
			List referencedColumns) {
		final ForeignKeyKey key = new ForeignKeyKey( keyColumns, referencedEntityName, referencedColumns );

		ForeignKey fk = foreignKeyMap.get( key );
		if ( fk == null ) {
			fk = new ForeignKey();
			fk.setTable( this );
			fk.setReferencedEntityName( referencedEntityName );
			fk.setKeyDefinition(keyDefinition);
			fk.addColumns( keyColumns.iterator() );
			if ( referencedColumns != null ) {
				fk.addReferencedColumns( referencedColumns.iterator() );
			}

			// NOTE : if the name is null, we will generate an implicit name during second pass processing
			// afterQuery we know the referenced table name (which might not be resolved yet).
			fk.setName( keyName );

			foreignKeyMap.put( key, fk );
		}

		if ( keyName != null ) {
			fk.setName( keyName );
		}

		return fk;
	}


	// This must be done outside of Table, rather than statically, to ensure
	// deterministic alias names.  See HHH-2448.
	@Override
	public void setUniqueInteger(int uniqueInteger) {
		this.uniqueInteger = uniqueInteger;
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}

	public void setIdentifierValue(KeyValue idValue) {
		this.idValue = idValue;
	}

	@Override
	public KeyValue getIdentifierValue() {
		return idValue;
	}

	@Override
	public void addCheckConstraint(String constraint) {
		checkConstraints.add( constraint );
	}

	@Override
	public boolean containsColumn(Column column) {
		return columns.containsValue( column );
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder().append( getClass().getName() )
				.append( '(' );
		if ( getCatalog() != null ) {
			buf.append( getCatalog() ).append( "." );
		}
		if ( getSchema() != null ) {
			buf.append( getSchema() ).append( "." );
		}
		buf.append( getName() ).append( ')' );
		return buf.toString();
	}

	@Override
	public String toLoggableString() {
		return toString();
	}

	@Override
	public String getSubselect() {
		return subselect;
	}

	@Override
	public void setSubselect(String subselect) {
		this.subselect = subselect;
	}

	public boolean isSubselect() {
		return subselect != null;
	}

	public boolean isAbstractUnionTable() {
		return hasDenormalizedTables() && isAbstract;
	}

	public boolean hasDenormalizedTables() {
		return hasDenormalizedTables;
	}

	@Override
	public void setHasDenormalizedTables() {
		hasDenormalizedTables = true;
	}

	@Override
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean isPhysicalTable() {
		return !isSubselect() && !isAbstractUnionTable();
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @deprecated since 6.0, use {@link #getCheckConstraints()}.
	 */
	@Deprecated
	public Iterator<String> getCheckConstraintsIterator() {
		return checkConstraints.iterator();
	}

	@Override
	public List<String> getCheckConstraints() {
		return Collections.unmodifiableList( checkConstraints );
	}

	@Override
	public boolean isExportable() {
		return isPhysicalTable();
	}

	private String render(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}


	public static class ForeignKeyKey implements Serializable {
		String referencedClassName;
		List columns;
		List referencedColumns;

		ForeignKeyKey(List columns, String referencedClassName, List referencedColumns) {
			this.referencedClassName = referencedClassName;
			this.columns = new ArrayList();
			this.columns.addAll( columns );
			if ( referencedColumns != null ) {
				this.referencedColumns = new ArrayList();
				this.referencedColumns.addAll( referencedColumns );
			}
			else {
				this.referencedColumns = Collections.EMPTY_LIST;
			}
		}

		public int hashCode() {
			return columns.hashCode() + referencedColumns.hashCode();
		}

		public boolean equals(Object other) {
			ForeignKeyKey fkk = (ForeignKeyKey) other;
			return fkk != null && fkk.columns.equals( columns ) && fkk.referencedColumns.equals( referencedColumns );
		}

		@Override
		public String toString() {
			return "ForeignKeyKey{" +
					"columns=" + StringHelper.join( ",", columns ) +
					", referencedClassName='" + referencedClassName + '\'' +
					", referencedColumns=" + StringHelper.join( ",", referencedColumns ) +
					'}';
		}
	}

	@Override
	public void addInitCommand(InitCommand command) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<>();
		}
		initCommands.add( command );
	}

	@Override
	public List<InitCommand> getInitCommands() {
		if ( initCommands == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( initCommands );
		}
	}

	@Override
	public Set<MappedColumn> getMappedColumns() {
		return Collections.unmodifiableSet( new HashSet<>( columns.values() ) );
	}

	@Override
	public String getUid() {
		// todo (6.0) what makes snese to impl this??
		throw new NotYetImplementedException();
	}

	@Override
	public InflightTable generateRuntimeTable(
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment,
			IdentifierGeneratorFactory identifierGeneratorFactory) {

		if ( getSubselect() != null ) {
			return new DerivedTable( getSubselect(), isAbstract() );
		}

		final PhysicalTable table = new PhysicalTable(
				catalog,
				schema,
				name,
				isAbstract(),
				hasPrimaryKey(),
				getComment(),
				namingStrategy,
				jdbcEnvironment
		);

		if ( hasPrimaryKey() && getIdentifierValue() != null && getIdentifierValue().isIdentityColumn(
				identifierGeneratorFactory ) ) {
			table.setPrimaryKeyIdentity( true );
		}
		table.setCheckConstraints( getCheckConstraints() );

		for ( MappedIndex index : indexes.values() ) {
			table.addIndex( index.generateRuntimeIndex( table, namingStrategy, jdbcEnvironment ) );
		}

		return table;
	}
}
