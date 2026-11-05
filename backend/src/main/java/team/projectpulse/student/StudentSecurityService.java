package team.projectpulse.student;

import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers ownership and lifecycle-scope questions about a student. The route authorization manager uses
 * {@code isStudentSelf} and {@code isCurrentUserInstructorOfStudentSection}; the student lifecycle service uses
 * {@code isLifecycleManager} after loading the student.
 *
 * <p>The route checks fail closed for missing ids or missing instructor links. Lifecycle authorization runs after
 * the service lookup so an unknown student still receives the use case's not-found response before scope is
 * checked.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    public boolean isLifecycleManager(Student student) {
        if (!this.userUtils.hasRole("ROLE_instructor")) {
            return false;
        }

        Integer callerId = this.userUtils.getUserId();
        if (callerId == null) {
            return false;
        }

        return student.getSection().getCourse().getCourseAdmin().getId().equals(callerId)
                || student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(callerId));
    }

}
