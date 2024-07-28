package com.mirna.hospitalmanagementapi.application.services;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.FindConsultationByDoctorAndDateUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.FindConsultationByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.FindConsultationByPatientAndDateUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.SaveConsultationUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindDoctorByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindOneFreeDoctorBySpecialtyUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.patient.FindPatientByIdUseCase;
import com.mirna.hospitalmanagementapi.domain.dtos.consultation.ConsultationCanceledDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.consultation.ConsultationDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorVerificationDTO;
import com.mirna.hospitalmanagementapi.domain.entities.Consultation;
import com.mirna.hospitalmanagementapi.domain.entities.Doctor;
import com.mirna.hospitalmanagementapi.domain.entities.Patient;
import com.mirna.hospitalmanagementapi.domain.enums.Specialty;
import com.mirna.hospitalmanagementapi.domain.exceptions.ConsultationValidationException;
import com.mirna.hospitalmanagementapi.domain.services.ConsultationService;

import jakarta.persistence.EntityNotFoundException;

/**
 * This class is an implementation of the ConsultationService interface.
 *
 * This class provides methods to perform operations on consultations
 *
 * @author Mirna Gama
 * @version 1.0
 */
@Service
public class ConsultationServiceImpl implements ConsultationService {

	@Autowired
	private SaveConsultationUseCase saveConsultation;

	@Autowired
	private FindConsultationByIdUseCase findConsultationById;
	
	@Autowired
	private FindConsultationByDoctorAndDateUseCase findConsultationByDoctorAndDate;

	@Autowired
	private FindConsultationByPatientAndDateUseCase findConsultationByPatientAndDate;

	@Autowired
	private FindPatientByIdUseCase findPatientById;

	@Autowired
	private FindDoctorByIdUseCase findDoctorById;

	@Autowired
	private FindOneFreeDoctorBySpecialtyUseCase findOneFreeDoctorBySpecialty;

	/**
	* Adds a new consultation to the repository.
	* 
	* @param consultationDTO A data transfer object representing a consultation to add.
	* @return The saved consultation if successful,  or throws an exception if there is an error.
	 * @throws ConsultationValidationException if there is a validation error
	*/
	@Override
	public Consultation addConsultation(ConsultationDTO consultationDTO) throws ConsultationValidationException {

		Patient patient = findPatientById.execute(consultationDTO.patientId());

		this.isPatientUnactive(patient);

		this.isPatientFreeForThisDate(patient, consultationDTO.consultationDate());

		Doctor doctor = this.doctorVerification(
			new DoctorVerificationDTO(consultationDTO.doctorId(), consultationDTO.consultationDate(), consultationDTO.specialty())
		);

		Consultation consultation = new Consultation(patient, doctor, consultationDTO.consultationDate());

		return saveConsultation.execute(consultation);
	}

	/**
   	* Finds a stored consultation by id.
   	* 
   	* @param id A long representing the consultation's unique identifier
   	* @return The corresponding consultation if successful, or throws an exception if it is non-existent.
   	*/
	@Override
	public Consultation findConsultationById(Long id) {
		Consultation consultation = findConsultationById.execute(id);

		if (this.isConsultationNull(consultation))
			throw new EntityNotFoundException("No existing consultation with this id");
		
		return consultation;
	}
	
	/**
	 * Cancels and updates an existing query in the repository
	 * @param consultationCanceledDTO A data transfer object representing the consultation that will be canceled.
	* @return The canceled consultation if successful,  or throws an exception if there is an error.
	 */
	@Override
	public Consultation cancelConsultation(ConsultationCanceledDTO consultationCanceledDTO) {
		Consultation consultation = this.findConsultationById(consultationCanceledDTO.consultationId());

		consultation.setCanceled(true);
		consultation.setReasonCancellation(consultationCanceledDTO.reasonCancellation());

		return saveConsultation.execute(consultation);
	}

	private boolean isConsultationNull(Consultation consultation){

		return consultation == null;

	}


	public void isPatientUnactive(Patient patient) throws ConsultationValidationException{

		if(!patient.isPatientActive()) this.errorMessageOnNullableDoctor("This patient is not active");
	
	}

	public void isPatientFreeForThisDate(Patient patient, LocalDateTime consultationDate) throws ConsultationValidationException{

		if (findConsultationByPatientAndDate.execute(patient.getId(), consultationDate) != null)
			this.errorMessageOnNullableDoctor("This patient is not free on this date");
	}

	public Doctor doctorVerification(DoctorVerificationDTO doctorVerificationRecord) throws ConsultationValidationException{

		var doctorId = doctorVerificationRecord.doctorId();
		var specialty = doctorVerificationRecord.specialty();
		var consultationDate = doctorVerificationRecord.consultationDate();

		this.doctorNullableVerify(doctorId, specialty);

		var doctor = this.findFreeDoctorForConsultationById(doctorId, consultationDate);

		 if (doctor != null) {

			return doctor;
			
		}

		return this.findFreeDoctorForConsultationBySpecialty(specialty, consultationDate);


	}

	private void doctorNullableVerify(Long doctorId, Specialty specialty) throws ConsultationValidationException{
		if(this.isDoctorNull(doctorId, specialty)){
			
			this.errorMessageOnNullableDoctor("At least the specialty or doctor ID must be filled in");

		}
	}

	private boolean isDoctorNull(Long doctorId, Specialty specialty){
		return doctorId == null && specialty == null;
	}

	private Doctor findFreeDoctorForConsultationById(Long doctorId, LocalDateTime consultationDate ) throws ConsultationValidationException{
		
		if (doctorId == null) {

			this.errorMessageOnNullableDoctor("No doctor's found");
		
		}

		var doctor = findDoctorById.execute(doctorId);

		this.isDoctorActive(doctor);

		this.isDoctorFreeForThisDate(doctor, consultationDate);

		return doctor;

	}

	private Doctor findFreeDoctorForConsultationBySpecialty(Specialty specialty, LocalDateTime consultationDate ) throws ConsultationValidationException{
		
		if (specialty == null) {

			this.errorMessageOnNullableDoctor("No specialties found");
		
		}

		var doctor = findOneFreeDoctorBySpecialty.execute(specialty, consultationDate);

		if (doctor == null) 
			this.errorMessageOnNullableDoctor("There is no free doctor for this date with this specialty");

		return doctor;
			
	}


	private void isDoctorActive(Doctor doctor) throws ConsultationValidationException{
		if (!doctor.getActive()){
			this.errorMessageOnNullableDoctor("This doctor is not active");	}
	}

	private void isDoctorFreeForThisDate(Doctor doctor, LocalDateTime consultationDate) throws ConsultationValidationException{
		if (findConsultationByDoctorAndDate.execute(doctor.getId(), consultationDate) != null)
			this.errorMessageOnNullableDoctor("This doctor is not free on this date");
	}

	public void errorMessageOnNullableDoctor(String message) throws ConsultationValidationException{
		throw new ConsultationValidationException(message);

	}

}
