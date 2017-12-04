package com.example.ficfinder.tracker;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;
import com.example.ficfinder.utils.Logger;

public class IssueHandle implements PubSub.Handle {

    private static final Logger logger = new Logger(IssueHandle.class);

    @Override
    public void handle(PubSub.Issue issue) {
        if (!(issue instanceof Issue)) {
            return ;
        }

        ApiContext issueModel = ((Issue) issue).getIssueModel();
        Api api = issueModel.getApi();
        Context context = issueModel.getContext();

        String badDevicesInfo = "";

        if (issueModel.hasBadDevices()) {
            String[] badDevices = context.getBadDevices();
            for (int i = 0, l = badDevices.length; i < l - 1; i ++) {
                badDevicesInfo += badDevices[i] + ", ";
            }
            badDevicesInfo += badDevices[badDevices.length - 1];
        }

        logger.c("INVALID use of API: " + api.getSiganiture());
        logger.c("  IN file:   " + ((Issue) issue).getSrcFile());
        logger.c("     method: " + ((Issue) issue).getMethod());
        logger.c("     line:   " + ((Issue) issue).getStartLineNumber());
        logger.c("     column: " + ((Issue) issue).getStartColumnNumber());
        logger.c("  SHOULD be used within the context:");
        logger.c("     android API level:  [ " + context.getMinApiLevel() + ", " + (context.getMaxApiLevel() == Context.DEFAULT_MAX_API_LEVEL ? "~" : context.getMaxApiLevel()) + " ]");
        logger.c("     android OS version: [ " + context.getMinSystemVersion() + ", " + (context.getMaxSystemVersion() == context.DEFAULT_MAX_SYSTEM_VERSITON ? "~" : context.getMaxSystemVersion()) + " ]");
        logger.c("     except:             [ " + badDevicesInfo + " ]");
    }

}
