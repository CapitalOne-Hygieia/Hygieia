package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.SplunkApplication;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SplunkApplicationRepository extends BaseCollectorItemRepository<SplunkApplication> {


    SplunkApplication findByCollectorIdAndAppName(ObjectId collectorId, String appName);

    SplunkApplication findByCollectorIdAndAppID(ObjectId collectorId, String appID);

    @Query(value="{ 'collectorId' : ?0, 'enabled': true}")
    List<SplunkApplication> findEnabledSplunkApplications(ObjectId collectorId);

    List<SplunkApplication> findByCollectorIdAndEnabled(ObjectId collectorId, boolean enabled);
}
