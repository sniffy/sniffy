package com.github.bedrin.jdbc.sniffer.sql;

import com.github.bedrin.jdbc.sniffer.Constants;
import com.github.bedrin.jdbc.sniffer.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 */
public class StatementMetaData {

    public final String sql;
    public final Query query;

    /**
     * Elapsed time in nanoseconds
     */
    public final long elapsedTime;
    public final Thread owner;
    //.jdbc-sniffer-ui{width:100%;height:100%;left:0;top:0;position:absolute;z-index:9999999991}.jdbc-sniffer-ui html{font-family:sans-serif;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%}.jdbc-sniffer-ui body{margin:0}.jdbc-sniffer-ui article,.jdbc-sniffer-ui aside,.jdbc-sniffer-ui details,.jdbc-sniffer-ui figcaption,.jdbc-sniffer-ui figure,.jdbc-sniffer-ui footer,.jdbc-sniffer-ui header,.jdbc-sniffer-ui hgroup,.jdbc-sniffer-ui main,.jdbc-sniffer-ui menu,.jdbc-sniffer-ui nav,.jdbc-sniffer-ui section,.jdbc-sniffer-ui summary{display:block}.jdbc-sniffer-ui audio,.jdbc-sniffer-ui canvas,.jdbc-sniffer-ui progress,.jdbc-sniffer-ui video{display:inline-block;vertical-align:baseline}.jdbc-sniffer-ui audio:not([controls]){display:none;height:0}.jdbc-sniffer-ui [hidden],.jdbc-sniffer-ui template{display:none}.jdbc-sniffer-ui a{background-color:transparent}.jdbc-sniffer-ui a:active,.jdbc-sniffer-ui a:hover{outline:0}.jdbc-sniffer-ui abbr[title]{border-bottom:1px dotted}.jdbc-sniffer-ui b,.jdbc-sniffer-ui strong{font-weight:bold}.jdbc-sniffer-ui dfn{font-style:italic}.jdbc-sniffer-ui h1{margin:.67em 0;font-size:2em}.jdbc-sniffer-ui mark{color:#000;background:#ff0}.jdbc-sniffer-ui small{font-size:80%}.jdbc-sniffer-ui sub,.jdbc-sniffer-ui sup{position:relative;font-size:75%;line-height:0;vertical-align:baseline}.jdbc-sniffer-ui sup{top:-0.5em}.jdbc-sniffer-ui sub{bottom:-0.25em}.jdbc-sniffer-ui img{border:0}.jdbc-sniffer-ui svg:not(:root){overflow:hidden}.jdbc-sniffer-ui figure{margin:1em 40px}.jdbc-sniffer-ui hr{height:0;-webkit-box-sizing:content-box;-moz-box-sizing:content-box;box-sizing:content-box}.jdbc-sniffer-ui pre{overflow:auto}.jdbc-sniffer-ui code,.jdbc-sniffer-ui kbd,.jdbc-sniffer-ui pre,.jdbc-sniffer-ui samp{font-family:monospace,monospace;font-size:1em}.jdbc-sniffer-ui button,.jdbc-sniffer-ui input,.jdbc-sniffer-ui optgroup,.jdbc-sniffer-ui select,.jdbc-sniffer-ui textarea{margin:0;font:inherit;color:inherit}.jdbc-sniffer-ui button{overflow:visible}.jdbc-sniffer-ui button,.jdbc-sniffer-ui select{text-transform:none}.jdbc-sniffer-ui button,.jdbc-sniffer-ui html input[type="button"],.jdbc-sniffer-ui input[type="reset"],.jdbc-sniffer-ui input[type="submit"]{-webkit-appearance:button;cursor:pointer}.jdbc-sniffer-ui button[disabled],.jdbc-sniffer-ui html input[disabled]{cursor:default}.jdbc-sniffer-ui button::-moz-focus-inner,.jdbc-sniffer-ui input::-moz-focus-inner{padding:0;border:0}.jdbc-sniffer-ui input{line-height:normal}.jdbc-sniffer-ui input[type="checkbox"],.jdbc-sniffer-ui input[type="radio"]{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:0}.jdbc-sniffer-ui input[type="number"]::-webkit-inner-spin-button,.jdbc-sniffer-ui input[type="number"]::-webkit-outer-spin-button{height:auto}.jdbc-sniffer-ui input[type="search"]{-webkit-box-sizing:content-box;-moz-box-sizing:content-box;box-sizing:content-box;-webkit-appearance:textfield}.jdbc-sniffer-ui input[type="search"]::-webkit-search-cancel-button,.jdbc-sniffer-ui input[type="search"]::-webkit-search-decoration{-webkit-appearance:none}.jdbc-sniffer-ui fieldset{padding:.35em .625em .75em;margin:0 2px;border:1px solid #c0c0c0}.jdbc-sniffer-ui legend{padding:0;border:0}.jdbc-sniffer-ui textarea{overflow:auto}.jdbc-sniffer-ui optgroup{font-weight:bold}.jdbc-sniffer-ui table{border-spacing:0;border-collapse:collapse}.jdbc-sniffer-ui td,.jdbc-sniffer-ui th{padding:0}@media print{.jdbc-sniffer-ui *,.jdbc-sniffer-ui *:before,.jdbc-sniffer-ui *:after{color:#000 !important;text-shadow:none !important;background:transparent !important;-webkit-box-shadow:none !important;box-shadow:none !important}.jdbc-sniffer-ui a,.jdbc-sniffer-ui a:visited{text-decoration:underline}.jdbc-sniffer-ui a[href]:after{content:" (" attr(href) ")"}.jdbc-sniffer-ui abbr[title]:after{content:" (" attr(title) ")"}.jdbc-sniffer-ui a[href^="#"]:after,.jdbc-sniffer-ui a[href^="javascript:"]:after{content:""}.jdbc-sniffer-ui pre,.jdbc-sniffer-ui blockquote{border:1px solid #999;page-break-inside:avoid}.jdbc-sniffer-ui thead{display:table-header-group}.jdbc-sniffer-ui tr,.jdbc-sniffer-ui img{page-break-inside:avoid}.jdbc-sniffer-ui img{max-width:100% !important}.jdbc-sniffer-ui p,.jdbc-sniffer-ui h2,.jdbc-sniffer-ui h3{orphans:3;widows:3}.jdbc-sniffer-ui h2,.jdbc-sniffer-ui h3{page-break-after:avoid}.jdbc-sniffer-ui .navbar{display:none}.jdbc-sniffer-ui .btn>.caret,.jdbc-sniffer-ui .dropup>.btn>.caret{border-top-color:#000 !important}.jdbc-sniffer-ui .label{border:1px solid #000}.jdbc-sniffer-ui .table{border-collapse:collapse !important}.jdbc-sniffer-ui .table td,.jdbc-sniffer-ui .table th{background-color:#fff !important}.jdbc-sniffer-ui .table-bordered th,.jdbc-sniffer-ui .table-bordered td{border:1px solid #ddd !important}}@font-face{font-family:'Glyphicons Halflings';src:url('../fonts/glyphicons-halflings-regular.eot');src:url('../fonts/glyphicons-halflings-regular.eot?#iefix') format('embedded-opentype'),url('../fonts/glyphicons-halflings-regular.woff2') format('woff2'),url('../fonts/glyphicons-halflings-regular.woff') format('woff'),url('../fonts/glyphicons-halflings-regular.ttf') format('truetype'),url('../fonts/glyphicons-halflings-regular.svg#glyphicons_halflingsregular') format('svg')}.jdbc-sniffer-ui .glyphicon{position:relative;top:1px;display:inline-block;font-family:'Glyphicons Halflings';font-style:normal;font-weight:normal;line-height:1;-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale}.jdbc-sniffer-ui .glyphicon-asterisk:before{content:"a"}.jdbc-sniffer-ui .glyphicon-plus:before{content:"b"}.jdbc-sniffer-ui .glyphicon-euro:before,.jdbc-sniffer-ui .glyphicon-eur:before{content:"ac"}.jdbc-sniffer-ui .glyphicon-minus:before{content:"2"}.jdbc-sniffer-ui .glyphicon-cloud:before{content:"°1"}.jdbc-sniffer-ui .glyphicon-envelope:before{content:"¸9"}.jdbc-sniffer-ui .glyphicon-pencil:before{content:"¸f"}.jdbc-sniffer-ui .glyphicon-glass:before{content:"e001"}.jdbc-sniffer-ui .glyphicon-music:before{content:"e002"}.jdbc-sniffer-ui .glyphicon-search:before{content:"e003"}.jdbc-sniffer-ui .glyphicon-heart:before{content:"e005"}.jdbc-sniffer-ui .glyphicon-star:before{content:"e006"}.jdbc-sniffer-ui .glyphicon-star-empty:before{content:"e007"}.jdbc-sniffer-ui .glyphicon-user:before{content:"e008"}.jdbc-sniffer-ui .glyphicon-film:before{content:"e009"}.jdbc-sniffer-ui .glyphicon-th-large:before{content:"e010"}.jdbc-sniffer-ui .glyphicon-th:before{content:"e011"}.jdbc-sniffer-ui .glyphicon-th-list:before{content:"e012"}.jdbc-sniffer-ui .glyphicon-ok:before{content:"e013"}.jdbc-sniffer-ui .glyphicon-remove:before{content:"e014"}.jdbc-sniffer-ui .glyphicon-zoom-in:before{content:"e015"}.jdbc-sniffer-ui .glyphicon-zoom-out:before{content:"e016"}.jdbc-sniffer-ui .glyphicon-off:before{content:"e017"}.jdbc-sniffer-ui .glyphicon-signal:before{content:"e018"}.jdbc-sniffer-ui .glyphicon-cog:before{content:"e019"}.jdbc-sniffer-ui .glyphicon-trash:before{content:"e020"}.jdbc-sniffer-ui .glyphicon-home:before{content:"e021"}.jdbc-sniffer-ui .glyphicon-file:before{content:"e022"}.jdbc-sniffer-ui .glyphicon-time:before{content:"e023"}.jdbc-sniffer-ui .glyphicon-road:before{content:"e024"}.jdbc-sniffer-ui .glyphicon-download-alt:before{content:"e025"}.jdbc-sniffer-ui .glyphicon-download:before{content:"e026"}.jdbc-sniffer-ui .glyphicon-upload:before{content:"e027"}.jdbc-sniffer-ui .glyphicon-inbox:before{content:"e028"}.jdbc-sniffer-ui .glyphicon-play-circle:before{content:"e029"}.jdbc-sniffer-ui .glyphicon-repeat:before{content:"e030"}.jdbc-sniffer-ui .glyphicon-refresh:before{content:"e031"}.jdbc-sniffer-ui .glyphicon-list-alt:before{content:"e032"}.jdbc-sniffer-ui .glyphicon-lock:before{content:"e033"}.jdbc-sniffer-ui .glyphicon-flag:before{content:"e034"}.jdbc-sniffer-ui .glyphicon-headphones:before{content:"e035"}.jdbc-sniffer-ui .glyphicon-volume-off:before{content:"e036"}.jdbc-sniffer-ui .glyphicon-volume-down:before{content:"e037"}.jdbc-sniffer-ui .glyphicon-volume-up:before{content:"e038"}.jdbc-sniffer-ui .glyphicon-qrcode:before{content:"e039"}.jdbc-sniffer-ui .glyphicon-barcode:before{content:"e040"}.jdbc-sniffer-ui .glyphicon-tag:before{content:"e041"}.jdbc-sniffer-ui .glyphicon-tags:before{content:"e042"}.jdbc-sniffer-ui .glyphicon-book:before{content:"e043"}.jdbc-sniffer-ui .glyphicon-bookmark:before{content:"e044"}.jdbc-sniffer-ui .glyphicon-print:before{content:"e045"}.jdbc-sniffer-ui .glyphicon-camera:before{content:"e046"}.jdbc-sniffer-ui .glyphicon-font:before{content:"e047"}.jdbc-sniffer-ui .glyphicon-bold:before{content:"e048"}.jdbc-sniffer-ui .glyphicon-italic:before{content:"e049"}.jdbc-sniffer-ui .glyphicon-text-height:before{content:"e050"}.jdbc-sniffer-ui .glyphicon-text-width:before{content:"e051"}.jdbc-sniffer-ui .glyphicon-align-left:before{content:"e052"}.jdbc-sniffer-ui .glyphicon-align-center:before{content:"e053"}.jdbc-sniffer-ui .glyphicon-align-right:before{content:"e054"}.jdbc-sniffer-ui .glyphicon-align-justify:before{content:"e055"}.jdbc-sniffer-ui .glyphicon-list:before{content:"e056"}.jdbc-sniffer-ui .glyphicon-indent-left:before{content:"e057"}.jdbc-sniffer-ui .glyphicon-indent-right:before{content:"e058"}.jdbc-sniffer-ui .glyphicon-facetime-video:before{content:"e059"}.jdbc-sniffer-ui .glyphicon-picture:before{content:"e060"}.jdbc-sniffer-ui .glyphicon-map-marker:before{content:"e062"}.jdbc-sniffer-ui .glyphicon-adjust:before{content:"e063"}.jdbc-sniffer-ui .glyphicon-tint:before{content:"e064"}.jdbc-sniffer-ui .glyphicon-edit:before{content:"e065"}.jdbc-sniffer-ui .glyphicon-share:before{content:"e066"}.jdbc-sniffer-ui .glyphicon-check:before{content:"e067"}.jdbc-sniffer-ui .glyphicon-move:before{content:"e068"}.jdbc-sniffer-ui .glyphicon-step-backward:before{content:"e069"}.jdbc-sniffer-ui .glyphicon-fast-backward:before{content:"e070"}.jdbc-sniffer-ui .glyphicon-backward:before{content:"e071"}.jdbc-sniffer-ui .glyphicon-play:before{content:"e072"}.jdbc-sniffer-ui .glyphicon-pause:before{content:"e073"}.…

