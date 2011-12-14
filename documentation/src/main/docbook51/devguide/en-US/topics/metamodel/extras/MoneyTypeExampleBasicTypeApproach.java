public class MoneyType implements BasicType {
    public String[] getRegistrationKeys() {
        return new String[] { Money.class.getName() };
    }

	public int[] sqlTypes(Mapping mapping) {
	    // We will simply use delegation to the standard basic types for
	    // BigDecimal and Currency for many of the Type methods...
	    return new int[] {
	             BigDecimalType.INSTANCE.sqlType(),
	             CurrencyType.INSTANCE.sqlType(),
	    };
	    // we could also have honored any registry overrides via...
	    //return new int[] {
	    //         mappings.getTypeResolver().basic( BigDecimal.class.getName() ).sqlTypes( mappings )[0],
	    //         mappings.getTypeResolver().basic( Currency.class.getName() ).sqlTypes( mappings )[0]
	    //};
	}

    public Class getReturnedClass() {
        return Money.class;
    }

    public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
        assert names.length == 2;
		// The standard types already handle null checks
        BigDecimal amount = BigDecimalType.INSTANCE.get( names[0] );
        Currency currency = CurrencyType.INSTANCE.get( names[1] );
        return amount == null && currency == null
                ? null
                : new Money( amount, currency );
    }

    public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws SQLException {
        if ( value == null ) {
            BigDecimalType.INSTANCE.set( st, null, index );
            CurrencyType.INSTANCE.set( st, null, index+1 );
        }
        else {
            final Money money = (Money) value;
            BigDecimalType.INSTANCE.set( st, money.getAmount(), index );
            CurrencyType.INSTANCE.set( st, money.getCurrency(), index+1 );
        }
    }

    ...
}