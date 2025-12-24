package com.fyp.digitaltwin.repository;

import com.fyp.digitaltwin.model.SimulationResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationResultRepository extends MongoRepository<SimulationResult, String> {
    // We can add custom queries here later, e.g., findByTimestampBetween(...)
}

