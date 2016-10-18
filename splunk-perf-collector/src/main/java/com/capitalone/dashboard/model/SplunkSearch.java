package com.capitalone.dashboard.model;


public class SplunkSearch extends CollectorItem {

    private String name = "NA";
    private int count = -1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    //TODO: expand this to include things that aren't just # of events


}
