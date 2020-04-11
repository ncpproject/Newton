package com.Newton.Newton.gui.dialog;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.config.ConfigService;
import com.Newton.Newton.gui.LocaleIcon;
import com.Newton.Newton.gui.MasterController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class TosController extends MasterController {
    private ConfigService configService;

    @FXML private TextArea tosTextArea;
    @FXML private Button disagreeButton;
    @FXML private Button agreeButton;
    @FXML private ComboBox<LocaleIcon> languageComboBox;

    public TosController(final RxBus rxBus) {
        super(rxBus);
        rxBus.getConfigService().subscribe(configService -> this.configService = configService).dispose();
    }

    @FXML
	public void initialize() {
    }

    @FXML
    void agree(ActionEvent event) {
        configService.agreeTos();
        getStage().close();
    }

    @FXML
    void disagree(ActionEvent event) {
        final var stageOwner = (Stage) getStage().getOwner();
        stageOwner.close();
    }
}
