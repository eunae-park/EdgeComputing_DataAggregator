# 엣지컴퓨팅 데이터 공유 관리 S/W
엣지 컴퓨팅 시스템을 구성된 디바이스들간의 데이터 공유를 위한 프레임워크로서, 시스템을 자동으로 구성하고 디바이스간 데이터 공유와 보안등급 및 접근 정책 관리 등의 주요 기능을 수행함

# info_device.txt
info_device.txt는 디바이스의 전체적인 정보를 가지는 설정 파일입니다.
```
uuid // 디바이스 ID
data path // 데이터 폴더 위치
cert path // 인증서 폴더 위치
DBMS // 사용하는 DBMS
database,table,id,password // 데이터베이스, 테이블명, 사용자ID, 사용자PW ','로 구분
mode // upnp or master or slave upnp일시 처음 접속하는 디바이스가 master 나머지는 slave
device IP // 디바이스IP or auto auto
```

# 사용
```
0 // 종료
1 // 디바이스 정보 읽기
2 // 전체 데이터 읽기
3 // 데이터 상세 정보
4 // 데이터 읽기 / 분산 공유
5 // 데이터 쓰기
6 // 데이터 제거
7 // 데이터 전송
```
