package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessItem {
    private String name;
    private String installPath;
    private String startCommand;

    @Override
    public String toString() {
        return "ProcessItem{name='" + name + "', installPath='" + installPath + "', startCommand='" + startCommand + "'}";
    }
}
