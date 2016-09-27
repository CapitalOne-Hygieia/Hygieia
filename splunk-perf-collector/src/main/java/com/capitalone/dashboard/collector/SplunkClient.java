package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.SplunkApplication;
import com.capitalone.dashboard.model.PerformanceMetric;

import java.util.List;
import java.util.Set;

public interface SplunkClient {

    //List<SplunkApplication> getApplications(String server);
    Set<SplunkApplication> getApplications();

    List<PerformanceMetric> getPerformanceMetrics(SplunkApplication application);
}