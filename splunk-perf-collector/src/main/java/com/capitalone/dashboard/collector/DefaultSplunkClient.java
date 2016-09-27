package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.SplunkApplication;
import com.capitalone.dashboard.model.PerformanceMetric;
import com.capitalone.dashboard.util.Supplier;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DefaultSplunkClient implements SplunkClient {
    private static final Log LOG = LogFactory.getLog(DefaultSplunkClient.class);
    private static final String APPLICATION_LIST_PATH = "/controller/rest/applications?output=json";
    private static final String OVERALL_SUFFIX = "Overall Application Performance|*";
    private static final String OVERALL_METRIC_PATH = "/controller/rest/applications/%s/metric-data?metric-path=%s&time-range-type=BEFORE_NOW&duration-in-mins=15&output=json";
    private static final String HEALTH_VIOLATIONS_PATH = "/controller/rest/applications/%s/problems/healthrule-violations?time-range-type=BEFORE_NOW&duration-in-mins=15&output=json";
    private static final String NODE_LIST_PATH = "/controller/rest/applications/%s/nodes?output=json";
    private static final String BUSINESS_TRANSACTION_LIST_PATH = "/controller/rest/applications/%s/business-transactions?output=json";
    private static final String METRIC_PATH_DELIMITER = "\\|";
    private final SplunkSettings settings;
    private final RestOperations rest;

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
        this.rest = restOperationsSupplier.get();
    }

    // join a base url to another path or paths - this will handle trailing or non-trailing /'s
    public static String joinURL(String base, String... paths) throws MalformedURLException {
        StringBuilder result = new StringBuilder(base);
        for (String path : paths) {
            String p = path.replaceFirst("^(\\/)+", "");
            if (result.lastIndexOf("/") != result.length() - 1) {
                result.append('/');
            }
            result.append(p);
        }
        return result.toString();
    }

    /**
     * Retrieves a JSON array of all of the applications that are registered in Splunk.
     *
     * @return Set of applications used to populate the collector_items database. This data is
     * later used by the front end to populate the dropdown list of applications.
     */
    @Override
    public Set<SplunkApplication> getApplications() {
        Set<SplunkApplication> returnSet = new HashSet<>();
        try {
            String url = joinURL(settings.getInstanceUrl(), APPLICATION_LIST_PATH);
            ResponseEntity<String> responseEntity = makeRestCall(url);
            String returnJSON = responseEntity.getBody();
            JSONParser parser = new JSONParser();

            try {
                JSONArray array = (JSONArray) parser.parse(returnJSON);

                for (Object entry : array) {
                    JSONObject jsonEntry = (JSONObject) entry;

                    String appName = getString(jsonEntry, "name");
                    String appId = String.valueOf(getLong(jsonEntry, "id"));
                    String desc = getString(jsonEntry, "description");
                    if (StringUtils.isEmpty(desc)) {
                        desc = appName;
                    }
                    SplunkApplication app = new SplunkApplication();
                    app.setAppID(appId);
                    app.setAppName(appName);
                    app.setAppDesc(desc);
                    app.setDescription(desc);
                    returnSet.add(app);
                }
            } catch (ParseException e) {
                LOG.error("Parsing applications on instance: " + settings.getInstanceUrl(), e);
            }
        } catch (RestClientException rce) {
            LOG.error("client exception loading applications", rce);
            throw rce;
        } catch (MalformedURLException mfe) {
            LOG.error("malformed url for loading applications", mfe);
        }
        return returnSet;
    }

    @Override
    public List<PerformanceMetric> getPerformanceMetrics(SplunkApplication application) {
        return null;
    }

    /**
     * Obtains the relevant data via different Splunk api calls.
     *
     * @param application the current application. Used to provide access to appID/name
     * @return List of PerformanceMetrics used to populate the performance database
55    @Override
    public List<PerformanceMetric> getPerformanceMetrics(SplunkApplication application) {

        return null;
    }


    /**
     * Obtains a list of health violations for the current application from Splunk
     * e.g. /controller/#/location=APP_INCIDENT_LIST&application=<APPID>
     *
     * @param application the current application. Used to provide access to appID/name
     * @return Single element list, value is the raw JSON object of the health violations
     */
    private List<PerformanceMetric> getViolations(SplunkApplication application) {
        List<PerformanceMetric> violationObjects = new ArrayList<>();

        try {
            String url = joinURL(settings.getInstanceUrl(), String.format(HEALTH_VIOLATIONS_PATH, application.getAppID()));
            ResponseEntity<String> responseEntity = makeRestCall(url);
            String returnJSON = responseEntity.getBody();
            JSONParser parser = new JSONParser();

            JSONArray array = (JSONArray) parser.parse(returnJSON);

            PerformanceMetric violationObject = new PerformanceMetric();
            violationObject.setName("Violation Object");

            violationObject.setValue(array);
            violationObjects.add(violationObject);

        } catch (MalformedURLException e) {
            LOG.error("client exception loading applications", e);
        } catch (ParseException e) {
            LOG.error("client exception loading applications", e);
        }

        return violationObjects;

    }


  /*  private List<PerformanceMetric> getSeverityMetrics(SplunkApplication application) {

     /*
            String url = joinURL(settings.getInstanceUrl(), String.format(HEALTH_VIOLATIONS_PATH, application.getAppID()));
            ResponseEntity<String> responseEntity = makeRestCall(url);
            String returnJSON = responseEntity.getBody();
            JSONParser parser = new JSONParser();

            JSONArray array = (JSONArray) parser.parse(returnJSON);



        return severityMetrics;

    }
    */


    protected ResponseEntity<String> makeRestCall(String sUrl) throws MalformedURLException {
        URI thisuri = URI.create(sUrl);
        String userInfo = thisuri.getUserInfo();

        //get userinfo from URI or settings (in spring properties)
        if (StringUtils.isEmpty(userInfo) && (this.settings.getUsername() != null) && (this.settings.getPassword() != null)) {
            userInfo = this.settings.getUsername() + ":" + this.settings.getPassword();
        }
        // Basic Auth only.
        if (StringUtils.isNotEmpty(userInfo)) {
            return rest.exchange(thisuri, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(userInfo)),
                    String.class);
        } else {
            return rest.exchange(thisuri, HttpMethod.GET, null,
                    String.class);
        }

    }

    protected HttpHeaders createHeaders(final String userInfo) {
        byte[] encodedAuth = Base64.encodeBase64(
                userInfo.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return headers;
    }

    private String getString(JSONObject json, String key) {
        return (String) json.get(key);
    }

    private Long getLong(JSONObject json, String key) {
        return (Long) json.get(key);
    }

    private JSONArray getJsonArray(JSONObject json, String key) {
        Object array = json.get(key);
        return array == null ? new JSONArray() : (JSONArray) array;
    }
}
