package com.example.ficfinder.finder.reflectionfinder;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;
import com.example.ficfinder.tracker.PubSub;
import com.example.ficfinder.utils.Logger;

public class RIssueHandle implements PubSub.Handle {

    private static final Logger logger = new Logger(RIssueHandle.class);

    @Override
    public void handle(PubSub.Message i) {
        if (!(i instanceof RIssue)) {
            return ;
        }

        RIssue rIssue = (RIssue) i;
        ApiContext model = rIssue.getModel();
        Api api = model.getApi();
        Context context = model.getContext();

        String badDevicesInfo = "";

        if (model.hasBadDevices()) {
            String[] badDevices = context.getBadDevices();
            for (int idx = 0, l = badDevices.length; idx < l - 1; idx ++) {
                badDevicesInfo += badDevices[idx] + ", ";
            }
            badDevicesInfo += badDevices[badDevices.length - 1];
        }

        logger.w("INVALID use of API: " + api.getSignature());

        logger.w("  INVOKED by reflection at " + rIssue.getMethod() + ">" + "(" + rIssue.getSrcFile() + ":" + rIssue.getStartLineNumber() + "):");

        logger.w("  SHOULD be used within the context:");
        logger.w("     android API level:  [ " + context.getMinApiLevel() + ", " + context.getMaxApiLevel() + " ]");
//        logger.w("     android OS version: [ " + context.getMinSystemVersion() + ", " + context.getMaxSystemVersion() + " ]");
        logger.w("     except devices:     [ " + badDevicesInfo + " ]");
        logger.w("  Please check your api version or devices\n");
    }
}
