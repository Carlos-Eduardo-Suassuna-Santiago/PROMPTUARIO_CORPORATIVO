"""
HTTP routers for the unified Patient-Clinical service.

The routers are kept internally separated by bounded context so the original
services can be rolled back at any time:

- ``patient_routers``  → patients, allergies, vaccines, medications, documents
- ``clinical_routers`` → appointments, schedules, medical records,
  prescriptions, exams and certificates
"""
