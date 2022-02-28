package org.apache.linkis.engineplugin.openlookeng.conf;

import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.governance.common.protocol.conf.RequestQueryEngineConfigWithGlobalConfig;
import org.apache.linkis.governance.common.protocol.conf.ResponseQueryConfig;
import org.apache.linkis.manager.label.entity.engine.EngineTypeLabel;
import org.apache.linkis.manager.label.entity.engine.UserCreatorLabel;
import org.apache.linkis.rpc.Sender;

import java.util.Map;

public class OpenLooKengEngineConfCache {


    public static Map<String, String> getConfMap(UserCreatorLabel userCreatorLabel, EngineTypeLabel engineTypeLabel) {
        Sender sender = Sender.getSender(Configuration.CLOUD_CONSOLE_CONFIGURATION_SPRING_APPLICATION_NAME().getValue());
        Object any = sender.ask(new RequestQueryEngineConfigWithGlobalConfig(userCreatorLabel, engineTypeLabel,null));
        if (any instanceof ResponseQueryConfig) {
            return ((ResponseQueryConfig)any).getKeyAndValue();
        }
        return null;
    }
}
