package com.mirna.hospitalmanagementapi.application.services;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.SaveDoctorUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindDoctorByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindDoctorsUseCase;
import com.mirna.hospitalmanagementapi.domain.dtos.AddressDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorPublicDataDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorUpdatedDataDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.patient.PatientUpdatedDataDTO;
import com.mirna.hospitalmanagementapi.domain.entities.Address;
import com.mirna.hospitalmanagementapi.domain.entities.Doctor;
import com.mirna.hospitalmanagementapi.domain.entities.Patient;
import com.mirna.hospitalmanagementapi.domain.services.DoctorService;

import jakarta.persistence.EntityNotFoundException;

/**
 * This class is an implementation of the DoctorService interface.
 *
 * This class provides methods to perform operations on doctors
 *
 * @author Mirna Gama
 * @version 1.0
 */

@Service
public class DoctorServiceImpl implements DoctorService {

	@Autowired
	private SaveDoctorUseCase saveDoctor;
	
	@Autowired
	private FindDoctorsUseCase findDoctors;
	
	@Autowired
	private FindDoctorByIdUseCase findDoctorById;
	
	/**
	 * Adds a new doctor to the database.
	 *
	 * @param doctorDTO A data transfer object containing the data for Doctor
	 *                    entity.
	 * @return The saved doctor if successful, or null if there is an error.
	 */
	@Override
	public Doctor addDoctor(DoctorDTO doctorDTO) {
		Doctor doctor = new Doctor(doctorDTO);
		
		return saveDoctor.execute(doctor);
	}
	
	/**
	 * Finds a stored doctor by id.
	 * 
	 * @param doctorId A long representing the doctor's unique identifier
	 * 
	 * @return The corresponding doctor if successful, or throws an
	 *         EntityNotFoundException if it is non-existent.
	 *         
	 * @throws EntityNotFoundException When doctor with id provided is non-existent
	 */
	@Override
	public Doctor findDoctorById(Long doctorId) throws EntityNotFoundException {
		Doctor doctor = findDoctorById.execute(doctorId);
		
		this.verifyIsDoctorNull(doctor);
		
		return doctor;
	}

	/**
	 * Finds doctors from the database.
	 *
	 * @param pageable Pagination information, such as size and page number
	 * 
	 * @return A paginated sublist containing data transfer objects with doctors public information in the repository
	 */
	@Override
	public Page<DoctorPublicDataDTO> findDoctors(Pageable pageable) {
		return findDoctors.execute(pageable).map(DoctorPublicDataDTO::new);
	}

	/**
     * Updates an existing doctor record
     * @param doctorUpdatedDataDTO Data transfer object containing the doctor updated data along with their corresponding id 
	 *  
	 * @return The updated doctor if successful,  or throws an
	 *         EntityNotFoundException if it is non-existent.
	 * @throws EntityNotFoundException When doctor with id provided is non-existent
	 */
	@Override
	public Doctor updateDoctor(DoctorUpdatedDataDTO doctorUpdatedDataDTO) throws EntityNotFoundException {
		Doctor doctor = findDoctorById.execute(doctorUpdatedDataDTO.id());
		
		this.verifyIsDoctorNull(doctor);
			
		doctor = this.doctorDTOValidation(doctorUpdatedDataDTO);
		
		return saveDoctor.execute(doctor);
				
	}

    /**
     * Deactivates an existing doctor record by provided id
     * @param doctorId Long that represents the doctor's unique identifier
	 *  
	 * @return The deactivated doctor if successful, or throws an
	 *         EntityNotFoundException if it is non-existent.
	 *  @throws EntityNotFoundException When doctor with id provided is non-existent 
	 */
	@Override
	public Doctor deactivateDoctor(Long doctorId) throws EntityNotFoundException {
		Doctor doctor = findDoctorById.execute(doctorId);

		this.verifyIsDoctorNull(doctor);
			
		doctor.setActive(false);

		return saveDoctor.execute(doctor);
	}

	private boolean isDoctorNull(Doctor doctor){

		return doctor == null;

	}

	private Doctor doctorDTOValidation(DoctorUpdatedDataDTO doctorDTO){

		var doctor = new Doctor();
		BeanUtils.copyProperties(doctor, doctorDTO);

		return doctor;
	}

	private void verifyIsDoctorNull(Doctor doctor) throws EntityNotFoundException{
		if (this.isDoctorNull(doctor)) {
			throw new EntityNotFoundException("No existing doctor with this id");
		}
	}

	

}