    public final List<StackTraceElement> stackTrace;


    protected StatementMetaData(String sql, Query query, long elapsedTime) {
        this.sql = sql;
        this.query = query;
        this.elapsedTime = elapsedTime;
        this.owner = Thread.currentThread();
        this.stackTrace = getStackTrace();
    }

    public static StatementMetaData parse(String sql) {
        return parse(sql, -1);
    }

    public static StatementMetaData parse(String sql, long elapsedTime) {

        if (null == sql) return null;

        String normalized = sql.trim().toLowerCase();

        Query query;

        if (normalized.startsWith("select ")) {
            // TODO: can start with "WITH" statement
            query = Query.SELECT;
        } else if (normalized.startsWith("insert ")) {
            query = Query.INSERT;
        } else if (normalized.startsWith("update ")) {
            query = Query.UPDATE;
        } else if (normalized.startsWith("delete ")) {
            query = Query.DELETE;
        } else if (normalized.startsWith("merge ")) {
            // TODO: can start with "WITH" statement
            query = Query.MERGE;
        } else {
            query = Query.OTHER;
        }

        return new StatementMetaData(sql, query, elapsedTime);
    }

    private static List<StackTraceElement> getStackTrace() {
        List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (!stackTraceElement.getClassName().startsWith(Constants.PACKAGE_NAME)) {
                stackTrace.add(stackTraceElement);
            }
        }
        return Collections.unmodifiableList(stackTrace);
    }

}
