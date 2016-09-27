package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.SplunkApplication;
import com.capitalone.dashboard.model.SplunkCollector;
import com.capitalone.dashboard.model.Performance;
import com.capitalone.dashboard.model.PerformanceMetric;
import com.capitalone.dashboard.model.PerformanceType;
import com.capitalone.dashboard.repository.SplunkApplicationRepository;
import com.capitalone.dashboard.repository.SplunkCollectorRepository;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.PerformanceRepository;
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
    private final SplunkApplicationRepository SplunkApplicationRepository;
    private final PerformanceRepository performanceRepository;
    private final SplunkClient SplunkClient;
    private final SplunkSettings SplunkSettings;




    @Autowired
    public SplunkCollectorTask(TaskScheduler taskScheduler,
                               SplunkCollectorRepository SplunkCollectorRepository,
                               SplunkApplicationRepository SplunkApplicationRepository,
                               PerformanceRepository performanceRepository,
                               SplunkSettings SplunkSettings,
                               SplunkClient SplunkClient) {
        super(taskScheduler, "Splunk");
        this.SplunkCollectorRepository = SplunkCollectorRepository;
        this.SplunkApplicationRepository = SplunkApplicationRepository;
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
        List<SplunkApplication> existingApps = SplunkApplicationRepository.findByCollectorIdIn(udId);
        List<SplunkApplication> latestProjects = new ArrayList<>();

        logBanner(collector.getInstanceUrl());

        Set<SplunkApplication> apps = SplunkClient.getApplications();
        latestProjects.addAll(apps);

        log("Fetched applications   " + ((apps != null) ? apps.size() : 0), start);

        addNewProjects(apps, existingApps, collector);

        refreshData(enabledApplications(collector));


        log("Finished", start);
    }




    private void refreshData(List<SplunkApplication> apps) {
        long start = System.currentTimeMillis();
        int count = 0;

        for (SplunkApplication app : apps) {
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

    private List<SplunkApplication> enabledApplications(SplunkCollector collector) {
//        return SplunkApplicationRepository.findEnabledSplunkApplications(collector.getId());
        return  SplunkApplicationRepository.findByCollectorIdAndEnabled(collector.getId(), true);
    }


    private void addNewProjects(Set<SplunkApplication> allApps, List<SplunkApplication> exisingApps, SplunkCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;
        Set<SplunkApplication> newApps = new HashSet<>();

        for (SplunkApplication app : allApps) {
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
            SplunkApplicationRepository.save(newApps);
        }
        log("New appplications: ", start, count);
    }

    private boolean isNewPerformanceData(SplunkApplication SplunkApplication, Performance performance) {
        return performanceRepository.findByCollectorItemIdAndTimestamp(
                SplunkApplication.getId(), performance.getTimestamp()) == null;
    }
}
