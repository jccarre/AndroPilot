# AndroPilot
Android and arduino app, to pilot a drone. The Android phone is embarked in the drone and sends its commands to the arduino via USB serial.

This app has been developped for fixed-wing drones.
The phone must be placed in the drone, with its screen facing up, and the top edge of the screen oriented towards the nose of the plane.

An arduino must be connected to the phone, via USB, using an OTG cable.

The android app does the following : 

 * attitude estimation (yaw, pitch and roll), by integrating the gyroscope measurements of the phone. Although it would be much easier, we can not use the ROTATION_VECTOR proposed by Android, since it uses gravity measurement to get the direction of the vertical. In a flying plane, the acceleration is not always oriented vertically. Indeed, when the planes is turning (i.e. when inclinaison is not null), the acceleration will be altered by the rate of turn. Using the magnetometer (with a sensor fusion) would help to reduce the gyrometer drift. I unhopefully didn't manage to do so.
 * Sms receiving from a pilot on ground. The SMS is interprated to get orders, and execute them.
 * pitch and roll regulation with a proportionnal - derived corrector.
 * Sending commands to the arduino via USB.
 
The Arduino App does the following : 
 * Receiving commands from tha Android via USB
 * Sending those commands to the servos.
 

# Configuration initiale
 * Chaque gouverne a une amplitude maximale qui est différente. Il faut donc commencer par noter les consignes maximales et minimales
 que chaque servo peut recevoir, correspondant aux positions maximales et minimales de chaque gouverne. Ces valeurs sont à renseigner dans les 
 tableaux `posMaxServos[]` et `posMinServos[]` du fichier communication_android_bidirectionnelle.ino
 
 * L'asservissement en assiette et en inclinaison est géré dans le fichier "Android app/app/src/main/java/com/ifnti/droneifnti/Vol.java". 
 Il s'agit d'un asservissement de type proportionnel-dérivé. Pour chaque axe, il faut jouer sur Kp (le gain proportionnel) et Kd (le gain dérivé).
 Augmenter Kp mènera à une plus grande réactivité, mais aussi à une plus grande instabilité.
 Augmenter Kd mènera à une meilleure stabilité.
 
# Lancement de la communication USB
La communication USB a été faite en m'inspirant de http://android-er.blogspot.com/2014/09/bi-directional-communication-between_24.html
Dans ce tuto, il propose d'ajouter une consigne dans le manifest, pour que le branchement d'un périphérique USB déclenche le lancement de l'application, et ainsi ouvre la communication. Cela peut sembler étrange, mais il faut donc suivre la procédure suivante : 

 * Lancer l'application
 * Brancher l'arduino -> Cela relance automatiquement l'application (la première fois, une autorisation d'accès à l'USB sera demandée)
 * La communication est établie.
 * Si l'arduino est déjà branché avant le lancement de l'application, la communication ne fonctionnera pas. Il faudra le débrancher puis le rebrancher.

# Pilotage à distance
Pour recevoir des sms, il est nécessaire d'accorder la mermission envoi/réception de SMS à l'application. Pour cela, une fois l'application installée, se rendre dans réglages->Applications->DroneIFNTI->Permissions accordées et cocher la case "SMS".
L'application attend des sms de la part du pilote suivant un format particulier. Voir la documentation de la méthode "interpreterSMS(String message)" dans le fichier "Android app/app/src/main/java/com/ifnti/droneifnti/RecepteurSMS.java".

Exemple : le sms "p12 r-5 t-8 v0" Signifie : tu es actuellement à 12° d'inclinaison et 5° d'assiette à piquer. Je te demande d'aller à 0° d'inclinaison (vol en ligne droite) et 8° d'assiette à piquer.

Note : bien que le gyromètre permette un certain niveau d'asservissement, il a naturellement tendance à dériver. D'où la nécessité de dire au drone "tu te trouves actuellement à ...".

# Suivi visuel
Il est possible de lancer une conversation vidéo sur Signal entre le téléphone du drone et un autre téléphone au sol. Ainsi, le pilote au sol voit ce que voit le drone. Ceci peut faciliter le pilotage.
De même, il est conseillé d'activer le partage de position sur google maps depuis le téléphone du drone. Ainsi, le pilote au sol pourra suivre la position du drone sur une carte en temps réel.
