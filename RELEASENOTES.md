# Release Notes

## Development

[Commits](https://github.com/sniffy/sniffy/compare/3.1.3...master)

## v3.1.3 - March 27th, 2017

 * [Firefox widget position shifted](https://github.com/sniffy/sniffy/issues/328)
 * [Support fault-tolerance testing for non-servlet applications](https://github.com/sniffy/sniffy/issues/327)
 * [Sniffy Chrome Extension breaks certain Angular1 apps](https://github.com/sniffy/sniffy/issues/325)
 * [Sniffy doesn't work with base tag](https://github.com/sniffy/sniffy/issues/321)
 * [Sniffy is injected incorrectly to pages with query parametes](https://github.com/sniffy/sniffy/issues/319)
 * [@SniffyAdvancedConfiguration(topSqlCapacity = "...") doesn't work](https://github.com/sniffy/sniffy/issues/311)
 * [webjar isn't added to shaded sources jar](https://github.com/sniffy/sniffy/issues/310)
 * [Simulate network failures in unit tests](https://github.com/sniffy/sniffy/issues/242)
 * [Disable network connectivity in unit tests](https://github.com/sniffy/sniffy/issues/224)
 * [Incorrect SQL formatting (missing spaces)](https://github.com/sniffy/sniffy/issues/182)

## v3.1.2 - March 20th, 2017

 * [Top SQL queries tab - add sorting and clear button](https://github.com/sniffy/sniffy/issues/306)
 * [excludePattern should manage injectHtml instead of enabled](https://github.com/sniffy/sniffy/issues/304)
 * [Backslashes are not escaped in connection registry](https://github.com/sniffy/sniffy/issues/302)
 * [Sniffy shows response time with leading zeroes](https://github.com/sniffy/sniffy/issues/300)
 * [Sniffy doesn't work with Angular1 ng-include directive](https://github.com/sniffy/sniffy/issues/297)
 * [Sniffy call getParameter() and cleans the request inputStream/reader](https://github.com/sniffy/sniffy/issues/295)
 * [Calculate Top slowest queries across all requests](https://github.com/sniffy/sniffy/issues/292)
 * [Produce JBoss module / update documentation](https://github.com/sniffy/sniffy/issues/291)
 * [Parse Content-Type from setHeader/addHeader methods](https://github.com/sniffy/sniffy/issues/290)
 * [Provide a version of SniffyFilter which is disabled by default](https://github.com/sniffy/sniffy/issues/288)
 * [IE11 - scrollbar overlaps sniffy](https://github.com/sniffy/sniffy/issues/286)

## v3.1.1 - March 14th, 2017

 * [Show exceptions raised on server side](https://github.com/sniffy/sniffy/issues/280)
 * [Sniffy filter is called twice in case of request forwarding](https://github.com/sniffy/sniffy/issues/275)
 * [Sniffy widget is not rendered when AUT is behind a reverse proxy which changes paths](https://github.com/sniffy/sniffy/issues/272)
 * [Sniffy uber jar doesn't come with sources in maven central](https://github.com/sniffy/sniffy/issues/267)
 * [Sniffy XHR wrapper is not compatible with zone.js](https://github.com/sniffy/sniffy/issues/266)
 * [Error pages are missing Sniffy](https://github.com/sniffy/sniffy/issues/260)
 * [JS Map does not work](https://github.com/sniffy/sniffy/issues/213)

## v3.1.0 - February 16th, 2017

 * New documentation at [http://sniffy.io/docs/latest/](http://sniffy.io/docs/latest/)
 * [ClassNotFoundException when using @EnableSniffy with params and without adapter ](https://github.com/sniffy/sniffy/issues/264)
 * [Make Sniffy disableable using HTTP header or user-agen ](https://github.com/sniffy/sniffy/issues/251)
 * [NPE in ConnectionsRegistry ](https://github.com/sniffy/sniffy/issues/250)
 * [Sniffy release binaries are not released automatically to maven central ](https://github.com/sniffy/sniffy/issues/249)
 * [Sniffy relative URLs are incorrect if web site is accessed without trailing slash ](https://github.com/sniffy/sniffy/issues/248)
 * [Cannot disable/enable datasource with slash in connection URL ](https://github.com/sniffy/sniffy/issues/245)
 * [High contention in Sniffy.hasSpies method ](https://github.com/sniffy/sniffy/issues/243)
 * [Sniffy tries to profile requests to other domains ](https://github.com/sniffy/sniffy/issues/232)
 * [Sniffy doesn't work for some URL's ](https://github.com/sniffy/sniffy/issues/228)
 * [Persist fault tolerance testing for application restart ](https://github.com/sniffy/sniffy/issues/220)
 * [Ability to disable connectivity on datasource level, not only host/port ](https://github.com/sniffy/sniffy/issues/218)
 * [Add font-family to sniffy widget CSS ](https://github.com/sniffy/sniffy/issues/216)
 * [java.lang.IndexOutOfBoundsException: Index: 0, Size: -1 ](https://github.com/sniffy/sniffy/issues/214)
 * [Make Sniffy 100% disableable](https://github.com/sniffy/sniffy/issues/211)
 * [Sniffy widget depends on app css ](https://github.com/sniffy/sniffy/issues/207)
 * [Sniffy doesn't work behind a reverse proxy changing the context ](https://github.com/sniffy/sniffy/issues/206)
 * [NPE when servlet context is not available and init() method is not called ](https://github.com/sniffy/sniffy/issues/205)
 * [NPE in Sniffy.Driver.connect method ](https://github.com/sniffy/sniffy/issues/204)
 * [Stop using X- headers ](https://github.com/sniffy/sniffy/issues/200)
 * [NPE in Driver.connect() method ](https://github.com/sniffy/sniffy/issues/194)
 * [X-Request-Details contains absolute URLs ](https://github.com/sniffy/sniffy/issues/191)
 * [Network calls aren't grouped under SniffyDataSource.getConnection ](https://github.com/sniffy/sniffy/issues/189)
 * [DataSource wrapper implementation ](https://github.com/sniffy/sniffy/issues/185)
 * [Servlet 3.1 support ](https://github.com/sniffy/sniffy/issues/184)
 * [Sniffy breaks character encoding ](https://github.com/sniffy/sniffy/issues/183)
 * [Thread local network firewall ](https://github.com/sniffy/sniffy/issues/180)
 * [Time units are inconsistent ](https://github.com/sniffy/sniffy/issues/173)
 * [Compare interned strings by object refrence ](https://github.com/sniffy/sniffy/issues/169)
 * [NPE on PreparedStatement.executeUpdate() ](https://github.com/sniffy/sniffy/issues/168)
 * [Socket sniffering should be disableable ](https://github.com/sniffy/sniffy/issues/167)
 * [Add clear data button to the widget ](https://github.com/sniffy/sniffy/issues/166)
 * [Network Issues simulation ](https://github.com/sniffy/sniffy/issues/165)
 * [Sniffy consumes a lot of memory under load ](https://github.com/sniffy/sniffy/issues/152)
 * [Time spent in SQL query is shown in microseconds instead of milliseconds ](https://github.com/sniffy/sniffy/issues/151)
 * [Do not show socket operations made by JDBC ](https://github.com/sniffy/sniffy/issues/150)
 * [Console error with Firefox ](https://github.com/sniffy/sniffy/issues/149)
 * [Sniffy widget should be dragable or minimizeable ](https://github.com/sniffy/sniffy/issues/146)
 * [Inconsisten HTTP method case in sniffy ](https://github.com/sniffy/sniffy/issues/145)
 * [Automatically add sniffy to Spring Boot datasources ](https://github.com/sniffy/sniffy/issues/144)
 * [Sniffy writes garbage to console.log ](https://github.com/sniffy/sniffy/issues/143)
 * [Track all socket outbound connections - not only JDBC ](https://github.com/sniffy/sniffy/issues/89)
 * [Capture time spent in getConnection() method ](https://github.com/sniffy/sniffy/issues/81)

## v3.0.7 - April 23rd, 2016
 * [Sniffy UI - Add UI effect on increasing the counter](https://github.com/sniffy/sniffy/issues/119)
 * [Sniffy widget not rendered in IE10 Win7](https://github.com/sniffy/sniffy/issues/123)
 * [Refused to get unsafe header "X-Sql-Queries"](https://github.com/sniffy/sniffy/issues/127)
 * [WrongNumberOfQueriesError doesn't contain information about query type ](https://github.com/sniffy/sniffy/issues/130)
 * [Print sniffy version in UI widget footer](https://github.com/sniffy/sniffy/issues/131)
 * [Scroller isn't triggered if Ajax requests are made when widget is open](https://github.com/sniffy/sniffy/issues/133)
 * [Query counter is larger than number of queries shown in sniffy](https://github.com/sniffy/sniffy/issues/134)
 * [Sniffy should provide JS map for debugging ](https://github.com/sniffy/sniffy/issues/135)
 * [Error in browser console](https://github.com/sniffy/sniffy/issues/137)
 * [Expose request elapsed server time in header](https://github.com/sniffy/sniffy/issues/138)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.6...3.0.7)

## v3.0.6 - March 20th, 2016
 * [Sniffy - stack trace is missing line breaks](https://github.com/sniffy/sniffy/issues/120)
 * [Sniffy UI: last row is sometimes hidden underneath the footer](https://github.com/sniffy/sniffy/issues/124)
 * [https://github.com/sniffy/sniffy/issues/125](https://github.com/sniffy/sniffy/issues/125)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.5...3.0.6)

## v3.0.5 - March 13th, 2016
 * [UI must be enableble using request parameter](https://github.com/sniffy/sniffy/issues/83)
 * [Return server response time](https://github.com/sniffy/sniffy/issues/95)
 * [Sniffy hides application exception and returns 200](https://github.com/sniffy/sniffy/issues/104)
 * [sniffy maven pulls bower dependencies](https://github.com/sniffy/sniffy/issues/111)
 * [Sniffy doesn't support async requests](https://github.com/sniffy/sniffy/issues/113)
 * [Sniffy doesn't inject JS to XHTML documents](https://github.com/sniffy/sniffy/issues/116)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.3...3.0.5)

## v3.0.3 - December 6th, 2015
 * [Implement dark theme for sniffy](https://github.com/sniffy/sniffy/issues/98)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.2...3.0.3)

## v3.0.2 - December 5th, 2015
 * [Sniffy must be injected after meta tags](https://github.com/sniffy/sniffy/issues/101)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.1...3.0.2)

## v3.0.1 - November 29nd, 2015
 * [setHeader('Content-Length') is not supported](https://github.com/sniffy/sniffy/issues/99)

[Commits](https://github.com/sniffy/sniffy/compare/3.0.0...3.0.1)

## v3.0.0 - November 22nd, 2015
 * License changed to [MIT](http://www.opensource.org/licenses/mit-license.php)
 * Project rebranded as [sniffy.io](http://sniffy.io)
 * Sniffy now uses [semantic versioning](http://semver.org/)

[Commits](https://github.com/sniffy/sniffy/compare/2.3.5...3.0.0)

## v2.3.5 - November 18th, 2015
 * [Counter in sniffer UI is invisible](https://github.com/sniffy/sniffy/issues/92)
 * [If Ajax query is done to another context, Sniffer won't load the exact queries](https://github.com/sniffy/sniffy/issues/88)
 * [NPE in SnifferFilter](https://github.com/sniffy/sniffy/issues/86)
 * [Track AJAX queries generated during page load](https://github.com/sniffy/sniffy/issues/85)
 * [JdbcSniffer spoils the global js context](https://github.com/sniffy/sniffy/issues/82)
 * [crappy UX in IE10](https://github.com/sniffy/sniffy/issues/80)
 * [Open sniffer UI in an iframe in order to avoid JS/CSS conflicts with parent page](https://github.com/sniffy/sniffy/issues/77)
 * [copy sql-queries to clipboard](https://github.com/sniffy/sniffy/issues/63)
 * [[filter] transparent back in UI intercepts clicks](https://github.com/sniffy/sniffy/issues/59)
 * [jdbc-sniffer should NEVER break existing code](https://github.com/sniffy/sniffy/issues/48)

[Commits](https://github.com/sniffy/sniffy/compare/2.3.4...2.3.5)

## v2.3.4 - August 6th, 2015
 * [View queries from AJAX requests](https://github.com/sniffy/sniffy/issues/57)

[Commits](https://github.com/sniffy/sniffy/compare/2.3.3...2.3.4)

## v2.3.3 - August 6th, 2015
 * [excludes-filter disabled the SnifferServlet as well](https://github.com/sniffy/sniffy/issues/73)
 * [JSON with queries contains unescaped data in strings](https://github.com/sniffy/sniffy/issues/72)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.3.2...2.3.3)

## v2.3.2 - August 5th, 2015
 * [Sniffer Filter requires Servlet 2.5+](https://github.com/sniffy/sniffy/issues/66)
 * [Add ability to filter out requests](https://github.com/sniffy/sniffy/issues/65)
 * [Do not inject sniffer to XML documents](https://github.com/sniffy/sniffy/issues/64)
 * [Sniffer shows 2 queries in the icon, but the list is empty when I click on them](https://github.com/sniffy/sniffy/issues/60)
 * [Use version in all resources to avoid cache problems](https://github.com/sniffy/sniffy/issues/58)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.3.1...2.3.2)

## v2.3.1 - August 3rd, 2015
 * [Servlet Filter - view executed queries](https://github.com/sniffy/sniffy/issues/54)
 * [Sniffer shows queries from all threads on error](https://github.com/sniffy/sniffy/issues/43)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.3...2.3.1)

## v2.3.0 - July 30th, 2015
 * [Servlet Filter recording the number of SQL queries](https://github.com/sniffy/sniffy/issues/51)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.2.2...2.3)

## v2.2.3 - July 3rd, 2015
 * [Spring integration doesn't work with spring 3](https://github.com/sniffy/sniffy/issues/42)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.2.2...2.2.3)

## v2.2.2 - July 2nd, 2015
 * [Spring integration does not work if there methods wo sniffer annotations](https://github.com/sniffy/sniffy/issues/47)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.2.1...2.2.2)

## v2.2.1 - July 1st, 2015
 * [Spring integration doesn't work with spring 3](https://github.com/sniffy/sniffy/issues/42)
 * [NPE when calling executeBatch on PS without calling addBatch](https://github.com/sniffy/sniffy/issues/44)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.2...2.2.1)

## v2.2 - May 22th, 2015
 * Parse executed query and count different statements (SELECT, UPDATE, e.t.c.) separatelly ([Issue 35](https://github.com/sniffy/sniffy/issues/35))
 * Integration with [Spring Framework](https://github.com/sniffy/sniffy/wiki/Spring-Framework)
 
[Commits](https://github.com/sniffy/sniffy/compare/2.1...2.2)
