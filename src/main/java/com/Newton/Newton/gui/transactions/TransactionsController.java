package com.Newton.Newton.gui.transactions;

import com.Newton.Newton.bus.RxBus;
import com.Newton.Newton.gui.MasterController;
import com.Newton.Newton.logic.*;
import com.Newton.Newton.utils.ApplicationSettings;
import com.wavesplatform.wavesj.AssetDetails;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transfer;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class TransactionsController extends MasterController {

    private static final Logger log = LogManager.getLogger();

    private ObservableList<TransactionDetails> transactionList;
    private ObservableList<AssetDetails> assetDetails;
    private Observable<Long> emitter;
    private SortedList<TransactionDetails> sortedList;
    private Clipboard clipboard;

    @FXML private TableView<TransactionDetails> transactionsTableView;
    @FXML private TableColumn<TransactionDetails, String> transactionDateTableColumn;
    @FXML private TableColumn<TransactionDetails, TransactionSummary> transactionTypeTableColumn;
    @FXML private ComboBox<Integer> txAmountComboBox;
    @FXML private ComboBox<TxFilter> txFilterComboBox;

    public TransactionsController(final RxBus rxBus) {
        super(rxBus);
        emitter = ConnectableObservable.interval(ApplicationSettings.TX_LIST_REQUEST_DELAY, TimeUnit.SECONDS);
    }

    @FXML
	public void initialize() {
        clipboard = Clipboard.getSystemClipboard();
        initializeTxAmountComboBox();
        initializeTxFilter();
        initializeTableView();

        privateKeyAccountSubject.observeOn(JavaFxScheduler.platform())
                .subscribe(pk -> clearTableViewForReload());

        final var txAmountObservable = JavaFxObservable.valuesOf(txAmountComboBox.getSelectionModel().selectedItemProperty())
                .doOnNext(integer -> clearTableViewForReload());

        ConnectableObservable.merge(privateKeyAccountSubject, emitter, txAmountObservable)
                .switchMap(o -> loadTransactionHistoryObservable())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::populateList, Throwable::printStackTrace);

        JavaFxObservable.valuesOf(txFilterComboBox.valueProperty())
                .subscribe(this::filterList);


    }

    private Observable<List<TransactionDetails>> loadTransactionHistoryObservable() {
        return Observable.just(0)
                .delay(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .map(o1 -> loadTransactionHistory());
    }

    private void initializeTxFilter() {
        txFilterComboBox.getItems().setAll(TxFilter.class.getEnumConstants());
        txFilterComboBox.getSelectionModel().select(0);
    }

    private void initializeTxAmountComboBox() {
        txAmountComboBox.getItems().addAll(50, 100, 250, 500, 1000);
        txAmountComboBox.getSelectionModel().select(0);
    }

    private void initializeTableView() {
        transactionList = FXCollections.observableArrayList();
        sortedList = new SortedList<TransactionDetails>(transactionList);
        sortedList.comparatorProperty().bind(transactionsTableView.comparatorProperty());
        transactionsTableView.setItems(sortedList);

        transactionsTableView.setRowFactory(p -> new TransactionTableRow(getMessages(), clipboard));
        transactionTypeTableColumn.setCellFactory(p -> new TransactionsTableCell());
    }

    private List<TransactionDetails> loadTransactionHistory() {
        final var address = getPrivateKeyAccount().getAddress();
        final var assetDetailsService1 = rxBus.getAssetDetailsService().getValue();



        final var txAmount = txAmountComboBox.getValue();
        final var txs = getNodeService().fetchAddressTransactions(address, txAmount).orElse(new ArrayList<>());

        return txs.stream().parallel()
                .map(tx -> new TransactionDetails(assetDetailsService1, tx, address, getMessages()))
                .filter(t -> t.getTransactionSummary().getHeadline().endsWith("NCP Project") ||
                             t.getTransactionSummary().getHeadline().endsWith(" Waves"))
                .sorted(Comparator.comparingLong(TransactionDetails::getEpochDateTime).reversed())
                .collect(Collectors.toUnmodifiableList());
    }

    private void filterList(final TxFilter txFilter) {
        if (txFilter == TxFilter.All) {
            transactionsTableView.setItems(sortedList);
        } else {
            final var sorted = transactionList.filtered(transactionDetails -> transactionDetails.isOfTypeFilter(txFilter));
            final var sortedList = new SortedList<>(sorted);
            sortedList.comparatorProperty().bind(transactionsTableView.comparatorProperty());
            transactionsTableView.getItems().removeAll();
            transactionsTableView.setItems(sortedList);
        }
    }

    private void clearTableViewForReload() {
        final var loadingLabel = new Label(getMessages().getString("loading"));
        transactionList.clear();
        transactionsTableView.setPlaceholder(loadingLabel);
    }

    private void populateList(List<TransactionDetails> list) {
        if (!list.equals(transactionList)) {
            final var focusedItem = transactionsTableView.selectionModelProperty().get().getFocusedIndex();
            transactionList.clear();
            transactionList.setAll(list);
            transactionsTableView.setPlaceholder(new Label());
            transactionsTableView.selectionModelProperty().get().focus(focusedItem);
        }
    }
}
