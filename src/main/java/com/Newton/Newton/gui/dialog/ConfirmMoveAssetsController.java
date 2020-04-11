package com.Newton.Newton.gui.dialog;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.logic.AssetDetailsService;
import com.Newton.Newton.logic.AssetNumeralFormatter;
import com.Newton.Newton.logic.Waves;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.transactions.TransferTransaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.Newton.Newton.utils.ApplicationSettings.NET_REQUEST_DELAY;
import static java.text.MessageFormat.format;

public class ConfirmMoveAssetsController extends DialogController  {

    private static final Logger log = LogManager.getLogger();

    private final List<TransferTransaction> transactions;
    private final double summand;
    private final AssetDetailsService assetDetailsService;

    @FXML private Button cancelButton;
    @FXML private Button sendButton;
    @FXML private TextArea resultTextArea;
    @FXML private TextField assetsTextField;
    @FXML private TextField feeTextField;
    @FXML private TextField recipientTextField;
    @FXML private ProgressBar progressBar;

    public ConfirmMoveAssetsController(RxBus rxBus) {
        super(rxBus);
        transactions = rxBus.getTransactions().getValue();
        summand = (double) 1/transactions.size();
        assetDetailsService = rxBus.getAssetDetailsService().getValue();
    }

    @FXML
	public void initialize() {
        assetsTextField.setText(String.valueOf(transactions.size()));
        feeTextField.setText(transactions.stream()
                .map(Transaction::getFee)
                .reduce(Long::sum)
                .map(aLong -> AssetNumeralFormatter.toReadable(aLong, Waves.DECIMALS))
                .orElse(""));
        recipientTextField.setText(transactions.get(0).getRecipient());

        JavaFxObservable.actionEventsOf(sendButton)
                .flatMap(ae -> getObservableResult())
                .subscribe(this::updateComponents);
    }

    private void disableButtons() {
        sendButton.setDisable(true);
        cancelButton.setText(getMessages().getString("close"));
    }

    private ObservableSource<String> getObservableResult() {
        progressBar.setVisible(true);
        return Observable.fromIterable(transactions)
                .delay(NET_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .map(this::broadcastTransaction)
                .doOnError(Throwable::printStackTrace)
                .retry(1)
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(result -> updateProgressBar());
    }

    private String broadcastTransaction(TransferTransaction transaction) {
        final var assetName = assetDetailsService.fetchAssetDetails(transaction.getAssetId()).getName();
        final var tx = getNodeService().sendTransaction(transaction);
        if (tx.isPresent())
            return format(getMessages().getString("move_success"), assetName);
        else
            return format(getMessages().getString("move_fail"), assetName);
    }

    private void updateComponents(String result){
        updateResultText(result);
        disableButtons();
    }

    private void updateProgressBar() {
        final var progress = progressBar.getProgress();
        progressBar.setProgress(progress+summand);
    }

    private void updateResultText(String result) {
        final var oldResultText = resultTextArea.getText();
        final var newResultText = oldResultText + "\n" + result;
        resultTextArea.setText(newResultText);
        resultTextArea.setScrollTop(Double.MAX_VALUE);
    }

}
