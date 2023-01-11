package com.ifnti.droneifnti;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class KalmanFilter {
    private static float[] vitesses_estimés = new float[3]; //dans l'ordre : vitesses angulaires selon x, y et z, dans le repère du téléphone.
    private static float timestamp_last_update = 0.0f;
    private static final float NS2S = 1.0f / 1000000000.0f; //pour convertir en nanosecondes
    private static Matrice matriceRotEstimée = Matrice.identite(3);

    /**
     * Méthode principale d'actualisation de l'estimation d'attitude.
     * Elle doit être invoquée à chaque fois qu'une nouvelle mesure est disponible.
     * C'est ici que devrait se passer la fusion de capteur avec le magnétomètre par exemple. J'ai essayé, en vain...
     * @param gyro_mesuré l'énenement correspondant à l'arrivée d'une nouvelle mesure issue du gyromètre.
     */
    public static void update_estimation(SensorEvent gyro_mesuré){
        if(timestamp_last_update == 0.0f){timestamp_last_update = gyro_mesuré.timestamp;return;}
        float dT = (gyro_mesuré.timestamp - timestamp_last_update) * NS2S;
        timestamp_last_update = gyro_mesuré.timestamp;
        Matrice deltaRotation = rotationVectorToRotationMatrix(gyro_mesuré.values[0] * dT, gyro_mesuré.values[1] * dT, gyro_mesuré.values[2] * dT);
        matriceRotEstimée = matriceRotEstimée.multiplierPar(deltaRotation);

        for(int i = 0; i < 3; i++){
            vitesses_estimés[i] = gyro_mesuré.values[i];
        }
    }

    /**
     * Convertit un vecteur de rotation, en la matrice qui exprime la même rotation.
     * Utile par exemple pour convertir les données issues du gyromètre en matrice de rotation.
     * @param x_rot Composante selon x du vecteur de rotation à convertir.
     * @param y_rot Composante selon y du vecteur de rotation à convertir.
     * @param z_rot Composante selon z du vecteur de rotation à convertir.
     * @return
     */
    private static Matrice rotationVectorToRotationMatrix(float x_rot, float y_rot, float z_rot){
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        float[] deltaRotationVector = new float[4];
        // Calculate the angular speed of the sample
        double omegaMagnitude = Math.sqrt(x_rot * x_rot + y_rot * y_rot + z_rot * z_rot);

        // Normalize the rotation vector if it's big enough to get the axis
        // (that is, EPSILON should represent your maximum allowable margin of error)
        if (omegaMagnitude > 0.005) {
            x_rot /= omegaMagnitude;
            y_rot /= omegaMagnitude;
            z_rot /= omegaMagnitude;
        }
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = new Float(omegaMagnitude / 2.0f);
        float sinThetaOverTwo = new Float(Math.sin(thetaOverTwo));
        float cosThetaOverTwo = new Float(Math.cos(thetaOverTwo));

        deltaRotationVector[0] = sinThetaOverTwo * x_rot;
        deltaRotationVector[1] = sinThetaOverTwo * y_rot;
        deltaRotationVector[2] = sinThetaOverTwo * z_rot;
        deltaRotationVector[3] = cosThetaOverTwo;

        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        return Matrice.matriceCarre(deltaRotationMatrix, 3);

    }

    /**
     * Convertit les angles atimuth, roll pitch en matrice de rotation.
     * Inspiré de https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartphone-apps/382048#382048
     * @param Φ azimuth entre -π et +π
     * @param α pitch   entre -π/2 et +π/2
     * @param β roll    entre -π et +π
     * @return la matrice de rotation
     */
    public static Matrice eulerToRotationMatrix(float α, float β, float Φ){
        Matrice m = new Matrice(3, 3);
        float cΦ = (float) Math.cos(Φ);
        float sΦ = (float) Math.sin(Φ);
        float cθ = (float) Math.cos(α);
        float sθ = (float) Math.sin(α);
        float cψ = (float) Math.cos(β);
        float sψ = (float) Math.sin(β);
        m.set(0, 0, cΦ*cψ - sΦ*sψ*sθ);                          //Composante du vecteur i selon E
        m.set(0, 1, sΦ*cθ);                 //Composante du vecteur k selon E
        m.set(0, 2, cΦ*sψ + sΦ*cψ*sθ);                 //Composante du vecteur l selon E
        m.set(1, 0, -sΦ*cψ - cΦ*sψ*sθ);                 //Composante du vecteur i selon N
        m.set(1, 1, cΦ*cθ);   //Composante du vecteur k selon N
        m.set(1, 2, -sΦ*sψ + cΦ*cψ*sθ);   //Composante du vecteur l selon N
        m.set(2, 0, -sψ*cθ);                //Composante du vecteur i selon G
        m.set(2, 1, -sθ);//Composante du vecteur k selon G
        m.set(2, 2, cψ*cθ);//Composante du vecteur l selon G
        return m;

    }

    /**
     * Renvoie un tableau contenant l'estimation d'orientation actuelle
     * @return un tableau contenant dans l'ordre : azimuth, pitch, roll, en radians.
     */
    public static float[] getCurrentOrientation(){
        float[] angles = new float[3];
        float[] matRot = matriceRotEstimée.toArray();
        SensorManager.getOrientation(matRot, angles);
        return angles;
    }

    /**
     * Réinitialise l'estimation d'orientation aux angles spécifiés. Cette méthode est invoquée lorsque le pilote envoie une indication d'orientation
     * selon au moins un des trois angles.
     * @param azimuth nouveau cap estimé en radians, entre -π et π
     * @param pitch nouvelle assiette estimée en radians, entre -π/2 et +π/2
     * @param roll nouvelle inclinaison estimée en radians, entre -π et +π
     */
    public static void setCurrentOrientation(float azimuth, float pitch, float roll){
        float pi = (float) Math.PI;
        if(azimuth<-pi || azimuth>pi || pitch < -pi/2 || pitch>pi/2 || roll<-pi || roll>pi){
            return;
        }
        matriceRotEstimée = eulerToRotationMatrix((float) (pitch), (float)(roll), (float)(azimuth));
    }

    /**
     * @return les dernières vitesses angulaires mesurées par le gyro, en radians/sec, dans l'ordre pitch, roll, azimuth
     */
    public static float[] getVitesses_estimés() {
        return vitesses_estimés;
    }
}
