package com.capitalone.dashboard.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//ConfigurationProperties means to find splunk.properties, and populate these values if specified <3
@Component
@ConfigurationProperties(prefix = "splunk")
public class SplunkSettings {
    private String username;
    private String password;
    private String account;
    private String instanceUrl;
    private String cron;
    private String dashboardUrl;
    private String searchesPath;



    public String getSearchesPath() {
        return searchesPath;
    }

    public void setSearchesPath(String searchesPath) {
        this.searchesPath = searchesPath;
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
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

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }

}
