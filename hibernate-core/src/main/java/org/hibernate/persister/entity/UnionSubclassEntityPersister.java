/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping
 * strategy for an entity and its inheritence hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassClosure;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final Map subclassByDiscriminatorValue = new HashMap();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//INITIALIZATION:

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " +
							getEntityName()
			);
		}

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Database database = creationContext.getMetadata().getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

		// TABLE

		tableName = determineTableName( persistentClass.getTable(), jdbcEnvironment );

		//Custom SQL

		String sql;
		boolean callable = false;
		ExecuteUpdateResultCheckStyle checkStyle = null;
		sql = persistentClass.getCustomSQLInsert();
		callable = sql != null && persistentClass.isCustomInsertCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLInsertCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLInsertCheckStyle();
		customSQLInsert = new String[] {sql};
		insertCallable = new boolean[] {callable};
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate = new String[] {sql};
		updateCallable = new boolean[] {callable};
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLDeleteCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete = new String[] {sql};
		deleteCallable = new boolean[] {callable};
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// PROPERTIES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator iter = persistentClass.getSubclassIterator();
			int k = 1;
			while ( iter.hasNext() ) {
				Subclass sc = (Subclass) iter.next();
				subclassClosure[k++] = sc.getEntityName();
				subclassByDiscriminatorValue.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}

		//SPACES
		//TODO: i'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}

		HashSet subclassTables = new HashSet();
		iter = persistentClass.getSubclassTableClosureIterator();
		while ( iter.hasNext() ) {
			final Table table = (Table) iter.next();
			subclassTables.add( determineTableName( table, jdbcEnvironment ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() );

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList tableNames = new ArrayList();
			ArrayList keyColumns = new ArrayList();
			if ( !isAbstract() ) {
				tableNames.add( tableName );
				keyColumns.add( getIdentifierColumnNames() );
			}
			iter = persistentClass.getSubclassTableClosureIterator();
			while ( iter.hasNext() ) {
				Table tab = (Table) iter.next();
				if ( !tab.isAbstractUnionTable() ) {
					final String tableName = determineTableName( tab, jdbcEnvironment );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					Iterator citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = ( (Column) citer.next() ).getQuotedName( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] {tableName};
			constraintOrderedKeyColumnNames = new String[][] {getIdentifierColumnNames()};
		}

		initLockers();

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );
	}

	@Override
	public TableGroup createRootTableGroup(
			NavigablePath navigablePath,
			String explicitSourceAlias,
			JoinType tableReferenceJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationContext creationContext) {
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );

		final TableReference tableReference = new TableReference(
				getTableName(),
				sqlAliasBase.generateNewAlias(),
				false,
				getFactory()
		);
		return new UnionTableGroup( navigablePath, tableReference, this );
	}

	@Override
	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	@Override
	public String getRootTableName() {
		return tableName;
	}

	@Override
	public String getTableName() {
		if ( hasSubclasses() ) {
			return subquery;
		}
		else {
			return tableName;
		}
	}

	@Override
	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get( value );
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		if ( accessOptimizer != null ) {
			accessOptimizer.setPropertyValues( object, values );
		}
		else {
			if ( hasSubclasses() ) {
				visitAttributeMappings(
						attribute -> {
							final int stateArrayPosition = ( (StateArrayContributorMapping) attribute ).getStateArrayPosition();
							final Object value = values[stateArrayPosition];
							if ( value != UNFETCHED_PROPERTY ) {
								final Setter setter = attribute.getPropertyAccess().getSetter();
								setter.set( object, value, getFactory() );
							}
						}
				);
			}
			else {
				visitFetchables(
						fetchable -> {
							final AttributeMapping attribute = (AttributeMapping) fetchable;
							final int stateArrayPosition = ( (StateArrayContributorMapping) attribute ).getStateArrayPosition();
							final Object value = values[stateArrayPosition];
							if ( value != UNFETCHED_PROPERTY ) {
								final Setter setter = attribute.getPropertyAccess().getSetter();
								setter.set( object, value, getFactory() );
							}

						},
						null
				);
			}
		}
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	@Override
	protected String getTableName(int j) {
		return tableName;
	}

	@Override
	protected String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	@Override
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	@Override
	protected boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	@Override
	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	public String filterFragment(String name) {
		return hasWhere()
				? " and " + getSQLWhereString( name )
				: "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	@Override
	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(), getDiscriminatorAlias() );
	}

	@Override
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[getPropertySpan()];
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	@Override
	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	@Override
	public boolean isMultiTable() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}


	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		final AtomicInteger tablePositionWrapper = new AtomicInteger(  );

		for ( String tableName : constraintOrderedTableNames ) {
			final int tablePosition = tablePositionWrapper.getAndIncrement();

			consumer.consume(
					tableName,
					() -> columnConsumer -> {
						final String[] keyColumnNames = constraintOrderedKeyColumnNames[tablePosition];
						for ( String column : keyColumnNames ) {
							columnConsumer.accept( column, tableName, null );
						}
					}
			);
		}
	}

	@Override
	protected void buildDiscriminatorMapping() {
		if ( hasSubclasses() ) {
			super.buildDiscriminatorMapping();
		}
	}

	@Override
	public int getTableSpan() {
		return 1;
	}

	@Override
	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[getSubclassColumnClosure().length];
	}

	@Override
	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[getSubclassFormulaClosure().length];
	}

	protected boolean[] getTableHasColumns() {
		return new boolean[] {true};
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	protected String generateSubquery(PersistentClass model, Mapping mapping) {

		Dialect dialect = getFactory().getDialect();
		Settings settings = getFactory().getSettings();

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					dialect,
					settings.getDefaultCatalogName(),
					settings.getDefaultSchemaName()
			);
		}

		HashSet columns = new LinkedHashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) {
					columns.add( citer.next() );
				}
			}
		}

		StringBuilder buf = new StringBuilder()
				.append( "( " );

		Iterator siter = new JoinedIterator(
				new SingletonIterator( model ),
				model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append( "select " );
				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					if ( !table.containsColumn( col ) ) {
						int sqlType = col.getSqlTypeCode( mapping );
						buf.append( dialect.getSelectClauseNullString( sqlType ) )
								.append( " as " );
					}
					buf.append( col.getQuotedName( dialect ) );
					buf.append( ", " );
				}
				buf.append( clazz.getSubclassId() )
						.append( " as clazz_" );
				buf.append( " from " )
						.append(
								table.getQualifiedName(
										dialect,
										settings.getDefaultCatalogName(),
										settings.getDefaultSchemaName()
								)
						);
				buf.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					buf.append( "all " );
				}
			}
		}

		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append( " )" ).toString();
	}

	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	@Override
	public String getSubclassTableName(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	@Override
	public int getSubclassTableSpan() {
		return 1;
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return true;
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}

	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
