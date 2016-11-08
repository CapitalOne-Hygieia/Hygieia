package com.capitalone.dashboard.collector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@ConfigurationProperties(prefix = "appdynamics")
public class AppdynamicsSettings {
    private String username;
    private String password;
    private String account;
    private String cron;
    private Integer timeWindow = 15; //default to 15 minutes
    @Value("#{'${instance.urls}'.split(',')}")
    private List<String> instanceUrls;



    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public List<String> getInstanceUrls() {

        return instanceUrls;
    }

    public void setInstanceUrls(List<String> instanceUrls) {
        this.instanceUrls = instanceUrls;
    }

    /**
     * Accessor method for the current chronology setting, for the scheduler
     */
    public String getCron() {
        return cron;
    }

    //TODO: implement users put in own metrics to use

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDashboardUrl(String instanceUrl) {
        String dashboardUrl = instanceUrl + "/controller/#/location=APP_DASHBOARD&timeRange=last_15_minutes.BEFORE_NOW.-1.-1.15&application=%s&dashboardMode=force";
        return dashboardUrl;
    }


    public Integer getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }
}
