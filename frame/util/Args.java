//Date: 2019/04/05
//Author: dylan
//Desc: 命令行参数解释
//http://jcommander.org/#_lists
//Because life is too short to parse command line parameters

package frame.util;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = { "--site", "-s" }, description = "The siteID  of server contains")
    public Integer siteID = 0;

    @Parameter(names = { "--gate", "-g" }, description = "The gate list of server contains")
    public List<String> gateUrlList = new ArrayList<>();

    @Parameter(names = { "--route", "-r" }, description = "The route list of server contains")
    public List<String> routeUrlList = new ArrayList<>();

    @Parameter(names = { "--monitorUrl", "-m" }, description = "the monitor server url")
    public String monitorUrl = "";

}