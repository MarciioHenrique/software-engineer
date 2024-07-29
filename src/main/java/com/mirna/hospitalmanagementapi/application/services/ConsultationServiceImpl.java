package com.mirna.hospitalmanagementapi.application.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.FindConsultationByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.SaveConsultationUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.patient.FindPatientByIdUseCase;
import com.mirna.hospitalmanagementapi.domain.dtos.consultation.ConsultationCanceledDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.consultation.ConsultationDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorVerificationDTO;
import com.mirna.hospitalmanagementapi.domain.entities.Consultation;
import com.mirna.hospitalmanagementapi.domain.entities.Doctor;
import com.mirna.hospitalmanagementapi.domain.entities.Patient;
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
	private FindPatientByIdUseCase findPatientById;

	@Autowired
	private DoctorServiceImpl doctorServiceImpl;

	@Autowired
	private PatientServiceImpl patientServiceImpl;

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

		this.patientServiceImpl.isPatientUnactive(patient);

		this.patientServiceImpl.isPatientFreeForThisDate(patient, consultationDTO.consultationDate());

		Doctor doctor = this.doctorServiceImpl.verifyAvailableDoctor(
			new DoctorVerificationDTO(consultationDTO.doctorId(), consultationDTO.consultationDate(), consultationDTO.specialty())
		);

		Consultation consultation = new Consultation(patient, doctor, consultationDTO.consultationDate());

		return saveConsultation.execute(consultation);
	}

	/**
   	* Finds a stored consultation by id.
   	* 
   	* @param consultationId A long representing the consultation's unique identifier
   	* @return The corresponding consultation if successful, or throws an exception if it is non-existent.
   	*/
	@Override
	public Consultation findConsultationById(Long consultationId) {
		Consultation consultation = findConsultationById.execute(consultationId);

		this.verifyConsultationNull(consultation);

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

	public void verifyConsultationNull(Consultation consultation){
		if (this.isConsultationNull(consultation)){
			throw new EntityNotFoundException("No existing consultation with this id");
		}
	
	}



	




}
