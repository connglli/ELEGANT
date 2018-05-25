package simonlee.elegant.models.context;

import com.alibaba.fastjson.annotation.JSONField;
import simonlee.elegant.utils.Strings;

import java.io.Serializable;

public class Context implements Serializable {

    // some default values
    public static final int DEFAULT_MIN_API_LEVEL = 1;
    public static final int DEFAULT_MAX_API_LEVEL = 27; // TODO - hard code, need to be extracted to a configuration file
    public static final double DEFAULT_MIN_SYSTEM_VERSION = 1;
    public static final double DEFAULT_MAX_SYSTEM_VERSITON = Double.MAX_VALUE;

    @JSONField(name = "min_api_level")
    private int minApiLevel = DEFAULT_MIN_API_LEVEL;

    @JSONField(name = "max_api_level")
    private int maxApiLevel = DEFAULT_MAX_API_LEVEL;

    @JSONField(name = "min_system_version")
    private double minSystemVersion = DEFAULT_MIN_SYSTEM_VERSION;

    @JSONField(name = "max_system_version")
    private double maxSystemVersion = DEFAULT_MAX_SYSTEM_VERSITON;

    @JSONField(name = "bad_devices")
    private String[] badDevices = {};

    @JSONField(name = "message")
    private String message = null;

    @JSONField(name = "important")
    private String important = null;

    public int getMinApiLevel() {
        return minApiLevel;
    }

    public void setMinApiLevel(int minSdkLvel) {
        this.minApiLevel = minSdkLvel;
    }

    public int getMaxApiLevel() {
        return maxApiLevel;
    }

    public void setMaxApiLevel(int maxApiLevel) {
        this.maxApiLevel = maxApiLevel;
    }

    public double getMinSystemVersion() {
        return minSystemVersion;
    }

    public void setMinSystemVersion(double minSystemVersion) {
        this.minSystemVersion = minSystemVersion;
    }

    public double getMaxSystemVersion() {
        return maxSystemVersion;
    }

    public void setMaxSystemVersion(double maxSystemVersion) {
        this.maxSystemVersion = maxSystemVersion;
    }

    public String[] getBadDevices() {
        return badDevices;
    }

    public void setBadDevices(String[] badDevices) {
        this.badDevices = badDevices;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public boolean isImportant() {
        try {
            if (null == important) { return false; }
            String t = Strings.underlineToCamel(important);
            return null != Context.class.getDeclaredField(t).get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Context)) {
            return false;
        }

        Context context = (Context) obj;
        boolean badDevicesEquals = true;

        if (badDevices.length != context.badDevices.length) {
            badDevicesEquals = false;
        } else {
            for (int i = 0, l = badDevices.length; i < l; i ++) {
                if (!badDevices[i].equals(context.badDevices[i])) {
                    badDevicesEquals = false;
                    break;
                }
            }
        }

        return badDevicesEquals &&
                minApiLevel == context.minApiLevel &&
                maxApiLevel == context.maxApiLevel &&
                minSystemVersion == context.minSystemVersion &&
                maxSystemVersion == context.maxSystemVersion;
    }

    @Override
    public int hashCode() {
        // we don't have to override hashCode because our equals method uses every field of a context obj
        return super.hashCode();
    }
}
