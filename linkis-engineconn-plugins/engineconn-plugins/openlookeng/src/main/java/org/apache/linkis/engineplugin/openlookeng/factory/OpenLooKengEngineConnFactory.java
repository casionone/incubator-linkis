package org.apache.linkis.engineplugin.openlookeng.factory;

import org.apache.linkis.engineconn.common.creation.EngineCreationContext;
import org.apache.linkis.engineconn.common.engineconn.EngineConn;
import org.apache.linkis.engineconn.computation.executor.creation.AbstractComputationSingleExecutorECFactory;
import org.apache.linkis.engineconn.executor.entity.Executor;
import org.apache.linkis.manager.label.entity.engine.EngineType;
import org.apache.linkis.manager.label.entity.engine.RunType;
import scala.Enumeration;

public class OpenLooKengEngineConnFactory extends AbstractComputationSingleExecutorECFactory {

    @Override
    public Enumeration.Value getRunType() {
        return RunType.SQL();
    }

    @Override
    public Executor newExecutor(int id, EngineCreationContext engineCreationContext, EngineConn engineConn) {
        return null;
    }

    @Override
    public Enumeration.Value getEngineConnType() {
        return EngineType.OPENLOOKENG();
    }
}
