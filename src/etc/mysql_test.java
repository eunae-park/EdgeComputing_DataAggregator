package etc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;


public class mysql_test {
    public static void main(String[] args) {
        // Connection 객체를 자동완성으로 import할 때는 com.mysql.connection이 아닌
        // java 표준인 java.sql.Connection 클래스를 import해야 한다.
        Connection conn = null;
        Statement state = null; 
        PreparedStatement pstate = null;

        try{
            // 1. 드라이버 로딩
            // 드라이버 인터페이스를 구현한 클래스를 로딩
            // mysql, oracle 등 각 벤더사 마다 클래스 이름이 다르다.
            // mysql은 "com.mysql.jdbc.Driver"이며, 이는 외우는 것이 아니라 구글링하면 된다.
            // 참고로 이전에 연동했던 jar 파일을 보면 com.mysql.jdbc 패키지에 Driver 라는 클래스가 있다.
        	Class.forName("com.mysql.cj.jdbc.Driver"); //com.mysql.jdbc.Driver

            // 2. 연결하기
            // 드라이버 매니저에게 Connection 객체를 달라고 요청한다.
            // Connection을 얻기 위해 필요한 url 역시, 벤더사마다 다르다.
            // mysql은 "jdbc:mysql://localhost/사용할db이름" 이다.
        	String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
        	//jdbc:mysql://localhost:3306/databasename?useSSL=false"


            // @param  getConnection(url, userName, password);
            // @return Connection
        	conn = DriverManager.getConnection(url, "file_user", "edgecomputing"); // DB_URL, USER_NAME, PASSWARD
        	state = conn.createStatement();
        	System.out.println("연결 성공");
        	
        	String sql; //SQL문을 저장할 String
/*      	
        	// 추가하려는 데이터의 값은 전달된 인자를 통해 동적으로 할당되는 값이다.
        	// sql 쿼리 내에서 + 연산자로 한 줄로 작성 가능하다.
        	sql = "insert into file_management (file_name, uuid, security, sharing, location) values(?, ?, ?, ?, ?);";
        	pstate = conn.prepareStatement(sql);
        	pstate.setString(1, "6.txt");
        	pstate.setString(2, "75d7e099-3520-4e92-b44a-9de82dacbd0d");
        	pstate.setInt(3, 1);
        	pstate.setInt(4, 0);
        	pstate.setString(5, "/home/eunae/keti/");
        	// INSERT는 반환되는 데이터들이 없으므로 ResultSet 객체가 필요 없고, 바로 pstmt.executeUpdate()메서드를 호출
        	// INSERT, UPDATE, DELETE 쿼리는 같은 메서드를 호출
        	// @return     int - 몇 개의 row가 영향을 미쳤는지를 반환
        	int count = pstate.executeUpdate();
        	if( count == 0 ){
                System.out.println("데이터 입력 실패");
            }
        	else{
                System.out.println("데이터 입력 성공");
            }
*/
            // 주의사항
        	// 1) JDBC에서 쿼리를 작성할 때는 세미콜론(;)을 빼고 작성한다.
        	// 2) SELECT 할 때 * 으로 모든 칼럼을 가져오는 것보다 가져와야 할 칼럼을 직접 명시해주는 것이 좋다.
            // 3) 원하는 결과는 쿼리로써 마무리 짓고, java 코드로 후작업 하는 것은 권하지 않음
        	sql = "SELECT * FROM file_management"; //(file_name, uuid, security, sharing, location)
        	ResultSet rs = state.executeQuery(sql); //SQL문을 전달하여 실행
        	while(rs.next()){
        		// 레코드의 칼럼은 배열과 달리 0부터 시작하지 않고 1부터 시작한다.
                // 데이터베이스에서 가져오는 데이터의 타입에 맞게 getString 또는 getInt 등을 호출한다.
        		String file_name = rs.getString("file_name");
        		String uuid = rs.getString("uuid");
        		int security = rs.getInt("security");
        		int sharing = rs.getInt("sharing");
        		String location = rs.getString("location");
        		System.out.print("Name: "+ file_name + "\nUUID: " + uuid + "\nSecurity: " + security); 
        		System.out.println("\nSharing: "+ sharing + "\nLocation: " + location + "\n-------------\n");
        	}
        		
        	rs.close();        	

       } catch(ClassNotFoundException e){
    	   System.out.println("드라이버 로딩 실패");
       } catch(SQLException e){
        	System.out.println("에러: " + e);
        }
       finally{
    	   try{
    		   if( conn != null && !conn.isClosed()){
    			   conn.close();
    		   }
    		   if( state != null && !state.isClosed()){
    			   state.close();
    		   }
    	   }
    	   catch( SQLException e){
    		   e.printStackTrace();
    	   }
       }
    }
}