package com.mirna.hospitalmanagementapi.application.services;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.mirna.hospitalmanagementapi.application.usecase.patient.FindPatientByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.patient.FindPatientsUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.patient.SavePatientUseCase;
import com.mirna.hospitalmanagementapi.domain.dtos.patient.PatientDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.patient.PatientPublicDataDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.patient.PatientUpdatedDataDTO;
import com.mirna.hospitalmanagementapi.domain.entities.Patient;
import com.mirna.hospitalmanagementapi.domain.services.PatientService;

import jakarta.persistence.EntityNotFoundException;

/**
 * This class is an implementation of the PatientService interface.
 *
 * This class provides methods to perform operations on patients
 *
 * @author Mirna Gama
 * @version 1.0
 */
@Service
public class PatientServiceImpl implements PatientService {

	@Autowired
	private SavePatientUseCase savePatient;
	
	@Autowired
	private FindPatientByIdUseCase findPatientById;
	
	@Autowired
	private FindPatientsUseCase findPatients;
	
	/**
	 * Adds a new patient to the database.
	 *
	 * @param patientDTO A data transfer object containing the data for Patient
	 *                    entity.
	 * @return The saved patient if successful, or null if there is an error.
	 */
	@Override
	public Patient addPatient(PatientDTO patientDTO) {
		Patient patient = new Patient(patientDTO);
		
		return this.savePatient.execute(patient);
	}

	/**
	 * Finds a stored patient by id.
	 * 
	 * @param patientId A long representing the patient's unique identifier
	 * @return The corresponding patient if successful, or throws an EntityNotFoundException if it is
	 *         non-existent.
	 * @throws EntityNotFoundException When patient with id provided is non-existent   
	 */
	@Override
	public Patient findPatientById(Long patientId) throws EntityNotFoundException {
		Patient patient = this.findPatientById.execute(patientId);
		
		this.verifyIsPatientNull(patient);
 
		return patient;
	}

	/**
	 * Finds patients from the database.
	 *
	 * @param pageable Pagination information, such as size and page number
	 * 
	 * @return A paginated sublist containing data transfer objects with patients public information in the repository
	 */
	@Override
	public Page<PatientPublicDataDTO> findPatients(Pageable pageable) {
		return findPatients.execute(pageable).map(PatientPublicDataDTO::new);
	}

	 /**
     * Updates an existing patient record
     * @param patientUpdatedDataDTO Data transfer object containing the patient updated data along with their corresponding id 
	 *  
	 * @return The updated patient if successful,  or null if there is an error.
	 */
	@Override
	public Patient updatePatient(PatientUpdatedDataDTO patientUpdatedDataDTO) {
		
		Patient patient = findPatientById.execute(patientUpdatedDataDTO.id());
		
		this.verifyIsPatientNull(patient);

		patient = this.patientDTOValidation(patientUpdatedDataDTO);
		
		return savePatient.execute(patient);

	}

	/**
	 * Deactivates an existing patient record by provided id
	 * 
	 * @param patientId Long that represents the patient's unique identifier
	 * 
	 * @return The deactivated patient if successful, or throws an
	 *         EntityNotFoundException if it is non-existent.
	 * @throws EntityNotFoundException When patient with id provided is non-existent
	 */
	@Override
	public Patient deactivatePatient(Long patientId) throws EntityNotFoundException {
		Patient patient = findPatientById.execute(patientId);

		this.verifyIsPatientNull(patient);
		 
		patient.setActive(false);

		return savePatient.execute(patient);
	}

	private boolean isPatientNull(Patient patient){

		return patient == null;

	}

	private Patient patientDTOValidation(PatientUpdatedDataDTO patientDTO){

		var patient = new Patient();
		BeanUtils.copyProperties(patient, patientDTO);

		return patient;
	}

	private void verifyIsPatientNull(Patient patient)  throws EntityNotFoundException{
		if (this.isPatientNull(patient)) {
			throw new EntityNotFoundException("No existing patient with this id");
		}
	}

}
