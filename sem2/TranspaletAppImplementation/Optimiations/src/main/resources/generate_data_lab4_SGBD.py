import csv
import random

NO_DEPARTMENTS = 20

def generate_employee_data(filename="employees.csv", num_records=10000):
    header = ['id', 'name', 'email', 'department_id', 'salary']
    
    with open(filename, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(header)
        
        for i in range(1, num_records + 1):
            name = f"Employee_{i}"
            email = f"user_{i}@example.com"
            dept_id = random.randint(1, NO_DEPARTMENTS)
            salary = random.randint(30000, 120000)
            
            writer.writerow([i, name, email, dept_id, salary])
            
    print(f"Succes! S-a generat fișierul '{filename}' cu {num_records} înregistrări.")

def generate_departments_data(filename="departments.csv", num_departments=NO_DEPARTMENTS):
    header = ['id', 'name']
    
    with open(filename, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(header)
        
        for i in range(1, num_departments + 1):
            name = f"Department_{i}"
            writer.writerow([i, name])

    print(f"Succes! S-a generat fișierul '{filename}' cu {num_departments} înregistrări.")


generate_employee_data()
generate_departments_data()