package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterClusterService;
import org.ovirt.engine.core.common.queries.gluster.GlusterServiceQueryParameters;

public class GetGlusterClusterServiceByClusterIdQuery<P extends GlusterServiceQueryParameters> extends GlusterQueriesCommandBase<P> {

    public GetGlusterClusterServiceByClusterIdQuery(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    @Override
    protected void executeQueryCommand() {
        List<GlusterClusterService> serviceList = null;

        if(getParameters().getServiceType() == null) {
            serviceList = glusterClusterServiceDao.getByClusterId(getParameters().getId());
        } else {
            serviceList = new ArrayList<>();
            serviceList.add(glusterClusterServiceDao.getByClusterIdAndServiceType(getParameters().getId(),
                        getParameters().getServiceType()));
        }

        getQueryReturnValue().setReturnValue(serviceList);
    }
}
