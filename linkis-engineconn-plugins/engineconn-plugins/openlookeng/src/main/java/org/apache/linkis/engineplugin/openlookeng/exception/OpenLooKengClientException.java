package org.apache.linkis.engineplugin.openlookeng.exception;

import org.apache.linkis.common.exception.ErrorException;

public class OpenLooKengClientException extends ErrorException {

    public OpenLooKengClientException(int errCode, String desc) {
        super(errCode, desc);
    }
}
