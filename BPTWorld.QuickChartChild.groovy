/**
 *  **************** Quick Chart Child App  ****************
 *
 *  Design Usage:
 *  Chart your data, quickly and easily. Display your charts in any dashboard.
 *
 *  Copyright 2022 Bryan Turcotte (@bptworld)
 * 
 *  This App is free. If you like and use this app, please be sure to mention it on the Hubitat forums! Thanks.
 *
 *  Remember...I am not a professional programmer, everything I do takes a lot of time and research!
 *  Donations are never necessary but always appreciated. Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://paypal.me/bptworld
 * 
 *  Unless noted in the code, ALL code contained within this app is mine. You are free to change, ripout, copy, modify or
 *  otherwise use the code in anyway you want. This is a hobby, I'm more than happy to share what I have learned and help
 *  the community grow. Have FUN with it!
 * 
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @BPTWorld
 *
 *  App and Driver updates can be found at https://github.com/bptworld/Hubitat/
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 *  0.2.4 - 08/09/22 - One step forward...two steps back
 *  0.2.3 - 08/09/22 - Added more data checks
 *  0.2.2 - 08/09/22 - Major change by @JustinL - See thread for details
 *  0.2.1 - 08/08/22 - Added some logging to see what's going on
 *  0.2.0 - 08/08/22 - Fixed a typo
 *  ---
 *  0.0.1 - 07/12/22 - Initial release.
 */

#include BPTWorld.bpt-normalStuff

def setVersion(){
    state.name = "Quick Chart"
	state.version = "0.2.4"
    sendLocationEvent(name: "updateVersionInfo", value: "${state.name}:${state.version}")
}

definition(
    name: "Quick Chart Child",
    namespace: "BPTWorld",
    author: "Bryan Turcotte",
    description: "Chart your data, quickly and easily. Display your charts in any dashboard.",
    category: "Convenience",
	parent: "BPTWorld:Quick Chart",
    oauth: [displayName: "Quick Chart", displayLink: ""],
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "",
)

mappings
{
    path("/quickChart/:appId") { action: [ GET: "fetchChart"] }
}

