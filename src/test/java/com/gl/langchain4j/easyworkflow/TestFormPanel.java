package com.gl.langchain4j.easyworkflow;

import com.gl.appframework.form.*;
import com.gl.langchain4j.easyworkflow.gui.PlaygroundIcons;
import com.gl.langchain4j.easyworkflow.gui.ToolbarIcons;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFormPanel {
    @Test
    void testBeanForm() {
        Person person = new Person("John", "Doe", new Date(1996 - 1900, Calendar.DECEMBER, 2), 30, true);
        FormPanel formPanel = new FormPanel();

        formPanel.setFormElements(FormElement.getFormElements(Person.class));
        formPanel.toForm(person);

        Person personCopy = new Person();
        formPanel.fromForm(personCopy);

        assertEquals(person, personCopy);
    }

    public static class Person {
        String firstName;
        String lastName;
        Date birthday;
        int age;
        boolean active;
        String gender;

        public Person() {
        }

        public Person(String firstName, String lastName, Date birthday, int age, boolean active) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthday = birthday;
            this.age = age;
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && active == person.active && Objects.equals(firstName, person.firstName) && Objects.equals(lastName, person.lastName) && Objects.equals(birthday, person.birthday) && Objects.equals(gender, person.gender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, birthday, age, active, gender);
        }

        @FormProperty(sortOrder = 0)
        public String getFirstName() {
            return firstName;
        }

        @FormProperty(sortOrder = 1)
        public String getLastName() {
            return lastName;
        }

//        @DateFormProperty(datePattern = "yyyy-MM-dd")
        @FormProperty(elementClass = DateFormElement.class, sortOrder = 2)
        public Date getBirthday() {
            return birthday;
        }

        @FormProperty(sortOrder = 2.5f)
        public int getAge() {
            return age;
        }

        @FormProperty(editorType = FormEditorType.Dropdown, editorChoices = {"Male", "Female", "Other"}, mandatory = true, sortOrder = 3)
        public String getGender() {
            return gender;
        }

        public boolean isActive() {
            return active;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", birthday=" + birthday + '\'' +
                    ", age=" + age + '\'' +
                    ", active=" + active + '\'' +
                    ", gender='" + gender +
                    '}';
        }
    }

    public static class Persons {
        private List<Person> persons;

        @ListFormProperty(elementClass = Person.class, elementDisplayName = "person",
                capabilities = {
                        ListFormProperty.ListCapability.ADD,
                        ListFormProperty.ListCapability.EDIT,
                        ListFormProperty.ListCapability.DELETE,
                        ListFormProperty.ListCapability.REORDER
                })
        @FormProperty(elementClass = ListFormElement.class, icon = "agent")
        public List<Person> getPersons() {
            return persons;
        }

        public void setPersons(List<Person> persons) {
            this.persons = persons;
        }
    }

    public static void main(String[] args) {
        PlaygroundIcons.loadIcons();

        Person person = new Person("John", "Doe", new Date(1996 - 1900, Calendar.DECEMBER, 2), 30, true);
        Person person1 = new Person("Jane", "Smith", new Date(1990 - 1900, Calendar.MAY, 15), 34, false);
        Person person2 = new Person("Bob", "Johnson", new Date(1985 - 1900, Calendar.JULY, 10), 38, true);

        Persons persons = new Persons();
        persons.setPersons(List.of(person, person1));

        FormPanel formPanel = new FormPanel();

        FormDialog<Persons> dialog = new FormDialog<>((JFrame) null, "Edit Persons", Persons.class);
        Persons personCopy = dialog.executeModal(persons);

        System.exit(0);
    }
}
