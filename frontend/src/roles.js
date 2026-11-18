export const ROLE_LABEL = {
  ADMIN: 'Administrator',
  DOCTOR: 'Doctor',
  NURSE: 'Nurse',
  RECEPTIONIST: 'Receptionist',
  PATIENT: 'Patient',
}

export const STAFF_ROLES = ['ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST']

// Front desk: registers patients and books appointments.
export const isFrontDesk = (role) => role === 'ADMIN' || role === 'RECEPTIONIST'

// Clinical staff: can open clinical records (doctors read and write, nurses read only).
export const isClinical = (role) => role === 'DOCTOR' || role === 'NURSE'