def getChartEndpoint() {
    return getFullApiServerUrl() + "/quickChart/${app.id}?access_token=${state.accessToken}"    
}

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
		display() 
        section("${getImage('instructions')} <b>Instructions:</b>", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
    		paragraph "Chart your data, quickly and easily. Display your charts in any dashboard."
		}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" App Control")) {
            appControlSection()
        }
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Virtual Device")) {
            createDeviceSection("Quick Chart Driver")           
            paragraph "Since the virtual device gets deleted if this child app is deleted, each child app needs to have a unique virtual device to store the chart."
        }
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Chart Options")) {
            input "gType", "enum", title: "Chart Style", options: ["bar","line", "horizontalBar","stateTiming","radar","pie","doughnut","polar","scatter","bubble","radialGauge","violin","sparkline","progressBar",""], submitOnChange:true, width:6, required: true
            input "theChartTitle", "text", title: "Chart Title", submitOnChange:true, width:6            
            input "bkgrdColor", "text", title: "Background Color", defaultValue:"white", submitOnChange:false, width: 4
            input "gridColor", "text", title: "Grid Color", defaultValue:"black", submitOnChange:false, width: 4
            input "labelColor", "text", title: "Label Color", defaultValue:"black", submitOnChange:false, width: 4
            input "showDevInAtt", "bool", title: "Show Device Name with Attribute on Axes", defaultValue:false, submitOnChange:true, width: 6
            input "onChartValueLabels", "bool", title: "Show Attribute Values as On-Chart Labels", defaultValue:false, submitOnChange:false, width: 6
            input "displayLegend", "bool", title: "Show Legend", defaultValue:true, submitOnChange:false, width: 6

            paragraph "<hr>"
            input "dataSource", "bool", title: "Get data from file (off) OR from device event history (on)", defaultValue:false, submitOnChange:true
            if(dataSource) {        // Event History
                paragraph "<b>Using Device History</b><br>"
                input "theDevice", "capability.*", title: "Select the Device(s)", multiple:true, submitOnChange:true, required: true
                if(theDevice) {
                    labelOptions = []
                    allAttrs = []
                    attTypes = [:]
                    theDevice.each { dev ->
                        labelOptions << dev.displayName
                        attributes = dev.supportedAttributes
                        attributes.each { att ->
                            allAttrs << att.name
                            attTypes[att.name] = att.getDataType()
                        }
                    }
                    devAtt = allAttrs.unique().sort()
                    input "theAtt", "enum", title: "Select the Attribute(s)<br><small>Attributes must be either all numbers or all non-numbers.</small>", options: devAtt, multiple:true, submitOnChange:true, required: true
                    
                    def anyNonNumber = false
                    def anyNumber = false
                    theAtt.each { att ->                        
                        theType = attTypes[att]
                        if (theType.toLowerCase() == "number") anyNumber = true
                        else anyNonNumber = true
                    }                    
                    if(logEnable) log.debug "Detected attribute type: ${attType}"
                    if (anyNumber && anyNonNumber) paragraph "*Warning: Selected attributes are not all numbers or all non-numbers as required*"
                    else if (anyNumber && !anyNonNumber) state.isNumericalData = true
                    else if (!anyNumber && anyNonNumber) state.isNumericalData = false
                    
                    input "labelDev", "enum", title: "Select 'Label' Device (X-Axis)", options: labelOptions, multiple:false, submitOnChange:true
                    dataType = "rawdata"
                }
            } else {
                paragraph "<b>Using Data File</b><br><small>Data files are made using the 'Quick Chart Data Collector'</small>"
                input "hubSecurity", "bool", title: "Using Hub Security", defaultValue:false, submitOnChange:true
                if(hubSecurity) {
                    input "hubUsername", "string", title: "Hub Security username", submitOnChange:true
                    input "hubPassword", "password", title: "Hub Security password", submitOnChange:true
                } else {
                    app.removeSetting("hubUsername")
                    app.removeSetting("hubPassword")
                }
                getFileList()
                if(fileList) {
                    input "fName", "enum", title: "Select a File", options: fileList, multiple:false, submitOnChange:true, required: true
                    if(fileList) {
                        getTheDevices()
                        if(deviceLabels) {
                            input "labelDev", "enum", title: "Select 'Label' Device (X-Axis)", options: deviceLabels, multiple:false, submitOnChange:true, required: true
                        }
                    }
                } else {
                    paragraph "If file list is empty. Be sure to enter in your Hub Security crediantials and then flip this switch."
                    input "getList", "bool", title: "Get List", defaultValue:false, submitOnChange:true
                    if(getList) {
                        app.updateSetting("getList",[value:"false",type:"bool"])
                    }
                }
                
                def dataTypeOptions = []
                if (gType != "stateTiming") {
                    dataTypeOptions = [
                        ["rawdata":"Raw Data Over Time - Temp,Humidity,etc."],
                        ["duration":"How long things have been on/open/active/etc per xx"]
                    ]    
                }
                else dataTypeOptions = [["rawdata":"Raw Data Over Time - Temp,Humidity,etc."]]       // force charting raw data if chart type is stateTiming          
                input "dataType", "enum", title: "Select the type of data to store", options: dataTypeOptions, submitOnChange:true, required: true
            }
            
            paragraph "<hr>"
            if(dataType == "rawdata") {
                input "theDays", "enum", title: "Select Days to Chart<br><small>* Remember to check how many data points are saved, per attribute, for device selected.</small>", multiple:false, required:true, options: [
                    ["999":"Today"],
                    ["1":"+ 1 Day"],
                    ["2":"+ 2 Days"],
                    ["3":"+ 3 Days"],
                    ["4":"+ 4 Days"],
                    ["5":"+ 5 Days"],
                    ["6":"+ 6 Days"],
                    ["7":"+ 7 Days"]
                ], defaultValue:"99", submitOnChange:true
                if (state.isNumericalData) input "decimals", "enum", title: "Number of Decimal places", options: ["None","1","2"], defaultValue:"None", submitOnChange:true                
            } else if(dataType == "duration") {
                input "theDays", "enum", title: "Select How to Chart", multiple:false, required:true, options: [
                    ["999":"Every Event"],
                    ["1":"Daily Total - 1 Day"],
                    ["2":"Daily Total - 2 Days"],
                    ["3":"Daily Total - 3 Days"],
                    ["4":"Daily Total - 4 Days"],
                    ["5":"Daily Total - 5 Days"],
                    ["6":"Daily Total - 6 Days"],
                    ["7":"Daily Total - 7 Days"]
                ], defaultValue:"7", submitOnChange:true
                input "decimals", "enum", title: "Number of Decimal places", options: ["None","1","2"], defaultValue:"None", submitOnChange:true, width:6
                input "secMin", "bool", title: "Chart using Seconds (off) or Minutes (on)", defaultValue:false, submitOnChange:true, width:6
            }
            input "updateTime", "enum", title: "When to Update", options: [
                ["manual":"Manual"],
                ["realTime":"Real Time"],
                ["5min":"Every 5 Minutes"],
                ["10min":"Every 10 Minutes"],
                ["15min":"Every 15 Minutes"],
                ["30min":"Every 30 Minutes"],
                ["1hour":"Every 1 Hour"],
                ["3hour":"Every 3 Hours"]
            ], defaultValue:"manual", submitOnChange:true 
            
            if (gType != "stateTiming") input "reverseMap", "bool", title: "Reverse Map Output", defaultValue:false, submitOnChange:true
            // force reverseMap for stateTiming

            paragraph "<hr>"
            
            def inputWidth = gType != "stateTiming" ? 4 : 6
            
            input "displayXAxis", "bool", title: "Show X-Axis", defaultValue:true, submitOnChange:false, width: inputWidth
            input "displayXAxisGrid", "bool", title: "Show X-Axis Gridlines", defaultValue:true, submitOnChange:false, width: inputWidth
            if (gType != "stateTiming") input "stackXAxis", "bool", title: "Stack X-Axis Data", defaultValue:false, submitOnChange:false, width: 4            
            
            input "displayYAxis", "bool", title: "Show Y-Axis", defaultValue:true, submitOnChange:false, width: inputWidth
            input "displayYAxisGrid", "bool", title: "Show Y-Axis Gridlines", defaultValue:true, submitOnChange:false, width: inputWidth
            if (gType != "stateTiming") input "stackYAxis", "bool", title: "Stack Y-Axis Data", defaultValue:false, submitOnChange:false, width: 4

            if(dataType == "rawdata" && (gType == "stateTiming" || state.isNumericalData == false)) {
                input "xAxisAnchor", "enum", title: "X-Axis Terminates", options: [
                    ["dataEnd":"At End of Data"],
                    ["dayEnd":"At End of Day"]
                ], defaultValue:"dataEnd", submitOnChange:false, width: 4
                
                if (!customizeStateColors) paragraph "<small><b>Using Default State Colors</b><br>Green: active, open, locked, present, on, open, true, dry<br>Red: inactive, closed, unlocked, not present, off, closed, false, wet</small>"
                input "customizeStateColors", "bool", title: "Customize State Colors?", defaultValue:false, submitOnChange:true, width: 12
                if (customizeStateColors) {    
                    input "numStates", "number", title: "How many states?", defaultValue:2, submitOnChange:true, width: 12
                    paragraph "<small>State Color is set to the color specified for whatever state matches first</small>"
                    if (!numStates) app.updateSetting("numStates",[type:"number",value:2]) 
                    if (numStates) {
                        for (i=1; i <= numStates; i++) {
                            input "state${i}", "text", title: "State ${i}", submitOnChange:false, width: 6
                            input "state${i}Color", "text", title: "Color", submitOnChange:false, width: 6
                        }
                    }                   
                }
            }             
            
            paragraph "<hr>"
        }
    
        section() {
            input "makeGraph", "bool", title: "Manually Trigger a Chart", defaultValue:false, submitOnChange:true
            if(makeGraph) {
                if(dataSource) {    // Device Event History
                    getEventsHandler()
                } else {            // File Data
                    getEventsHandler()
                }
                app.updateSetting("makeGraph",[value:"false",type:"bool"])
            }
            if(buildChart) {
                paragraph "${buildChart}"
            } else {
                paragraph ""
            }
        }
        
        section(getFormat("header-green", "${getImage("Blank")}"+" General")) {
            appGeneralSection()
        }
		display2()
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {	
    if(logEnable) log.debug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
    if(logEnable && logOffTime == "1 Hour") runIn(3600, logsOff, [overwrite:false])
    if(logEnable && logOffTime == "2 Hours") runIn(7200, logsOff, [overwrite:false])
    if(logEnable && logOffTime == "3 Hours") runIn(10800, logsOff, [overwrite:false])
    if(logEnable && logOffTime == "4 Hours") runIn(14400, logsOff, [overwrite:false])
    if(logEnable && logOffTime == "5 Hours") runIn(18000, logsOff, [overwrite:false])
    if(logEnagle && logOffTime == "Keep On") unschedule(logsOff)
	initialize()
}

