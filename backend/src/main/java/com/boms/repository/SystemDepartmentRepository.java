package com.boms.repository;

import com.boms.model.SystemDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemDepartmentRepository extends JpaRepository<SystemDepartment, Long> {
    Optional<SystemDepartment> findByDepartmentId(String departmentId);
}
