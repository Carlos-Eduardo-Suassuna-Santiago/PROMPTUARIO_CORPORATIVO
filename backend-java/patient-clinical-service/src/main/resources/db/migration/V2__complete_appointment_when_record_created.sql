-- A medical record finalizes the appointment that it belongs to.
-- The trigger keeps both writes atomic, regardless of which API path creates the record.
CREATE OR REPLACE FUNCTION complete_appointment_after_record()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE appointments
       SET status = 'COMPLETED',
           updated_at = CURRENT_TIMESTAMP
     WHERE id = NEW.appointment_id
       AND status <> 'CANCELLED';
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_complete_appointment_after_record ON medical_records;

CREATE TRIGGER trg_complete_appointment_after_record
AFTER INSERT ON medical_records
FOR EACH ROW
EXECUTE FUNCTION complete_appointment_after_record();