def initialize() {
    if(!state.accessToken) createAccessToken()
    checkEnableHandler()
    if(pauseApp) {
        log.info "${app.label} is Paused"
    } else {
        if(updateTime == "realTime") {
            if(theDevice && theAtt) {
                theDevice.each { td ->
                    theAtt.each { ta ->
                        subscribe(td, ta, getEventsHandler)
                    }
                }
            }
        } else if(updateTime == "5min") {
            runEvery5Minutes(getEventsHandler)
        } else if(updateTime == "10min") {
            runEvery10Minutes(getEventsHandler) 
        } else if(updateTime == "15min") {
            runEvery15Minutes(getEventsHandler)
        } else if(updateTime == "30min") {
            runEvery30Minutes(getEventsHandler)
        } else if(updateTime == "1hour") {
            runEvery1Hour(getEventsHandler)
        } else if(updateTime == "3hour") {
            runEvery3Hours(getEventsHandler)
        }
    }
}

def getEventsHandler(evt) {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "----------------------------------------------- Start Quick Chart -----------------------------------------------"
        if(dataSource) {
            if(logEnable) log.debug "In getEventsHandler (${state.version}) - Event History"
            events = []
            def today = new Date().clearTime()
            if(theDays) {
                if(theDays == "999") {
                    days = today
                } else {
                    days = today - theDays.toInteger()
                }
                if (gType == "stateTiming" || state.isNumericalData == false) days = days - 1 // if building state timing chart, retrieve states for the previous day as well, so that can determine the state at 12:00:00 AM on the first day of the chart

                if(logEnable) log.debug "In getEventsHandler - theDevice: ${theDevice} - ${theAtt} - ${days} (${theDays})"
                if(theDevice && theAtt) {
                    eventMap = [:]
                    theDevice.each { theD ->
                        theAtt.each { att ->
                            if(theD.hasAttribute(att)) {
                                if(reverseMap || gType == "stateTiming") {  // force reverseMap for stateTiming
                                    events = theD.statesSince(att, days, [max: 2000]).collect{[ date:it.date, value:it.value]}.flatten().reverse()
                                } else {
                                    events = theD.statesSince(att, days, [max: 2000]).collect{[ date:it.date, value:it.value]}.flatten()
                                }
                                if(logEnable) log.debug "In getEventsHandler - events: $events for device: ${theD} - ${att}"
                                def eventsForMap = events
                                if (gType == "stateTiming" || state.isNumericalData == false) {
                                    def chartDate = days + 1
                                    if(logEnable) log.debug "In getEventsHandler - splitting events based on the first day of the chart being $chartDate"
                                    def groupedEvents = events.groupBy{ new Date(it.date.getTime()) < chartDate}
                                    if(logEnable) log.debug "In getEventsHandler - groupedEvents: $groupedEvents"
                                    def preEvents = groupedEvents[true]  // all events before the first day of the chart (days + 1), for use in determining the state at 12:00:00 AM on the first date
                                    def postEvents = groupedEvents[false] // all events after the first day of the chart (days + 1)
                                    if(logEnable) log.debug "In getEventsHandler - preEvents: $preEvents postEvents: $postEvents"
                                    if (preEvents != null && preEvents.size() > 0) {
                                        def latestPreEvent = preEvents.pop()
                                        if(logEnable) log.debug "In getEventsHandler - latestPreEvent: $latestPreEvent"
                                        latestPreEvent.date = chartDate.format("yyyy-MM-dd HH:mm:ss.SSS") // redefine the date as being 12:00:00 AM on the first day of the chart
                                        if(logEnable) log.debug "In getEventsHandler - latestPreEvent after date change: $latestPreEvent"
                                        if (postEvents != null) postEvents.add(0,latestPreEvent)
                                        if(logEnable) log.debug "In getEventsHandler - postEvents after adding latest PreEvent: $postEvents"
                                    }
                                    else if(logEnable) log.debug "In getEventsHandler - no event found for the previous day; unable to determine state at start of the first day of the chart"
                                    if (postEvents != null && postEvents.size() > 0) eventsForMap = postEvents
                                }
                                
                                theKey = "${theD};${att.capitalize()}"
                                if (eventsForMap != null && eventsForMap != []) eventMap.put(theKey,eventsForMap)
                            }
                        }
                    }
                    if(logEnable) log.debug "In getEventsHandler - eventMap: $eventMap"
                }
            }
            if(eventMap) {eventChartingHandler(eventMap) }
        } else {
            if(logEnable) log.debug "In getEventsHandler (${state.version}) - Data File: ${fName}"
            readFile(fName)
            if(eventMap) {
                if(dataType == "rawdata") { 
                  //  if(logEnable) log.debug "In getEventsHandler - eventMap before sorting: ${eventMap}"
                    def sortedMap = [:]
                    if(reverseMap || gType == "stateTiming") {
                        eventMap.each { attribute, attributeEvents ->
                            sortedMap[attribute] = attributeEvents.sort { a, b -> Date.parse("yyyy-MM-dd HH:mm:ss.SSS", a.date) <=> Date.parse("yyyy-MM-dd HH:mm:ss.SSS", b.date) }
                        }
                     } else {
                         eventMap.each { attribute, attributeEvents ->
                            sortedMap[attribute] = attributeEvents.sort { a, b -> Date.parse("yyyy-MM-dd HH:mm:ss.SSS", b.date) <=> Date.parse("yyyy-MM-dd HH:mm:ss.SSS", a.date) }
                        }
                     }    
                  //  if(logEnable) log.debug "In getEventsHandler - eventMap after sorting: ${sortedMap}"
                    eventChartingHandler(sortedMap)
                }
                else if(theDays == "999") {
                    // don't have to do anymore processesing
                    eventChartingHandler(eventMap)
                } 
                else {
                    dailyMap = [:]
                    eventMap.each { it ->  
                        theKey = it.key
                        (theDev,theAtt) = theKey.split(";")
                        theD = it.value
                        if(logEnable) log.debug "In getEventsHandler - eventMap - theKey: ${theKey} --- theD: ${theD}"
                        dailyList = []; hourlyList = []; sunList = []; monList = []; tueList = []; wedList = []; thuList = []; friList = []; satList = []
                        
                        total = 0
                        try{
                            theD.each { tdata ->
                                
                                theDate = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", tdata.date)
                                tDay = theDate.format("EEE")
                                tDay3 = theDate.format("yyyy-MM-dd")
                                
                                if(decimals == "None") {
                                    tValue = new BigDecimal(tdata.value).setScale(0, java.math.RoundingMode.HALF_UP)
                                } else if(decimals == "1") {
                                    tValue = new BigDecimal(tdata.value).setScale(1, java.math.RoundingMode.HALF_UP)
                                } else if(decimals == "2") {
                                    tValue = new BigDecimal(tdata.value).setScale(2, java.math.RoundingMode.HALF_UP)
                                }

                                if(tDay == "Sun") {
                                    sunList << [date:tDay3,value:tValue]
                                    sunDate = tDay3
                                } else if(tDay == "Mon") {
                                    monList << [date:tDay3,value:tValue]
                                    monDate = tDay3
                                } else if(tDay == "Tue") {
                                    tueList << [date:tDay3,value:tValue]
                                    tueDate = tDay3
                                } else if(tDay == "Wed") {
                                    wedList << [date:tDay3,value:tValue]
                                    wedDate = tDay3
                                } else if(tDay == "Thu") {
                                    thuList << [date:tDay3,value:tValue]
                                    thuDate = tDay3
                                } else if(tDay == "Fri") {
                                    friList << [date:tDay3,value:tValue]
                                    friDate = tDay3
                                } else if(tDay == "Sat") {
                                    satList << [date:tDay3,value:tValue]
                                    satDate = tDay3
                                }
                            } 
                        } catch(e) {
                            log.warn "In getEventsHandler - Something went wrong"
                            log.warn "In getEventsHandler - tdata: ${tdata}"
                            log.error(getExceptionMessageWithLine(e))
                        }
                        sunTotal = 0; monTotal = 0; tueTotal = 0; wedTotal = 0; thuTotal = 0; friTotal = 0; satTotal = 0
                        
                        if(sunList) { sunTotal = sunList.value.sum() }
                        if(monList) { monTotal = monList.value.sum() }
                        if(tueList) { tueTotal = tueList.value.sum() }
                        if(wedList) { wedTotal = wedList.value.sum() }
                        if(thuList) { thuTotal = thuList.value.sum() }
                        if(friList) { friTotal = friList.value.sum() }
                        if(satList) { satTotal = satList.value.sum() }

                        if(sunList) dailyList << [date:sunDate,value:sunTotal]
                        if(monList) dailyList << [date:monDate,value:monTotal]
                        if(tueList) dailyList << [date:tueDate,value:tueTotal]
                        if(wedList) dailyList << [date:wedDate,value:wedTotal]
                        if(thuList) dailyList << [date:thuDate,value:thuTotal]
                        if(friList) dailyList << [date:friDate,value:friTotal]
                        if(satList) dailyList << [date:satDate,value:satTotal]

                        if(reverseMap) {
                            dailyList = dailyList.sort { a, b -> Date.parse("yyyy-MM-dd", a.date) <=> Date.parse("yyyy-MM-dd", b.date) }
                        } else {
                            dailyList = dailyList.sort { a, b -> Date.parse("yyyy-MM-dd", b.date) <=> Date.parse("yyyy-MM-dd", a.date) }
                        }
                        dailyMap.put(theKey,dailyList)
                    }
                    eventChartingHandler(dailyMap)
                }    
            }
        }
    }
}

