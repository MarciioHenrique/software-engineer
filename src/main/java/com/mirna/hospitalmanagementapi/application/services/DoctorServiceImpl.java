package com.mirna.hospitalmanagementapi.application.services;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.SaveDoctorUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.consultation.FindConsultationByDoctorAndDateUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindDoctorByIdUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindDoctorsUseCase;
import com.mirna.hospitalmanagementapi.application.usecase.doctor.FindOneFreeDoctorBySpecialtyUseCase;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorPublicDataDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorUpdatedDataDTO;
import com.mirna.hospitalmanagementapi.domain.dtos.doctor.DoctorVerificationDTO;
import com.mirna.hospitalmanagementapi.domain.entities.Doctor;
import com.mirna.hospitalmanagementapi.domain.enums.Specialty;
import com.mirna.hospitalmanagementapi.domain.exceptions.ConsultationValidationException;
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

	@Autowired
	private FindConsultationByDoctorAndDateUseCase findConsultationByDoctorAndDate;

	@Autowired
	private FindOneFreeDoctorBySpecialtyUseCase findOneFreeDoctorBySpecialty;
	
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

	public Doctor verifyAvailableDoctor(DoctorVerificationDTO doctorVerificationRecord) throws ConsultationValidationException{

		var doctorId = doctorVerificationRecord.doctorId();
		var specialty = doctorVerificationRecord.specialty();
		var consultationDate = doctorVerificationRecord.consultationDate();

		this.doctorNullableVerify(doctorId, specialty);

		var doctor = this.findFreeDoctorForConsultationById(doctorId, consultationDate);

		if (!isDoctorNull(doctor)) {
			return doctor;
		} else {
			return this.findFreeDoctorForConsultationBySpecialty(specialty, consultationDate);
		}

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

	public Doctor findFreeDoctorForConsultationById(Long doctorId, LocalDateTime consultationDate ) throws ConsultationValidationException{
		
		if (doctorId == null) {

			throw new ConsultationValidationException("No doctor's found");
		
		}

		var doctor = findDoctorById.execute(doctorId);

		this.isDoctorActive(doctor);

		this.isDoctorFreeForThisDate(doctor, consultationDate);

		return doctor;

	}

	private void isDoctorActive(Doctor doctor) throws EntityNotFoundException{
		if (!doctor.isActive()){
			throw new EntityNotFoundException("This doctor is not active");	}
	}

	private void isDoctorFreeForThisDate(Doctor doctor, LocalDateTime consultationDate) throws ConsultationValidationException{
		if (findConsultationByDoctorAndDate.execute(doctor.getId(), consultationDate) != null)
			throw new ConsultationValidationException("This doctor is not free on this date");
	}

	private Doctor findFreeDoctorForConsultationBySpecialty(Specialty specialty, LocalDateTime consultationDate ) throws ConsultationValidationException{
		
		if (specialty == null) {

			throw new ConsultationValidationException("No specialties found");
		
		}

		var doctor = findOneFreeDoctorBySpecialty.execute(specialty, consultationDate);

		if (doctor == null) 
			throw new ConsultationValidationException("There is no free doctor for this date with this specialty");

		return doctor;
			
	}


	private void doctorNullableVerify(Long doctorId, Specialty specialty) throws ConsultationValidationException{
		if(this.isDoctorNull(doctorId, specialty)){
			
			throw new ConsultationValidationException("At least the specialty or doctor ID must be filled in");

		}
	}

	private boolean isDoctorNull(Long doctorId, Specialty specialty){
		return doctorId == null && specialty == null;
	}




	

}
