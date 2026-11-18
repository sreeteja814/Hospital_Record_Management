package com.hospital.records.security;

import com.hospital.records.domain.StaffUser;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final StaffUserRepository users;
    private final PatientRepository patients;

    public AppUserDetailsService(StaffUserRepository users, PatientRepository patients) {
        this.users = users;
        this.patients = patients;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        StaffUser user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        // A patient login also stops working the moment the patient record is deactivated.
        boolean patientBlocked = user.getPatientId() != null
                && patients.findById(user.getPatientId()).map(p -> !p.isActive()).orElse(true);

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())          // becomes authority ROLE_ADMIN, ROLE_DOCTOR, ...
                .disabled(!user.isActive() || patientBlocked) // deactivated accounts can no longer log in or use old tokens
                .build();
    }
}
