package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.SplunkSearch;

import java.io.IOException;
import java.util.Set;

public interface SplunkClient {

    Set<SplunkSearch> getSearches() throws IOException;

}