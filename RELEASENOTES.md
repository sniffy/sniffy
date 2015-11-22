# Release Notes

## Development

[Commits](https://github.com/sniffy/sniffy/compare/3.0.0...master)

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
