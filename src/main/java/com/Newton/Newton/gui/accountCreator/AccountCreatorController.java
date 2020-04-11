package com.Newton.Newton.gui.accountCreator;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.gui.MasterController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class AccountCreatorController extends MasterController {
    protected final AccountCreator accountCreator;

    @FXML protected Button backButton;
    @FXML protected Button nextButton;

    AccountCreatorController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus);
        this.accountCreator = accountCreator;
    }
}
