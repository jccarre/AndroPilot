package com.ifnti.droneifnti;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class RecepteurSMS extends BroadcastReceiver {
    private static final String TAG = RecepteurSMS.class.getSimpleName();
    private static final String pdu_type = "pdus";



    private static String numTelPilote = "+22812345678"; //Numéro de téléphone auquel seront envoyé les sms de feedBack. Et aussi seul numéro dont les sms reçus seront interprétés.



    private static String message_reçu = "";

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the SMS message.
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;

        String format = bundle.getString("format");
        // Retrieve the SMS message received.
        Object[] pdus = (Object[]) bundle.get(pdu_type);
        if (pdus != null) {
            // Check the Android version.
            boolean isVersionM =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
            // Fill the msgs array.
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                // Check Android version and use appropriate createFromPdu.
                if (isVersionM) {
                    // If Android version M or newer:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    // If Android version L or older:
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }

                // On vérifie si le SMS reçu vient du pilote. Sinon, on l'ignore.
                if(numTelPilote.equals(msgs[i].getOriginatingAddress())){

                }

                String strMessage = msgs[i].getMessageBody();
                // Log and display the SMS message.
                Log.d(TAG, "onReceive: " + strMessage);


                //interpréter le SMS

                interpreterSMS(strMessage);
            }
        }
    }

    /**
     * Les SMS seront composés d'une succession de commandes, séparés par des espaces.
     * Une commande est constituée d'une lettre indiquant le type de commande, suivi de la valeur de cette commande.
     * c : cap en degrés, entre 0 (plein nord) et 359. 90 = est, 180=sud et 270=ouest
     * t : taux de descente en degrés, entre 0 (vol horizontal) et 90 (plein piqué vers le sol).
     * v : virage, en degrés. entre -30 (virage à gauche) et +30 (virage à droite). Cette consigne spécifie l'angle d'inclinaison que le drone doit prendre.
     *     On ne peut pas spécifier un cap et un virage. Ce serait contradictoire. Dans ce cas, le drone ignorera la consigne de cap et suivra la consigne de virage.
     * a : azimuth en degrés, entre 0 et 359. Pour corriger l'estimateur du drone. Cette consigne indique au drone : "Tu te trouves actuellement au cap x".
     * p : pitch en degrés, entre -90 et +90. Pour corriger l'estimateur du drone. Cette consigne indique au drone : "Ton assiette est actuellement de x degrés.".
     * r : roll en degrés, entre -180 et +180. Pour corriger l'estimateur du drone. Cette consigne indique au drone : "Tu es actuellement incliné de x degrés.".
     * @param message La chaine de caractère à interpréter.
     */
    public static void interpreterSMS(String message){
        String[] messages = message.split(" ");
        int cap = -1000, tauxDescente = -1000, virage = -1000, azimuth_reçu = -1000, pitch_reçu = -1000, roll_reçu = -1000;
        for(String str : messages){
            String typeCommande = str.substring(0, 1);
            int valeur_commande = Integer.parseInt(str.substring(1, str.length()));
            switch (typeCommande){
                case "c":
                    cap = valeur_commande;
                    break;
                case "t":
                    tauxDescente = valeur_commande;
                    break;
                case "v":
                    virage = valeur_commande;
                    break;
                case "a":
                    azimuth_reçu = valeur_commande;
                    break;
                case "p":
                    pitch_reçu = valeur_commande;
                    break;
                case "r":
                    roll_reçu = valeur_commande;
                    break;
            }
            if(cap!=-1000 && virage!=-1000){cap=-1000; } //En cas d'ordres contradictoires, l'ordre de virage est prioritaire.
            float azimuth_actuel = KalmanFilter.getCurrentOrientation()[0];
            float pitch_actuel = KalmanFilter.getCurrentOrientation()[1];
            float roll_actuel = KalmanFilter.getCurrentOrientation()[2];

            if(azimuth_reçu > 180){azimuth_reçu -= 360;} //Le reste des fonctions d'Android attendent un cap entre -Pi et Pi. Et pas entre 0 et 2Pi.

            if(azimuth_reçu != -1000){azimuth_actuel = (float)(azimuth_reçu * Math.PI / 180.0f);}
            if(pitch_reçu != -1000){pitch_actuel = (float) (pitch_reçu * Math.PI / 180.0f);}
            if(roll_reçu != -1000){roll_actuel = (float) (roll_reçu * Math.PI / 180.0f);}
            KalmanFilter.setCurrentOrientation(azimuth_actuel, pitch_actuel, roll_actuel);
        }
    }

    public static String getMessage_reçu() {
        return message_reçu;
    }

    public static void setNumTelPilote(String numTelPilote) {
        RecepteurSMS.numTelPilote = numTelPilote;
    }


}