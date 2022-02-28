package org.apache.linkis.engineplugin.openlookeng.exception;

import org.apache.linkis.common.exception.ErrorException;

public class OpenLooKengSourceGroupException extends ErrorException {

    public OpenLooKengSourceGroupException(int errCode, String desc) {
        super(errCode, desc);
    }
}