def eventChartingHandler(eventMap) {
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "In eventChartingHandler (${state.version}) - Device Events"

        if (gType == "stateTiming" || state.isNumericalData == false)  {
            if(logEnable) log.debug "In eventChartingHandler -- Building Non-Numerical Chart --"
            theLabels = []
            theDatasets = []
            
            Date today = new Date()
            Date startOfToday = today.clearTime()
            Calendar cal = Calendar.getInstance()
            cal.setTimeZone(location.timeZone)
            cal.setTime(startOfToday + 1)
            cal.add(Calendar.SECOND, -1)
            Date endOfToday = cal.getTime()
            
            def minDate = null
            def maxDate = null
            def dayOffset = 0            
            if (theDays != "999" && theDays != "99") dayOffset = theDays.toInteger()
            
            if (xAxisAnchor == "dayEnd") {
                if(gType == "stateTiming" || reverseMap) {
                    maxDate = endOfToday
                    minDate = startOfToday - dayOffset
                }
                else {
                    minDate = endOfToday
                    maxDate = startOfToday - dayOffset
                }          
            }
            else if (xAxisAnchor == "dataEnd") {
                if(gType == "stateTiming" || reverseMap) {
                    minDate = startOfToday - dayOffset
                    maxDate = new Date()
                }     
                else {
                    minDate = new Date()
                    maxDate = startOfToday - dayOffset
                }         
            }
            
            def barThickness = 30
            def extraChartSpacing = 20
            def legendSpace = displayLegend ? 30 : 0
            def titleSpace = (theChartTitle != "" && theChartTitle != null) ? 40 : 0
            def numAttributes = eventMap.size()
            
            def legendItems = []
            def uniqueLegendItemIndices = []
            
            def chartType = gType == "stateTiming" ? "horizontalBar" : gType
            
            buildChart = "f=png&bkg=$bkgrdColor&height=${ legendSpace + titleSpace + (barThickness + extraChartSpacing)*numAttributes}&c={type:'${chartType}'"
            if(eventMap) {
                x = 0
                eventMap.each { it ->  
                    (theDev,theAtt) = it.key.split(";")
                    theD = it.value
                
                    if(showDevInAtt) {
                        theAtt = "${theDev} - ${theAtt}"
                    } else {
                        theAtt = "${theAtt}"
                    }                    
                    theLabels << "'${theAtt}'"
                    
                    theDataset = ""
                    if(logEnable) log.debug "In eventChartingHandler - building dataset for ${theAtt} from data: ${theD}"
                    
                    y=0
                    theD.each { tdata ->
                        theDate = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", tdata.date.toString())
                        
                        if (y < theD.size() - 1) theNextDate = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", theD[y+1].date.toString())
                        else {
                            // last date will either be the latest date (today) or the earliest date depending on reverseMap
                            if(gType == "stateTiming" || reverseMap) theNextDate = new Date()
                            else theNextDate = minDate
                        }      
                            
                        if(logEnable) log.debug "In eventChartingHandler - theDate = ${theDate} theNextDate = ${theNextDate}"
 
                        tDateStart = theDate.format("yyyy-MM-dd'T'HH:mm:ss")
                        tDateEnd = theNextDate.format("yyyy-MM-dd'T'HH:mm:ss")
                        
                        theData = []
                        for (i=0; i < x; i++) {
                            def spacer = [] 
                            theData.add(spacer)
                        }
                        if(gType == "stateTiming" || reverseMap) {
                            datelist = ["new Date('" + tDateStart + "')", "new Date('" + tDateEnd + "')"]
                        }
                        else {
                            datelist = ["new Date('" + tDateEnd + "')", "new Date('" + tDateStart + "')"]
                        }
                        
                        theData.add(datelist)
                  
                        if (y>0) theDataset += ","
                        theDataset += "{"
                        theDataset += "label:'${tdata.value}'"
                        theDataset += ",data:${theData}"
                        theDataset += ", barThickness: ${barThickness}"
                        
                        if (!legendItems.contains(tdata.value)) {
                            legendItems.add(tdata.value)
                            uniqueLegendItemIndices.add(x+y)
                        }
                        
                         if (customizeStateColors) {    
                             def color = null
                            for (i=1; i <= numStates; i++) {
                                if (settings["state${i}"] != null && settings["state${i}"].contains(tdata.value) && color == null) color = settings["state${i}Color"]
                            }
                             if (color == null) {
                                 if(logEnable) log.debug "In eventChartingHandler - No color found for state ${tdata.value}. Using default color"
                                 color = 'black'
                             }
                             theDataset += ",backgroundColor:'${color}'"
                        }   
                        else {                        
                            def theGreenData = ["active", "open", "locked", "present", "on", "open", "true", "dry"]
                            def theRedData = ["inactive", "closed", "unlocked", "not present", "off", "closed", "false", "wet"]
                        
                            if (theGreenData.contains(tdata.value)) theDataset += ",backgroundColor:'green'"
                            else if (theRedData.contains(tdata.value)) theDataset += ",backgroundColor:'red'"
                        }
                        
                        theDataset += "}"
                        
                        y++
                    }
                    if(logEnable) log.debug "In eventChartingHandler - dataset: ${theDataset}"
                    
                    theDatasets << theDataset
                    x++
                }
                
                if(logEnable) log.debug "In eventChartingHandler - the datasets: ${theDatasets}"
                buildChart += ",data:{labels:${theLabels},datasets:${theDatasets}}"        
                buildChart += ",options: {"     
                buildChart += "title: {display: ${(theChartTitle != "" && theChartTitle != null) ? 'true' : 'false'}, text: '${theChartTitle}', fontColor: '${labelColor}'}"
                
                // filter out redundant legend items
                def legendFilterLogic = ""
                for (i=0; i<=uniqueLegendItemIndices.size()-1; i++) {
                    legendFilterLogic += "item.datasetIndex == ${uniqueLegendItemIndices[i]}"
                    if (i < uniqueLegendItemIndices.size()-1) legendFilterLogic += " || "
                }
                buildChart += ",legend:{display: ${displayLegend}, labels: { filter: function(item, chartData) { return ${legendFilterLogic}}}}"
                
                def displayFormat = 'ha'
                if (theDays == "999") displayFormat = 'ha'
                else displayFormat = 'ddd ha'
                
                def maxRotation = 0
                if (theDays != "999") maxRotation = 75
                
                if (onChartValueLabels) buildChart += ",plugins: {datalabels: {anchor: 'start', clamp: true, display: 'auto', align:'end', offset: 0, color:'black', formatter: function(value,context) { return context.chart.data.datasets[context.datasetIndex].label;}}}"
                
                // if state timing chart, force stacking Y axis and not stacking X axis
                if (gType == "stateTiming") buildChart += ",scales: {xAxes: [{display: ${displayXAxis}, stacked: false, type: 'time', unit: 'hour', time: {displayFormats: {'hour': '${displayFormat}'}}, ticks: {fontColor: '${labelColor}', maxRotation: ${maxRotation}, ${minDate != null && maxDate != null ? "min: new Date('" + minDate.format("yyyy-MM-dd'T'HH:mm:ss").toString() + "'), max: new Date('" + maxDate.format("yyyy-MM-dd'T'HH:mm:ss").toString() + "')" : ""}}, gridLines:{display: ${displayXAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}', tickMarkLength: 5, drawBorder: false}}], yAxes: [{display: ${displayYAxis}, stacked: true, ticks: {fontColor: '${labelColor}'}, gridLines:{display: ${displayYAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}'}}]}"
                else buildChart += ",scales: {xAxes: [{display: ${displayXAxis}, stacked: ${stackXAxis}, type: 'time', unit: 'hour', time: {displayFormats: {'hour': '${displayFormat}'}}, ticks: {fontColor: '${labelColor}', maxRotation: ${maxRotation}, ${minDate != null && maxDate != null ? "min: new Date('" + minDate.format("yyyy-MM-dd'T'HH:mm:ss").toString() + "'), max: new Date('" + maxDate.format("yyyy-MM-dd'T'HH:mm:ss").toString() + "')" : ""}}, gridLines:{display: ${displayXAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}', tickMarkLength: 5, drawBorder: false}}], yAxes: [{display: ${displayYAxis}, stacked: ${stackYAxis}, ticks: {fontColor: '${labelColor}'}, gridLines:{display: ${displayYAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}'}}]}"
                
                buildChart += "}}"
                
                if(logEnable) log.debug "In eventChartingHandler - buildChart arguments: ${buildChart}"
                buildChart = "<img width='100%' src=\"https://quickchart.io/chart?" + buildChart + "\" onclick=\"window.open(this.src)\">"
            }            
        }
        else {
            if(logEnable) log.debug "In eventChartingHandler -- Building Numerical Chart --"

            x=1
            theLabels = []
            theData = []
            
            if(eventMap) {
                eventMap.each { it ->  
                    (theDev,theAtt) = it.key.split(";")
                    theD = it.value
                
                    if(showDevInAtt) {
                        theAtt = "${theDev} - ${theAtt}"
                    } else {
                        theAtt = "${theAtt}"
                    }
                    theD.each { tdata ->        
                        if(logEnable) log.debug "In eventChartingHandler -- tdata.date = ${tdata.date.toString()} --"
                        if(dataSource) {
                            def dateObj = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", tdata.date.toString())
                            tDate = dateObj.format("EEE hh:mm")
                        } else {
                            if(dataType == "rawdata") {
                                if(tdata) {
                                    theDate = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", tdata.date.toString())
                                    tDate = theDate.format("EEE hh:mm")
                                } else {
                                    log.warn "There doesn't seem to be any data"
                                    log.warn "tdata: $tdata"
                                }
                            } else {
                                theDate = Date.parse("yyyy-MM-dd", tdata.date.toString())
                                tDate = theDate.format("EEE")
                            }
                        }
                        if(decimals == "None") {
                            tValue = new BigDecimal(tdata.value).setScale(0, java.math.RoundingMode.HALF_UP)
                        } else if(decimals == "1") {
                            tValue = new BigDecimal(tdata.value).setScale(1, java.math.RoundingMode.HALF_UP)
                        } else if(decimals == "2") {
                            tValue = new BigDecimal(tdata.value).setScale(2, java.math.RoundingMode.HALF_UP)
                        }
                        if(secMin) {
                            theT = tValue / 60
                        } else {
                            theT = tValue
                        }
                        
                        if(theDev.toString() == labelDev.toString()) {
                            theLabels << "'${tDate}'"
                        } 
                        theData << theT
                    }
                    if(x==1) {
                        buildChart = "<img width='100%' src=\"https://quickchart.io/chart?f=png&bkg=$bkgrdColor&c={type:'${gType}',data:{labels:${theLabels},datasets:[{label:'${theAtt}',data:${theData}}"
                    } else {
                        buildChart += ",{label:'${theAtt}',data:${theData}}"
                    }

                    x += 1
                    theLabels = []
                    theData = []
                }
                buildChart += "]},options: {"
                buildChart += "title: {display: ${(theChartTitle != "" && theChartTitle != null) ? 'true' : 'false'}, text: '${theChartTitle}', fontColor: '${labelColor}'}"
                buildChart += ",legend:{display: ${displayLegend}}"
                if (onChartValueLabels) buildChart += ",plugins: {datalabels: {anchor: 'center', align:'center', formatter: function(value,context) { return context.chart.data.datasets[context.datasetIndex].label;}}}"
                buildChart += ",scales: {xAxes: [{display: ${displayXAxis}, stacked: ${stackXAxis}, ticks: {fontColor: '${labelColor}'}, gridLines:{display: ${displayXAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}'}}], yAxes: [{display: ${displayYAxis}, stacked: ${stackYAxis}, ticks: {fontColor: '${labelColor}'}, gridLines:{display: ${displayYAxisGrid}, zeroLineColor: '${gridColor}', color: '${gridColor}'}}]}"
                buildChart += "}}\" onclick=\"window.open(this.src)\">"
            }
        }
        
        // Send Chart to Device
        if(dataDevice && buildChart) {
            theCLength = buildChart.length()
            if(logEnable) log.debug "In eventChartingHandler - Chart length: $theCLength"
            if(theCLength > 1024) {
                if(logEnable) log.debug "Chart is too big to fit in an attribute as HTML. Using endpoint."
                
                if (!state.refreshNum) state.refreshNum = 0
                state.refreshNum++
                def chartUrl = getChartEndpoint() + '&version=' + state.refreshNum   
                state.buildChart = buildChart
                def chartTile =     "<div style='height:100%;width:100%'><iframe src='${chartUrl}' style='height:100%;width:100%;border:none'></iframe></div>"
                dataDevice.sendEvent(name: "chart", value: chartTile, isStateChange: true)
            } else {
                dataDevice.sendEvent(name: "chart", value: buildChart, isStateChange: true)
            }
            dataDevice.sendEvent(name: "chartLength", value: theCLength, isStateChange: true)
        }
    }
    if(logEnable) log.debug "----------------------------------------------- End Quick Chart -----------------------------------------------"
}

