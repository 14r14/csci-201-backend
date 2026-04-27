package com.csci201.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.csci201.backend.dto.StudentCoursesResponse;
import com.csci201.backend.service.StudentCourseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class StudentCourseServiceIntegrationTest {

    @Autowired StudentCourseService studentCourseService;

    private static final Long ALICE = 1L;
    private static final Long CAROL = 3L;

    @Test
    void getCourses_seedData_legacyCommaSeparated() {
        StudentCoursesResponse res = studentCourseService.getCourses(ALICE);

        assertThat(res.getUserId()).isEqualTo(ALICE);
        assertThat(res.getCourseCodes()).containsExactly("CSCI 201", "CSCI 270");
    }

    @Test
    void setStudentCourses_persistsAsJson_roundTrips() {
        List<String> next = List.of("CSCI-201", "CSCI-350", "MATH-125");
        StudentCoursesResponse updated = studentCourseService.setStudentCourses(ALICE, next);

        assertThat(updated.getCourseCodes()).containsExactly("CSCI-201", "CSCI-350", "MATH-125");

        StudentCoursesResponse read = studentCourseService.getCourses(ALICE);
        assertThat(read.getCourseCodes()).containsExactly("CSCI-201", "CSCI-350", "MATH-125");
    }

    @Test
    void setStudentCourses_deduplicatesCaseInsensitive() {
        StudentCoursesResponse updated =
                studentCourseService.setStudentCourses(ALICE, List.of("CSCI-201", "csci-201", "EE 109"));

        assertThat(updated.getCourseCodes()).containsExactly("CSCI-201", "EE 109");
    }

    @Test
    void setStudentCourses_clearWithEmptyList() {
        studentCourseService.setStudentCourses(ALICE, List.of());

        assertThat(studentCourseService.getCourses(ALICE).getCourseCodes()).isEmpty();
    }

    @Test
    void setStudentCourses_nonStudent_throws() {
        assertThatThrownBy(() -> studentCourseService.setStudentCourses(CAROL, List.of("CSCI-201")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only students");
    }
}
