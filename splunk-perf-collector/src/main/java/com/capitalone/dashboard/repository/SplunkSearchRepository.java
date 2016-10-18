package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.SplunkSearch;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SplunkSearchRepository extends BaseCollectorItemRepository<SplunkSearch> {


    SplunkSearch findByCollectorIdAndAppName(ObjectId collectorId, String appName);

    SplunkSearch findByCollectorIdAndAppID(ObjectId collectorId, String appID);

    @Query(value="{ 'collectorId' : ?0, 'enabled': true}")
    List<SplunkSearch> findEnabledSplunkApplications(ObjectId collectorId);

    List<SplunkSearch> findByCollectorIdAndEnabled(ObjectId collectorId, boolean enabled);
}
