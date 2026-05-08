package org.example;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Department {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    public Department() {}

    public Department(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId()                       { return id; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public List<Employee> getEmployees()      { return employees; }
}
