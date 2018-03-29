package simonlee.elegant.models.api;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

@JSONType(typeName = "iface")
public class ApiIface extends Api {

    public static final String TAG = "com.example.ficfinder.models.api.ApiIface";

    @JSONField(name = "iface")
    private String iface;

    public String getIface() {
        return iface;
    }

    public void setIface(String iface) {
        this.iface = iface;
    }

    @Override
    public String getSignature() {
        // TODO maynot be compatible with soot
        return "<" + this.pkg + "." + this.iface + ">";
    }
}
