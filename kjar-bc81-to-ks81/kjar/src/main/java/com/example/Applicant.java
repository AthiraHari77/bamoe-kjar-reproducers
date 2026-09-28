package com.example;

import java.io.Serializable;

/**
 * Applicant — fact class used by AgeRule.drl.
 * Business Central 8.1 generates this class from the Data Object editor.
 */
public class Applicant implements Serializable {

    private static final long serialVersionUID = 1L;

    private int age;

    public Applicant() {}

    public Applicant(int age) {
        this.age = age;
    }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return "Applicant{age=" + age + "}";
    }
}
