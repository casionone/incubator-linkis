package org.apache.linkis.engineplugin.openlookeng;

import org.apache.linkis.manager.engineplugin.common.EngineConnPlugin;
import org.apache.linkis.manager.engineplugin.common.creation.EngineConnFactory;
import org.apache.linkis.manager.engineplugin.common.launch.EngineConnLaunchBuilder;
import org.apache.linkis.manager.engineplugin.common.resource.EngineResourceFactory;
import org.apache.linkis.manager.label.entity.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenLooKengECPlugin implements EngineConnPlugin {

    private Object resourceLocker = new Object();


    private Object engineFactoryLocker = new Object();

    private EngineResourceFactory engineResourceFactory;


    private EngineConnFactory engineFactory;

    private List<Label<?>> defaultLabels = new ArrayList<Label<?>>();

    @Override
    public void init(Map<String, Object> params) {

    }

    @Override
    public EngineResourceFactory getEngineResourceFactory() {
        return null;
    }

    @Override
    public EngineConnLaunchBuilder getEngineConnLaunchBuilder() {
        return null;
    }

    @Override
    public EngineConnFactory getEngineConnFactory() {
        return null;
    }

    @Override
    public List<Label<?>> getDefaultLabels() {
        return defaultLabels;
    }
}
