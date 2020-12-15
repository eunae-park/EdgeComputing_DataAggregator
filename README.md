# EdgeComputing_DataAggregator

folder_name.txt
   - 데이터(파일)가 있는 메인 폴더 이름 작성(본인에 맞춰 수정) (ex) /home/data/

hash_info.txt
   - indirect data의 경우, 해시코드 인증에 따라 전달 여부를 결정하며 해시코드 제작 데이터가 저장되있음(본인에 맞춰 수정)

 mysql_info.txt
   - edge 마다 메타데이터 DB를 가지고 있으며, 현재 DB명과 user는 고정되있으므로, 정보에 맞춰 DB 생성
   - mysql DB 정보를 확인하여, DB를 작성하고 메인 폴더 내의 데이터에 대한 메타데이터를 DB에 추가
   - 추후, 정보 입력 방식으로 변경할 예정
  
src/EdgeDataAggregator.java
   - main() class가 있으며, 여기서 run()
   - master가 없으면 자신이 master가 되며, master가 있으면 자동으로 slave로 동작함
   - 메타데이터 DB의 security 정보에 따라 데이터를 읽어오며, direct data는 데이터에 접근한 엣지 노드에 저장함
   (security(1):indirect data, security(2):direct data)
   

