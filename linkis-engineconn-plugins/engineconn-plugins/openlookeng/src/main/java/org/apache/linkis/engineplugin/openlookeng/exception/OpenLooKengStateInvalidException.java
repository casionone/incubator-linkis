package org.apache.linkis.engineplugin.openlookeng.exception;

import org.apache.linkis.common.exception.ErrorException;

public class OpenLooKengStateInvalidException extends ErrorException {

    public OpenLooKengStateInvalidException(int errCode, String desc) {
        super(errCode, desc);
    }
}
