package org.apache.linkis.engineplugin.openlookeng.builder;

import org.apache.linkis.manager.engineplugin.common.launch.process.JavaProcessEngineConnLaunchBuilder;
import org.apache.linkis.manager.label.entity.engine.UserCreatorLabel;
import org.apache.linkis.storage.utils.StorageConfiguration;

public class OpenLooKengProcessECLaunchBuilder extends JavaProcessEngineConnLaunchBuilder {

    @Override
    public String getEngineStartUser(UserCreatorLabel label) {
        return StorageConfiguration.HDFS_ROOT_USER().getValue();
    }
}
