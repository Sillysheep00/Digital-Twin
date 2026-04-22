package com.fyp.digitaltwin.repository;

import com.fyp.digitaltwin.model.SensorData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SensorDataRepository extends MongoRepository<SensorData, String> {
    // find a record by its timestamp string
    SensorData findByDate(String date);
    
    // Get records after the current moment
    List<SensorData> findByDateGreaterThan(String date, Pageable pageable);

    // To check if we have data
    long count();
}