def fetchChart() {
    if(params.appId.toInteger() != app.id) {
        logDebug("Returning null since app ID received at endpoint is ${params.appId.toInteger()} whereas the app ID of this app is ${app.id}")
        return null    // request was not for this app/team, so return null
    }    
    if(logEnable) log.debug "In fetchChart - Rendering HTML"
    render contentType: "text/html", data: state.buildChart, status: 200
}

def login() {        // Modified from code by @dman2306
    if(logEnable) log.debug "In login - Checking Hub Security"
    state.cookie = ""
    if(hubSecurity) {
        try{
            httpPost(
                [
                    uri: "http://127.0.0.1:8080",
                    path: "/login",
                    query: 
                    [
                        loginRedirect: "/"
                    ],
                    body:
                    [
                        username: hubUsername,
                        password: hubPassword,
                        submit: "Login"
                    ],
                    textParser: true,
                    ignoreSSLIssues: true
                ]
            )
            { resp ->
                if (resp.data?.text?.contains("The login information you supplied was incorrect.")) {
                    log.warn "Quick Chart Data Collector - username/password is incorrect."
                } else {
                    state.cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
                }
            }
        } catch (e) {
            log.error(getExceptionMessageWithLine(e))
        }
    }
}

String getTheDevices(){
    if(logEnable) log.debug "In getTheDevices (${state.version})"
    login()
    uri = "http://${location.hub.localIP}:8080/local/${fName}"
    def params = [
        uri: uri,
        contentType: "text/html; charset=UTF-8",
        headers: [
            "Cookie": state.cookie
        ]
    ]
    try {
        dLabels = []
        httpGet(params) { resp ->
            if(resp!= null) {
                def theD = resp.getData()
                theData = theD.toString().split(", ")
                dSize = theData.size()
                if(logEnable) log.debug "In getTheDevices  - dSize: ${dSize}"               
                if(dSize == 0 || theD == "[]\n") {
                    log.trace "There is no data to process"
                } else {
                    if(logEnable) log.debug "In getTheDevices  - Found data: ${theData}"
                    theData.each { it ->
                        (theDev, theAtt, theDate, theValue, theStatus) = it.replace("[","").replace("]","").split(";")
                        state.isNumericalData = theValue.isNumber()
                        dLabels << theDev
                    }
                    if(logEnable) log.debug "In getTheDevices  - Finished collecting Data"
                    deviceLabels = dLabels.unique().sort()
                    return deviceLabels
                }   
            } else {
                if(logEnable) log.debug "In getTheDevices  - No Data"
            }     
        }
    } catch(e) {
        log.error(getExceptionMessageWithLine(e))
    }
}

