package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.PerformanceRepository;
import com.capitalone.dashboard.repository.SplunkCollectorRepository;
import com.capitalone.dashboard.repository.SplunkSearchRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SplunkCollectorTask extends CollectorTask<SplunkCollector> {


    private final SplunkCollectorRepository SplunkCollectorRepository;
    private final SplunkSearchRepository SplunkSearchRepository;
    private final PerformanceRepository performanceRepository; //TODO: performance or nah?
    private final SplunkClient SplunkClient;
    private final SplunkSettings SplunkSettings;




    @Autowired
    public SplunkCollectorTask(TaskScheduler taskScheduler,
                               SplunkCollectorRepository SplunkCollectorRepository,
                               SplunkSearchRepository SplunkSearchRepository,
                               PerformanceRepository performanceRepository,
                               SplunkSettings SplunkSettings,
                               SplunkClient SplunkClient) {
        super(taskScheduler, "Splunk");
        this.SplunkCollectorRepository = SplunkCollectorRepository;
        this.SplunkSearchRepository = SplunkSearchRepository;
        this.performanceRepository = performanceRepository;
        this.SplunkSettings = SplunkSettings;
        this.SplunkClient = SplunkClient;
    }

    @Override
    public SplunkCollector getCollector() {
        return SplunkCollector.prototype(SplunkSettings);
    }

    @Override
    public BaseCollectorRepository<SplunkCollector> getCollectorRepository() {
        return SplunkCollectorRepository;
    }

    @Override
    public String getCron() {
        return SplunkSettings.getCron();
    }

    @Override
    public void collect(SplunkCollector collector) {

        long start = System.currentTimeMillis();
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        List<SplunkSearch> existingApps = SplunkSearchRepository.findByCollectorIdIn(udId);
        List<SplunkSearch> latestProjects = new ArrayList<>();

        logBanner(collector.getInstanceUrl());

        Set<SplunkSearch> apps = SplunkClient.getSearches();
        latestProjects.addAll(apps);

        log("Fetched applications   " + ((apps != null) ? apps.size() : 0), start);

        addNewSearches(apps, existingApps, collector);

        refreshData(enabledApplications(collector));


        log("Finished", start);
    }




    private void refreshData(List<SplunkSearch> apps) {
        long start = System.currentTimeMillis();
        int count = 0;

        for (SplunkSearch app : apps) {
            List<PerformanceMetric> metrics = SplunkClient.getPerformanceMetrics(app);

            if (!CollectionUtils.isEmpty(metrics)) {
                Performance performance = new Performance();
                performance.setCollectorItemId(app.getId());
                performance.setTimestamp(System.currentTimeMillis());
                performance.setType(PerformanceType.ApplicationPerformance);
                performance.getMetrics().addAll(metrics);
                if (isNewPerformanceData(app, performance)) {
                    performanceRepository.save(performance);
                    count++;
                }
            }
        }
        log("Updated", start, count);
    }

    private List<SplunkSearch> enabledApplications(SplunkCollector collector) {
//        return SplunkSearchRepository.findEnabledSplunkSearchs(collector.getId());
        return  SplunkSearchRepository.findByCollectorIdAndEnabled(collector.getId(), true);
    }


    private void addNewSearches(Set<SplunkSearch> allApps, List<SplunkSearch> exisingApps, SplunkCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;
        Set<SplunkSearch> newApps = new HashSet<>();

        for (SplunkSearch app : allApps) {
            if (!exisingApps.contains(app)) {
                app.setCollectorId(collector.getId());
                app.setAppDashboardUrl(String.format(SplunkSettings.getDashboardUrl(),app.getAppID()));
                app.setEnabled(false);
                newApps.add(app);
                count++;
            }
        }
        //save all in one shot
        if (!CollectionUtils.isEmpty(newApps)) {
            SplunkSearchRepository.save(newApps);
        }
        log("New appplications: ", start, count);
    }

    private boolean isNewPerformanceData(SplunkSearch SplunkSearch, Performance performance) {
        return performanceRepository.findByCollectorItemIdAndTimestamp(
                SplunkSearch.getId(), performance.getTimestamp()) == null;
    }
}
