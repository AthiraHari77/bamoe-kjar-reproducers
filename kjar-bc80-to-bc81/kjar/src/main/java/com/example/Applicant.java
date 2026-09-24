package com.example;

/**
 * Applicant fact class — used by AgeRule.drl.
 * Must be present for DRL compilation.
 */
public class Applicant {

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