String readFile(fName){
    if(logEnable) log.debug "In readFile (${state.version}) - ${fName}"
    state.isData = false
    login()
    uri = "http://${location.hub.localIP}:8080/local/${fName}"
    def params = [
        uri: uri,
        contentType: "text/html; charset=UTF-8",
        headers: [
            "Cookie": state.cookie
        ]
    ]
    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                def theD = resp.getData()
                theData = theD.toString().split(", ")
                dSize = theData.size()
                if(logEnable) log.debug "In readFile  - dSize: ${dSize} - dataType: ${dataType}"               
                if(dSize == 0 || theD == "[]\n") {
                    if(logEnable) log.debug "In readFile - There is no data to process"
                } else {                    
                    def xMax = 1
                    if(dataType == "rawdata") {
                        if(theDays != "99" && theDays != "999") xMax = 1 + theDays.toInteger()
                    }
                    else if(dataType == "duration") {
                        if(theDays == "999") xMax = 7  // TO DO: figure out what xMax should be here? What does "every event" mean?
                        else xMax = 1 + theDays.toInteger()
                    }
                    if(logEnable) log.debug "In readFile - Duration ***** Iterating through file with xMax: ${xMax} *****"
                    
                    newData = []
                    def today = new Date()
                    def theDate = today                    
                    for(x=1;x<=xMax;x++) {
                        String tDate = theDate.format("yyyy-MM-dd")
                        if(logEnable) log.debug "In readFile - Duration ***** Checking file for tDate: $tDate *****"
                        theData.each { el ->
                            if(logEnable) log.debug "In readFile - Checking ${el} -VS- ${tDate}"
                            if(el.toString().contains("${tDate}")) {
                                newData << el
                                state.isData = true
                            }
                        }
                        theDate = today - x
                        if(logEnable) log.debug "In readFile ----------------------------------------------------------------------------------"
                    }
 
                    if(state.isData) {
                        eventMap = [:]
                        newData.each { it ->
                            (theDev, theAtt, theDate, theValue, theStatus) = it.replace("[","").replace("]","").split(";")
                            theKey = "${theDev};${theAtt}"
                            theValue = theValue.trim()
                            state.isNumericalData = theValue.isNumber()
                            newValue = []
                            looking = eventMap.get(theKey)
                            if(looking) {
                                theFindings = eventMap.get(theKey)
                                theFindings.each { tf ->
                                    newValue << [date:tf.date,value:tf.value]
                                }
                                newValue << [date:theDate,value:theValue]
                            } else {
                                newValue << [date:theDate,value:theValue]
                            }
                            eventMap.put(theKey, newValue)
                        }
                        return eventMap
                    }
                }
            } else {
                if(logEnable) log.debug "In readFile - There is no data to process"
                state.isData = false
            }
        }
    } catch (e) {
        log.error(getExceptionMessageWithLine(e))
    }
}

Boolean getFileList(){
    login()
    if(logEnable) log.debug "In getFileList - Getting list of files"
    uri = "http://${location.hub.localIP}:8080/hub/fileManager/json";
    def params = [
        uri: uri,
        headers: [
            "Cookie": state.cookie,
        ]
    ]
    try {
        fileList = []
        httpGet(params) { resp ->
            if (resp != null){
                if(logEnable) log.debug "In getFileList - Found the files"
                def json = resp.data
                for (rec in json.files) {
                    fileType = rec.name[-3..-1]
                    if(fileType == "txt") {
                        fileList << rec.name
                    }
                }
            } else {
                //
            }
        }
        return fileList
    } catch (e) {
        log.error(getExceptionMessageWithLine(e))
    }
}
