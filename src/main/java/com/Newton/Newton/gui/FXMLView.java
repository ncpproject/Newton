package com.Newton.Newton.gui;

public enum FXMLView {
    ADDRESS("/wallet/address.fxml"),
    ASSETS("/wallet/assets.fxml"),
//    BLOCKHEADER_CELL("/wallet/blockHeaderCell.fxml"),
    BURN_ASSETS("/wallet/burnAssets.fxml"),
    BURN_TOKEN("/dialog/burnToken.fxml"),
    CONFIRM_SEED("/accountCreator/confirmSeed.fxml"),
    CONFIRM_BURN_ASSETS("/dialog/confirmBurnAssets.fxml"),
    CONFIRM_MASS_TRANSFER("/dialog/confirmMassTransfer.fxml"),
    CONFIRM_TRANSACTION("/dialog/confirmTransfer.fxml"),
    DASHBOARD("/wallet/dashboard.fxml"),
    GET_SEED("/accountCreator/getSeed.fxml"),
    IMPORT_ACCOUNT("/accountCreator/importAccount.fxml"),
    LEASING("/wallet/leasing.fxml"),
    LOGIN("/login/login.fxml"),
    LOGIN_INFO("/accountCreator/loginInfo.fxml"),
    MASS_TRANSFER("/wallet/massTransfer.fxml"),
    NETWORK("/accountCreator/network.fxml"),
    REISSUE_TOKEN("/dialog/reissueToken.fxml"),
    SEND("/wallet/send.fxml"),
    SIGN("/wallet/sign.fxml"),
    SETTINGS("/wallet/settings.fxml"),
    TOS("/dialog/tos.fxml"),
    TRANSACTIONS("/wallet/transactions.fxml"),
    TRANSACTION_INFO("/dialog/transactionInfo.fxml"),
    QR_CODE("/dialog/qrcode.fxml"),
    TRANSFER("/wallet/transfer.fxml"),
    WALLET("/wallet/walletView.fxml");

    final private String path;

    FXMLView(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }

}
