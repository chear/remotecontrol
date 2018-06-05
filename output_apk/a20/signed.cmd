java -jar signapk.jar platform.x509.pem platform.pk8 ./com.zkar.pis.remotecontrol.apk com.zkar.pis.remotecontrol_signed.apk

echo "--- A20 signed finished , com.zkar.pis.remotecontrol.apk should be deleted.---"
del com.zkar.pis.remotecontrol.apk
