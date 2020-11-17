# EdgeComputing_DataAggregator

folder_name.txt
   - 파일이 있는 메인 폴더 이름 작성 (ex) /home/data/
 
 mysql_info.txt
   - edge 마다 DB를 가지고 있으며, 현재 DB명과 user는 고정
   - 추후, 정보 입력 방식으로 변경해야함.
  
src/EdgeDataAggregator.java
   - main() class

fileRead()
   - 현재 분산 시스템으로 파일 가져오기 하는 중
