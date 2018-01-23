package com.example.ficfinder.finder.plainfinder;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;
import com.example.ficfinder.tracker.PubSub;
import com.example.ficfinder.utils.Logger;

public class PIssueHandle implements PubSub.Handle {

    private static final Logger logger = new Logger(PIssueHandle.class);

    @Override
    public void handle(PubSub.Message i) {
        if (!(i instanceof PIssue)) {
            return ;
        }

        PIssue pIssue = (PIssue) i;
        ApiContext model = pIssue.getModel();
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

        logger.w("  CHAIN of invoked method: [");
        pIssue.getCallerPoints().forEach(p ->
                logger.w("    " + p.getMethod() + "(" + p.getSrcFile() + ":" + p.getStartLineNumber() + ") ->")
        );
        logger.w("    " + pIssue.getCalleePoint().getMethod());
        logger.w("  ]");

        logger.w("  SHOULD be used within the context:");
        logger.w("     android API level:  [ " + context.getMinApiLevel() + ", " + context.getMaxApiLevel() + " ]");
//        logger.w("     android OS version: [ " + context.getMinSystemVersion() + ", " + context.getMaxSystemVersion() + " ]");
        logger.w("     except devices:     [ " + badDevicesInfo + " ]");
        logger.w("  Please check your api version or devices\n");
    }

}
