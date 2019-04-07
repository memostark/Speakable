package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.TreeMap;

// https://stackoverflow.com/questions/33758601/parse-dynamic-key-json-string-using-retrofit
public class WiktionaryResponse {


    @SerializedName("query")
    @Expose
    private Query query;


    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public class Query {

        @SerializedName("pages")
        @Expose
        private TreeMap<String, PageInfo> pageNumber;

        public TreeMap<String, PageInfo> getPageNumber() {
            return pageNumber;
        }

        public  void setPageNumber(TreeMap<String, PageInfo> pageNumber) {
            this.pageNumber = pageNumber;
        }

    }

    public class PageInfo {

        @SerializedName("pageid")
        @Expose
        private Integer pageid;
        @SerializedName("ns")
        @Expose
        private Integer ns;
        @SerializedName("title")
        @Expose
        private String title;
        @SerializedName("extract")
        @Expose
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
