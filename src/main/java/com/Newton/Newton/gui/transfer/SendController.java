package com.Newton.Newton.gui.transfer;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.gui.FXMLView;
import com.Newton.Newton.gui.assets.AssetCell;
import com.Newton.Newton.gui.assets.AssetConverterProfile;
import com.Newton.Newton.gui.dialog.ConfirmTransferController;
import com.Newton.Newton.gui.style.StyleHandler;
import com.Newton.Newton.logic.*;
import com.Newton.Newton.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.TransferTransactionV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.rxjavafx.sources.ObservableListSource;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.Newton.Newton.logic.AssetNumeralFormatter.toLong;
import static com.Newton.Newton.logic.AssetNumeralFormatter.toReadable;

public class SendController extends TransferTransactionController  {

    private static final Logger log = LogManager.getLogger();

    @FXML private Button sendButton;
    @FXML private ComboBox<Transferable> assetComboBox;
    @FXML private ComboBox<Transferable> feeAssetComboBox;
    @FXML private Label messageLimitLabel;
    @FXML private TextField recipientTextField;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private TextArea messageTextArea;

    public SendController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
        assetComboBox.setConverter(new AssetConverterProfile());
        assetComboBox.setCellFactory(param -> new AssetCell());
        feeAssetComboBox.setConverter(new AssetConverterProfile());

       /*final var AssetList = rxBus.getAssetList().getValue();
       Observable.fromIterable(AssetList)
                .filter(t -> t.getAssetId().equals(NCP.ASSET_ID) ||
                            t.getAssetId().equals(Waves.ASSET_ID))
                .toList()
                .subscribe(this::updateComboBoxes);*/

       rxBus.getAssetList()
               .observeOn(Schedulers.io())
               .observeOn(JavaFxScheduler.platform())
               .subscribe(this::updateComboBoxes);

        final var amountIsValidObservable = JavaFxObservable.valuesOf(amountTextField.textProperty())
                .map(this::isValidAmount).cache();

        BehaviorSubject<Boolean> observableRecipientIsValid = BehaviorSubject.create();

