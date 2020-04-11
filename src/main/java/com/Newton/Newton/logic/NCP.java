package com.Newton.Newton.logic;

import com.wavesplatform.wavesj.AssetDetails;

import java.util.Objects;

import static com.Newton.Newton.logic.AssetNumeralFormatter.toLong;
import static com.Newton.Newton.logic.AssetNumeralFormatter.toReadable;

public class NCP implements Transferable {

    public static final long FEE = 100000;
    public static final String NAME = "NCP";
    public static final String ASSET_ID = "G1emXxRGHxDSe5sduNmqULJdKngRjk3C6d7nPw19fK86";
    public static final String ISSUER = "3PFWyRcfC16ZnJ372sDAvgjUyp9R9WrJmyw";
    public static final String MIN_FEE = "0.001";
    public static final String SPONSOR_BALANCE = "0.001";
    public final static long QUANTITY = 13646999900000000L;
    public static final int DECIMALS = 8;
    private final String balance;


    public NCP(Long balance) {
        this.balance = toReadable(balance, DECIMALS);
    }

    public NCP(String balance) {
        this.balance = balance;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getBalance() {
        return balance;
    }

    @Override
    public String getAssetId() {
        return ASSET_ID;
    }

    @Override
    public int getDecimals() {
        return DECIMALS;
    }

    @Override
    public String getMinFee() {
        return MIN_FEE;
    }

    @Override
    public String getSponsorBalance() {
        return SPONSOR_BALANCE;
    }

    @Override
    public long balanceAsLong() {
        return toLong(balance, DECIMALS);
    }

    @Override
    public long minFeeAsLong() {
        return toLong(MIN_FEE, DECIMALS);
    }

    @Override
    public long sponsorBalanceAsLong() {
        return toLong(SPONSOR_BALANCE, DECIMALS);
    }

    public static AssetDetails getAssetDetails() {
        return new AssetDetails(ASSET_ID, 0L, 0L, ISSUER, NAME, "", 8, false, QUANTITY, false, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Waves)) return false;
        Waves waves = (Waves) o;
        return Objects.equals(getBalance(), waves.getBalance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBalance());
    }

}
