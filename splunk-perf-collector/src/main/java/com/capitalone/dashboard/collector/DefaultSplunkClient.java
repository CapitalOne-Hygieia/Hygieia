package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.SplunkSearch;
import com.capitalone.dashboard.util.Supplier;
import com.splunk.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class DefaultSplunkClient implements SplunkClient {

    private static final Log LOG = LogFactory.getLog(DefaultSplunkClient.class);
    private final SplunkSettings settings;
    private Service service;

    /* Example .conf file: (could maybe just sync up with hygieiea settings?)
        [Too Many Errors Today]
        # send an email notification
        action.email = 1
        action.email.message.alert = The alert condition for '$name$' in the $app$ fired with $job.resultCount$ error events.
        action.email.to = address@example.com
        action.email.useNSSubject = 1

        alert.suppress = 0
        alert.track = 0

        counttype = number of events
        quantity = 5
        relation = greater than

        # run every day at 14:00
        cron_schedule = 0 14 * * *

        #search for results in the last day
        dispatch.earliest_time = -1d
        dispatch.latest_time = now

        display.events.fields = ["host","source","sourcetype","latitude"]
        display.page.search.mode = verbose
        display.visualizations.charting.chart = area
        display.visualizations.type = mapping

        enableSched = 1

        request.ui_dispatch_app = search
        request.ui_dispatch_view = search
        search = index=_internal " error " NOT debug source=*splunkd.log* earliest=-7d latest=now
        disabled = 1
     */

    @Autowired
    public DefaultSplunkClient(SplunkSettings settings, Supplier<RestOperations> restOperationsSupplier) {
        this.settings = settings;

        //TODO: change credentials obvs
        ServiceArgs loginArgs = new ServiceArgs();
        loginArgs.setUsername("admin");
        loginArgs.setPassword("changeme");
        loginArgs.setHost("localhost");
        loginArgs.setPort(8089);

    // Create a Service instance and log in with the argument map
        service = Service.connect(loginArgs);
    }


    @Override
    public Set<SplunkSearch> getSearches() throws IOException {

        JobExportArgs exportArgs = new JobExportArgs();
        exportArgs.setEarliestTime("rt-1m");    //window of 1 minute from real time
        exportArgs.setLatestTime("rt");
        exportArgs.setSearchMode(JobExportArgs.SearchMode.REALTIME);
        exportArgs.setOutputMode(JobExportArgs.OutputMode.JSON);

        // Run the search with a search query and export arguments
        String mySearch = "search index=_internal"; //todo: read searches from properties file
        InputStream exportSearch = service.export(mySearch, exportArgs);

        // Display results using the SDK's multi-results reader for XML
        MultiResultsReaderXml multiResultsReader = new MultiResultsReaderXml(exportSearch);

        int counter = 0;  // count the number of events
        for (SearchResults searchResults : multiResultsReader)
        {
            for (Event event : searchResults) {
                counter++;
                for (String key: event.keySet())
                    System.out.println("   " + key + ":  " + event.get(key));
            }
        }
        multiResultsReader.close();
    }


}
