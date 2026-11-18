package com.hospital.records.domain;

public enum AuditAction {
    LOGIN,
    LOGIN_FAILED,
    CREATE,
    VIEW,
    UPDATE,
    DEACTIVATE,
    ACTIVATE,
    RESCHEDULE,
    CANCEL,
    COMPLETE,
    REGISTER,
    REGISTER_FAILED
}
