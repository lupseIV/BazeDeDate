package org.example;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {

    @Column(nullable = false)
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Double salary;

    @Column(name = "department_id", insertable = false, updatable = false)
    private Long departmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public Employee() {}

    public Employee(String name, String email, Double salary, Department department) {
        this.name       = name;
        this.email      = email;
        this.salary     = salary;
        this.department = department;
    }

    public Long getId()                              { return id; }
    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }
    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }
    public Double getSalary()                        { return salary; }
    public void setSalary(Double salary)             { this.salary = salary; }
    public Long getDepartmentId()                     { return departmentId; }
    public Department getDepartment()                { return department; }
    public void setDepartment(Department department) { this.department = department; }
}
