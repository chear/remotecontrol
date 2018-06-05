1) ./sys_signed/a20_android4p2.jks while signd command ,and pwd by zkar123

keytool-importkeypair -k ./a20/a20_android4p2.jks -p zkar1234 -pk8 ./a20/platform.pk8 -cert ./a20/platform.x509.pem -alias a20

2) ./a20_android4p2.jks without system signed ,pwd is 'a2012345'