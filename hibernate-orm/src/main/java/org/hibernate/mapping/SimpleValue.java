/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.persistence.AttributeConverter;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.internal.AttributeConverterDescriptorNonAutoApplicableImpl;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BasicTypeResolverConvertibleSupport;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * Any value that maps to columns.
 * @author Gavin King
 */
public class SimpleValue implements KeyValue {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SimpleValue.class );

	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";

	private final MetadataBuildingContext buildingContext;

	private final List<Selectable> columns = new ArrayList<>();

	private final BasicTypeParametersImpl basicTypeParameters = new BasicTypeParametersImpl();

	private String typeName;
	private Properties typeParameters;
	private boolean isNationalized;
	private boolean isLob;

	private Properties identifierGeneratorProperties;
	private String identifierGeneratorStrategy = DEFAULT_ID_GEN_STRATEGY;
	private String nullValue;
	private Table table;
	private String foreignKeyName;
	private String foreignKeyDefinition;
	private boolean alternateUniqueKey;
	private boolean cascadeDeleteEnabled;

	private AttributeConverterDescriptor attributeConverterDescriptor;
	private Type type;
	private BasicTypeResolver basicTypeResolver;

	public SimpleValue(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public SimpleValue(MetadataBuildingContext buildingContext, Table table) {
		this( buildingContext );
		this.table = table;
	}

	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	public AttributeConverterDescriptor getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getBuildingContext().getMetadataCollector().getMetadataBuildingOptions().getServiceRegistry();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public void addColumn(Column column) {
		if ( !columns.contains(column) ) {
			columns.add(column);
		}
		column.setValue(this);
		column.setTypeIndex( columns.size() - 1 );
	}

	public void addFormula(Formula formula) {
		columns.add( formula );
	}

	@Override
	public boolean hasFormula() {
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if (o instanceof Formula) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getColumnSpan() {
		return columns.size();
	}

	@Override
	public Iterator<Selectable> getColumnIterator() {
		return columns.iterator();
	}

	public List getConstraintColumns() {
		return columns;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( AttributeConverterDescriptor.EXPLICIT_TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( AttributeConverterDescriptor.EXPLICIT_TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getBuildingContext().getMetadataCollector().getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			try {
				final Class<AttributeConverter> converterClass = cls.classForName( converterClassName );
				attributeConverterDescriptor = new AttributeConverterDescriptorNonAutoApplicableImpl(
						converterClass.newInstance(),
						getBuildingContext().getBootstrapContext().getTypeConfiguration().getJavaTypeDescriptorRegistry()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}

		this.typeName = typeName;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public void makeLob() {
		this.isLob = true;
	}

	public boolean isLob() {
		return isLob;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	@Override
	public void createForeignKey() throws MappingException {}

	@Override
	public void createForeignKeyOfEntity(String entityName) {
		if ( !hasFormula() && !"none".equals(getForeignKeyName())) {
			ForeignKey fk = table.createForeignKey( getForeignKeyName(), getConstraintColumns(), entityName, getForeignKeyDefinition() );
			fk.setCascadeDeleteEnabled(cascadeDeleteEnabled);
		}
	}

	private IdentifierGenerator identifierGenerator;

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException {

		if ( identifierGenerator != null ) {
			return identifierGenerator;
		}

		Properties params = new Properties();

		//if the hibernate-mapping did not specify a schema/catalog, use the defaults
		//specified by properties - but note that if the schema/catalog were specified
		//in hibernate-mapping, or as params, they will already be initialized and
		//will override the values set here (they are in identifierGeneratorProperties)
		if ( defaultSchema!=null ) {
			params.setProperty(PersistentIdentifierGenerator.SCHEMA, defaultSchema);
		}
		if ( defaultCatalog!=null ) {
			params.setProperty(PersistentIdentifierGenerator.CATALOG, defaultCatalog);
		}

		//pass the entity-name, if not a collection-id
		if (rootClass!=null) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
		}

		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//      splitting it up into schema/catalog/table names
		String tableName = getTable().getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		String columnName = ( (Column) getColumnIterator().next() ).getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		if (rootClass!=null) {
			StringBuilder tables = new StringBuilder();
			Iterator iter = rootClass.getIdentityTables().iterator();
			while ( iter.hasNext() ) {
				Table table= (Table) iter.next();
				tables.append( table.getQuotedName(dialect) );
				if ( iter.hasNext() ) {
					tables.append(", ");
				}
			}
			params.setProperty( PersistentIdentifierGenerator.TABLES, tables.toString() );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
		}

		if (identifierGeneratorProperties!=null) {
			params.putAll(identifierGeneratorProperties);
		}

		// TODO : we should pass along all settings once "config lifecycle" is hashed out...
		final ConfigurationService cs = buildingContext.getMetadataCollector().getMetadataBuildingOptions().getServiceRegistry()
				.getService( ConfigurationService.class );

		params.put(
				AvailableSettings.PREFER_POOLED_VALUES_LO,
				cs.getSetting( AvailableSettings.PREFER_POOLED_VALUES_LO, StandardConverters.BOOLEAN, false )
		);
		if ( cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) != null ) {
			params.put(
					AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER )
			);
		}

		identifierGeneratorFactory.setDialect( dialect );
		identifierGenerator = identifierGeneratorFactory.createIdentifierGenerator( identifierGeneratorStrategy, getType(), params );

		return identifierGenerator;
	}

	public boolean isUpdateable() {
		//needed to satisfy KeyValue
		return true;
	}

	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	public Properties getIdentifierGeneratorProperties() {
		return identifierGeneratorProperties;
	}

	public String getNullValue() {
		return nullValue;
	}

	public Table getTable() {
		return table;
	}

	/**
	 * Returns the identifierGeneratorStrategy.
	 * @return String
	 */
	public String getIdentifierGeneratorStrategy() {
		return identifierGeneratorStrategy;
	}

	public boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory, Dialect dialect) {
		identifierGeneratorFactory.setDialect( dialect );
		return IdentityGenerator.class.isAssignableFrom(identifierGeneratorFactory.getIdentifierGeneratorClass( identifierGeneratorStrategy ));
	}

	/**
	 * Sets the identifierGeneratorProperties.
	 * @param identifierGeneratorProperties The identifierGeneratorProperties to set
	 */
	public void setIdentifierGeneratorProperties(Properties identifierGeneratorProperties) {
		this.identifierGeneratorProperties = identifierGeneratorProperties;
	}

	/**
	 * Sets the identifierGeneratorStrategy.
	 * @param identifierGeneratorStrategy The identifierGeneratorStrategy to set
	 */
	public void setIdentifierGeneratorStrategy(String identifierGeneratorStrategy) {
		this.identifierGeneratorStrategy = identifierGeneratorStrategy;
	}

	/**
	 * Sets the nullValue.
	 * @param nullValue The nullValue to set
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public String getForeignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void setForeignKeyDefinition(String foreignKeyDefinition) {
		this.foreignKeyDefinition = foreignKeyDefinition;
	}

	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}

	public boolean isNullable() {
		Iterator itr = getColumnIterator();
		while ( itr.hasNext() ) {
			final Object selectable = itr.next();
			if ( selectable instanceof Formula ) {
				// if there are *any* formulas, then the Value overall is
				// considered nullable
				return true;
			}
			else if ( !( (Column) selectable ).isNullable() ) {
				// if there is a single non-nullable column, the Value
				// overall is considered non-nullable.
				return false;
			}
		}
		// nullable by default
		return true;
	}

	public boolean isSimpleValue() {
		return true;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getColumnSpan()==getType().getColumnSpan();
	}

	public Type getCurrentType() {
		return type;
	}

	public Type getType() throws MappingException {
		if ( type == null ) {
			if ( basicTypeResolver == null ) {
				throw new MappingException( "Access to Type was requested, but Type is not yet resolved and no BasicTypeProducer was injected" );
			}

			type = basicTypeResolver.resolveBasicType();
		}

		if ( type == null ) {
			String msg = "Could not determine type for: " + typeName;
			if ( table != null ) {
				msg += ", at table: " + table.getName();
			}
			if ( columns != null && columns.size() > 0 ) {
				msg += ", for columns: " + columns;
			}
			throw new MappingException( msg );
		}

		return type;
	}

	public JdbcRecommendedSqlTypeMappingContext makeJdbcRecommendedSqlTypeMappingContext(TypeConfiguration typeConfiguration) {
		return new LocalJdbcRecommendedSqlTypeMappingContext( typeConfiguration );
	}

	private class LocalJdbcRecommendedSqlTypeMappingContext implements JdbcRecommendedSqlTypeMappingContext {
		private final TypeConfiguration typeConfiguration;

		private LocalJdbcRecommendedSqlTypeMappingContext(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public EnumType getEnumeratedType() {
			return EnumType.STRING;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}
	}

	private static class BasicTypeResolverUsingReflection extends BasicTypeResolverConvertibleSupport {
		private final JavaTypeDescriptor javaTypeDescriptor;
		private final SqlTypeDescriptor sqlTypeDescriptor;
		private final boolean isLob;
		private final boolean isNationalized;

		public BasicTypeResolverUsingReflection(
				MetadataBuildingContext buildingContext,
				AttributeConverterDescriptor converterDefinition,
				String className,
				String propertyName,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDefinition );
			this.isLob = isLob;
			this.isNationalized = isNationalized;

			if ( converterDefinition == null ) {
				final Class attributeType = ReflectHelper.reflectedPropertyClass(
						className,
						propertyName,
						buildingContext.getBootstrapContext().getServiceRegistry().getService( ClassLoaderService.class )
				);
				javaTypeDescriptor = buildingContext.getBootstrapContext().getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( attributeType );
				sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType(
						buildingContext.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry().getBaseJdbcRecommendedSqlTypeMappingContext()
				);

			}
			else {
				javaTypeDescriptor = converterDefinition.getDomainType();
				sqlTypeDescriptor = converterDefinition.getJdbcType().getJdbcRecommendedSqlType(
						buildingContext.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry().getBaseJdbcRecommendedSqlTypeMappingContext()
				);
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return (BasicJavaDescriptor) javaTypeDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return sqlTypeDescriptor;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return ConfigurationHelper.getPreferredSqlTypeCodeForBoolean(
					getBuildingContext().getBootstrapContext().getServiceRegistry()
			);
		}
	}


	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if ( basicTypeResolver == null ) {
			// for now throw an exception - not sure yet if this is valid
			//		it would mean (most likely) that annotation binding injected
			//		a BasicTypeResolver earlier and then calling this method (or
			//		something else calls it after).  Throw the exception for now
			//		because I want to see if this happens in reality.
			throw new NotYetImplementedException( "not yet sure this is a valid condition" );
		}
		basicTypeResolver = new BasicTypeResolverUsingReflection(
				buildingContext,
				attributeConverterDescriptor,
				className,
				propertyName,
				isLob,
				isNationalized
		);
	}

	public boolean isTypeSpecified() {
		return typeName != null;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public void copyTypeFrom( SimpleValue sourceValue ) {
		setTypeName( sourceValue.getTypeName() );
		setTypeParameters( sourceValue.getTypeParameters() );

		type = sourceValue.type;
		attributeConverterDescriptor = sourceValue.attributeConverterDescriptor;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + columns.toString() + ')';
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean[] getColumnInsertability() {
		boolean[] result = new boolean[ getColumnSpan() ];
		int i = 0;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Selectable s = (Selectable) iter.next();
			result[i++] = !s.isFormula();
		}
		return result;
	}

	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}

	public void setJpaAttributeConverterDescriptor(AttributeConverterDescriptor attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	private void createParameterImpl() {
		try {
			String[] columnsNames = new String[columns.size()];
			for ( int i = 0; i < columns.size(); i++ ) {
				Selectable column = columns.get(i);
				if (column instanceof Column){
					columnsNames[i] = ((Column) column).getName();
				}
			}

			final XProperty xProperty = (XProperty) typeParameters.get( DynamicParameterizedType.XPROPERTY );
			// todo : not sure this works for handling @MapKeyEnumerated
			final Annotation[] annotations = xProperty == null
					? null
					: xProperty.getAnnotations();

			final ClassLoaderService classLoaderService = getBuildingContext().getBootstrapContext()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			typeParameters.put(
					DynamicParameterizedType.PARAMETER_TYPE,
					new ParameterTypeImpl(
							classLoaderService.classForName(
									typeParameters.getProperty( DynamicParameterizedType.RETURNED_CLASS )
							),
							annotations,
							table.getCatalog(),
							table.getSchema(),
							table.getName(),
							Boolean.valueOf( typeParameters.getProperty( DynamicParameterizedType.IS_PRIMARY_KEY ) ),
							columnsNames
					)
			);
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not create DynamicParameterizedType for type: " + typeName, e );
		}
	}

	public void setBasicTypeResolver(BasicTypeResolver basicTypeResolver) {
		this.basicTypeResolver = basicTypeResolver;
	}

	private static final class ParameterTypeImpl implements DynamicParameterizedType.ParameterType {

		private final Class returnedClass;
		private final Annotation[] annotationsMethod;
		private final String catalog;
		private final String schema;
		private final String table;
		private final boolean primaryKey;
		private final String[] columns;

		private ParameterTypeImpl(
				Class returnedClass,
				Annotation[] annotationsMethod,
				String catalog,
				String schema,
				String table,
				boolean primaryKey,
				String[] columns) {
			this.returnedClass = returnedClass;
			this.annotationsMethod = annotationsMethod;
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.primaryKey = primaryKey;
			this.columns = columns;
		}

		@Override
		public Class getReturnedClass() {
			return returnedClass;
		}

		@Override
		public Annotation[] getAnnotationsMethod() {
			return annotationsMethod;
		}

		@Override
		public String getCatalog() {
			return catalog;
		}

		@Override
		public String getSchema() {
			return schema;
		}

		@Override
		public String getTable() {
			return table;
		}

		@Override
		public boolean isPrimaryKey() {
			return primaryKey;
		}

		@Override
		public String[] getColumns() {
			return columns;
		}
	}

	@SuppressWarnings("unchecked")
	public <J> BasicTypeParameters<J> getBasicTypeParameters() {
		return basicTypeParameters;
	}

	private class BasicTypeParametersImpl implements BasicTypeParameters {
		private BasicJavaDescriptor javaTypeDescriptor;
		private SqlTypeDescriptor sqlTypeDescriptor;
		private MutabilityPlan mutabilityPlan;
		private Comparator comparator;
		private TemporalType temporalPrecision;

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return sqlTypeDescriptor;
		}

		@Override
		public AttributeConverterDefinition getAttributeConverterDefinition() {
			return attributeConverterDescriptor;
		}

		@Override
		public MutabilityPlan getMutabilityPlan() {
			return mutabilityPlan;
		}

		@Override
		public Comparator getComparator() {
			return comparator;
		}

		@Override
		public TemporalType getTemporalPrecision() {
			return temporalPrecision;
		}
	}
}
