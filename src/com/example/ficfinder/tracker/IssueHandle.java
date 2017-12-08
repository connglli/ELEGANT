package com.example.ficfinder.tracker;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;
import com.example.ficfinder.utils.Logger;

public class IssueHandle implements PubSub.Handle {

    private static final Logger logger = new Logger(IssueHandle.class);

    @Override
    public void handle(PubSub.Issue i) {
        if (!(i instanceof Issue)) {
            return ;
        }

        Issue issue = (Issue) i;
        ApiContext model = issue.getModel();
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

        logger.c("INVALID use of API: " + api.getSiganiture());

        logger.c("  PATH of method calling: [");
        issue.getCallerPoints().forEach(p ->
                logger.c("    " + p.getMethod() + "(" + p.getSrcFile() + ":" + p.getStartLineNumber() + ") ->")
        );
        logger.c("    " + issue.getCalleePoint().getMethod());
        logger.c("  ]");

        logger.c("  SHOULD be used within the context:");
        logger.c("     android API level:  [ " + context.getMinApiLevel() + ", " + (context.getMaxApiLevel() == Context.DEFAULT_MAX_API_LEVEL ? "~" : context.getMaxApiLevel()) + " ]");
        logger.c("     android OS version: [ " + context.getMinSystemVersion() + ", " + (context.getMaxSystemVersion() == context.DEFAULT_MAX_SYSTEM_VERSITON ? "~" : context.getMaxSystemVersion()) + " ]");
        logger.c("     except:             [ " + badDevicesInfo + " ]");
        logger.c("  Please check your api version or devices");
    }

}
