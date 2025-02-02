package org.example.repository;

import org.example.data_classes.data_base.entity.UsrExtraInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsrExtraInfoRepository extends JpaRepository<UsrExtraInfo, Long> { }