        JavaFxObservable.valuesOf(recipientTextField.textProperty())
                .doOnNext(s -> observableRecipientIsValid.onNext(false))
                .observeOn(Schedulers.computation())
                .throttleLast(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .map(address -> NodeAddressValidator.isValidAddress(address, nodeSubject.getValue()))
                .retry()
                .subscribe(observableRecipientIsValid::onNext, Throwable::printStackTrace);

        final var messageIsValidObservable = JavaFxObservable.valuesOf(messageTextArea.textProperty())
                .map(this::isValidMessage);

        final var assetFeeIsNotEmptyObservable = JavaFxObservable.nullableValuesOf(feeAssetComboBox.valueProperty())
                .map(transferable -> !feeAssetComboBox.getSelectionModel().isEmpty());

        final var assetIsNotEmptyObservable = JavaFxObservable.nullableValuesOf(assetComboBox.valueProperty())
                .map(transferable -> !assetComboBox.getSelectionModel().isEmpty());

        final var assetAndAmountIsValidObservable = Observable.combineLatest(
                JavaFxObservable.valuesOf(amountTextField.textProperty()),
                JavaFxObservable.valuesOf(assetComboBox.valueProperty()),
                privateKeyAccountSubject,(amount, asset, privateKeyAccount) -> isValidAmount(amount)).cache();

        StyleHandler.setBorderDisposable(observableRecipientIsValid, recipientTextField);
        StyleHandler.setBorderDisposable(assetAndAmountIsValidObservable, amountTextField);
        StyleHandler.setBorderDisposable(messageIsValidObservable, messageTextArea);
        StyleHandler.setBorderDisposable(assetFeeIsNotEmptyObservable, feeAssetComboBox);
        StyleHandler.setBorderDisposable(assetIsNotEmptyObservable, assetComboBox);
        StyleHandler.setBorderDisposable(amountIsValidObservable, amountTextField);

        JavaFxObservable.actionEventsOf(sendButton)
                .subscribe(ae -> sendTransaction());

        JavaFxObservable.changesOf(amountTextField.textProperty())
                .filter(s -> !FormValidator.isWellFormed(s.getNewVal(), FormValidator.AMOUNT_PATTERN))
                .map(Change::getOldVal)
                .subscribe(amountTextField::setText);

        final var validFormObservable = ConnectableObservable
                .combineLatest(observableRecipientIsValid, messageIsValidObservable, assetAndAmountIsValidObservable,
                        assetFeeIsNotEmptyObservable, assetIsNotEmptyObservable, FormValidator::areValid)
                        .map(Boolean::booleanValue);

        validFormObservable.filter(aBoolean -> !aBoolean)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(aBoolean -> feeTextField.clear());

        final var feeObservable = validFormObservable.observeOn(Schedulers.io()).filter(Boolean::booleanValue)
                .map(b2 -> signTransaction())
                .map(getNodeService()::calculateFee)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .cache()
                .doOnError(Throwable::printStackTrace);

        final var feeIsValidObservable = feeObservable.observeOn(Schedulers.io())
                .map(this::hasSufficientFunds).cache();

        feeObservable.observeOn(JavaFxScheduler.platform())
                .subscribe(aLong -> feeTextField.setText(toReadable(aLong, feeAssetComboBox.getSelectionModel().getSelectedItem().getDecimals())));

        ConnectableObservable.combineLatest(validFormObservable, feeIsValidObservable, FormValidator::areValid)
                .observeOn(Schedulers.io())
                .map(aBoolean -> !aBoolean)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sendButton::setDisable);
    }

    private void sendTransaction() {
        final var tx = signTransaction();
        rxBus.getTransaction().onNext(tx);
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        createDialog(parent);
    }

    private TransferTransactionV2 signTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var asset = assetComboBox.getSelectionModel().getSelectedItem();
        final var assetFee = feeAssetComboBox.getSelectionModel().getSelectedItem();
        final var recipient = recipientTextField.getText();
        return Transactions.makeTransferTx(
                privateKeyAccount,
                AddressConverter.toRawString(recipient, privateKeyAccount.getChainId()),
                toLong(amountTextField.getText(), asset.getDecimals()),
                asset.getAssetId(),
                feeTextField.getText().isEmpty() ? Waves.FEE : toLong(feeTextField.getText(), assetFee.getDecimals()),
                assetFee.getAssetId(),
                messageTextArea.getText()
        );
    }

    private void updateComboBoxes(List<Transferable> assetList) {

        final var combAssetlist = assetList.stream().parallel()
                .filter( t -> t.getAssetId().equals(NCP.ASSET_ID) ||
                              t.getAssetId().equals(Waves.ASSET_ID))
                .collect(Collectors.toUnmodifiableList());

        final var feeList = combAssetlist.stream().parallel()
                .filter(t -> t.getAssetId().equals(Waves.ASSET_ID) &&
                             t.getMinFee() != null &&
                             t.balanceAsLong() >= t.minFeeAsLong() &&
                             t.sponsorBalanceAsLong() >= Waves.FEE)
                .collect(Collectors.toUnmodifiableList());

        reinitializeComboBox(assetComboBox, combAssetlist);
        reinitializeComboBox(feeAssetComboBox, feeList);
    }


    private boolean hasSufficientFunds(final long fee) {
        final var selectedAsset = Optional.of(assetComboBox.getSelectionModel().getSelectedItem()).get();
        final var selectedAssetFee = Optional.of(feeAssetComboBox.getSelectionModel().getSelectedItem()).get();
        final var selectedAssetBalance = Optional.of(selectedAsset.balanceAsLong()).get();

        if (!selectedAsset.getAssetId().equals(selectedAssetFee.getAssetId())) {
            return selectedAssetFee.balanceAsLong() >= fee;
        }

        final var amount = toLong(amountTextField.getText(), selectedAsset.getDecimals());
        return selectedAssetBalance >= (amount + fee);
    }
}
