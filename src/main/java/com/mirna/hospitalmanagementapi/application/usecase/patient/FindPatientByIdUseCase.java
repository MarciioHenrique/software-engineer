package com.mirna.hospitalmanagementapi.application.usecase.patient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.mirna.hospitalmanagementapi.domain.entities.Patient;
import com.mirna.hospitalmanagementapi.domain.repositories.PatientRepository;

/**
 * This class is used to execute the findById method 
 * 
 * @author Mirna Gama
 * @version 1.0
 */
@Component
public class FindPatientByIdUseCase {

	@Autowired
	private PatientRepository patientRepository;
	
	/**
	 * Executes the findById method from Patient repository
	 * 
	 * @param patientId A long representing the patient's unique identifier
	 * 
	 * @return The corresponding patient if successful, or null if it is non-existent
	 *
	 */
	public Patient execute(Long patientId) {
		return patientRepository.findById(patientId).orElse(null);
	}
}
