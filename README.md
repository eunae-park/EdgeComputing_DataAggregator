# EdgeComputing_DataAggregator

folder_name.txt
   - 데이터(파일)가 있는 메인 폴더 이름 작성 (ex) /home/data/
 
 mysql_info.txt
   - edge 마다 DB를 가지고 있으며, 현재 DB명과 user는 고정
   - mysql DB 정보를 확인하여, DB를 작성하고 메인 폴더 내의 데이터에 대한 메타데이터를 DB에 추가
   - 추후, 정보 입력 방식으로 변경할 예정
  
src/EdgeDataAggregator.java
   - main() class가 있으며, 여기서 run()
   
파일 읽어오기
   - 현재 분산 시스템으로 파일 가져오기 접목 중
   - 파일이 있으면 읽어오기 : slaveWorker.java와 masterWorker.java에서 else if(func == 5) 안 내용 주석 처리 및 version #2 주석 해제
