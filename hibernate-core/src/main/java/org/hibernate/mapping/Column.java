/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.naming.Identifier;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.sql.Template;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import static org.hibernate.mapping.SimpleValue.*;

/**
 * A column of a relational database table
 *
 * @author Gavin King
 */
public class Column implements Selectable, Serializable, Cloneable {
	private Identifier tableName;
	private Identifier name;

	private SqlTypeDescriptor sqlTypeDescriptor;
	private TypeDescriptorResolver typeDescriptorResolver;

	private String sqlType;

	private int uniqueInteger;

	private boolean quoted;

	private Long length;
	private Integer precision;
	private Integer scale;

	private boolean nullable = true;
	private boolean unique;
	private String checkConstraint;
	private String comment;
	private String defaultValue;
	private String customWrite;
	private String customRead;

	public Column(String columnName) {
		setName( Identifier.toIdentifier( columnName ) );
	}

	public Column(Identifier columnName) {
		setName( columnName );
	}

	public Identifier getName() {
		return name;
	}

	public Identifier getTableName(){
		return tableName;
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setTableName(Identifier tableName) {
		this.tableName = tableName;
	}

	public void setName(Identifier columnName) {
		this.name = columnName;
		if ( columnName != null ) {
			this.quoted = columnName.isQuoted();
		}
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}

	public void setUniqueInteger(int uniqueInteger) {
		this.uniqueInteger = uniqueInteger;
	}

	public String getQuotedName() {
		return name.render();
	}

	public String getQuotedName(Dialect d) {
		return name.render( d );
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isUnique() {
		return unique;
	}

	@Override
	public int hashCode() {
		//used also for generation of FK names!
		return isQuoted() ?
				name.hashCode() :
				name.getText().toLowerCase( Locale.ROOT ).hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Column && equals( (Column) object );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	public boolean equals(Column column) {
		if ( null == column ) {
			return false;
		}
		if ( this == column ) {
			return true;
		}

		return name.equals( column.name );
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getName() + ')';
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	@Override
	public String getTemplate(Dialect dialect, SqmFunctionRegistry functionRegistry) {
		return hasCustomRead()
				? Template.renderWhereStringTemplate( customRead, dialect, functionRegistry )
				: Template.TEMPLATE + '.' + name.render( dialect );
	}

	public boolean hasCustomRead() {
		return ( customRead != null && customRead.length() > 0 );
	}

	public String getReadExpr(Dialect dialect) {
		return hasCustomRead() ? customRead : name.render( dialect );
	}

	public String getWriteExpr() {
		return ( customWrite != null && customWrite.length() > 0 ) ? customWrite : "?";
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getText(Dialect dialect) {
		return name.render(dialect);
	}

	@Override
	public String getText() {
		return name.getText();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		if ( sqlTypeDescriptor == null ) {
			sqlTypeDescriptor = typeDescriptorResolver.resolveSqlTypeDescriptor();
		}
		return sqlTypeDescriptor;
	}

	public void setTypeDescriptorResolver(TypeDescriptorResolver typeDescriptorResolver) {
		this.typeDescriptorResolver = typeDescriptorResolver;
	}

	@Override
	public org.hibernate.metamodel.model.relational.spi.PhysicalColumn generateRuntimeColumn(
			org.hibernate.metamodel.model.relational.spi.Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {

		final Identifier physicalName = namingStrategy.toPhysicalColumnName(
				getName(),
				jdbcEnvironment
		);

		final Dialect dialect = jdbcEnvironment.getDialect();
		Size size = new Size.Builder().setLength( getLength() )
				.setPrecision( getPrecision() )
				.setScale( getScale() )
				.build();
		if ( size.getLength() == null
				|| ( size.getScale() == null && size.getPrecision() == null ) ) {
			size = dialect.getDefaultSizeStrategy().resolveDefaultSize(
					getSqlTypeDescriptor(),
					typeDescriptorResolver.resolveJavaTypeDescriptor()
			);
		}

		String columnSqlType = getSqlType();
		if ( columnSqlType == null ) {
			columnSqlType = dialect.getTypeName( getSqlTypeDescriptor().getJdbcTypeCode(), size );
		}

		final PhysicalColumn column = new PhysicalColumn(
				runtimeTable,
				physicalName,
				getSqlTypeDescriptor(),
				getDefaultValue(),
				columnSqlType,
				isNullable(),
				isUnique()
		);
		column.setSize(	size );
		column.setCheckConstraint( getCheckConstraint() );
		return column;
	}

	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer scale) {
		this.precision = scale;
	}

	public Integer getScale() {
		return scale;
	}

	public void setScale(Integer scale) {
		this.scale = scale;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getCustomWrite() {
		return customWrite;
	}

	public void setCustomWrite(String customWrite) {
		this.customWrite = customWrite;
	}

	public String getCustomRead() {
		return customRead;
	}

	public void setCustomRead(String customRead) {
		this.customRead = customRead;
	}

	public String getCanonicalName() {
		return name.getCanonicalName();
	}

	/**
	 * Shallow copy, the value is not copied
	 */
	@Override
	public Column clone() {
		Column copy = new Column( name );
		copy.setTableName( tableName );
		copy.setLength( length );
		copy.setScale( scale );
		copy.setNullable( nullable );
		copy.setPrecision( precision );
		copy.setUnique( unique );
		copy.setSqlType( sqlType );
		copy.setUniqueInteger( uniqueInteger ); //usually useless
		copy.setCheckConstraint( checkConstraint );
		copy.setComment( comment );
		copy.setDefaultValue( defaultValue );
		copy.setCustomRead( customRead );
		copy.setCustomWrite( customWrite );
		copy.setTypeDescriptorResolver( typeDescriptorResolver );
		return copy;
	}

}
