package com.fresherradar.repository;

import com.fresherradar.entity.CollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    List<CollectionRun> findTop20ByOrderByStartedAtDesc();

    @Query("select max(r.finishedAt) from CollectionRun r where r.status = 'SUCCESS'")
    Instant findLastSuccessfulFinish();
}
