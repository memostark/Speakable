package com.guillermonegrete.tts.data.source.remote;

import com.squareup.moshi.Json;

import java.util.Map;

// https://stackoverflow.com/questions/33758601/parse-dynamic-key-json-string-using-retrofit
public class WiktionaryResponse {

    private Query query;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public static class Query {

        @Json(name="pages")
        private Map<String, PageInfo> pageNumber;

        public Map<String, PageInfo> getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(Map<String, PageInfo> pageNumber) {
            this.pageNumber = pageNumber;
        }

    }

    public static class PageInfo {

        private Integer pageid;
        private Integer ns;
        private String title;
        private String extract;

        public Integer getPageid() {
            return pageid;
        }

        public void setPageid(Integer pageid) {
            this.pageid = pageid;
        }

        public Integer getNs() {
            return ns;
        }

        public void setNs(Integer ns) {
            this.ns = ns;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getExtract() {
            return extract;
        }

        public void setExtract(String extract) {
            this.extract = extract;
        }

    }
}
