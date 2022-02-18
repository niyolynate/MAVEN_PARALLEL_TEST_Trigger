package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.gluster.ResetBrickModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class ResetBrickPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<ResetBrickModel, ResetBrickPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<ResetBrickModel> {
    }

    @Inject
    public ResetBrickPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }

}
