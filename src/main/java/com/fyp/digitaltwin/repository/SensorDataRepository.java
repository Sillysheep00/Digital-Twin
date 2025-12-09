package com.fyp.digitaltwin.repository;

import com.fyp.digitaltwin.model.SensorData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorDataRepository extends MongoRepository<SensorData, String> {
    // This method allows us to find a record by its timestamp string
    SensorData findByDate(String date);
    
    // To check if we have data
    long count();
}

