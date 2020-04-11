package com.Newton.Newton.gui.dialog;

import com.Newton.Newton.bus.RxBus;
import com.wavesplatform.wavesj.AssetDetails;
import io.nayuki.qrcodegen.QrCode;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;



public class QrCodeController extends DialogController  {
    private static final Logger logger = LogManager.getLogger();
    private String address;
    private Image image;
    @FXML private ImageView qrimage;
    @FXML private Button closeButton;
    @FXML private Label addressLabel;

    public QrCodeController(RxBus rxBus) {
        super(rxBus);
        address = rxBus.getPrivateKeyAccount().getValue().getAddress();
    }

    @FXML
    public void initialize() {
        addressLabel.setText(address);
        QRCode(address);
        qrimage.setImage(image);

        JavaFxObservable.actionEventsOf(closeButton)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::closeDialog);
    }
    private Image QRCode(String add){
        try {
        QrCode qr0 = QrCode.encodeText(add, QrCode.Ecc.HIGH);
        BufferedImage img = qr0.toImage(4, 10);
        image = SwingFXUtils.toFXImage(img, null);
        // ImageIO.write(img, "png", new File("qr-code.png"));
        } catch (Exception ex) {
            logger.log(Level.FATAL,"",ex);
        }
        return image;
    }

    private void closeDialog(ActionEvent actionEvent) {
        getStage().close();
    }

}
