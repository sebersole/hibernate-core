package org.hibernate.orm.test.support.domains.gambit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;

/**
 * @author Andrea Boriero
 */
@Entity
public class EntityWithOneToOneJoinTable {
	private Integer id;

	// alphabetical
	private String name;
	private SimpleEntity other;
	private Integer someInteger;

	public EntityWithOneToOneJoinTable() {
	}

	public EntityWithOneToOneJoinTable(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToOne
	@JoinTable(name = "Entity_SimpleEntity")
	public SimpleEntity getOther() {
		return other;
	}

	public void setOther(SimpleEntity other) {
		this.other = other;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}


