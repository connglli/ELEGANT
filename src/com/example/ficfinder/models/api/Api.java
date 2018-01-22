package com.example.ficfinder.models.api;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

import java.io.Serializable;

@JSONType(seeAlso = { ApiIface.class, ApiMethod.class, ApiField.class })
public abstract class Api implements Serializable {

    public static class Type {

        @JSONField(name = "pkg")
        private String pkg;

        @JSONField(name = "iface")
        private String iface;

        public String getPkg() {
            return pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public String getIface() {
            return iface;
        }

        public void setIface(String iface) {
            this.iface = iface;
        }

        public String toString() {
            // TODO maynot be compatible with soot
            return (this.pkg != null && !this.pkg.isEmpty() ? (this.pkg + ".") : "") + this.iface;
        }

    }

    @JSONField(name = "pkg")
    protected String pkg;

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public abstract String getSignature();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Api)) {
            return false;
        }

        return this.getSignature().equals(((Api) obj).getSignature());
    }

    @Override
    public int hashCode() {
        return this.getSignature().hashCode();
    }
}