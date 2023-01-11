package com.ifnti.droneifnti;

/**
 * Cette classe sert à réaliser l'asservissement en assiette, inclinaison et cap. À noter : l'asservissement en Cap devra modifier l'inclinaison.
 * Ainsi, il n'est pas possible d'asservir le cap et l'inclinaison simultanément.
 *
 * L'asservissement retenu est de type proportionnel-dérivé.
 */
public class Vol {
    private static float consigneCap;
    private static boolean suiviCapActif = false;

    private static float consigneInclinaison = 0.0f; //Par défaut, on va tout droit
    private static float consigneAssiette = -10.0f; //Le drone doit descendre un peu pour se maintenir en vol.

    private static float Kp_incli = 3.0f; //Gain proportionnel pour l'asservissement de l'inclinaison
    private static float Kd_incli = 1.0f; //Gain dérivé        pour l'asservissement de l'inclinaison
    private static float Kp_assiette = 1.0f;       //Gain proportionnel pour l'asservissement de la pente
    private static float Kd_assiette = 1.0f;       //Gain dérivé        pour l'asservissement de la pente

    public static byte[] calculerCommandesServos(){
        byte commandeServosInclinaison = 0;
        if(suiviCapActif){
            float capActuel = (float)(KalmanFilter.getCurrentOrientation()[0] * 180.0 / Math.PI);
            //Là, je vous laisse coder. Cette fonctionnalité n'est pas obligatoire mais elle peut etre sympa.
            //Il s'agit de faire en sorte de maintenir capActuel égal à consigneCap, en passant par des consignes d'inclinaison

            //commandeServosInclinaison = quelque chose ... à vous de trouver !
        }else{
            float inclinaisonActuelle = (float)(KalmanFilter.getCurrentOrientation()[2] * 180.0 / Math.PI);
            float vitesseIncliActu = (float)(KalmanFilter.getVitesses_estimés()[1] * 180.0 / Math.PI);  //la dérivée de l'inclinaison actuelle
            commandeServosInclinaison = calculerCommande(consigneInclinaison, inclinaisonActuelle, vitesseIncliActu, Kp_incli, Kd_incli);
        }


        float assietteActuelle = (float)(KalmanFilter.getCurrentOrientation()[1] * 180.0 / Math.PI);
        float vitesseAssietteActu = (float)(KalmanFilter.getVitesses_estimés()[0] * 180.0 / Math.PI);  //la dérivée de l'assiette actuelle
        byte commandeServoAssiette = calculerCommande(consigneAssiette, assietteActuelle, vitesseAssietteActu, Kp_assiette, Kd_assiette);

        byte[] consignes = {commandeServosInclinaison, (byte)-commandeServosInclinaison, commandeServoAssiette, 0};
        return consignes;
    }

    /**
     * Correcteur proportionnel-dérivé. Calcule la commande à envoyer à un actionneur
     * @param consigne but à atteindre pour la grandeur à asservir.
     * @param valActuelle position actuelle de la grandeur à asservir
     * @param vitesseActuelle vitesse d'évolution actuelle de la grandeur à asservir
     * @param Kp Gain proportionnel
     * @param Kd Gain dérivé
     * @return La commande à envoyer à l'actionneur, entre -127 et +127.
     */
    private static byte calculerCommande(float consigne, float valActuelle, float vitesseActuelle, float Kp, float Kd){
        float erreur = consigne - valActuelle;
        int commande = (int) Math.floor(Kp * erreur + Kd * vitesseActuelle);
        if(commande > 127) commande = 127;
        if(commande < -127) commande = -127;
        return (byte) commande;
    }
}
