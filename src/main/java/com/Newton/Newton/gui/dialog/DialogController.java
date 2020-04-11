package com.Newton.Newton.gui.dialog;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.gui.MasterController;
import javafx.fxml.FXML;

class DialogController extends MasterController {
    DialogController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
    }

    @FXML
    void closeDialog() {
        getStage().close();
    }
}
