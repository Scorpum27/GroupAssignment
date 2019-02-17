package tcDietlikon;

import java.util.Date;

public class Employee {

	    private String name;
	    private String email;
	    private Date dateOfBirth;
	    private double salary;

	    public Employee(String name, String email, Date dateOfBirth, double salary) {
	        this.name = name;
	        this.email = email;
	        this.dateOfBirth = dateOfBirth;
	        this.salary = salary;
	    }

	
	
	public String getName() {
		// TODO Auto-generated method stub
		return this.name;
	}

	public Date getDateOfBirth() {
		// TODO Auto-generated method stub
		return this.dateOfBirth;
	}

	public String getEmail() {
		// TODO Auto-generated method stub
		return this.email;
	}

	public Double getSalary() {
		// TODO Auto-generated method stub
		return this.salary;
	}

}
