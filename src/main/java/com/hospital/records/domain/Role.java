package com.hospital.records.domain;

public enum Role {
    ADMIN,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    /** A patient with a portal login. Can only book and manage their own appointments. */
    PATIENT
}
