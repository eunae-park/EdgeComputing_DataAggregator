# EdgeComputing_DataAggregator

info_device.txt

Edge Node UUID
Data Folder DIR/
Cert Folder DIR/
DBMS name
DB information
UPNP mode
upnp information
My IP Address
 

//DB info
MySQL :  DB명,table명,userID,userPW
 (ex) mecTrace,file_management,mecTrace,penta&keti0415!
SQLite : DB명,table명,userID,userPW,DB경로 
// 없으면 null


//upnp info
자동모드 : upnp
수동모드 : master
수동모드 : slave\nmaster_ip

//My IP Address
자동으로 가져오기 : auto
수동설정 : ip 입력 


수동모드 - keti.d-sharing.kr   or   keties.iptime.org
vi /etc/hosts
127.0.0.1 keti.d-sharing.kr
keti.d-sharing.kr
keties.iptime.org
