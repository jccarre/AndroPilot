package com.ifnti.droneifnti;
//inspiré de http://android-er.blogspot.com/2014/09/bi-directional-communication-between_24.html
//Pour le débuggage par wifi : https://www.how2shout.com/how-to/android-adb-over-wifi-without-usb.html

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class CommunicationUSB {
    private MainActivity mainActivityInstance = null;  //stock la référence vers l'activité en cours.

    private static final String TAG = "AndroidUsbHostArduino";

    private static final byte IGNORE_00 = (byte) 0x00;
    private static final byte SYNC_WORD = (byte) 0xFF;

    private static final int CMD_LED_OFF = 2;
    private static final int CMD_LED_ON = 1;
    private static final int CMD_TEXT = 3;
    private static final int MAX_TEXT_LENGTH = 16;

    private UsbManager usbManager;
    private UsbDevice deviceFound;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterfaceFound = null;
    private UsbEndpoint endpointOut = null;
    private UsbEndpoint endpointIn = null;

    public CommunicationUSB(MainActivity instance){
        this.mainActivityInstance = instance;
    }

    private void setDevice(UsbDevice device) {
        usbInterfaceFound = null;
        endpointOut = null;
        endpointIn = null;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbif = device.getInterface(i);

            UsbEndpoint tOut = null;
            UsbEndpoint tIn = null;

            int tEndpointCnt = usbif.getEndpointCount();
            if (tEndpointCnt >= 2) {
                for (int j = 0; j < tEndpointCnt; j++) {
                    if (usbif.getEndpoint(j).getType()
                            == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (usbif.getEndpoint(j).getDirection()
                                == UsbConstants.USB_DIR_OUT) {
                            tOut = usbif.getEndpoint(j);
                        } else if (usbif.getEndpoint(j).getDirection()
                                == UsbConstants.USB_DIR_IN) {
                            tIn = usbif.getEndpoint(j);
                        }
                    }
                }

                if (tOut != null && tIn != null) {
                    // This interface have both USB_DIR_OUT
                    // and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
                    usbInterfaceFound = usbif;
                    endpointOut = tOut;
                    endpointIn = tIn;
                }
            }
        }

        if (usbInterfaceFound == null) {
            return;
        }

        deviceFound = device;

        if (device != null) {
            UsbDeviceConnection connection =
                    usbManager.openDevice(device);
            if (connection != null &&
                    connection.claimInterface(usbInterfaceFound, true)) {

                connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
                connection.controlTransfer(0x21, 32, 0, 0,
                        new byte[] { (byte) 0x80, 0x25, 0x00,
                                0x00, 0x00, 0x00, 0x08 },
                        7, 0);

                usbDeviceConnection = connection;

                //Les deux lignes suivantes ont été commentées car je n'utilise pas la récéption de
                //données en provenance de l'Arduino. Inutile de lancer le thread correspondant.
                //Ce Thread servait à écouter l'entrée serie sur le port USB.
                //Thread thread = new Thread(this);
                //thread.start();

            } else {
                usbDeviceConnection = null;
            }
        }
    }

    /**
     * Envoie une commande à l'arduino. Trois commandes sont disponibles :
     * CMD_LED_OFF, CMD_LED_ON et CMD_TEXT
     * CMD_TEXT est utilisé pour envoyer une chaine de caractères.
     * @param control la commande à envoyer.
     */
    private void sendArduinoCommand(int control) {
        synchronized (this) {

            if (usbDeviceConnection != null) {
                byte[] message = new byte[2];
                message[0] = SYNC_WORD;
                message[1] = (byte)control;

                usbDeviceConnection.bulkTransfer(endpointOut,
                        message, message.length, 0);

                Log.d(TAG, "sendArduinoCommand: " + String.valueOf(control));
            }
        }
    }

    /**
     * Envoie une chaine de caractères à l'arduino. La chaine ne doit pas dépasser MAX_TEXT_LENGTH
     * @param s la chaîne à envoyer.
     */
    private void sendArduinoText(String s) {
        synchronized (this) {
            if (usbDeviceConnection != null) {
                Log.d(TAG, "sendArduinoText: " + s);
                int length = s.length();
                if(length>MAX_TEXT_LENGTH){
                    length = MAX_TEXT_LENGTH;
                }
                byte[] message = new byte[length + 3];
                message[0] = SYNC_WORD;
                message[1] = (byte)CMD_TEXT;
                message[2] = (byte)length;
                s.getBytes(0, length, message, 3);

                usbDeviceConnection.bulkTransfer(endpointOut,
                        message, message.length, 0);
            }
        }
    }

    /**
     * Envoie les consignes pour les servos du drone. Pour le moment, il n'y a que trois gouvernes.
     * Si vous voulez en rajouter une dernière (gouverne de direction ou bien parachute), il reste de la place.
     * @param consignesServos tableau de 4 éléments :
     *  [0] consigne pour l'aileron gauche, entre -126 et +127
     *  [1] consigne pour l'aileron droit, entre -126 et +127
     *  [2] consigne pour la gouverne de profondeur, entre -126 et +127
     *  [3] consigne pour un éventuel quatrième servo, entre -126 et +127
     */
    public void sendArduinoConsigneServos(byte[] consignesServos){
        if (usbDeviceConnection != null) {
            byte[] message = new byte[7];
            message[0] = SYNC_WORD;
            message[1] = CMD_TEXT;
            message[2] = 4;
            for(byte i = 0; i < 4; i++){
                if(consignesServos[i] == -127){consignesServos[i] = -126;}
                message[i+3] = (byte) (consignesServos[i] + 127);
            }
            usbDeviceConnection.bulkTransfer(endpointOut,
                    message, message.length, 0);
        }
    }

    /**
     * Cette méthode doit être invoquée dans le onResume de l'activité principale.
     * En effet, un intent filter du manifest permet de faire en sorte que MainActivity soit lancée (ou relancée) dès qu'un nouveau périphérique USB est inséré.
     * le onResume est alors invoqué et cette méthode permet d'affecter la référence vers ce périphérique
     */
    public void resumeUSB(){
        Intent intent = this.mainActivityInstance.getIntent();
        String action = intent.getAction();
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (deviceFound != null && deviceFound.equals(device)) {
                setDevice(null);
            }
        }
    }

    public void assignUsbManager(){
        usbManager = (UsbManager)this.mainActivityInstance.getSystemService(Context.USB_SERVICE);
    }

    /**
     * Méthode utilisée uniquement pour le débug : envoie la chaîne de caractères ayant la plus petite valeur possible en byte.
     * Doit faire bouger tous les servos vers le bas.
     */
    public void sendServoBas(){
        sendArduinoText("!!!!");
    }

    /**
     * Méthode utilisée uniquement pour le débug : envoie la chaîne de caractères ayant la plus grande valeur possible en byte.
     * Doit faire bouger tous les servos vers le haut.
     */
    public void sendServoHaut(){
        sendArduinoText("ýýýý");
    }
}
