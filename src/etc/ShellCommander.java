package etc;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellCommander {
  public static void main(String[] args) throws Exception {
                String command = "gssdp-discover -t \"upnp:edgedevice\" --timeout 30 | tail -n 1|awk '{print $2}'"; //"ls -al";  
               // 이 부분에 실행할 리눅스 Shell 명령어를 입력하면 된다. (여기선 ls -al 명령어 입력)
               shellCmd(command);
   }
  
   public static void shellCmd(String command) throws Exception {
               Runtime runtime = Runtime.getRuntime();
               Process process = runtime.exec(command);
               InputStream is = process.getInputStream();
               InputStreamReader isr = new InputStreamReader(is);
               BufferedReader br = new BufferedReader(isr);
               String line;
               while((line = br.readLine()) != null) {
                              System.out.println(line);
               }
   }
}
