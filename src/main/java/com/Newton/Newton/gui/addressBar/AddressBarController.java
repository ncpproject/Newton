package com.Newton.Newton.gui.addressBar;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.config.ConfigService;
import com.Newton.Newton.gui.FXMLView;
import com.Newton.Newton.gui.MasterController;
import com.Newton.Newton.gui.dialog.AssetInfoController;
import com.Newton.Newton.gui.dialog.QrCodeController;
import com.Newton.Newton.logic.Profile;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;



public class AddressBarController extends MasterController {

    private final BehaviorSubject<Profile> profileSubject;
    private final BehaviorSubject<ConfigService> configServiceSubject;
    private Clipboard clipboard;

    @FXML private ComboBox<PrivateKeyAccount> addressComboBox;
    @FXML private Button addAddressButton;
    @FXML private Button qrcodeButton;
    @FXML private Button copyClipboardButton;

    public AddressBarController(final RxBus rxBus) {
        super(rxBus);
        configServiceSubject = rxBus.getConfigService();
        profileSubject = rxBus.getProfile();
    }

    @FXML
	public void initialize() {
        clipboard = Clipboard.getSystemClipboard();
        addressComboBox.setConverter(new PrivateKeyAccountConverterProfile());

        rxBus.getProfile()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::initializeAddresses);

        JavaFxObservable.valuesOf(addressComboBox.valueProperty())
                .observeOn(Schedulers.io())
                .subscribe(this::selectAddress);

        JavaFxObservable.actionEventsOf(addAddressButton)
                .observeOn(Schedulers.io())
                .subscribe(actionEvent -> addAddress());

        JavaFxObservable.actionEventsOf(qrcodeButton)
                .subscribe(actionEvent -> showQR());
    }

    @FXML
    void copyAddressToClipboard(ActionEvent event) {
        final var cbContent = new ClipboardContent();
        cbContent.putString(addressComboBox.valueProperty().get().getAddress());
        clipboard.setContent(cbContent);
    }

    private void addAddress() {
        final var configService = configServiceSubject.getValue();
        final var profile = profileSubject.getValue().addAddress();
        rxBus.getProfile().onNext(profile);
        configService.updateProfile(profile);
    }

    private void showQR(){
         final var parent = loadParent(FXMLView.QR_CODE, new QrCodeController(rxBus));
         createDialog(parent);
    }

    private void initializeAddresses(final Profile profile) {
        addressComboBox.getItems().setAll(profile.loadPrivateKeyAccounts());
        addressComboBox.getSelectionModel().select(profile.getLastNonce());

        if (profile.isPrivateKeyAccount()){
            addressComboBox.setDisable(true);
            addAddressButton.setDisable(true);
        }

    }

    private void selectAddress(PrivateKeyAccount privateKeyAccount) {
        final var configService = configServiceSubject.getValue();
        final var profile = profileSubject.getValue();

        rxBus.getPrivateKeyAccount().onNext(privateKeyAccount);
        final var updatedProfile = profile.updateLastNonce(addressComboBox.getSelectionModel().getSelectedIndex());
        configService.updateProfile(updatedProfile);
    }
}
